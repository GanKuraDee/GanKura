package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.util.SkyblockItemId;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.List;
import java.util.Set;

/**
 * Etherwarp で飛べる先を割り出す。
 *
 * Hypixel は視線の先のブロックを探し、その上に立てるなら跳ばしてくれる。
 * 探し方はバニラの当たり判定とは違い、
 * 当たり判定を持たないブロックに加えて、頭や旗竿のような
 * 「当たり判定はあるが Etherwarp は素通りする」ブロックがある。
 * 逆に看板や幟は、薄くても遮る扱いになる
 */
public final class EtherwarpHandler {

    // 融合済みの印と、伸ばした距離。どちらもアイテムに書き込まれている
    private static final String MERGE_KEY = "ethermerge";
    private static final String RANGE_KEY = "tuned_transmission";

    // 素の飛距離と、Tuned Transmission 1段ごとの伸び
    private static final int BASE_RANGE = 57;
    private static final int RANGE_PER_TUNE = 1;

    // 遠すぎる先も示したいので、探すのは飛距離より十分に先まで
    private static final double SEARCH_DISTANCE = 160.0;

    /** 飛べるかどうかと、その理由 */
    public enum Result {
        /** 飛べる */
        OK("§a§lETHERWARP"),
        /** 上が塞がっていて立てない */
        BLOCKED("§c§lBLOCKED"),
        /** 飛距離の外 */
        TOO_FAR("§e§lTOO FAR"),
        /** 触れるブロックに向いていて、右クリックが取られる */
        INTERACT("§6§lINTERACT");

        private final String label;

        Result(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /** 割り出した行き先 */
    public record Target(BlockPos pos, Result result) {
    }

    /**
     * 当たり判定はあるが Etherwarp は素通りするブロック。
     * 頭や花鉢のような、置いてあっても立てる薄いもの
     */
    private static final Set<Block> PASSABLE_BLOCKS = Set.of(
            Blocks.CREEPER_HEAD, Blocks.CREEPER_WALL_HEAD,
            Blocks.DRAGON_HEAD, Blocks.DRAGON_WALL_HEAD,
            Blocks.SKELETON_SKULL, Blocks.SKELETON_WALL_SKULL,
            Blocks.WITHER_SKELETON_SKULL, Blocks.WITHER_SKELETON_WALL_SKULL,
            Blocks.PIGLIN_HEAD, Blocks.PIGLIN_WALL_HEAD,
            Blocks.ZOMBIE_HEAD, Blocks.ZOMBIE_WALL_HEAD,
            Blocks.PLAYER_HEAD, Blocks.PLAYER_WALL_HEAD,
            Blocks.REPEATER, Blocks.COMPARATOR,
            Blocks.BIG_DRIPLEAF_STEM, Blocks.MOSS_CARPET, Blocks.PALE_MOSS_CARPET,
            Blocks.COCOA, Blocks.LADDER, Blocks.SEA_PICKLE);

    private static final List<TagKey<Block>> PASSABLE_TAGS =
            List.of(BlockTags.FLOWER_POTS, BlockTags.WOOL_CARPETS);

    // 薄くても遮る扱いになるもの。立て札や幟は当たり判定を持っている
    private static final List<TagKey<Block>> SOLID_TAGS =
            List.of(BlockTags.ALL_SIGNS, BlockTags.ALL_HANGING_SIGNS, BlockTags.BANNERS);

    // 向いていると右クリックが取られてしまうブロック
    private static final Set<Block> INTERACTABLE_BLOCKS = Set.of(
            Blocks.HOPPER, Blocks.CHEST, Blocks.ENDER_CHEST, Blocks.TRAPPED_CHEST,
            Blocks.FURNACE, Blocks.CRAFTING_TABLE, Blocks.ENCHANTING_TABLE,
            Blocks.CAULDRON, Blocks.WATER_CAULDRON,
            Blocks.DISPENSER, Blocks.DROPPER, Blocks.BREWING_STAND, Blocks.LEVER);

    private static final List<TagKey<Block>> INTERACTABLE_TAGS =
            List.of(BlockTags.DOORS, BlockTags.TRAPDOORS, BlockTags.ANVIL, BlockTags.FENCE_GATES);

    private EtherwarpHandler() {
    }

    /** いま示す行き先。示さないときは null */
    public static Target target(Minecraft client) {
        ModConfig.MiscCategory config = ModConfig.INSTANCE.misc;
        if (!config.showEtherwarpTarget) return null;
        if (!GameState.Server.isSkyblock()) return null;

        Player player = client.player;
        if (player == null || client.level == null) return null;
        if (config.etherwarpOnlySneaking && !player.isShiftKeyDown()) return null;

        ItemStack held = player.getMainHandItem();
        if (!SkyblockItemId.hasAttribute(held, MERGE_KEY)) return null;

        Level level = client.level;
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0f).scale(SEARCH_DISTANCE));

        Hit hit = raycast(level, start, end);
        if (hit == null) return null;

        return new Target(hit.pos(), verdict(client, level, start, hit, held));
    }

    /** その行き先へ飛べるか。飛べない理由まで返す */
    private static Result verdict(Minecraft client, Level level, Vec3 start, Hit hit, ItemStack held) {
        // 立つのは的の上。頭2つ分が空いていないと弾かれる
        if (!isPassable(level, hit.pos().above())) return Result.BLOCKED;
        if (!isPassable(level, hit.pos().above(2))) return Result.BLOCKED;

        Vec3 landing = hit.exact() == null ? Vec3.atCenterOf(hit.pos()) : hit.exact();
        int range = BASE_RANGE + RANGE_PER_TUNE * SkyblockItemId.attribute(held, RANGE_KEY);
        if (start.distanceToSqr(landing) > (double) range * range) return Result.TOO_FAR;

        if (aimingAtInteractable(client, level)) return Result.INTERACT;
        return Result.OK;
    }

    /**
     * 手の届くところに触れるブロックがあると、右クリックがそちらに取られる。
     * 判定はバニラの照準に任せる。届く距離もそちらが見てくれる
     */
    private static boolean aimingAtInteractable(Minecraft client, Level level) {
        if (!(client.hitResult instanceof BlockHitResult block)) return false;
        if (block.getType() != HitResult.Type.BLOCK) return false;

        BlockState state = level.getBlockState(block.getBlockPos());
        return INTERACTABLE_BLOCKS.contains(state.getBlock())
                || tagged(state.typeHolder(), INTERACTABLE_TAGS);
    }

    // -------------------------------------------------- 視線の先探し

    private record Hit(BlockPos pos, Vec3 exact) {
    }

    /**
     * 視線の先で最初に遮るブロック。
     *
     * バニラの当たり判定とは通る範囲が違うので、辿り方だけ借りて自前で判じる
     */
    private static Hit raycast(Level level, Vec3 start, Vec3 end) {
        return BlockGetter.traverseBlocks(start, end, level,
                (world, pos) -> {
                    if (isPassable(world, pos)) return null;

                    // 面の当たった場所まで分かると、飛距離の判定を細かくできる
                    BlockHitResult exact = world.clipWithInteractionOverride(start, end, pos,
                            Shapes.block(), world.getBlockState(pos).getBlock().defaultBlockState());
                    return new Hit(pos.immutable(), exact == null ? null : exact.getLocation());
                },
                world -> null);
    }

    /** Etherwarp が素通りするブロックか */
    private static boolean isPassable(BlockGetter world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Holder<Block> block = state.typeHolder();

        // 薄くても遮るものが先。当たり判定の有無では分けられない
        if (tagged(block, SOLID_TAGS)) return false;
        // 置き方で当たり判定が変わるものは、置いていない状態で見る。Hypixel もそうしている
        if (state.getBlock().defaultBlockState().getCollisionShape(world, pos).isEmpty()) return true;

        return PASSABLE_BLOCKS.contains(state.getBlock()) || tagged(block, PASSABLE_TAGS);
    }

    private static boolean tagged(Holder<Block> block, List<TagKey<Block>> tags) {
        for (TagKey<Block> tag : tags) {
            if (block.is(tag)) return true;
        }
        return false;
    }
}

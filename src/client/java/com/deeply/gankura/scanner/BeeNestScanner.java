package com.deeply.gankura.scanner;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.render.EntityHighlightManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Critter Safari の Forest Biome にあるミツバチの巣を探す。
 *
 * Forest Biome には養蜂箱(beehive)も置かれているが、Honeybug が付くのは
 * ミツバチの巣(bee_nest)だけなので、そちらだけを拾う。
 *
 * ブロックエンティティ(BeehiveBlockEntity)の一覧から拾うほうが手軽だが、
 * サーバーが同期しなかった巣はクライアント側にブロックエンティティが作られず取りこぼす。
 * ブロックそのものは必ず届くので、チャンクの中身を直接見る。
 * 区画(ChunkSection)ごとに「そもそも巣が含まれているか」を先に判定できるため、
 * 含まない区画は 4096 マスを見に行かずに飛ばせる。
 */
public class BeeNestScanner {

    // 走査の間隔。巣は動かないので、毎tick探し直す意味はない
    private static final int SCAN_INTERVAL_TICKS = 20;
    // プレイヤーを中心に見に行くチャンクの半径(8チャンク = 128ブロック)
    private static final int CHUNK_RADIUS = 8;
    // 1つの区画の一辺
    private static final int SECTION_SIZE = 16;

    private static final Predicate<BlockState> IS_BEE_NEST = state -> state.getBlock() == Blocks.BEE_NEST;

    // 描画スレッドからも読むため、丸ごと差し替える形で更新する
    private static volatile Set<BlockPos> nests = Set.of();
    // 手を出した巣。巣のブロックは採ってもその場に残るので、消えたかどうかでは判定できない。
    // エリアを出入りするまで(reset() が呼ばれるまで)覚えておき、その間は目印を出さない
    private static final Set<BlockPos> dismissed = ConcurrentHashMap.newKeySet();
    private static int tickCounter = 0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(BeeNestScanner::scan);

        // Honeybug を湧かせた巣はもう使えないので、目印を降ろす。
        // 左クリック・右クリックのどちらでも湧くため、両方を拾う
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            dismiss(pos);
            return InteractionResult.PASS;
        });
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            dismiss(hitResult.getBlockPos());
            return InteractionResult.PASS;
        });
    }

    public static void reset() {
        nests = Set.of();
        dismissed.clear();
        tickCounter = 0;
    }

    // 目印を降ろし、次の走査で拾い直さないよう覚えておく
    private static void dismiss(BlockPos pos) {
        if (!nests.contains(pos)) return;

        BlockPos immutable = pos.immutable();
        dismissed.add(immutable);

        // 次の走査を待たずに消えるよう、その場でも外しておく
        Set<BlockPos> remaining = new HashSet<>(nests);
        remaining.remove(immutable);
        nests = Set.copyOf(remaining);
    }

    // 設定と現在のエリアの前提。どのバイオームにいるかは見ない
    private static boolean isEnabled() {
        return ModConfig.INSTANCE.foraging.enableBeeNestWaypoints && GameState.Server.isSafari();
    }

    // 目印を出してよい状況か。
    // Critter Safari の Mob Visuals と同じく、自分が Forest Biome にいるときだけ出す
    public static boolean isActive() {
        if (!isEnabled()) return false;

        Minecraft client = Minecraft.getInstance();
        return client.player != null && EntityHighlightManager.inSafariForest(client.player.blockPosition());
    }

    // 描画側から参照する、目印を出す位置
    public static Set<BlockPos> positions() {
        return nests;
    }

    private static void scan(Minecraft client) {
        if (!isEnabled() || client.level == null || client.player == null) {
            // エリアを出たら、湧かせ済みの記憶ごと捨てる
            if (!nests.isEmpty() || !dismissed.isEmpty()) reset();
            return;
        }

        // Forest Biome から出ている間は目印を出さない。
        // ただし湧かせ済みの記憶は、戻ってきたときのために残しておく
        if (!EntityHighlightManager.inSafariForest(client.player.blockPosition())) {
            if (!nests.isEmpty()) nests = Set.of();
            return;
        }

        if (++tickCounter < SCAN_INTERVAL_TICKS) return;
        tickCounter = 0;

        Set<BlockPos> found = new HashSet<>();
        ChunkPos center = client.player.chunkPosition();
        int centerX = center.x();
        int centerZ = center.z();

        for (int cx = centerX - CHUNK_RADIUS; cx <= centerX + CHUNK_RADIUS; cx++) {
            for (int cz = centerZ - CHUNK_RADIUS; cz <= centerZ + CHUNK_RADIUS; cz++) {
                // 未読み込みのチャンクは null が返る(load=false なので読み込みは起こさない)
                LevelChunk chunk = client.level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk != null) collectFromChunk(chunk, cx << 4, cz << 4, found);
            }
        }

        nests = Set.copyOf(found);
    }

    private static void collectFromChunk(LevelChunk chunk, int originX, int originZ, Set<BlockPos> found) {
        LevelChunkSection[] sections = chunk.getSections();
        int minY = chunk.getMinY();

        for (int index = 0; index < sections.length; index++) {
            LevelChunkSection section = sections[index];
            // 巣を1つも含まない区画は、ここで丸ごと飛ばせる
            if (section.hasOnlyAir() || !section.maybeHas(IS_BEE_NEST)) continue;

            int baseY = minY + (index * SECTION_SIZE);
            for (int y = 0; y < SECTION_SIZE; y++) {
                for (int x = 0; x < SECTION_SIZE; x++) {
                    for (int z = 0; z < SECTION_SIZE; z++) {
                        if (section.getBlockState(x, y, z).getBlock() != Blocks.BEE_NEST) continue;

                        BlockPos pos = new BlockPos(originX + x, baseY + y, originZ + z);
                        // Forest Biome にある、まだ手を出していないものだけを拾う
                        if (!EntityHighlightManager.inSafariForest(pos)) continue;
                        if (dismissed.contains(pos)) continue;

                        found.add(pos);
                    }
                }
            }
        }
    }
}

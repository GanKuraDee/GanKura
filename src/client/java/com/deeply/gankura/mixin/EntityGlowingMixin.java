package com.deeply.gankura.mixin;

import com.deeply.gankura.data.CrimsonBossEntry;
import com.deeply.gankura.data.GameState;
import com.deeply.gankura.render.EntityHighlightManager;
import net.minecraft.world.entity.Entity;

import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityGlowingMixin {

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void forceBossGlowing(CallbackInfoReturnable<Boolean> cir) {
        if (EntityHighlightManager.highlightedEntities.contains((Entity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * バニラは「当たり判定の平均辺長 × 64 × 描画距離倍率」より遠いエンティティを描画しない。
     * Glow はエンティティが描画されて初めて輪郭が出るため、この距離を超えると
     * Tracer だけが表示されて Glow が消える、という食い違いが起きる。
     * 追跡中のボスに限り距離によるカリングを無効化し、両者の見え方を揃える。
     * (フラスタム外のカリングは別処理なので、画面外のエンティティは従来どおり描画されない)
     */
    @Inject(method = "shouldRenderAtSqrDistance(D)Z", at = @At("HEAD"), cancellable = true)
    private void forceBossRenderDistance(double distanceSqr, CallbackInfoReturnable<Boolean> cir) {
        if (EntityHighlightManager.highlightedEntities.contains((Entity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void overrideGlowingColor(CallbackInfoReturnable<Integer> cir) {
        Entity entity = (Entity) (Object) this;

        if (EntityHighlightManager.highlightedEntities.contains(entity)) {
            // Shulker: エリアと体色で色が変わるため、走査時に決めた色をそのまま使う
            Integer shulkerColor = EntityHighlightManager.shulkerColors.get(entity);
            if (shulkerColor != null) {
                cir.setReturnValue(shulkerColor);
                return;
            }

            // Crimson Isle ボス（Wither Skeleton 等を含む）
            CrimsonBossEntry boss = EntityHighlightManager.crimsonBossEntities.get(entity);
            if (boss != null) {
                cir.setReturnValue(boss.glowColorRGB());
                return;
            }

            // Magma Glare (Magma Boss 配下の MagmaCube): 赤
            if (EntityHighlightManager.magmaGlareEntities.contains(entity)) {
                cir.setReturnValue(0xFF5555);
                return;
            }

            // Arachne (Broodmother と同じ Spider 型のため instanceof 判定より先に判定する): 紫
            if (EntityHighlightManager.arachneEntities.contains(entity)) {
                cir.setReturnValue(0xAA00AA);
                return;
            }

            // Arachne's Brood (Arachne 分裂後の Spider/CaveSpider 系のため instanceof 判定より先に判定する): 明るい紫
            if (EntityHighlightManager.arachneBroodEntities.contains(entity)) {
                cir.setReturnValue(0xFF55FF);
                return;
            }

            if (entity instanceof IronGolem) {
                cir.setReturnValue(0xFFAA00); // ゴーレム: 金色
            } else if (entity instanceof Spider) {
                cir.setReturnValue(0xFF5555); // ブルードマザー: 赤色
            } else if (entity instanceof EnderDragon) {
                cir.setReturnValue(dragonColor(GameState.Dragon.type));
            } else if (entity instanceof Ravager) {
                cir.setReturnValue(0x55FFFF); // Wumpa: 水色
            } else if (entity instanceof Warden) {
                cir.setReturnValue(0xAA00AA); // Doomspiral: 紫
            } else {
                cir.setReturnValue(0xFFFFFF);
            }
        }
    }

    private static int dragonColor(String type) {
        if (type == null) return 0xFF55FF;
        return switch (type) {
            case "Protector" -> 0x555555; // §8 DARK_GRAY
            case "Old"       -> 0xAAAAAA; // §7 GRAY
            case "Unstable"  -> 0xAA00AA; // §5 DARK_PURPLE
            case "Young"     -> 0xFFFFFF; // §f WHITE
            case "Strong"    -> 0xFF5555; // §c RED
            case "Wise"      -> 0x55FFFF; // §b AQUA
            case "Superior"  -> 0xFFFF55; // §e YELLOW
            default          -> 0xFF55FF; // §d LIGHT_PURPLE
        };
    }
}

package com.deeply.gankura.mixin.client;

import com.deeply.gankura.data.CrimsonBossEntry;
import com.deeply.gankura.data.GameState;
import com.deeply.gankura.render.EntityHighlightManager;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityGlowingMixin {

    // 1. 強制的に発光(Glowing)をONにする
    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
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
    @Inject(method = "shouldRender(D)Z", at = @At("HEAD"), cancellable = true)
    private void forceBossRenderDistance(double distanceSqr, CallbackInfoReturnable<Boolean> cir) {
        if (EntityHighlightManager.highlightedEntities.contains((Entity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }

    // =======================================================
    // ★追加: 発光色（アウトラインの色）を強制的に上書きする
    // マイクラが色を取得しようとした瞬間に割り込み、好きな色を渡します。
    // =======================================================
    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void overrideGlowingColor(CallbackInfoReturnable<Integer> cir) {
        Entity entity = (Entity) (Object) this;

        // 私たちのハイライトリストに入っているエンティティなら...
        if (EntityHighlightManager.highlightedEntities.contains(entity)) {

            // 同じ型でもエリア・体色・変種で色が変わるモブ(Shulker やエリア固有モブ)は、
            // 走査時に決めた色をそのまま使う
            Integer customColor = EntityHighlightManager.customGlowColors.get(entity);
            if (customColor != null) {
                cir.setReturnValue(customColor);
                return;
            }

            // --- ボスごとに色を変更 ---
            if (entity instanceof IronGolemEntity) {
                // ゴーレム: 金色
                cir.setReturnValue(0xFFAA00);
            }
            else if (EntityHighlightManager.arachneEntities.contains(entity)) {
                // Arachne は Broodmother と同じ SpiderEntity 型のため、instanceof 判定より先に判定する: 紫
                cir.setReturnValue(0xAA00AA);
            }
            else if (EntityHighlightManager.arachneBroodEntities.contains(entity)) {
                // Arachne's Brood も同じ SpiderEntity(CaveSpiderEntity) 系のため、instanceof 判定より先に判定する: 明るい紫
                cir.setReturnValue(0xFF55FF);
            }
            else if (entity instanceof SpiderEntity) {
                // ブルードマザー: 赤色
                cir.setReturnValue(0xFF5555);
            }
            else if (entity instanceof EnderDragonEntity) {
                cir.setReturnValue(dragonColor(GameState.Dragon.type));
            }
            else if (entity instanceof RavagerEntity) {
                // Wumpa: 水色
                cir.setReturnValue(0x55FFFF);
            }
            else if (entity instanceof WardenEntity) {
                // Doomspiral: 紫
                cir.setReturnValue(0xAA00AA);
            }
            else if (entity instanceof MagmaCubeEntity
                    && EntityHighlightManager.magmaGlareEntities.contains(entity)) {
                // Magma Glare: 赤
                cir.setReturnValue(0xFF5555);
            }
            else {
                CrimsonBossEntry boss = EntityHighlightManager.crimsonBossEntities.get(entity);
                cir.setReturnValue(boss != null ? boss.glowColorRGB() : 0xFFFFFF);
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
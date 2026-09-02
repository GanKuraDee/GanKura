package com.deeply.gankura.mixin;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.render.TabListRenderer;
import com.deeply.gankura.util.TabListCleaner;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Tab の一覧を読みやすくする。
 *
 * Hypixel は上下に宣伝文を出し、各行に接続状態の目盛りと顔を並べる。
 * SkyBlock では一覧に情報を詰め込んでいるので、これらが邪魔になりやすい。
 *
 * 列ごとに幅を変える設定を入れているときは、配置の計算ごと
 * {@link TabListRenderer} に任せる
 */
@Mixin(PlayerTabOverlay.class)
public class TabListMixin {

    @Shadow
    private Component header;
    @Shadow
    private Component footer;

    @Shadow
    private List<PlayerInfo> getPlayerInfos() {
        throw new AssertionError();
    }

    @Shadow
    protected void extractPingIcon(GuiGraphicsExtractor graphics, int width, int x, int y, PlayerInfo entry) {
        throw new AssertionError();
    }

    // 宣伝文を隠している間の預かり所。描き終えたら戻す
    @Unique
    private Component gankura$savedHeader;
    @Unique
    private Component gankura$savedFooter;

    // 今どの行を描いているか。顔を出すかどうかの判断に使う
    @Unique
    private PlayerInfo gankura$currentEntry;

    // -------------------------------------------------- 自前で描く場合

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void gankura$renderTabList(GuiGraphicsExtractor graphics, int width, Scoreboard scoreboard,
                                       Objective objective, CallbackInfo ci) {
        // 点数や体力を出す一覧は作りが変わるので、そのときはバニラに任せる
        if (!ModConfig.INSTANCE.interfaceSettings.enableTabListTweaks) return;
        if (!GameState.Server.isSkyblock() || objective != null) return;

        PlayerTabOverlay overlay = (PlayerTabOverlay) (Object) this;
        List<PlayerInfo> entries = TabListCleaner.clean(getPlayerInfos(), overlay);

        if (TabListRenderer.render(graphics, width, overlay, entries, header, footer, this::extractPingIcon)) {
            ci.cancel();
        }
    }

    // -------------------------------------------------- 上下の宣伝文

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void gankura$hideAds(GuiGraphicsExtractor graphics, int width, Scoreboard scoreboard,
                                 Objective objective, CallbackInfo ci) {
        if (!gankura$enabled(ModConfig.INSTANCE.interfaceSettings.hideTabListAds)) return;

        gankura$savedHeader = header;
        gankura$savedFooter = footer;
        // null にしておくと、バニラは宣伝文の行も、その分の幅も取らない
        header = null;
        footer = null;
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void gankura$restoreAds(GuiGraphicsExtractor graphics, int width, Scoreboard scoreboard,
                                    Objective objective, CallbackInfo ci) {
        if (gankura$savedHeader != null) header = gankura$savedHeader;
        if (gankura$savedFooter != null) footer = gankura$savedFooter;

        gankura$savedHeader = null;
        gankura$savedFooter = null;
    }

    // -------------------------------------------------- 空きばかりの列

    /**
     * 並べる行を絞り込む。
     *
     * 数が変わるとバニラの列の組み方も変わるので、
     * 減らし方は {@link TabListCleaner} 側で 20 行単位に揃えている
     */
    @Redirect(method = "extractRenderState",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/PlayerTabOverlay;getPlayerInfos()Ljava/util/List;"))
    private List<PlayerInfo> gankura$cleanEntries(PlayerTabOverlay overlay) {
        return TabListCleaner.clean(getPlayerInfos(), overlay);
    }

    // -------------------------------------------------- 接続状態の目盛り

    @Inject(method = "extractPingIcon", at = @At("HEAD"), cancellable = true)
    private void gankura$hidePingIcon(GuiGraphicsExtractor graphics, int width, int x, int y,
                                      PlayerInfo entry, CallbackInfo ci) {
        if (gankura$enabled(ModConfig.INSTANCE.interfaceSettings.hideTabListPing)) ci.cancel();
    }

    // -------------------------------------------------- 顔

    /**
     * 顔を描く直前に、その行の持ち主を控えておく。
     *
     * 顔を描くのは静的なメソッドで、どの行のものかが引数から分からない。
     * 直前に必ず呼ばれるここで拾っておく
     */
    @Redirect(method = "extractRenderState",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/PlayerInfo;getSkin()Lnet/minecraft/world/entity/player/PlayerSkin;"))
    private PlayerSkin gankura$rememberEntry(PlayerInfo entry) {
        gankura$currentEntry = entry;
        return entry.getSkin();
    }

    @Redirect(method = "extractRenderState",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/PlayerFaceExtractor;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/resources/Identifier;IIIZZI)V"))
    private void gankura$hideExtraHead(GuiGraphicsExtractor graphics, Identifier texture, int x, int y,
                                       int size, boolean hat, boolean upsideDown, int color) {
        if (gankura$enabled(ModConfig.INSTANCE.interfaceSettings.hideTabListHeads)
                && !TabListCleaner.hasHead((PlayerTabOverlay) (Object) this, gankura$currentEntry)) {
            return;
        }
        PlayerFaceExtractor.extractRenderState(graphics, texture, x, y, size, hat, upsideDown, color);
    }

    @Unique
    private boolean gankura$enabled(boolean option) {
        return ModConfig.INSTANCE.interfaceSettings.enableTabListTweaks
                && option && GameState.Server.isSkyblock();
    }
}

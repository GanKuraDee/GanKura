package com.deeply.gankura.mixin;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.gui.SignInputScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Bazaar や Auction の検索を、看板ではなく入力欄で受け取る。
 *
 * Hypixel は文字を打たせたいときに看板の編集画面を開く。
 * 看板は Enter で確定できず、長い名前も入りきらないので、
 * 案内の文面から検索の看板だと分かるものだけ、自前の画面に差し替える
 */
@Mixin(LocalPlayer.class)
public class SignInputMixin {

    // 検索の看板に書かれている案内
    @Unique
    private static final String SEARCH_PROMPT = "Enter query";

    @Inject(method = "openTextEdit", at = @At("HEAD"), cancellable = true)
    private void gankura$openSearchInput(SignBlockEntity sign, SignTextSlot slot, CallbackInfo ci) {
        if (!ModConfig.Interface.enableSearchInputScreen || !GameState.Server.isSkyblock()) return;

        Component prompt = gankura$searchPrompt(sign, slot);
        if (prompt == null) return;

        ci.cancel();
        Minecraft.getInstance().gui.setScreen(
                new SignInputScreen(sign.getBlockPos(), slot, prompt));
    }

    // 検索の看板なら、その案内の行。違う看板なら null
    @Unique
    private Component gankura$searchPrompt(SignBlockEntity sign, SignTextSlot slot) {
        SignText text = sign.getText(slot);

        // 26.3: SignText#getMessage(int, boolean) が廃止され、行のリストを返す getMessages に変わった
        for (Component message : text.getMessages(false)) {
            if (message.getString().contains(SEARCH_PROMPT)) return message;
        }
        return null;
    }
}

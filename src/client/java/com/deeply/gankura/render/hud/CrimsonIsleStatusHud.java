package com.deeply.gankura.render.hud;

import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.GameState;
import com.deeply.gankura.render.EntityHighlightManager;
import com.deeply.gankura.render.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class CrimsonIsleStatusHud extends HudElement {
    public CrimsonIsleStatusHud() {
        super("crimson_isle_status", 305, 10, 1.0f, 185, 76,
                () -> ModConfig.INSTANCE.crimsonIsle.showCrimsonIsleStatusHud,
                () -> GameState.Server.isCrimsonIsle());
    }

    @Override
    public void renderElement(DrawContext context, boolean isPreview) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        drawTextWithShadow(context, tr, "§c§lNether Boss Status", 0, 0, 0xFFFFFFFF);
        int y = 12;
        for (var boss : EntityHighlightManager.CRIMSON_BOSSES) {
            String status;
            // Magma Boss はエリア内にいる間だけサイドバーからフェーズも取れる
            // フェーズ行に加え、念のため「Magma Chamber」の行も確認する
            String magmaPhase = "Magma Boss".equals(boss.nameTag()) && GameState.MagmaBoss.inArena
                    ? GameState.MagmaBoss.spawnStatus
                    : null;
            if (isPreview) {
                status = "§aSpawned";
            } else if (magmaPhase != null) {
                status = "§aSpawned §7(" + magmaPhase + ")";
            } else {
                long respawnEnd = getRespawnEndTime(boss.nameTag());
                long remaining = respawnEnd - System.currentTimeMillis();
                if (remaining > 0) {
                    long secs = remaining / 1000;
                    status = String.format("§eRespawning §f(%dm %02ds)", secs / 60, secs % 60);
                } else if (boss.getIsDetected().get()) {
                    // エリア外でもエンティティ検出で存在は分かる
                    status = "§aSpawned";
                } else if (respawnEnd > 0 && System.currentTimeMillis() - respawnEnd < 10_000L) {
                    status = "§aReady";
                } else {
                    status = absenceStatus(boss.nameTag());
                }
            }
            String nameColor = colorCode(boss.glowColorRGB());
            drawTextWithShadow(context, tr, nameColor + boss.nameTag() + "§f: " + status, 0, y, 0xFFFFFFFF);
            y += 12;
        }
    }

    // スポーン地点のチャンクが読み込まれていて検出できない場合のみ「いない」と確定できる。
    // 遠い場合はエンティティが送られてこないだけの可能性があるためUnknownのままにする
    private static String absenceStatus(String bossName) {
        // 撃破メッセージを見ていないためリスポーン時刻が不明。
        // 「いないと確認できた時点」からリスポーン間隔を数えた推定値を出す。
        // このタイマーは範囲外に出ても継続する
        long remaining = EntityHighlightManager.killedRemainingMs(bossName);
        if (remaining > 0) {
            long secs = remaining / 1000;
            return String.format("§cKilled §f(%dm %02ds)", secs / 60, secs % 60);
        }
        // 範囲内で未検出が続いているならまだいない
        if (EntityHighlightManager.canConfirmAbsence(bossName)) return "§cKilled";
        // 範囲外。居たことがある / リスポーン時間を過ぎた のいずれも生死は判別できない
        return EntityHighlightManager.wasSpawnedWhenLastConfirmed(bossName)
                || EntityHighlightManager.wasKilledConfirmed(bossName)
                ? "§6Spawned/Killed"
                : "§7Unknown";
    }

    private static long getRespawnEndTime(String nameTag) {
        return switch (nameTag) {
            case "Barbarian Duke X" -> GameState.BarbarianDukeX.respawnEndTime;
            case "Bladesoul"        -> GameState.Bladesoul.respawnEndTime;
            case "Mage Outlaw"      -> GameState.MageOutlaw.respawnEndTime;
            case "Ashfang"          -> GameState.Ashfang.respawnEndTime;
            case "Magma Boss"       -> GameState.MagmaBoss.respawnEndTime;
            default                 -> 0L;
        };
    }

    private static String colorCode(int rgb) {
        return switch (rgb) {
            case 0xFF5555 -> "§c"; // Barbarian Duke X
            case 0x555555 -> "§8"; // Bladesoul
            case 0xAA00AA -> "§5"; // Mage Outlaw
            case 0xAAAAAA -> "§7"; // Ashfang
            case 0xFFAA00 -> "§6"; // Magma Boss
            default -> "§f";
        };
    }
}

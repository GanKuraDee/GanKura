package com.deeply.gankura.scanner;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Team;

// スコアボードに直接表示される「Arachne's Sanctuary」の行を検知し、そのエリアにいるかどうかを判定する
public class AreaScanner {

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(AreaScanner::scan);
    }

    private static void scan(MinecraftClient client) {
        if (client.world == null || client.player == null) return;
        if (!ModConstants.MAP_SPIDERS_DEN.equals(GameState.Server.map)) return;

        boolean found = false;
        for (Team team : client.world.getScoreboard().getTeams()) {
            String line = (team.getPrefix().getString() + team.getSuffix().getString())
                    .replaceAll("§[0-9a-fk-or]", "");
            if (ModConstants.containsIgnoreCase(line, ModConstants.AREA_ARACHNES_SANCTUARY)) {
                found = true;
                break;
            }
        }
        // Sanctuaryへの再入場(外→内への切り替わり)を検知したら、DOWN!等のチャットを見逃していても
        // 古いカウントダウン/セリフ状態を引きずらないようReadyへ強制リセットする。
        // 実際にArachneが健在なら、蜘蛛の巣検知が同tick中に改めてSpawnedへ確定させる
        if (found && !GameState.Arachne.inSanctuary) {
            GameState.Arachne.isSummoning = false;
            GameState.Arachne.size = null;
            GameState.Arachne.awaitingCrystalParticles = false;
            GameState.Arachne.arachneMessageSeen = false;
        }

        GameState.Arachne.inSanctuary = found;
    }
}

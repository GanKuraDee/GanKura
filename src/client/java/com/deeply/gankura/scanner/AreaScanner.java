package com.deeply.gankura.scanner;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.PlayerTeam;

// スコアボードに直接表示される「Arachne's Sanctuary」の行を検知し、そのエリアにいるかどうかを判定する
public class AreaScanner {

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(AreaScanner::scan);
    }

    private static void scan(Minecraft client) {
        if (client.level == null || client.player == null) return;
        if (!ModConstants.MAP_SPIDERS_DEN.equals(GameState.Server.map)) return;

        boolean found = false;
        for (PlayerTeam team : client.level.getScoreboard().getPlayerTeams()) {
            String line = (team.getPlayerPrefix().getString() + team.getPlayerSuffix().getString())
                    .replaceAll("§[0-9a-fk-or]", "");
            if (ModConstants.containsIgnoreCase(line, ModConstants.AREA_ARACHNES_SANCTUARY)) {
                found = true;
                break;
            }
        }
        // Sanctuaryへの再入場(外→内への切り替わり)を検知したら、DOWN!等のチャットを見逃していても
        // 古いカウントダウン/セリフ状態を引きずらないようReadyへ強制リセットする。
        // 実際にArachneが健在なら、蜘蛛の巣検知が同tick中に改めてSpawnedへ確定させる
        // ただし、カウントダウンが作動中(isSummoning)の場合はそのタイマーが正当なものなので、
        // Sanctuary外へ移動して戻ってきただけでReadyに巻き戻さないようにする
        if (found && !GameState.Arachne.inSanctuary && !GameState.Arachne.isSummoning) {
            GameState.Arachne.size = null;
            GameState.Arachne.awaitingCrystalParticles = false;
            GameState.Arachne.arachneMessageSeen = false;
        }

        GameState.Arachne.inSanctuary = found;
    }
}

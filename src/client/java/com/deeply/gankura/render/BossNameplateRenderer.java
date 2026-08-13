package com.deeply.gankura.render;

import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

// EntityHighlightManagerが検出したボスの視覚エンティティに追従する形で、名前とHPを2行で表示する。
// 表示位置はモブの当たり判定の中心(足元 + 高さの半分)をスクリーン座標へ投影したもので、
// バニラのネームタグ(頭上)とは重ならない。
//
// ワールド内テキスト(TextGizmo)ではなくHUDとして描画している理由:
// Gizmoはワールド描画パスの中で処理されるため、Glow(エンティティ輪郭)のポストエフェクトに
// 上書きされてしまう(26.2ではGizmoがmainパス内でsubmitされるため特に顕著)。
// HUDはワールド描画とポストエフェクトがすべて終わった後に描かれるので、確実に最前面になる。
//
// 表示対象と文字列の組み立ては tick 側(EntityHighlightManager)で行い、ここでは毎フレームの描画のみを担当する。
public class BossNameplateRenderer {
    // 行の区切り
    private static final String LINE_SEPARATOR = "\n";
    // ネームプレートの基準GUIスケール。実際のGUIスケールでこの値を割った倍率で描画することで、
    // GUIスケール設定を変えても画面上での大きさが変わらないようにする。
    // 設定のサイズ(既定1.0)はこの見え方に対する倍率として掛ける
    private static final double REFERENCE_GUI_SCALE = 4.0;
    // projectPointToScreen の戻り値は正規化デバイス座標。z > 1 はカメラの後方を意味する
    private static final double NDC_BEHIND_CAMERA_Z = 1.0;

    public static void render(GuiGraphicsExtractor graphics, Minecraft client, float partialTicks) {
        if (EntityHighlightManager.nameplateEntities.isEmpty()) return;

        Font font = client.font;
        Camera camera = client.gameRenderer.mainCamera();
        Vec3 cameraPos = camera.position();
        float screenWidth = client.getWindow().getGuiScaledWidth();
        float screenHeight = client.getWindow().getGuiScaledHeight();
        // GUIスケールの影響を打ち消し、設定を変えても常に同じ大きさで表示する
        float textScale = (float) (REFERENCE_GUI_SCALE / client.getWindow().getGuiScale())
                * ModConfig.INSTANCE.mobVisuals.nameplateScale;

        for (Map.Entry<Entity, String> entry : EntityHighlightManager.nameplateEntities.entrySet()) {
            Entity entity = entry.getKey();
            if (entity.isRemoved()) continue;

            // 補間済み座標を使い、tick間でも滑らかにモブへ追従させる
            Vec3 center = entity.getPosition(partialTicks).add(0, entity.getBbHeight() / 2.0, 0);
            // カメラと同じ位置だと投影が破綻するため除外する
            if (cameraPos.distanceToSqr(center) < 1.0E-4) continue;

            Vec3 ndc = client.gameRenderer.projectPointToScreen(center);
            // カメラの後方にある対象は、投影結果が反転して画面内に現れてしまうので描画しない
            if (ndc.z > NDC_BEHIND_CAMERA_Z) continue;

            float centerX = (float) (ndc.x * 0.5 + 0.5) * screenWidth;
            float centerY = (float) (0.5 - ndc.y * 0.5) * screenHeight;

            String[] lines = entry.getValue().split(LINE_SEPARATOR);

            graphics.pose().pushMatrix();
            // 先にモブの中心へ移動してから縮小する。順序が逆だと表示位置までスケールされてずれる
            graphics.pose().translate(centerX, centerY);
            graphics.pose().scale(textScale, textScale);
            // 以降はスケール後の座標系。2行分の高さの中心が原点に来るように上へずらす
            int topY = -lines.length * font.lineHeight / 2;
            for (int i = 0; i < lines.length; i++) {
                graphics.text(font, lines[i], -font.width(lines[i]) / 2, topY + i * font.lineHeight, 0xFFFFFFFF, true);
            }
            graphics.pose().popMatrix();
        }
    }

    // 1行目に名前、2行目にHPを置いた表示文字列を組み立てる。
    // HPが未取得(スキャン直後など)の場合は名前のみを返す
    public static String buildLabel(String coloredName, String rawHealth) {
        String hp = formatHealth(rawHealth);
        return hp.isEmpty() ? coloredName : coloredName + LINE_SEPARATOR + hp;
    }

    // ハイライト色(ARGB)に対応する§カラーコードを求め、名前部分の色として使う
    public static String colorCode(int argb) {
        // MobVisualTarget が持つ色をすべて網羅する。抜けがあると
        // ネームプレートだけ白くなり、Highlight/Tracer と色が食い違う
        return switch (argb & 0xFFFFFF) {
            case 0x555555 -> "§8";
            case 0xAAAAAA -> "§7";
            case 0xFF5555 -> "§c";
            case 0xAA00AA -> "§5";
            case 0xFFAA00 -> "§6";
            case 0x5555FF -> "§9";
            case 0xFF55FF -> "§d";
            case 0x55FFFF -> "§b";
            case 0x55FF55 -> "§a";
            case 0xFFFF55 -> "§e";
            default       -> "§f";
        };
    }

    // HP HUD (CrimsonBossHealthHud) と同じ配色ルールで残量に応じて色分けする
    private static String formatHealth(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        // Magma Boss のみ、サイドバー由来の色コード込み文字列がそのまま入る
        if (raw.startsWith(ModConstants.RAW_HEALTH_PREFIX)) return raw.substring(ModConstants.RAW_HEALTH_PREFIX.length());

        String[] parts = raw.split("/");
        if (parts.length != 2) return "§a" + raw.replace("/", "§f/§a");

        double current = parseHealthValue(parts[0]);
        double max = parseHealthValue(parts[1]);
        String color = "§a";
        if (current >= 0 && max > 0) {
            if (current < max * 0.2) color = "§c";
            else if (current < max * 0.5) color = "§e";
        }
        return color + parts[0] + "§f/§a" + parts[1];
    }

    // 「1.2M」「850k」のような省略表記を数値に戻す
    private static double parseHealthValue(String s) {
        try {
            s = s.trim().replace(",", "");
            if (s.isEmpty()) return 0;
            double mult = 1.0;
            char last = s.charAt(s.length() - 1);
            if (last == 'M' || last == 'm') { mult = 1_000_000.0; s = s.substring(0, s.length() - 1); }
            else if (last == 'k' || last == 'K') { mult = 1_000.0; s = s.substring(0, s.length() - 1); }
            return Double.parseDouble(s) * mult;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import com.deeply.gankura.data.ModConstants;
import com.deeply.gankura.util.NotificationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;

public class BroodmotherHandler {
    // タブリストが揃うまでの行数の目安。読み込み途中で「ウィジェットが無い」と誤判定しないための待ち
    private static final int MIN_LOADED_LINES = 20;
    // Stage 4 から湧くまでの時間 (サーバー Tick)。ほかのカウントダウンと同じく TPS を考慮して数える
    public static final int STAGE_4_TICKS = 600;
    // Stage 4 より前の段階。ここから Stage 4 に変わるのを見ていれば、湧くまでを数えられる
    private static final Set<String> BEFORE_STAGE_4 = Set.of("Slain", "Dormant", "Soon", "Awakening");


    public static void processTabList(List<String> lines) {
        for (String line : lines) {
            Matcher bmMatcher = ModConstants.BROODMOTHER_PATTERN.matcher(line);
            if (bmMatcher.find()) {
                GameState.Broodmother.widgetMissing = false;
                String bmStageName = bmMatcher.group(1).trim();
                updateBroodmotherStage(bmStageName);
                return;
            }
        }
        // タブリストが揃っているのに行が無い。HUD で出し方を案内する
        if (lines.size() >= MIN_LOADED_LINES) GameState.Broodmother.widgetMissing = true;
    }

    private static void updateBroodmotherStage(String newStage) {
        String oldStage = GameState.Broodmother.stage;
        if (oldStage.equals(newStage)) return;

        GameState.Broodmother.stage = newStage;
        Minecraft client = Minecraft.getInstance();

        // =======================================================
        // Stage 4 (Imminent) 検知
        // =======================================================
        if ("Imminent".equals(newStage)) {
            GameState.Broodmother.stage4StartTime = System.currentTimeMillis();

            // Stage 3 以下から変わる瞬間を見ていれば、湧くまでの30秒を数えられる。
            // ロビーに入った時点ですでに Stage 4 だったなら、いつ始まったか分からないので数えない
            boolean witnessed = BEFORE_STAGE_4.contains(oldStage) && client.level != null;
            GameState.Broodmother.spawnTargetTick = witnessed
                    ? GameState.Server.estimatedTicks(client.level.getGameTime()) + STAGE_4_TICKS
                    : 0;

            // ★変更: TitleとSoundを独立して判定
            client.execute(() -> {
                if (ModConfig.Combat.SpidersDen.enableStage4Title) {
                    MutableComponent title = Component.literal("BROODMOTHER SOON").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
                    NotificationUtils.showTitle(client, title, null);
                }
                if (ModConfig.Combat.SpidersDen.enableStage4Sound) {
                    NotificationUtils.playSound(client, SoundEvents.CREEPER_HURT, 1.0f, 1.0f);
                }
            });
        }
        // =======================================================
        // Stage 5 (Alive!) 検知
        // =======================================================
        else if ("Alive!".equals(newStage)) {
            if ("Imminent".equals(oldStage) && GameState.Broodmother.stage4StartTime > 0) {
                long seconds = (System.currentTimeMillis() - GameState.Broodmother.stage4StartTime) / 1000;

                if (ModConfig.Combat.SpidersDen.showBroodmotherStage4Duration) {
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            client.execute(() -> {
                                Component durationText = Component.literal(String.format("§aBroodmother Stage 4 Duration: %dm %ds", seconds / 60, seconds % 60));
                                NotificationUtils.sendSystemChat(client, durationText);
                            });
                        }
                    }, 100);
                }
            }
            GameState.Broodmother.stage4StartTime = 0;
            GameState.Broodmother.spawnTargetTick = 0;

            // ★変更: TitleとSoundを独立して判定
            client.execute(() -> {
                if (ModConfig.Combat.SpidersDen.enableStage5Title) {
                    MutableComponent title = Component.literal("BROODMOTHER SPAWNED").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
                    NotificationUtils.showTitle(client, title, null);
                }
                if (ModConfig.Combat.SpidersDen.enableStage5Sound) {
                    NotificationUtils.playSound(client, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 1.0f, 1.0f);
                }
            });
        }
        else {
            if (!"Imminent".equals(newStage)) {
                GameState.Broodmother.stage4StartTime = 0;
                GameState.Broodmother.spawnTargetTick = 0;
            }
        }
    }
}
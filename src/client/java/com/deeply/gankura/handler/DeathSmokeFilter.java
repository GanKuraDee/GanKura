package com.deeply.gankura.handler;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 死んだエンティティから出る白い煙を見分ける。
 *
 * Hypixel では、この煙はサーバーから "poof" のパーティクルとして1粒ずつ送られてくる。
 * 死んだ知らせ (エンティティイベント 3) が届いたら場所を覚えておき、
 * その後しばらくの間、その近くに届いた "poof" だけを止める
 */
public final class DeathSmokeFilter {

    // 死んだ知らせのエンティティイベント
    private static final byte DEATH_EVENT = 3;
    // 煙は死んでから1秒ほど後に届く。少し余裕を持たせる
    private static final long WINDOW_MS = 3000;
    // 煙が広がる範囲。大きな Mob でも収まるように取る
    private static final double RADIUS = 5.0;

    // 死んだ知らせが届いた後も、ノックバックで体が滑って煙の出る場所がずれることがある。
    // 死んだ場所と、そのエンティティの今の場所の両方から測る
    private record Death(Entity entity, Vec3 pos, long at) {
        boolean isNear(double x, double y, double z) {
            double r2 = RADIUS * RADIUS;
            return pos.distanceToSqr(x, y, z) <= r2 || entity.position().distanceToSqr(x, y, z) <= r2;
        }
    }

    // 最近死んだ場所。古いものから捨てる
    private static final Deque<Death> recentDeaths = new ArrayDeque<>();

    private DeathSmokeFilter() {
    }

    /** メインスレッドで、エンティティイベントを受け取るたびに呼ばれる */
    public static void onEntityEvent(Entity entity, byte eventId) {
        if (eventId != DEATH_EVENT || !(entity instanceof LivingEntity)) return;
        if (entity == Minecraft.getInstance().player) return;

        prune();
        recentDeaths.addLast(new Death(entity, entity.position(), System.currentTimeMillis()));
    }

    /** メインスレッドで、パーティクルを受け取るたびに呼ばれる。止めるべき煙なら true */
    public static boolean shouldHide(ClientboundLevelParticlesPacket packet) {
        if (!ModConfig.Combat.hideDeathAnimation || !GameState.Server.isSkyblock()) return false;
        if (packet.getParticle().getType() != ParticleTypes.POOF) return false;

        prune();
        for (Death death : recentDeaths) {
            if (death.isNear(packet.getX(), packet.getY(), packet.getZ())) return true;
        }
        return false;
    }

    private static void prune() {
        long now = System.currentTimeMillis();
        while (!recentDeaths.isEmpty() && now - recentDeaths.peekFirst().at > WINDOW_MS) recentDeaths.removeFirst();
    }
}

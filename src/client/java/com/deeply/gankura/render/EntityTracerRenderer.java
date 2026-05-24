package com.deeply.gankura.render;

import com.deeply.gankura.data.GameState;
import com.deeply.gankura.data.ModConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class EntityTracerRenderer {

    public static void render(GuiGraphicsExtractor graphics, Minecraft client) {
        if (EntityHighlightManager.highlightedEntities.isEmpty()) return;
        if (client.player == null) return;
        if (!"SKYBLOCK".equals(GameState.Server.gametype)) return;

        Camera camera = client.gameRenderer.getMainCamera();

        for (Entity entity : EntityHighlightManager.highlightedEntities) {
            if (entity instanceof IronGolem   && !ModConfig.INSTANCE.golem.enableGolemTracer)             continue;
            if (entity instanceof Spider       && !ModConfig.INSTANCE.broodmother.enableBroodmotherTracer) continue;
            if (entity instanceof EnderDragon  && !ModConfig.INSTANCE.dragon.enableDragonTracer)           continue;

            renderTracer(graphics, client, camera, entity, getTracerColor(entity));
        }
    }

    private static void renderTracer(GuiGraphicsExtractor graphics, Minecraft client, Camera camera, Entity entity, int color) {
        Vec3 camPos = camera.position();
        Vec3 entityCenter = entity.position().add(0, entity.getBbHeight() / 2.0, 0);
        Vec3 relative = entityCenter.subtract(camPos);

        // Transform world-relative vector into camera space
        Matrix4f viewRot = camera.getViewRotationMatrix(new Matrix4f());
        Vector4f cs = viewRot.transform(
            new Vector4f((float) relative.x, (float) relative.y, (float) relative.z, 0.0f)
        );

        // Camera looks down -Z; positive depth means entity is in front
        float depth = -cs.z;
        if (depth <= 0.001f) return;

        int sw = client.getWindow().getGuiScaledWidth();
        int sh = client.getWindow().getGuiScaledHeight();

        // Focal length from vertical FOV
        float fov = (float) Math.toRadians(client.options.fov().get());
        float f = 1.0f / (float) Math.tan(fov * 0.5f);
        float aspect = (float) sw / sh;

        // Project to screen-space offsets from center (screen Y increases downward)
        float screenX =  cs.x / depth * f / aspect * (sw * 0.5f);
        float screenY = -cs.y / depth * f           * (sh * 0.5f);

        float angle  = (float) Math.atan2(screenY, screenX);
        float length = (float) Math.sqrt(screenX * screenX + screenY * screenY);
        // Clamp to screen diagonal so the line never draws past the edge
        float maxLen = (float) Math.sqrt((double) sw * sw + (double) sh * sh) * 0.5f;
        length = Math.min(length, maxLen);
        if (length < 1f) return;

        // Draw a single 2-pixel-tall rectangle rotated to point at the entity
        graphics.pose().pushMatrix();
        graphics.pose().translate(sw * 0.5f, sh * 0.5f);
        graphics.pose().rotate(angle);
        graphics.fill(0, -1, (int) length, 1, color);
        graphics.pose().popMatrix();
    }

    private static int getTracerColor(Entity entity) {
        if (entity instanceof IronGolem)   return 0xFFFFAA00;
        if (entity instanceof Spider)      return 0xFFFF5555;
        if (entity instanceof EnderDragon) return 0xFFFF55FF;
        return 0xFFFFFFFF;
    }
}

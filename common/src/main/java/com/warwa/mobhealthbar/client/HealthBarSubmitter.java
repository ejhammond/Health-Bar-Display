package com.warwa.mobhealthbar.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.warwa.mobhealthbar.config.BarPosition;
import com.warwa.mobhealthbar.config.HealthBarConfig;
import com.warwa.mobhealthbar.config.SpecialMobConfig;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.EntityType;

/**
 * Resolves a mob's measured size into a bar position and scale, then draws it.
 * Both loaders call this from a mixin on {@code EntityRenderer.submit}, where the pose is clean.
 */
public final class HealthBarSubmitter {

    /** Clearance between the mob's surface and a MIDDLE bar. */
    private static final float FRONT_MARGIN = 0.15f;

    private HealthBarSubmitter() {}

    public static void submit(EntityRenderState state, PoseStack poseStack,
                              SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (!(state instanceof HealthBarDataAccessor accessor)) return;

        HealthBarData data = accessor.mobhealthbar$getHealthBarData();
        if (data == null || !data.shouldRender()) return;

        EntityType<?> type = state.entityType;
        if (type == null) return;

        HealthBarConfig cfg = HealthBarConfig.get();
        ModelExtents.Extent extent = accessor.mobhealthbar$getExtent();

        float visualTop = resolveVisualTop(state, type, extent);
        float visualMiddle = extent != null ? extent.middle() : visualTop * 0.5f;
        float anchorY = HealthBarAnchor.anchorY(type, visualTop, visualMiddle, cfg);
        float scale = HealthBarAnchor.scale(type, cfg, state.distanceToCameraSq);

        float offsetX = 0.0f;
        float offsetZ = 0.0f;
        if (positionFor(type, cfg) == BarPosition.MIDDLE) {
            // A bar at mid height sits inside the mob, where its own geometry hides it — the
            // hearts are depth-tested. Slide it out past the model's widest point towards the
            // viewer so it reads as floating in front. The bounding box is not enough here:
            // a ghast's body is a 4.5-block ball inside a 4.0-wide box.
            double dx = camera.pos.x - state.x;
            double dz = camera.pos.z - state.z;
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length > 1.0e-4) {
                float reach = resolveRadius(state, extent) + FRONT_MARGIN;
                offsetX = (float) (dx / length * reach);
                offsetZ = (float) (dz / length * reach);
            }
        }

        HealthBarRenderer.render(poseStack, submitNodeCollector, data,
                offsetX, anchorY, offsetZ, scale, camera.orientation);
    }

    private static BarPosition positionFor(EntityType<?> type, HealthBarConfig cfg) {
        SpecialMobConfig special = cfg.specialFor(type);
        return special != null ? special.position : cfg.position;
    }

    /**
     * Prefers the measured model height, falls back to an override for the mobs that cannot
     * be measured, and finally to the bounding box.
     */
    private static float resolveVisualTop(EntityRenderState state, EntityType<?> type,
                                          ModelExtents.Extent extent) {
        float measured = extent != null ? extent.top() : Float.NaN;
        float override = MobAnchorOverrides.visualTop(type, state, measured);
        if (!Float.isNaN(override)) return override;
        if (!Float.isNaN(measured)) return measured;
        return state.boundingBoxHeight;
    }

    private static float resolveRadius(EntityRenderState state, ModelExtents.Extent extent) {
        float boxRadius = state.boundingBoxWidth * 0.5f;
        return extent != null ? Math.max(extent.radius(), boxRadius) : boxRadius;
    }
}

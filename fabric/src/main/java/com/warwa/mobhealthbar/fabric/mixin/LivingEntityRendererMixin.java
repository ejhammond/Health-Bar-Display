package com.warwa.mobhealthbar.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.warwa.mobhealthbar.client.ModelExtents;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Records the entity's clean render origin before any model transforms are applied.
 *
 * <p>{@link com.warwa.mobhealthbar.client.HealthBarMeasureLayer} measures the model through a
 * pose that already includes the entity's scale, rotations and the per-renderer
 * {@code scale()} override — exactly what we want for a true visual height — but that pose is
 * expressed relative to the camera. Capturing the origin here, at the top of the same method
 * and before the transform chain begins, gives the measurement something to subtract so it
 * comes out as a plain height above the entity's feet.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD")
    )
    private void mobhealthbar$captureBasePose(LivingEntityRenderState state, PoseStack poseStack,
                                              SubmitNodeCollector submitNodeCollector,
                                              CameraRenderState camera, CallbackInfo ci) {
        ModelExtents.captureBase(poseStack.last().pose());
    }

    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("RETURN")
    )
    private void mobhealthbar$clearBasePose(LivingEntityRenderState state, PoseStack poseStack,
                                            SubmitNodeCollector submitNodeCollector,
                                            CameraRenderState camera, CallbackInfo ci) {
        // Never let a stale origin leak to an entity whose renderer isn't a LivingEntityRenderer.
        ModelExtents.clearBase();
    }
}

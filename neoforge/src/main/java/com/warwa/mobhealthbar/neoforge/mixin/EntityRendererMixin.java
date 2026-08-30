package com.warwa.mobhealthbar.neoforge.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.warwa.mobhealthbar.client.HealthBarExtractor;
import com.warwa.mobhealthbar.client.HealthBarSubmitter;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Extracts health data, then draws the bar from a clean PoseStack.
 *
 * <p>{@code EntityRenderDispatcher.submit} calls {@code renderer.submit} having applied only
 * a translate to the entity's position — no scale, no rotation, no Y flip. That is the same
 * clean pose vanilla renders name tags from, and rendering here is what fixes bars being
 * flung sideways off beached cod, buried in squid, hung below phantoms and floated eight
 * blocks over withers: all of those came from a render layer inheriting the model transforms.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V",
            at = @At("TAIL")
    )
    private void mobhealthbar$extract(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
        HealthBarExtractor.extract(entity, state, partialTick);
    }

    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("TAIL")
    )
    private void mobhealthbar$submit(EntityRenderState state, PoseStack poseStack,
                                     SubmitNodeCollector submitNodeCollector, CameraRenderState camera,
                                     CallbackInfo ci) {
        HealthBarSubmitter.submit(state, poseStack, submitNodeCollector, camera);
    }
}

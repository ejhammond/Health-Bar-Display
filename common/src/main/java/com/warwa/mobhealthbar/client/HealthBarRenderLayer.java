package com.warwa.mobhealthbar.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.warwa.mobhealthbar.MobHealthBar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * Render layer that draws health bar hearts above living entities.
 *
 * IMPORTANT: When this layer's submit() is called, the PoseStack has entity transforms:
 *   - body rotation via setupRotations
 *   - scale(-1, -1, 1) flip
 *   - translate(0, -1.501, 0) model offset
 * We must account for the flipped Y axis and offset.
 */
public class HealthBarRenderLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>>
        extends RenderLayer<S, M> {

    private static boolean debugLogged = false;

    public HealthBarRenderLayer(LivingEntityRenderer<?, S, M> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
                       S renderState, float yRot, float xRot) {
        HealthBarData data = null;

        if (renderState instanceof HealthBarDataAccessor accessor) {
            data = accessor.mobhealthbar$getHealthBarData();
        }

        if (data == null || !data.shouldRender()) {
            return;
        }

        if (!debugLogged) {
            MobHealthBar.LOGGER.info("[MobHealthBar] Rendering health bar! health={}, maxHealth={}, alpha={}, entityHeight={}",
                    data.currentHealth(), data.maxHealth(), data.alpha(), renderState.boundingBoxHeight);
            debugLogged = true;
        }

        float entityHeight = renderState.boundingBoxHeight;

        // The PoseStack currently has entity model transforms applied:
        //   scale(-1, -1, 1) means Y is INVERTED (positive Y = DOWN in world)
        //   translate(0, -1.501, 0) offsets the model origin
        //
        // In this inverted space, the entity feet are at local Y = +1.501
        // The entity top (head) is at local Y = 1.501 - entityHeight
        // Above the head by offset: local Y = 1.501 - entityHeight - HEIGHT_OFFSET
        //
        // We also need to undo the body rotation and X/Y flip for billboard rendering.

        HealthBarRenderer.renderHealthBar(poseStack, submitNodeCollector, data, entityHeight, lightCoords, yRot);
    }
}

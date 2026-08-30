package com.warwa.mobhealthbar.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * Draws nothing. This layer exists purely to measure how tall each mob's model actually is.
 *
 * <p>The hearts themselves are drawn from {@code EntityRenderer.submit}, where the pose is
 * clean. But this is the only point in the pipeline where the model is posed for <em>this</em>
 * entity: {@code LivingEntityRenderer.submit} calls {@code model.setupAnim(state)} immediately
 * before iterating its layers, and the pose here carries the full transform chain including
 * the per-renderer {@code scale()} override that makes a wither twice its model size.
 *
 * <p>Ordering works out within a single frame: this layer runs inside
 * {@code LivingEntityRenderer.submit}, which only calls {@code super.submit} — and therefore
 * the drawing code — afterwards. So the bar is always drawn with a height measured moments
 * earlier from the same entity's current pose.
 *
 * <p>The measurement only runs for mobs that currently have a bar, so it costs nothing for
 * the rest of the world.
 */
public class HealthBarMeasureLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>>
        extends RenderLayer<S, M> {

    public HealthBarMeasureLayer(LivingEntityRenderer<?, S, M> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
                       S renderState, float yRot, float xRot) {
        if (!(renderState instanceof HealthBarDataAccessor accessor)) return;

        HealthBarData data = accessor.mobhealthbar$getHealthBarData();
        if (data == null || !data.shouldRender()) return;

        M model = getParentModel();
        if (model == null) return;

        accessor.mobhealthbar$setExtent(ModelExtents.measure(poseStack, model));
    }
}

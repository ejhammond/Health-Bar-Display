package com.warwa.mobhealthbar.fabric.mixin;

import com.warwa.mobhealthbar.client.HealthBarData;
import com.warwa.mobhealthbar.client.HealthBarDataAccessor;
import com.warwa.mobhealthbar.client.ModelExtents;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Carries the health bar snapshot from extraction through to rendering.
 *
 * <p>Attached to {@link EntityRenderState} rather than {@code LivingEntityRenderState} so
 * that the Ender Dragon — whose render state extends {@code EntityRenderState} directly —
 * is covered too.
 */
@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements HealthBarDataAccessor {

    @Unique
    private HealthBarData mobhealthbar$data;

    @Unique
    private ModelExtents.Extent mobhealthbar$extent;

    @Override
    public void mobhealthbar$setHealthBarData(HealthBarData data) {
        this.mobhealthbar$data = data;
    }

    @Override
    public HealthBarData mobhealthbar$getHealthBarData() {
        return this.mobhealthbar$data;
    }

    @Override
    public void mobhealthbar$setExtent(ModelExtents.Extent extent) {
        this.mobhealthbar$extent = extent;
    }

    @Override
    public ModelExtents.Extent mobhealthbar$getExtent() {
        return this.mobhealthbar$extent;
    }
}

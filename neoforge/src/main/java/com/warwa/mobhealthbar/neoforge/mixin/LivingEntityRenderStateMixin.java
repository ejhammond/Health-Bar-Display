package com.warwa.mobhealthbar.neoforge.mixin;

import com.warwa.mobhealthbar.client.HealthBarData;
import com.warwa.mobhealthbar.client.HealthBarDataAccessor;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public class LivingEntityRenderStateMixin implements HealthBarDataAccessor {

    @Unique
    private HealthBarData mobhealthbar$healthBarData;

    @Override
    public void mobhealthbar$setHealthBarData(HealthBarData data) {
        this.mobhealthbar$healthBarData = data;
    }

    @Override
    public HealthBarData mobhealthbar$getHealthBarData() {
        return this.mobhealthbar$healthBarData;
    }
}

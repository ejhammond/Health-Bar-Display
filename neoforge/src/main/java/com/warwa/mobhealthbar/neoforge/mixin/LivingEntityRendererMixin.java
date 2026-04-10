package com.warwa.mobhealthbar.neoforge.mixin;

import com.warwa.mobhealthbar.client.HealthBarData;
import com.warwa.mobhealthbar.client.HealthBarDataAccessor;
import com.warwa.mobhealthbar.tracking.EntityHealthTracker;
import com.warwa.mobhealthbar.tracking.HostilityClassifier;
import com.warwa.mobhealthbar.tracking.TrackedEntity;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL")
    )
    private void mobhealthbar$extractHealthData(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        TrackedEntity tracked = EntityHealthTracker.INSTANCE.getTrackedEntity(entity.getId());
        if (tracked == null) {
            if (state instanceof HealthBarDataAccessor accessor) {
                accessor.mobhealthbar$setHealthBarData(null);
            }
            return;
        }

        tracked.updateHealth(entity.getHealth());

        boolean poisoned = entity.hasEffect(MobEffects.POISON);
        boolean withered = entity.hasEffect(MobEffects.WITHER);

        HealthBarData data = new HealthBarData(
                entity.getHealth(),
                entity.getMaxHealth(),
                entity.getAbsorptionAmount(),
                tracked.getAlpha(partialTick),
                tracked.isHostile(),
                tracked.getPreviousHealth(),
                tracked.getTicksSinceDamage(),
                tracked.isDamageFlashing(),
                tracked.isShaking(),
                poisoned,
                withered
        );

        if (state instanceof HealthBarDataAccessor accessor) {
            accessor.mobhealthbar$setHealthBarData(data);
        }
    }
}

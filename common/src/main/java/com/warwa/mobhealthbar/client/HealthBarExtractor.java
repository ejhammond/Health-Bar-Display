package com.warwa.mobhealthbar.client;

import com.warwa.mobhealthbar.config.HealthBarConfig;
import com.warwa.mobhealthbar.tracking.BossClassifier;
import com.warwa.mobhealthbar.tracking.EntityHealthTracker;
import com.warwa.mobhealthbar.tracking.HealthBarRules;
import com.warwa.mobhealthbar.tracking.TrackedEntity;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Builds the health bar snapshot during the render state extraction phase, where the entity
 * is still available. Both loaders call this from a mixin on {@code EntityRenderer}.
 *
 * <p>Hooking {@code EntityRenderer} rather than {@code LivingEntityRenderer} is deliberate:
 * the Ender Dragon's renderer extends {@code EntityRenderer} directly, so a
 * {@code LivingEntityRenderer} hook never sees it — which is why the dragon had no bar at all.
 */
public final class HealthBarExtractor {

    private HealthBarExtractor() {}

    public static void extract(Entity entity, EntityRenderState state, float partialTick) {
        if (!(state instanceof HealthBarDataAccessor accessor)) return;

        // Always assign: render states are pooled and reused, so stale data would otherwise
        // linger on an entity that should no longer have a bar.
        accessor.mobhealthbar$setHealthBarData(build(entity, partialTick));
        // The measure layer refills this later this frame; clear it so a mob whose model
        // cannot be measured falls back rather than inheriting another mob's height.
        accessor.mobhealthbar$setExtent(null);
    }

    private static HealthBarData build(Entity entity, float partialTick) {
        if (!(entity instanceof LivingEntity living)) return null;

        TrackedEntity tracked = EntityHealthTracker.INSTANCE.getTrackedEntity(entity.getId());
        if (tracked == null || !HealthBarRules.canShow(living)) return null;

        tracked.updateHealth(living.getHealth());

        float alpha = (float) (tracked.getAlpha(partialTick) * HealthBarConfig.get().opacity);

        return new HealthBarData(
                living.getHealth(),
                living.getMaxHealth(),
                living.getAbsorptionAmount(),
                alpha,
                tracked.isHostile(),
                tracked.getPreviousHealth(),
                tracked.getTicksSinceDamage(),
                tracked.isDamageFlashing(),
                tracked.isShaking(),
                living.hasEffect(MobEffects.POISON),
                living.hasEffect(MobEffects.WITHER),
                BossClassifier.isBoss(living)
        );
    }
}

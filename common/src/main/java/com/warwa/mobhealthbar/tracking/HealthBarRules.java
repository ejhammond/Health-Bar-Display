package com.warwa.mobhealthbar.tracking;

import com.warwa.mobhealthbar.config.HealthBarConfig;
import net.minecraft.world.entity.LivingEntity;

/**
 * Central place that turns the user config into yes/no decisions about whether a
 * given entity should show a health bar, so both loaders apply identical rules.
 */
public final class HealthBarRules {

    /** Global gate: respects the "show bosses" toggle and the per-mob disable list. */
    public static boolean canShow(LivingEntity entity) {
        HealthBarConfig cfg = HealthBarConfig.get();
        if (BossClassifier.isBoss(entity) && !cfg.showBosses) {
            return false;
        }
        return cfg.isMobEnabled(entity.getType());
    }

    /** Whether merely looking at this entity should show its bar. */
    public static boolean showOnLook(LivingEntity entity) {
        if (!canShow(entity)) return false;
        boolean hostile = HostilityClassifier.isHostile(entity);
        HealthBarConfig cfg = HealthBarConfig.get();
        return hostile ? cfg.enemyOnLook : cfg.friendlyOnLook;
    }

    /** Whether hitting this entity should show its bar. */
    public static boolean showOnHit(LivingEntity entity) {
        return canShow(entity);
    }

    private HealthBarRules() {}
}

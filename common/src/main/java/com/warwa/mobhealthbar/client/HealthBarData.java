package com.warwa.mobhealthbar.client;

/**
 * Immutable snapshot of health bar data extracted during the render state phase.
 *
 * <p>Deliberately carries no position or size: those depend on the mob's measured visual
 * height, which is only known once the measure layer has run later in the frame.
 */
public record HealthBarData(
        float currentHealth,
        float maxHealth,
        float absorption,
        float alpha,
        boolean hostile,
        float previousHealth,
        int ticksSinceDamage,
        boolean damageFlashing,
        boolean shaking,
        boolean poisoned,
        boolean withered,
        /** Draw a fixed, smoothly-draining percentage bar instead of one heart per 2 HP. */
        boolean boss
) {
    public boolean shouldRender() {
        return alpha > 0.001f && maxHealth > 0;
    }

    /** Health as a 0..1 fraction of max, used by the boss bar. */
    public float healthFraction() {
        return maxHealth <= 0 ? 0.0f : Math.max(0.0f, Math.min(1.0f, currentHealth / maxHealth));
    }
}

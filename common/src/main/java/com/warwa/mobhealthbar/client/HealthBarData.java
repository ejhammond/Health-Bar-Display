package com.warwa.mobhealthbar.client;

/**
 * Immutable snapshot of health bar data extracted during the render state phase.
 * Passed from entity extraction to the render layer's submit/render method.
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
        boolean withered
) {
    public boolean shouldRender() {
        return alpha > 0.001f && maxHealth > 0;
    }
}

package com.warwa.mobhealthbar.client;

/**
 * Duck interface for injecting HealthBarData into LivingEntityRenderState.
 * Implemented by Fabric via mixin, by NeoForge via ContextKey.
 */
public interface HealthBarDataAccessor {

    void mobhealthbar$setHealthBarData(HealthBarData data);

    HealthBarData mobhealthbar$getHealthBarData();
}

package com.warwa.mobhealthbar.client;

/**
 * Duck interface for carrying health bar state on an entity's render state.
 * Implemented on {@code EntityRenderState} via mixin on each loader.
 */
public interface HealthBarDataAccessor {

    void mobhealthbar$setHealthBarData(HealthBarData data);

    HealthBarData mobhealthbar$getHealthBarData();

    /**
     * The mob's measured rendered size, or null if it was not measured this frame.
     * Written by {@link HealthBarMeasureLayer}, read when the bar is drawn.
     */
    void mobhealthbar$setExtent(ModelExtents.Extent extent);

    ModelExtents.Extent mobhealthbar$getExtent();
}

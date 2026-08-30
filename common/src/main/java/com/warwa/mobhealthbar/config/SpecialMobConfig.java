package com.warwa.mobhealthbar.config;

/**
 * Position, size and height offset for one of the oversized mobs that get their own settings
 * (ghast, happy ghast, elder guardian). These are deliberately independent of the
 * global settings so that tuning a ghast's bar cannot disturb every other mob.
 */
public final class SpecialMobConfig {

    public BarPosition position;
    public double size;

    /** Vertical nudge in blocks, applied on top of {@link #position}. */
    public double heightOffset;

    public SpecialMobConfig() {
        this(BarPosition.ABOVE, 1.0, 0.0);
    }

    public SpecialMobConfig(BarPosition position, double size, double heightOffset) {
        this.position = position;
        this.size = size;
        this.heightOffset = heightOffset;
    }

    /**
     * Copies {@code other}'s values in without replacing this object, so the config screens
     * can hold a reference to a special mob across a reset.
     */
    void copyFrom(SpecialMobConfig other) {
        position = other.position;
        size = other.size;
        heightOffset = other.heightOffset;
    }

    void clamp() {
        if (position == null || !BarPosition.SPECIAL_VALUES.contains(position)) {
            position = BarPosition.ABOVE;
        }
        size = HealthBarConfig.clampSize(size);
        heightOffset = HealthBarConfig.clampHeightOffset(heightOffset);
    }
}

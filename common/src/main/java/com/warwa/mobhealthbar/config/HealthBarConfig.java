package com.warwa.mobhealthbar.config;

public final class HealthBarConfig {

    // How long the health bar stays visible after damaging a hostile entity (ticks)
    public static final int HOSTILE_DISPLAY_DURATION = 400; // 20 seconds

    // How long the health bar stays visible after damaging a non-hostile entity (ticks)
    public static final int FRIENDLY_DISPLAY_DURATION = 140; // 7 seconds

    // Fade-in duration (ticks)
    public static final int FADE_IN_DURATION = 6; // ~0.3 seconds

    // Fade-out duration (ticks)
    public static final int FADE_OUT_DURATION = 15; // ~0.75 seconds

    // How far above the entity's head the hearts render (world units)
    public static final float HEIGHT_OFFSET = 0.5f;

    // Scale factor for heart rendering in world space
    public static final float HEART_SCALE = 0.025f;

    // Maximum render distance squared (32 blocks)
    public static final double MAX_RENDER_DISTANCE_SQ = 1024.0;

    // Maximum hearts per row before wrapping
    public static final int HEARTS_PER_ROW = 10;

    // Duration of the damage flash animation (ticks)
    public static final int DAMAGE_FLASH_DURATION = 10;

    // Duration of the heart shake animation (ticks)
    public static final int SHAKE_DURATION = 10;

    // For entities with very high health, scale down hearts if total exceeds this
    public static final int MAX_HEART_ROWS = 5;

    private HealthBarConfig() {}
}

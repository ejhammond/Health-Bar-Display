package com.warwa.mobhealthbar.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.warwa.mobhealthbar.MobHealthBar;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Mod configuration. The user-facing fields are instance fields (persisted to JSON
 * via Gson), while the internal rendering/animation tuning values remain compile-time
 * constants. Access the live config through {@link #get()}.
 */
public final class HealthBarConfig {

    // ===================== User-configurable (persisted) =====================

    /** Show a health bar for enemies just by looking at them. */
    public boolean enemyOnLook = true;
    /** Show a health bar for friendly/neutral mobs just by looking at them. */
    public boolean friendlyOnLook = false;

    /** How long (seconds) an enemy bar stays after you stop looking. */
    public int enemyLookSeconds = 20;
    /** How long (seconds) a friendly bar stays after you stop looking. */
    public int friendlyLookSeconds = 7;

    /** How long (seconds) an enemy bar stays after you hit it. */
    public int enemyHitSeconds = 20;
    /** How long (seconds) a friendly bar stays after you hit it. */
    public int friendlyHitSeconds = 7;

    /** Whether bosses (Ender Dragon, Wither) get a health bar at all. */
    public boolean showBosses = true;

    /** Overall opacity of the health bar, 0.0 (invisible) .. 1.0 (opaque). */
    public double opacity = 1.0;

    /** Hide the vanilla boss health bar drawn across the top of the screen. */
    public boolean hideVanillaBossBar = false;

    // ---- Global placement / sizing (does not apply to the special mobs below) ----

    /** Where bars sit relative to the mob. */
    public BarPosition position = BarPosition.ABOVE;

    /** Global size multiplier for health bars. Ignored by the special mobs below. */
    public double size = 1.0;

    /**
     * Vertical nudge in blocks applied on top of {@link #position}, for taste. The measured
     * anchor aims to sit just clear of each model, but "just clear" is a judgement call and
     * mobs vary; this is the dial for it. Ignored by the special mobs below.
     */
    public double heightOffset = 0.0;

    // ---- Distance scaling ----

    /** Grow bars with camera distance so far-away mobs stay readable. */
    public boolean distanceScaling = true;

    /**
     * How strongly distance scaling applies. 0.0 = no growth (same as disabling it),
     * 1.0 = the bar keeps a roughly constant on-screen size at any distance.
     */
    public double distanceScalingAmount = 0.5;

    // ---- Per-mob overrides, independent of the global settings above ----

    public SpecialMobConfig ghast = new SpecialMobConfig();
    public SpecialMobConfig happyGhast = new SpecialMobConfig();
    public SpecialMobConfig elderGuardian = new SpecialMobConfig();

    /**
     * Entity type ids (e.g. {@code "minecraft:zombie"}) whose bars are switched off.
     * Only disabled mobs are stored, so the file stays small and mobs added by a future
     * game version default to enabled.
     */
    public Set<String> disabledMobs = new HashSet<>();

    // ===================== Internal tuning (not persisted) =====================

    /** Fade-in duration (ticks). */
    public static final int FADE_IN_DURATION = 6; // ~0.3 seconds
    /** Fade-out duration (ticks). */
    public static final int FADE_OUT_DURATION = 15; // ~0.75 seconds
    /** How far above the entity's head the hearts render (world units). */
    public static final float HEIGHT_OFFSET = 0.5f;
    /** Scale factor for heart rendering in world space. */
    public static final float HEART_SCALE = 0.025f;
    /** Maximum render distance squared (32 blocks). */
    public static final double MAX_RENDER_DISTANCE_SQ = 1024.0;
    /** Maximum hearts per row before wrapping. */
    public static final int HEARTS_PER_ROW = 10;
    /** Duration of the damage flash animation (ticks). */
    public static final int DAMAGE_FLASH_DURATION = 10;
    /** Duration of the heart shake animation (ticks). */
    public static final int SHAKE_DURATION = 10;
    /** For entities with very high health, scale down hearts if total exceeds this. */
    public static final int MAX_HEART_ROWS = 5;

    /**
     * Bosses show a fixed bar of this many hearts representing 100% of max health,
     * drained smoothly rather than in half-heart steps. A wither has 150 hearts' worth
     * of HP, which is illegible drawn literally.
     */
    public static final int BOSS_HEART_COUNT = 10;

    /**
     * Gap between the top of the model and the bottom of the bar, for {@link BarPosition#ABOVE}.
     *
     * <p>Deliberately small. This is a flat distance in blocks, so it reads very differently on
     * a two-block mob than on a calf — at 0.4 a baby's bar sat more than half its own body
     * height above its head. The measured extent already clears horns, hats and shells, so the
     * gap only has to separate the bar from the silhouette rather than clear anything.
     */
    public static final float ABOVE_GAP = 0.15f;
    /** Gap between the bottom of the model and the top of the bar, for {@link BarPosition#BELOW}. */
    public static final float BELOW_GAP = 0.4f;

    /** Distance (blocks) at which distance scaling starts to take effect. */
    public static final float DISTANCE_SCALING_START = 8.0f;

    // Slider bounds shared with the config screen.
    public static final int MIN_DURATION_SECONDS = 1;
    public static final int MAX_DURATION_SECONDS = 60;
    public static final double MIN_SIZE = 0.25;
    public static final double MAX_SIZE = 4.0;
    public static final double MIN_HEIGHT_OFFSET = -2.0;
    public static final double MAX_HEIGHT_OFFSET = 2.0;
    /**
     * Slider granularity for the height offset, in blocks. Just under one pixel of a
     * 16-pixel block, which is fine enough to place a bar exactly and coarse enough that
     * the arrow keys still cross the range in a sensible number of presses.
     */
    public static final double HEIGHT_OFFSET_STEP = 0.05;

    // ===================== Persistence plumbing =====================

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "mobhealthbar.json";

    private static HealthBarConfig INSTANCE = new HealthBarConfig();
    private static Path configFile;

    public HealthBarConfig() {}

    public static HealthBarConfig get() {
        return INSTANCE;
    }

    /**
     * A throwaway config holding nothing but the built-in defaults, for the Reset buttons to
     * read a single option's default out of. Deliberately a fresh instance rather than a shared
     * constant: a shared one is a mutable static that any caller could scribble on.
     */
    public static HealthBarConfig defaults() {
        return new HealthBarConfig();
    }

    /**
     * Puts every setting back to its default, including the per-mob disable list.
     *
     * <p>Copies into the live instance rather than replacing it. The option screens capture the
     * config — and individual {@link SpecialMobConfig}s — in their widget callbacks, and
     * swapping the object out from under them would leave those callbacks writing to a config
     * nothing reads.
     */
    public static void resetAll() {
        INSTANCE.copyFrom(new HealthBarConfig());
        save();
    }

    private void copyFrom(HealthBarConfig other) {
        enemyOnLook = other.enemyOnLook;
        friendlyOnLook = other.friendlyOnLook;
        enemyLookSeconds = other.enemyLookSeconds;
        friendlyLookSeconds = other.friendlyLookSeconds;
        enemyHitSeconds = other.enemyHitSeconds;
        friendlyHitSeconds = other.friendlyHitSeconds;
        showBosses = other.showBosses;
        opacity = other.opacity;
        hideVanillaBossBar = other.hideVanillaBossBar;
        position = other.position;
        size = other.size;
        heightOffset = other.heightOffset;
        distanceScaling = other.distanceScaling;
        distanceScalingAmount = other.distanceScalingAmount;
        ghast.copyFrom(other.ghast);
        happyGhast.copyFrom(other.happyGhast);
        elderGuardian.copyFrom(other.elderGuardian);
        disabledMobs.clear();
    }

    public int enemyLookTicks() { return enemyLookSeconds * 20; }
    public int friendlyLookTicks() { return friendlyLookSeconds * 20; }
    public int enemyHitTicks() { return enemyHitSeconds * 20; }
    public int friendlyHitTicks() { return friendlyHitSeconds * 20; }

    /** Settings for a mob that has its own position/size, or null if it uses the global ones. */
    public SpecialMobConfig specialFor(EntityType<?> type) {
        if (type == EntityTypes.GHAST) return ghast;
        if (type == EntityTypes.HAPPY_GHAST) return happyGhast;
        if (type == EntityTypes.ELDER_GUARDIAN) return elderGuardian;
        return null;
    }

    public boolean isMobEnabled(EntityType<?> type) {
        return !disabledMobs.contains(idOf(type));
    }

    public void setMobEnabled(EntityType<?> type, boolean enabled) {
        if (enabled) {
            disabledMobs.remove(idOf(type));
        } else {
            disabledMobs.add(idOf(type));
        }
    }

    public static String idOf(EntityType<?> type) {
        Identifier key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return key == null ? "" : key.toString();
    }

    private void clamp() {
        enemyLookSeconds = clampSeconds(enemyLookSeconds);
        friendlyLookSeconds = clampSeconds(friendlyLookSeconds);
        enemyHitSeconds = clampSeconds(enemyHitSeconds);
        friendlyHitSeconds = clampSeconds(friendlyHitSeconds);
        opacity = Math.max(0.0, Math.min(1.0, opacity));

        if (position == null || !BarPosition.GLOBAL_VALUES.contains(position)) {
            position = BarPosition.ABOVE;
        }
        size = clampSize(size);
        heightOffset = clampHeightOffset(heightOffset);
        distanceScalingAmount = Math.max(0.0, Math.min(1.0, distanceScalingAmount));

        // Gson leaves absent objects null, so re-create rather than NPE on a hand-edited file.
        if (ghast == null) ghast = new SpecialMobConfig();
        if (happyGhast == null) happyGhast = new SpecialMobConfig();
        if (elderGuardian == null) elderGuardian = new SpecialMobConfig();
        ghast.clamp();
        happyGhast.clamp();
        elderGuardian.clamp();

        if (disabledMobs == null) disabledMobs = new HashSet<>();
    }

    private static int clampSeconds(int v) {
        return Math.max(MIN_DURATION_SECONDS, Math.min(MAX_DURATION_SECONDS, v));
    }

    public static double clampSize(double v) {
        return Math.max(MIN_SIZE, Math.min(MAX_SIZE, v));
    }

    public static double clampHeightOffset(double v) {
        return Math.max(MIN_HEIGHT_OFFSET, Math.min(MAX_HEIGHT_OFFSET, v));
    }

    /**
     * Loads the config from {@code <configDir>/mobhealthbar.json}, creating it with
     * defaults if absent. Called once per loader during initialization.
     */
    public static void load(Path configDir) {
        configFile = configDir.resolve(FILE_NAME);
        try {
            if (Files.exists(configFile)) {
                String json = Files.readString(configFile);
                HealthBarConfig loaded = GSON.fromJson(json, HealthBarConfig.class);
                if (loaded != null) {
                    loaded.clamp();
                    INSTANCE = loaded;
                }
                save(); // re-write to fill in any missing keys
            } else {
                save();
            }
        } catch (Exception e) {
            MobHealthBar.LOGGER.error("[MobHealthBar] Failed to load config, using defaults", e);
        }
    }

    /** Writes the current config to disk. */
    public static void save() {
        if (configFile == null) return;
        try {
            Files.createDirectories(configFile.getParent());
            Files.writeString(configFile, GSON.toJson(INSTANCE));
        } catch (Exception e) {
            MobHealthBar.LOGGER.error("[MobHealthBar] Failed to save config", e);
        }
    }
}

package com.warwa.mobhealthbar.config;

import net.minecraft.util.StringRepresentable;

/**
 * Where a health bar sits relative to the mob it belongs to.
 *
 * <p>Heights are resolved against the mob's <em>visual</em> height (see
 * {@code MobAnchors}) rather than its bounding box, so bars clear models that
 * are taller than their collision box (ravager, witch hat, mooshroom, ...).
 */
public enum BarPosition implements StringRepresentable {

    /** Above the top of the model. */
    ABOVE("above"),
    /** Halfway up the model, i.e. floating in front of the body. */
    MIDDLE("middle"),
    /** Down at the mob's feet. */
    FEET("feet"),
    /** Below the bottom of the model. Only offered for the special large mobs. */
    BELOW("below");

    public static final StringRepresentable.EnumCodec<BarPosition> CODEC =
            StringRepresentable.fromEnum(BarPosition::values);

    /** The choices offered by the global position option. */
    public static final java.util.List<BarPosition> GLOBAL_VALUES =
            java.util.List.of(ABOVE, MIDDLE, FEET);

    /** The choices offered per special mob (ghast, happy ghast, elder guardian). */
    public static final java.util.List<BarPosition> SPECIAL_VALUES =
            java.util.List.of(ABOVE, MIDDLE, BELOW);

    private final String name;

    BarPosition(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public String translationKey() {
        return "mobhealthbar.position." + name;
    }
}

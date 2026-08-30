package com.warwa.mobhealthbar.client;

import com.warwa.mobhealthbar.config.BarPosition;
import com.warwa.mobhealthbar.config.HealthBarConfig;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.function.DoubleConsumer;

/**
 * Builders for the option widgets shared across the config screens.
 */
public final class ConfigOptions {

    private ConfigOptions() {}

    /** A duration slider labelled in seconds. */
    public static OptionInstance<Integer> seconds(String key, int initial,
                                                  OptionInstance.ValueUpdateListener<Integer> onChange) {
        return new OptionInstance<>(
                key,
                OptionInstance.noTooltip(),
                (caption, value) -> Options.genericValueLabel(caption,
                        Component.translatable("mobhealthbar.seconds", value)),
                new OptionInstance.IntRange(HealthBarConfig.MIN_DURATION_SECONDS, HealthBarConfig.MAX_DURATION_SECONDS, true),
                initial,
                onChange);
    }

    /** A percentage slider (0-100%) bound to a 0..1 double. */
    public static OptionInstance<Integer> percent(String key, double initial, DoubleConsumer onChange) {
        return new OptionInstance<>(
                key,
                OptionInstance.noTooltip(),
                ConfigOptions::percentLabel,
                new OptionInstance.IntRange(0, 100, true),
                (int) Math.round(initial * 100.0),
                value -> onChange.accept(value / 100.0));
    }

    /**
     * A size slider expressed as a percentage of the normal heart size.
     * Bounds come from the config so the JSON and the UI can never disagree.
     */
    public static OptionInstance<Integer> size(String key, double initial, DoubleConsumer onChange) {
        int min = (int) Math.round(HealthBarConfig.MIN_SIZE * 100.0);
        int max = (int) Math.round(HealthBarConfig.MAX_SIZE * 100.0);
        return new OptionInstance<>(
                key,
                OptionInstance.noTooltip(),
                ConfigOptions::percentLabel,
                new OptionInstance.IntRange(min, max, true),
                (int) Math.round(HealthBarConfig.clampSize(initial) * 100.0),
                value -> onChange.accept(value / 100.0));
    }

    /**
     * A vertical nudge slider, in blocks either side of zero.
     *
     * <p>Held as whole {@link HealthBarConfig#HEIGHT_OFFSET_STEP} steps rather than blocks so
     * the slider lands on exact values: an {@code IntRange} over the raw block range would
     * only offer whole blocks, and a float slider would drift to values like 0.1499999.
     */
    public static OptionInstance<Integer> heightOffset(String key, double initial, DoubleConsumer onChange) {
        int min = (int) Math.round(HealthBarConfig.MIN_HEIGHT_OFFSET / HealthBarConfig.HEIGHT_OFFSET_STEP);
        int max = (int) Math.round(HealthBarConfig.MAX_HEIGHT_OFFSET / HealthBarConfig.HEIGHT_OFFSET_STEP);
        int start = (int) Math.round(
                HealthBarConfig.clampHeightOffset(initial) / HealthBarConfig.HEIGHT_OFFSET_STEP);
        return new OptionInstance<>(
                key,
                OptionInstance.noTooltip(),
                ConfigOptions::blocksLabel,
                new OptionInstance.IntRange(min, max, true),
                start,
                value -> onChange.accept(value * HealthBarConfig.HEIGHT_OFFSET_STEP));
    }

    /** A cycling position option restricted to the given choices. */
    public static OptionInstance<BarPosition> position(String key, List<BarPosition> choices, BarPosition initial,
                                                       OptionInstance.ValueUpdateListener<BarPosition> onChange) {
        return new OptionInstance<>(
                key,
                OptionInstance.noTooltip(),
                (caption, value) -> Options.genericValueLabel(caption,
                        Component.translatable(value.translationKey())),
                new OptionInstance.Enum<>(choices, BarPosition.CODEC),
                choices.contains(initial) ? initial : choices.get(0),
                onChange);
    }

    private static Component percentLabel(Component caption, Integer value) {
        return Options.genericValueLabel(caption, Component.literal(value + "%"));
    }

    /** Renders a step count as signed blocks, e.g. {@code +0.15} — zero reads as "0". */
    private static Component blocksLabel(Component caption, Integer steps) {
        double blocks = steps * HealthBarConfig.HEIGHT_OFFSET_STEP;
        String text = steps == 0 ? "0" : String.format(Locale.ROOT, "%+.2f", blocks);
        return Options.genericValueLabel(caption, Component.literal(text));
    }
}

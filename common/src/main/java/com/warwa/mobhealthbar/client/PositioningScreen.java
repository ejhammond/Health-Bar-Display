package com.warwa.mobhealthbar.client;

import com.warwa.mobhealthbar.config.BarPosition;
import com.warwa.mobhealthbar.config.HealthBarConfig;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Global placement and size. These apply to every mob <em>except</em> the ones with their own
 * settings on {@link SpecialMobsScreen}.
 */
public class PositioningScreen extends MobHealthBarScreen {

    public PositioningScreen(Screen lastScreen) {
        super(lastScreen, Component.translatable("mobhealthbar.positioning.title"));
    }

    @Override
    protected void addOptions() {
        HealthBarConfig cfg = HealthBarConfig.get();
        HealthBarConfig def = HealthBarConfig.defaults();

        OptionInstance<BarPosition> position = ConfigOptions.position(
                "mobhealthbar.option.position", BarPosition.GLOBAL_VALUES, cfg.position, v -> cfg.position = v);
        this.list.addSmall(position, null);

        addResettable(
                ConfigOptions.size("mobhealthbar.option.size", cfg.size, v -> cfg.size = v),
                () -> cfg.size = def.size);
        addResettable(
                ConfigOptions.heightOffset("mobhealthbar.option.height_offset", cfg.heightOffset,
                        v -> cfg.heightOffset = v),
                () -> cfg.heightOffset = def.heightOffset);

        this.list.addHeader(Component.translatable("mobhealthbar.category.distance"));

        OptionInstance<Boolean> distanceScaling = OptionInstance.createBoolean(
                "mobhealthbar.option.distance_scaling", cfg.distanceScaling, v -> cfg.distanceScaling = v);
        this.list.addSmall(distanceScaling, null);

        addResettable(
                ConfigOptions.percent("mobhealthbar.option.distance_scaling_amount", cfg.distanceScalingAmount,
                        v -> cfg.distanceScalingAmount = v),
                () -> cfg.distanceScalingAmount = def.distanceScalingAmount);
    }

    @Override
    protected void resetAll() {
        HealthBarConfig cfg = HealthBarConfig.get();
        HealthBarConfig def = HealthBarConfig.defaults();
        applyReset(() -> {
            cfg.position = def.position;
            cfg.size = def.size;
            cfg.heightOffset = def.heightOffset;
            cfg.distanceScaling = def.distanceScaling;
            cfg.distanceScalingAmount = def.distanceScalingAmount;
        });
    }
}

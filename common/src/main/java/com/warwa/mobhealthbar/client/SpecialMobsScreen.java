package com.warwa.mobhealthbar.client;

import com.warwa.mobhealthbar.config.BarPosition;
import com.warwa.mobhealthbar.config.HealthBarConfig;
import com.warwa.mobhealthbar.config.SpecialMobConfig;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Placement and size for the oversized mobs, independent of the global settings.
 *
 * <p>Ghasts, happy ghasts and elder guardians are big enough that a bar sitting above them
 * is easy to miss (and impossible to see from below a ghast), so each gets its own position
 * and size that the global options do not touch.
 */
public class SpecialMobsScreen extends MobHealthBarScreen {

    public SpecialMobsScreen(Screen lastScreen) {
        super(lastScreen, Component.translatable("mobhealthbar.special.title"));
    }

    @Override
    protected void addOptions() {
        HealthBarConfig cfg = HealthBarConfig.get();

        addMob("ghast", cfg.ghast);
        addMob("happy_ghast", cfg.happyGhast);
        addMob("elder_guardian", cfg.elderGuardian);
    }

    private void addMob(String key, SpecialMobConfig mob) {
        SpecialMobConfig def = new SpecialMobConfig();

        this.list.addHeader(Component.translatable("mobhealthbar.special." + key));

        OptionInstance<BarPosition> position = ConfigOptions.position(
                "mobhealthbar.option.position", BarPosition.SPECIAL_VALUES, mob.position, v -> mob.position = v);
        addResettable(position, () -> mob.position = def.position);

        addResettable(
                ConfigOptions.size("mobhealthbar.option.size", mob.size, v -> mob.size = v),
                () -> mob.size = def.size);
        addResettable(
                ConfigOptions.heightOffset("mobhealthbar.option.height_offset", mob.heightOffset,
                        v -> mob.heightOffset = v),
                () -> mob.heightOffset = def.heightOffset);
    }

    @Override
    protected void resetAll() {
        HealthBarConfig cfg = HealthBarConfig.get();
        SpecialMobConfig def = new SpecialMobConfig();
        applyReset(() -> {
            for (SpecialMobConfig mob : List.of(cfg.ghast, cfg.happyGhast, cfg.elderGuardian)) {
                mob.position = def.position;
                mob.size = def.size;
                mob.heightOffset = def.heightOffset;
            }
        });
    }
}

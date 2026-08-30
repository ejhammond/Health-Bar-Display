package com.warwa.mobhealthbar.client;

import com.warwa.mobhealthbar.config.HealthBarConfig;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Root config screen. Shared by both loaders: Fabric surfaces it through Mod Menu,
 * NeoForge through the Mods list config button.
 */
public class HealthBarConfigScreen extends MobHealthBarScreen {

    private static final int BUTTON_WIDTH = 150;

    public HealthBarConfigScreen(Screen lastScreen) {
        super(lastScreen, Component.translatable("mobhealthbar.config.title"));
    }

    @Override
    protected void addOptions() {
        HealthBarConfig cfg = HealthBarConfig.get();
        HealthBarConfig def = HealthBarConfig.defaults();

        this.list.addHeader(Component.translatable("mobhealthbar.category.visibility"));

        OptionInstance<Boolean> enemyOnLook = OptionInstance.createBoolean(
                "mobhealthbar.option.enemy_on_look", cfg.enemyOnLook, v -> cfg.enemyOnLook = v);
        OptionInstance<Boolean> friendlyOnLook = OptionInstance.createBoolean(
                "mobhealthbar.option.friendly_on_look", cfg.friendlyOnLook, v -> cfg.friendlyOnLook = v);
        this.list.addSmall(enemyOnLook, friendlyOnLook);

        // The durations each get their own Reset: they are the settings people drag around
        // while experimenting, and landing back on exactly 20s or 7s by hand is fiddly.
        addResettable(
                ConfigOptions.seconds("mobhealthbar.option.enemy_look_seconds", cfg.enemyLookSeconds,
                        v -> cfg.enemyLookSeconds = v),
                () -> cfg.enemyLookSeconds = def.enemyLookSeconds);
        addResettable(
                ConfigOptions.seconds("mobhealthbar.option.friendly_look_seconds", cfg.friendlyLookSeconds,
                        v -> cfg.friendlyLookSeconds = v),
                () -> cfg.friendlyLookSeconds = def.friendlyLookSeconds);
        addResettable(
                ConfigOptions.seconds("mobhealthbar.option.enemy_hit_seconds", cfg.enemyHitSeconds,
                        v -> cfg.enemyHitSeconds = v),
                () -> cfg.enemyHitSeconds = def.enemyHitSeconds);
        addResettable(
                ConfigOptions.seconds("mobhealthbar.option.friendly_hit_seconds", cfg.friendlyHitSeconds,
                        v -> cfg.friendlyHitSeconds = v),
                () -> cfg.friendlyHitSeconds = def.friendlyHitSeconds);

        this.list.addHeader(Component.translatable("mobhealthbar.category.appearance"));

        OptionInstance<Boolean> showBosses = OptionInstance.createBoolean(
                "mobhealthbar.option.show_bosses", cfg.showBosses, v -> cfg.showBosses = v);
        OptionInstance<Boolean> hideVanillaBossBar = OptionInstance.createBoolean(
                "mobhealthbar.option.hide_vanilla_boss_bar", cfg.hideVanillaBossBar,
                v -> cfg.hideVanillaBossBar = v);
        this.list.addSmall(showBosses, hideVanillaBossBar);

        addResettable(opacityOption(cfg), () -> cfg.opacity = def.opacity);

        this.list.addHeader(Component.translatable("mobhealthbar.category.more"));
        this.list.addSmall(
                subScreenButton("mobhealthbar.positioning.title", PositioningScreen::new),
                subScreenButton("mobhealthbar.special.title", SpecialMobsScreen::new));
        this.list.addSmall(subScreenButton("mobhealthbar.mobs.title", MobToggleScreen::new), null);
    }

    private static OptionInstance<Double> opacityOption(HealthBarConfig cfg) {
        return new OptionInstance<>(
                "mobhealthbar.option.opacity",
                OptionInstance.noTooltip(),
                (caption, value) -> Options.genericValueLabel(caption,
                        Component.literal((int) Math.round(value * 100) + "%")),
                OptionInstance.UnitDouble.INSTANCE,
                cfg.opacity,
                v -> cfg.opacity = v);
    }

    private Button subScreenButton(String key, java.util.function.Function<Screen, Screen> factory) {
        return Button.builder(Component.translatable(key),
                        b -> this.minecraft.setScreenAndShow(factory.apply(this)))
                .width(BUTTON_WIDTH)
                .build();
    }

    @Override
    protected String resetAllKey() {
        return "mobhealthbar.reset_all";
    }

    /**
     * Resets the entire mod rather than just this screen, so it asks first. It also clears the
     * per-mob disable list, which can represent a lot of clicking that would be tedious to redo.
     */
    @Override
    protected void resetAll() {
        this.minecraft.setScreenAndShow(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        HealthBarConfig.resetAll();
                    }
                    // Re-showing this instance runs repositionElements() rather than init(),
                    // so the explicit rebuild is what actually refreshes the rows.
                    this.minecraft.setScreenAndShow(this);
                    if (confirmed) {
                        this.rebuild();
                    }
                },
                Component.translatable("mobhealthbar.reset_all.title"),
                Component.translatable("mobhealthbar.reset_all.message")));
    }
}

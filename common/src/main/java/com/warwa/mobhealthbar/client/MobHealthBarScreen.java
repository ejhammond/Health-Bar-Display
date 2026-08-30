package com.warwa.mobhealthbar.client;

import com.warwa.mobhealthbar.config.HealthBarConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Shared plumbing for the mod's option screens: saving on close, rebuilding rows in place, and
 * the Reset buttons that put one option — or a whole screen — back to its default.
 */
public abstract class MobHealthBarScreen extends OptionsSubScreen {

    /**
     * Width of a per-option Reset button.
     *
     * <p>An {@code OptionsList} row lays its widgets out from {@code centre - 155} in steps of
     * 160, so a standard 150-wide option ends 10px short of the second slot. A narrow button
     * dropped into that slot sits right beside its option and reads as belonging to it, rather
     * than as a second column of settings.
     */
    private static final int RESET_WIDTH = 45;

    private static final int FOOTER_BUTTON_WIDTH = 150;

    protected MobHealthBarScreen(Screen lastScreen, Component title) {
        super(lastScreen, Minecraft.getInstance().options, title);
    }

    /**
     * Rebuilds every row from the current config.
     *
     * <p>Clearing the layout first is not optional — {@code OptionsSubScreen.layout} is a final
     * field that {@code init()} appends to, and {@code rebuildWidgets()} does not touch it. See
     * {@link MobToggleScreen#rebuild()} for what that looks like when it goes wrong.
     */
    protected void rebuild() {
        rebuild(true);
    }

    /**
     * @param keepScroll whether to restore the scroll position afterwards. True whenever the
     *                   rows stay the same and only a value changed — being thrown back to the
     *                   top of the list because you clicked Reset on one row is jarring, and on
     *                   a long screen it loses your place entirely. False when the set of rows
     *                   itself changes, such as a search filter, where the old offset points at
     *                   nothing meaningful.
     */
    protected void rebuild(boolean keepScroll) {
        // Read before the rebuild: it replaces the OptionsList with a fresh one at scroll 0.
        double scroll = this.list != null ? this.list.scrollAmount() : 0.0;

        this.layout.removeChildren();
        this.rebuildWidgets();

        // After rebuildWidgets(), so the new list has been arranged and can clamp this against
        // its real content height.
        if (keepScroll && this.list != null) {
            this.list.setScrollAmount(scroll);
        }
    }

    /**
     * Adds an option with a Reset button beside it that returns just that option to its default.
     *
     * <p>{@code reset} writes the default into the config and the screen is then rebuilt, rather
     * than the widget being set directly: an {@code OptionInstance} owns its value independently
     * of the config field behind it, so moving one without the other leaves the slider showing a
     * number that is no longer true.
     */
    protected void addResettable(OptionInstance<?> option, Runnable reset) {
        this.list.addSmall(option.createButton(this.options), resetButton(reset));
    }

    /** A Reset button that applies {@code reset}, saves, and redraws the screen. */
    protected Button resetButton(Runnable reset) {
        return Button.builder(Component.translatable("mobhealthbar.reset"), b -> {
            applyPending();
            applyReset(reset);
        }).width(RESET_WIDTH).build();
    }

    /** Writes a reset into the config, persists it, and redraws the rows that show it. */
    protected void applyReset(Runnable reset) {
        reset.run();
        HealthBarConfig.save();
        this.rebuild();
    }

    /**
     * Lands pending slider edits in the config, so resetting one option cannot silently
     * discard an adjustment just made to another.
     */
    private void applyPending() {
        if (this.list != null) {
            this.list.applyUnsavedChanges();
        }
    }

    /**
     * The default footer: Done, preceded by a reset covering everything on this screen.
     *
     * <p>The button hands straight off to {@link #resetAll()} rather than wrapping it, because
     * a screen may want to ask for confirmation first and therefore has to own what happens
     * afterwards.
     */
    @Override
    protected void addFooter() {
        LinearLayout row = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        row.addChild(Button.builder(Component.translatable(resetAllKey()), b -> {
                    applyPending();
                    resetAll();
                })
                .width(FOOTER_BUTTON_WIDTH)
                .build());
        row.addChild(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
                .width(FOOTER_BUTTON_WIDTH)
                .build());
    }

    /** Translation key for this screen's wholesale reset button. */
    protected String resetAllKey() {
        return "mobhealthbar.reset_screen";
    }

    /** Puts every option this screen owns back to its default. */
    protected abstract void resetAll();

    @Override
    public void onClose() {
        if (this.list != null) {
            this.list.applyUnsavedChanges();
        }
        HealthBarConfig.save();
        this.minecraft.gui.setScreen(this.lastScreen);
    }
}

package com.warwa.mobhealthbar.client;

import com.warwa.mobhealthbar.config.HealthBarConfig;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Per-mob on/off toggles, with a search box because there are well over a hundred of them.
 */
public class MobToggleScreen extends MobHealthBarScreen {

    private static final int TOGGLE_WIDTH = 150;

    private EditBox search;
    private String filter = "";

    public MobToggleScreen(Screen lastScreen) {
        super(lastScreen, Component.translatable("mobhealthbar.mobs.title"));
    }

    @Override
    protected void addTitle() {
        this.layout.addTitleHeader(this.title, this.font);

        // Built once and re-attached on every rebuild. Creating a fresh EditBox per keystroke
        // would throw away the caret position along with the widget.
        if (this.search == null) {
            this.search = new EditBox(this.font, 200, 20, Component.translatable("mobhealthbar.mobs.search"));
            this.search.setHint(Component.translatable("mobhealthbar.mobs.search"));
            // Set the value before attaching the responder: a responder firing on setValue
            // would re-enter rebuild().
            this.search.setValue(this.filter);
            this.search.setResponder(text -> {
                if (text.equals(this.filter)) return;
                this.filter = text;
                // Back to the top: a new search means a different set of rows, so holding the
                // old offset would drop you into the middle of results you have not seen.
                this.rebuild(false);
            });
        }
        this.layout.addToHeader(this.search);
    }

    @Override
    protected void addOptions() {
        this.populate();
    }

    /**
     * Rebuilds the visible rows after the search text changes.
     *
     * <p>Clearing the layout first is not optional. {@code OptionsSubScreen.layout} is a final
     * field built once, but {@code init()} appends a brand new {@code OptionsList} to it on
     * every call, and {@code rebuildWidgets()} only clears the screen's own widget list — not
     * the layout's. Without this, each keystroke left another fully populated list behind, all
     * of them re-registered and drawn on top of one another: the non-matching rows stayed on
     * screen and merely dimmed a little further under every new list's background.
     *
     * <p>Focus has to be restored afterwards because {@code rebuildWidgets()} drops it, which
     * would otherwise swallow the next keypress.
     */
    @Override
    protected void rebuild(boolean keepScroll) {
        super.rebuild(keepScroll);
        this.setFocused(this.search);
        this.search.setFocused(true);
    }

    private void populate() {
        HealthBarConfig cfg = HealthBarConfig.get();
        String needle = this.filter.trim().toLowerCase(Locale.ROOT);

        List<AbstractWidget> toggles = new ArrayList<>();
        for (EntityType<?> type : MobRegistry.livingTypes()) {
            if (!matches(type, needle)) continue;
            toggles.add(toggleFor(cfg, type));
        }

        if (toggles.isEmpty()) {
            this.list.addHeader(Component.translatable("mobhealthbar.mobs.none"));
            return;
        }
        this.list.addSmall(toggles);
    }

    private static boolean matches(EntityType<?> type, String needle) {
        if (needle.isEmpty()) return true;
        // Match the display name or the registry id, so both "creeper" and "minecraft:creeper" work.
        return type.getDescription().getString().toLowerCase(Locale.ROOT).contains(needle)
                || HealthBarConfig.idOf(type).toLowerCase(Locale.ROOT).contains(needle);
    }

    private AbstractWidget toggleFor(HealthBarConfig cfg, EntityType<?> type) {
        return CycleButton.onOffBuilder(cfg.isMobEnabled(type))
                .create(0, 0, TOGGLE_WIDTH, 20, type.getDescription(),
                        (button, enabled) -> cfg.setMobEnabled(type, enabled));
    }

    @Override
    protected String resetAllKey() {
        return "mobhealthbar.mobs.enable_all";
    }

    /** Every mob back on, which is this screen's only default. */
    @Override
    protected void resetAll() {
        applyReset(() -> HealthBarConfig.get().disabledMobs.clear());
    }

    /**
     * The toggles write straight through to the config as they are clicked, so unlike the
     * slider screens there is nothing pending to apply on the way out.
     */
    @Override
    public void onClose() {
        HealthBarConfig.save();
        this.minecraft.gui.setScreen(this.lastScreen);
    }
}

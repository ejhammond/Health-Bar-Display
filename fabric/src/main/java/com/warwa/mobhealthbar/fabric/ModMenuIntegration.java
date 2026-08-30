package com.warwa.mobhealthbar.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.warwa.mobhealthbar.client.HealthBarConfigScreen;

/**
 * Exposes the config screen through Mod Menu's "Configure" button on Fabric.
 * Mod Menu is an optional dependency: this entrypoint is only invoked when it is installed.
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return HealthBarConfigScreen::new;
    }
}

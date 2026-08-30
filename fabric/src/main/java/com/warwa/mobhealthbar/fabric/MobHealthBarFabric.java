package com.warwa.mobhealthbar.fabric;

import com.warwa.mobhealthbar.MobHealthBar;
import com.warwa.mobhealthbar.config.HealthBarConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class MobHealthBarFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        MobHealthBar.init();
        HealthBarConfig.load(FabricLoader.getInstance().getConfigDir());
    }
}

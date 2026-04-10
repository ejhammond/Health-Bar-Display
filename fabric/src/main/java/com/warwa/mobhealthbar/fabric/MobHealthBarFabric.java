package com.warwa.mobhealthbar.fabric;

import com.warwa.mobhealthbar.MobHealthBar;
import net.fabricmc.api.ModInitializer;

public class MobHealthBarFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        MobHealthBar.init();
    }
}

package com.warwa.mobhealthbar.neoforge;

import com.warwa.mobhealthbar.MobHealthBar;
import com.warwa.mobhealthbar.client.HealthBarConfigScreen;
import com.warwa.mobhealthbar.config.HealthBarConfig;
import com.warwa.mobhealthbar.neoforge.client.ClientEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(MobHealthBar.MOD_ID)
public class MobHealthBarNeoForge {

    public MobHealthBarNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        MobHealthBar.init();
        HealthBarConfig.load(FMLPaths.CONFIGDIR.get());

        // Client-only mod - no dist check needed (declared in mods.toml)
        modEventBus.addListener(ClientEvents::onAddLayers);
        modEventBus.addListener(ClientEvents::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onClientTick);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onLivingDamage);

        // Config button in the Mods list
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (container, modListScreen) -> new HealthBarConfigScreen(modListScreen));
    }
}

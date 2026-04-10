package com.warwa.mobhealthbar.neoforge;

import com.warwa.mobhealthbar.MobHealthBar;
import com.warwa.mobhealthbar.neoforge.client.ClientEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(MobHealthBar.MOD_ID)
public class MobHealthBarNeoForge {

    public MobHealthBarNeoForge(IEventBus modEventBus) {
        MobHealthBar.init();

        // Client-only mod - no dist check needed (declared in mods.toml)
        modEventBus.addListener(ClientEvents::onAddLayers);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onClientTick);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onLivingDamage);
    }
}

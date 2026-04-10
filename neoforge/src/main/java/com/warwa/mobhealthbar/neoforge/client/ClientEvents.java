package com.warwa.mobhealthbar.neoforge.client;

import com.warwa.mobhealthbar.MobHealthBar;
import com.warwa.mobhealthbar.client.HealthBarRenderLayer;
import com.warwa.mobhealthbar.tracking.EntityHealthTracker;
import com.warwa.mobhealthbar.tracking.HostilityClassifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

public final class ClientEvents {

    private static boolean debugDamage = false;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (EntityType<?> type : event.getEntityTypes()) {
            EntityRenderer<?, ?> renderer = event.getRenderer(type);
            if (renderer instanceof LivingEntityRenderer livingRenderer) {
                livingRenderer.addLayer(new HealthBarRenderLayer<>(livingRenderer));
            }
        }

        for (var skinType : event.getSkins()) {
            var playerRenderer = event.getPlayerRenderer(skinType);
            if (playerRenderer != null) {
                playerRenderer.addLayer(new HealthBarRenderLayer<>(playerRenderer));
            }
        }
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        EntityHealthTracker.INSTANCE.tick();

        // Check if looking at a hostile mob
        if (mc.hitResult instanceof EntityHitResult entityHit) {
            if (entityHit.getEntity() instanceof LivingEntity living) {
                boolean hostile = HostilityClassifier.isHostile(living);
                if (hostile) {
                    EntityHealthTracker.INSTANCE.onEntityLookedAt(living.getId(), true);
                }
            }
        }

        // Update health for ALL tracked entities from the client world
        // This ensures health values stay current even after the initial damage event
        EntityHealthTracker.INSTANCE.updateAllHealthFromWorld(mc.level);
    }

    /**
     * Detect when the local player damages an entity.
     * NOTE: This event fires on the SERVER thread (integrated server).
     * The event's player entity is a ServerPlayer, not the client's LocalPlayer.
     * We compare by UUID, not by reference.
     */
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Compare by UUID since ServerPlayer != LocalPlayer (different objects)
        if (event.getSource().getEntity() instanceof Player player
                && player.getUUID().equals(mc.player.getUUID())) {
            LivingEntity target = event.getEntity();
            boolean hostile = HostilityClassifier.isHostile(target);

            if (!debugDamage) {
                MobHealthBar.LOGGER.info("[MobHealthBar] Damage detected! target={}, hostile={}, health={}/{}",
                        target.getType().getDescriptionId(), hostile, target.getHealth(), target.getMaxHealth());
                debugDamage = true;
            }

            EntityHealthTracker.INSTANCE.onEntityDamaged(target.getId(), hostile);
        }
    }

    private ClientEvents() {}
}

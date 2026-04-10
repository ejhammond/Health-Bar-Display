package com.warwa.mobhealthbar.fabric;

import com.warwa.mobhealthbar.client.HealthBarRenderLayer;
import com.warwa.mobhealthbar.tracking.EntityHealthTracker;
import com.warwa.mobhealthbar.tracking.HostilityClassifier;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

public class MobHealthBarFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register the health bar render layer on all living entity renderers
        LivingEntityRenderLayerRegistrationCallback.EVENT.register(
                (entityType, entityRenderer, registrationHelper, context) -> {
                    @SuppressWarnings("unchecked")
                    var layer = (net.minecraft.client.renderer.entity.layers.RenderLayer) new HealthBarRenderLayer<>(entityRenderer);
                    registrationHelper.register(layer);
                }
        );

        // Client tick: update tracker + crosshair check
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null || client.player == null) return;

            EntityHealthTracker.INSTANCE.tick();

            // Check if looking at a hostile mob
            if (client.hitResult instanceof EntityHitResult entityHit) {
                if (entityHit.getEntity() instanceof LivingEntity living) {
                    boolean hostile = HostilityClassifier.isHostile(living);
                    if (hostile) {
                        EntityHealthTracker.INSTANCE.onEntityLookedAt(living.getId(), true);
                    }
                }
            }

            // Update health for ALL tracked entities from the client world
            EntityHealthTracker.INSTANCE.updateAllHealthFromWorld(client.level);
        });

        // Detect when player attacks an entity
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide() && entity instanceof LivingEntity living) {
                boolean hostile = HostilityClassifier.isHostile(living);
                EntityHealthTracker.INSTANCE.onEntityDamaged(living.getId(), hostile);
                var tracked = EntityHealthTracker.INSTANCE.getTrackedEntity(living.getId());
                if (tracked != null) {
                    tracked.updateHealth(living.getHealth());
                }
            }
            return InteractionResult.PASS;
        });
    }
}

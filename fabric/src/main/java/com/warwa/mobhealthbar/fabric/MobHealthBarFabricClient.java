package com.warwa.mobhealthbar.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import com.warwa.mobhealthbar.client.HealthBarConfigScreen;
import com.warwa.mobhealthbar.client.HealthBarMeasureLayer;
import com.warwa.mobhealthbar.tracking.EntityHealthTracker;
import com.warwa.mobhealthbar.tracking.HealthBarRules;
import com.warwa.mobhealthbar.tracking.HostilityClassifier;
import com.warwa.mobhealthbar.tracking.TargetResolver;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

public class MobHealthBarFabricClient implements ClientModInitializer {

    private static KeyMapping openConfigKey;

    @Override
    public void onInitializeClient() {
        // Keybind (unbound by default) to open the config screen in-game
        openConfigKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.mobhealthbar.open_config",
                InputConstants.Type.KEYBOARD,
                InputConstants.UNKNOWN.getValue(),
                KeyMapping.Category.MISC));

        // The bar itself is drawn from a mixin on EntityRenderer, where the pose is clean.
        // This layer only measures each model's true visual height (see HealthBarMeasureLayer).
        LivingEntityRenderLayerRegistrationCallback.EVENT.register(
                (entityType, entityRenderer, registrationHelper, context) -> {
                    @SuppressWarnings("unchecked")
                    var layer = (net.minecraft.client.renderer.entity.layers.RenderLayer) new HealthBarMeasureLayer<>(entityRenderer);
                    registrationHelper.register(layer);
                }
        );

        // Client tick: update tracker + crosshair check
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigKey.consumeClick()) {
                client.setScreenAndShow(new HealthBarConfigScreen(null));
            }

            if (client.level == null || client.player == null) return;

            EntityHealthTracker.INSTANCE.tick();

            // Check if looking at a mob the config says should show on look
            if (client.hitResult instanceof EntityHitResult entityHit) {
                LivingEntity living = TargetResolver.resolve(entityHit.getEntity());
                if (living != null && HealthBarRules.showOnLook(living)) {
                    boolean hostile = HostilityClassifier.isHostile(living);
                    EntityHealthTracker.INSTANCE.onEntityLookedAt(living.getId(), hostile);
                }
            }

            // Update health for ALL tracked entities from the client world
            EntityHealthTracker.INSTANCE.updateAllHealthFromWorld(client.level);
        });

        // Detect when player attacks an entity
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClientSide()) {
                return InteractionResult.PASS;
            }
            // Attacking a dragon hands us one of its parts, not the dragon itself.
            LivingEntity living = TargetResolver.resolve(entity);
            if (living != null && HealthBarRules.showOnHit(living)) {
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

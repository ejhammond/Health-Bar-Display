package com.warwa.mobhealthbar.neoforge.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.warwa.mobhealthbar.MobHealthBar;
import com.warwa.mobhealthbar.client.HealthBarConfigScreen;
import com.warwa.mobhealthbar.client.HealthBarMeasureLayer;
import com.warwa.mobhealthbar.tracking.EntityHealthTracker;
import com.warwa.mobhealthbar.tracking.HealthBarRules;
import com.warwa.mobhealthbar.tracking.HostilityClassifier;
import com.warwa.mobhealthbar.tracking.TargetResolver;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

public final class ClientEvents {

    private static KeyMapping openConfigKey;

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        openConfigKey = new KeyMapping(
                "key.mobhealthbar.open_config",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                KeyMapping.Category.MISC);
        event.register(openConfigKey);
    }

    /**
     * The bar itself is drawn from a mixin on EntityRenderer, where the pose is clean.
     * These layers only measure each model's true visual height (see HealthBarMeasureLayer).
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (EntityType<?> type : event.getEntityTypes()) {
            EntityRenderer<?, ?> renderer = event.getRenderer(type);
            if (renderer instanceof LivingEntityRenderer livingRenderer) {
                livingRenderer.addLayer(new HealthBarMeasureLayer<>(livingRenderer));
            }
        }

        for (var skinType : event.getSkins()) {
            var playerRenderer = event.getPlayerRenderer(skinType);
            if (playerRenderer != null) {
                playerRenderer.addLayer(new HealthBarMeasureLayer<>(playerRenderer));
            }
        }
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        if (openConfigKey != null) {
            while (openConfigKey.consumeClick()) {
                mc.setScreenAndShow(new HealthBarConfigScreen(null));
            }
        }

        if (mc.level == null || mc.player == null) return;

        EntityHealthTracker.INSTANCE.tick();

        // Check if looking at a mob the config says should show on look.
        // Looking at a dragon hands us one of its parts, not the dragon itself.
        if (mc.hitResult instanceof EntityHitResult entityHit) {
            LivingEntity living = TargetResolver.resolve(entityHit.getEntity());
            if (living != null && HealthBarRules.showOnLook(living)) {
                boolean hostile = HostilityClassifier.isHostile(living);
                EntityHealthTracker.INSTANCE.onEntityLookedAt(living.getId(), hostile);
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
            if (!HealthBarRules.showOnHit(target)) {
                return;
            }
            boolean hostile = HostilityClassifier.isHostile(target);
            EntityHealthTracker.INSTANCE.onEntityDamaged(target.getId(), hostile);
        }
    }

    private ClientEvents() {}
}

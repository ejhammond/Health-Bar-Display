package com.warwa.mobhealthbar.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Lists every mob that can have a health bar, for the per-mob toggle screen.
 *
 * <p>Filtering on {@code MobCategory} would be the obvious approach and is subtly wrong:
 * iron golems and copper golems are registered as {@link net.minecraft.world.entity.MobCategory#MISC},
 * so {@code category != MISC} silently drops them — two of the mobs people most want to toggle.
 * There is no {@code EntityType.isLiving()} either.
 *
 * <p>Instead we ask the renderer registry. {@code EntityRenderDispatcher.getRenderer(state)}
 * only reads {@code state.entityType}, so a throwaway state is enough to look up which
 * renderer a type uses, and anything drawn by a {@code LivingEntityRenderer} is a mob.
 * This also picks up modded mobs for free.
 */
public final class MobRegistry {

    private static List<EntityType<?>> cached;

    private MobRegistry() {}

    /** All mobs that can show a bar, sorted by display name. */
    public static List<EntityType<?>> livingTypes() {
        if (cached != null) return cached;

        Minecraft mc = Minecraft.getInstance();
        EntityRenderState probe = new EntityRenderState();
        List<EntityType<?>> types = new ArrayList<>();

        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            probe.entityType = type;
            EntityRenderer<?, ?> renderer;
            try {
                renderer = mc.getEntityRenderDispatcher().getRenderer(probe);
            } catch (Exception e) {
                continue; // A misbehaving modded renderer must not break the whole screen.
            }
            // The Ender Dragon is a LivingEntity but its renderer extends EntityRenderer
            // directly, so it would fall through the instanceof check.
            if (renderer instanceof LivingEntityRenderer<?, ?, ?> || type == EntityTypes.ENDER_DRAGON) {
                types.add(type);
            }
        }

        types.sort(Comparator.comparing(t -> t.getDescription().getString()));
        cached = List.copyOf(types);
        return cached;
    }

    /** Renderers are rebuilt on resource reload, so drop the cache with them. */
    public static void invalidate() {
        cached = null;
    }
}

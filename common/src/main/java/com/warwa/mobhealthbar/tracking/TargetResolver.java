package com.warwa.mobhealthbar.tracking;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;

/**
 * Maps whatever the player actually looked at or hit onto the mob that owns the health bar.
 */
public final class TargetResolver {

    private TargetResolver() {}

    /**
     * The living entity a bar should be shown for, or null if there isn't one.
     *
     * <p>The Ender Dragon is the reason this exists. Its hitbox is made of
     * {@link EnderDragonPart}s, which are plain entities rather than {@code LivingEntity}s, so
     * looking at or attacking a dragon hands you a part and a naive {@code instanceof
     * LivingEntity} check quietly fails. The dragon then never gets tracked and never gets a
     * bar, no matter how correct the rendering side is.
     */
    public static LivingEntity resolve(Entity entity) {
        if (entity instanceof EnderDragonPart part) {
            return part.parentMob;
        }
        return entity instanceof LivingEntity living ? living : null;
    }
}

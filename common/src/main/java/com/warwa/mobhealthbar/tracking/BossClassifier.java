package com.warwa.mobhealthbar.tracking;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;

/**
 * Identifies vanilla boss entities (those that display a boss bar).
 */
public final class BossClassifier {

    public static boolean isBoss(LivingEntity entity) {
        return entity instanceof EnderDragon || entity instanceof WitherBoss;
    }

    private BossClassifier() {}
}

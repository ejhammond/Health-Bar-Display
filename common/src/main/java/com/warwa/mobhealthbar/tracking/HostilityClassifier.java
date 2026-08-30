package com.warwa.mobhealthbar.tracking;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;

public final class HostilityClassifier {

    public static boolean isHostile(LivingEntity entity) {
        if (entity instanceof ArmorStand) {
            return false;
        }
        if (entity instanceof Enemy) {
            return true;
        }
        // Load-bearing, not a safety net: Sulfur Cube is MONSTER-category but does not
        // implement Enemy, so it reaches here. Don't collapse this into the check above.
        return entity.getType().getCategory() == MobCategory.MONSTER;
    }

    private HostilityClassifier() {}
}

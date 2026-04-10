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
        return entity.getType().getCategory() == MobCategory.MONSTER;
    }

    private HostilityClassifier() {}
}

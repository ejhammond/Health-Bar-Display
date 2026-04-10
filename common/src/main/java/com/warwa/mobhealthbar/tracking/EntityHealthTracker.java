package com.warwa.mobhealthbar.tracking;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.concurrent.ConcurrentHashMap;

public final class EntityHealthTracker {

    public static final EntityHealthTracker INSTANCE = new EntityHealthTracker();

    private final ConcurrentHashMap<Integer, TrackedEntity> trackedEntities = new ConcurrentHashMap<>();

    private EntityHealthTracker() {}

    public void onEntityDamaged(int entityId, boolean isHostile) {
        trackedEntities.compute(entityId, (id, existing) -> {
            if (existing != null) {
                existing.refresh(isHostile);
                return existing;
            }
            return new TrackedEntity(entityId, isHostile);
        });
    }

    public void onEntityLookedAt(int entityId, boolean isHostile) {
        // Only show for hostile mobs when looking at them
        if (!isHostile) return;

        trackedEntities.compute(entityId, (id, existing) -> {
            if (existing != null) {
                existing.refresh(isHostile);
                return existing;
            }
            return new TrackedEntity(entityId, isHostile);
        });
    }

    public void tick() {
        trackedEntities.values().removeIf(tracked -> {
            tracked.tick();
            return tracked.isHidden();
        });
    }

    public TrackedEntity getTrackedEntity(int entityId) {
        return trackedEntities.get(entityId);
    }

    public boolean isTracked(int entityId) {
        return trackedEntities.containsKey(entityId);
    }

    /**
     * Updates health for all tracked entities by looking them up in the client world.
     * Called every client tick to keep health values current.
     */
    public void updateAllHealthFromWorld(Level level) {
        for (var entry : trackedEntities.entrySet()) {
            Entity entity = level.getEntity(entry.getKey());
            if (entity instanceof LivingEntity living) {
                entry.getValue().updateHealth(living.getHealth());
            }
        }
    }

    public void clear() {
        trackedEntities.clear();
    }
}

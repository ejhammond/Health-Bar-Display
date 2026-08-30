package com.warwa.mobhealthbar.client;

import com.warwa.mobhealthbar.config.BarPosition;
import com.warwa.mobhealthbar.config.HealthBarConfig;
import com.warwa.mobhealthbar.config.SpecialMobConfig;
import net.minecraft.world.entity.EntityType;

/**
 * Turns the config plus a mob's measured visual height into the two numbers the renderer
 * needs: where the bar's bottom edge sits above the entity's feet, and how big to draw it.
 */
public final class HealthBarAnchor {

    private HealthBarAnchor() {}

    /**
     * Y offset from the entity's feet to the bottom of the bar.
     *
     * @param visualTop the mob's visual height in blocks (see {@link ModelExtents})
     * @param visualMiddle the vertical centre of the model, which is not simply half its
     *                     height — a ghast's body hangs below its feet
     */
    public static float anchorY(EntityType<?> type, float visualTop, float visualMiddle,
                                HealthBarConfig cfg) {
        SpecialMobConfig special = cfg.specialFor(type);
        BarPosition position = special != null ? special.position : cfg.position;

        float base = switch (position) {
            case ABOVE -> visualTop + HealthBarConfig.ABOVE_GAP;
            case MIDDLE -> visualMiddle;
            case FEET -> 0.0f;
            // The bar hangs under the model, so its bottom sits a bar-height further down.
            case BELOW -> -HealthBarConfig.BELOW_GAP - barHeight(cfg, special);
        };

        // Applied to every position, not just ABOVE: a bar at the feet or hanging below a
        // ghast is just as likely to want nudging as one over a mob's head.
        double offset = special != null ? special.heightOffset : cfg.heightOffset;
        return base + (float) offset;
    }

    /** Final heart scale: base size, times the global or special size, times distance scaling. */
    public static float scale(EntityType<?> type, HealthBarConfig cfg, double distanceSq) {
        SpecialMobConfig special = cfg.specialFor(type);
        double size = special != null ? special.size : cfg.size;
        return (float) (HealthBarRenderer.BASE_SCALE * size * distanceFactor(cfg, distanceSq));
    }

    /**
     * Bars shrink with distance like anything else in 3D. Ghasts in particular are usually
     * far away, so optionally grow the bar to keep it readable.
     *
     * <p>An amount of 1.0 grows the bar in exact proportion to distance, holding its
     * on-screen size constant; 0.0 leaves perspective alone.
     */
    private static double distanceFactor(HealthBarConfig cfg, double distanceSq) {
        if (!cfg.distanceScaling || cfg.distanceScalingAmount <= 0.0) {
            return 1.0;
        }
        double distance = Math.sqrt(Math.max(distanceSq, 0.0));
        if (distance <= HealthBarConfig.DISTANCE_SCALING_START) {
            return 1.0;
        }
        double ratio = distance / HealthBarConfig.DISTANCE_SCALING_START;
        // Interpolate between "no correction" (1.0) and "fully constant on screen" (ratio).
        return 1.0 + (ratio - 1.0) * cfg.distanceScalingAmount;
    }

    /** Height of the drawn bar in blocks, used to hang a BELOW bar clear of the model. */
    private static float barHeight(HealthBarConfig cfg, SpecialMobConfig special) {
        double size = special != null ? special.size : cfg.size;
        return (float) (10.0 * HealthBarRenderer.BASE_SCALE * size);
    }
}

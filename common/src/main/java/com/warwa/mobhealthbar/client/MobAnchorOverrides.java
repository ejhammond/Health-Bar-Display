package com.warwa.mobhealthbar.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;

/**
 * The two mobs {@link ModelExtents} structurally cannot measure.
 *
 * <p>Everything else is measured from its baked model, so this deliberately stays tiny —
 * a per-mob constant is a maintenance burden and silently rots when Mojang edits a model.
 * Only add an entry when measurement genuinely cannot see the geometry.
 */
public final class MobAnchorOverrides {

    /**
     * A mooshroom's mushrooms are submitted as <em>block</em> models by its render layer, not
     * as {@code ModelPart} cubes, so the cube walk cannot see them and reports only the cow's
     * horns at ~1.56.
     *
     * <p>Derived from the layer's real transforms rather than the mushroom's bounding cube.
     * The tallest of the three is the one parented to the head: the head pivot sits 4/16 up,
     * the layer then translates -0.7 and centres the block, which places a block-space height
     * {@code by} at {@code 1.451 + by} above the mob's feet.
     *
     * <p>The trap is {@code by}. A mushroom is a {@code block/cross} model whose quads span the
     * full 0..16 of the cube, so taking the cube top gives 2.451 — and 2.351 for the two body
     * mushrooms, which is the value this constant used to hold. But the cross is mostly
     * transparent: the topmost non-blank row of {@code red_mushroom.png} is row 9 of 16, so the
     * mushroom's <em>visible</em> top is only 7/16 up its cube. That puts the real top at
     * 1.451 + 0.4375 = 1.889; everything above it was a bar floating over blank pixels.
     */
    private static final float MOOSHROOM_TOP = 1.9f;

    /**
     * The dragon's renderer extends {@code EntityRenderer} and therefore has no render layers,
     * so the measure layer never runs for it. Its bounding box is 8 blocks tall but the visible
     * body sits in the bottom portion, so the box would float the bar in empty air.
     */
    private static final float ENDER_DRAGON_TOP = 3.63f;

    private MobAnchorOverrides() {}

    /**
     * A visual height for types measurement cannot handle, or NaN to use the measured value.
     *
     * @param measured the measured height, used as a floor so an override never lowers a bar
     */
    public static float visualTop(EntityType<?> type, EntityRenderState state, float measured) {
        if (type == EntityTypes.ENDER_DRAGON) {
            return ENDER_DRAGON_TOP;
        }
        if (type == EntityTypes.MOOSHROOM && !isBaby(state)) {
            // Calves render no mushrooms at all, so they use the plain measured cow height.
            return Float.isNaN(measured) ? MOOSHROOM_TOP : Math.max(measured, MOOSHROOM_TOP);
        }
        return Float.NaN;
    }

    private static boolean isBaby(EntityRenderState state) {
        return state instanceof LivingEntityRenderState living && living.isBaby;
    }
}

package com.warwa.mobhealthbar.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Measures how tall a mob actually <em>looks</em>, which is often nothing like its bounding
 * box: a rabbit's ears reach 1.06 blocks against a 0.6 box, and a witch's hat tip reaches
 * 2.77 against a 1.95 box. Anchoring bars to {@code boundingBoxHeight} is what makes them
 * clip through ears, hats, shells, horns and antennae.
 *
 * <p>Rather than hard-coding hand-measured constants — which would cover only the handful of
 * mobs someone thought to check, and nothing from other mods — we walk the baked model and
 * transform each cube's eight corners.
 *
 * <p>Crucially the walk uses the <em>render layer's own</em> PoseStack, not a fresh one.
 * By the time a layer runs, {@code LivingEntityRenderer.submit} has applied the entity scale,
 * the per-renderer {@code scale()} override and {@code setupRotations()}. Those first two are
 * a genuine part of the mob's rendered size — a wither is drawn at {@code scale(2.0)}, so
 * measuring pure model space would report 1.75 blocks for a boss that really stands 3.5 and
 * bury its bar in its chest. Rotation matters too: a cod beached on its side is genuinely
 * short, and its bar should sit low.
 *
 * <p>Because the measurement is taken in the layer's frame, it is expressed relative to
 * {@link #captureBase} — the clean pose the bar itself is drawn from.
 */
public final class ModelExtents {

    /**
     * The entity's render origin in the layer's coordinate frame, captured at the top of
     * {@code LivingEntityRenderer.submit} before any model transforms are applied.
     * Entity rendering is single-threaded, and the capture always immediately precedes the
     * layers that read it.
     */
    private static final Vector3f BASE = new Vector3f();
    private static boolean baseValid = false;

    private ModelExtents() {}

    /** Records the clean pose the bar will be drawn from, for the layer to measure against. */
    public static void captureBase(Matrix4f cleanPose) {
        cleanPose.transformPosition(0.0f, 0.0f, 0.0f, BASE);
        baseValid = true;
    }

    public static void clearBase() {
        baseValid = false;
    }

    /**
     * Measures the model through {@code poseStack}, relative to the entity's render origin.
     *
     * @param poseStack the render layer's pose, carrying the full entity transform chain
     * @return the measured extent, or null if the model contributed no geometry
     */
    public static Extent measure(PoseStack poseStack, EntityModel<?> model) {
        if (!baseValid) return null;

        ExtentVisitor visitor = new ExtentVisitor(BASE);
        model.root().visit(poseStack, visitor);

        if (visitor.maxY == Float.NEGATIVE_INFINITY) return null;
        return new Extent(visitor.maxY, visitor.minY, (float) Math.sqrt(visitor.maxRadiusSq));
    }

    /**
     * A mob's rendered size relative to its feet.
     *
     * @param top height in blocks of the highest point of the model
     * @param bottom height in blocks of the lowest point, which can be negative — a ghast's
     *               body hangs below its feet
     * @param radius horizontal distance in blocks from the entity's centre to the widest point
     */
    public record Extent(float top, float bottom, float radius) {

        /** The vertical centre of the model, which is not simply half its height. */
        public float middle() {
            return (top + bottom) * 0.5f;
        }
    }

    /**
     * Tracks the highest and widest points across every cube corner. The model's own Y-down
     * convention has already been flipped by the transforms on the layer's pose, so the
     * maximum Y here is simply the highest point of the mob as drawn.
     */
    private static final class ExtentVisitor implements ModelPart.Visitor {

        private static final float MODEL_UNITS_PER_BLOCK = 16.0f;

        private final Vector3f base;
        private final Vector3f scratch = new Vector3f();

        private float maxY = Float.NEGATIVE_INFINITY;
        private float minY = Float.POSITIVE_INFINITY;
        private float maxRadiusSq = 0.0f;

        ExtentVisitor(Vector3f base) {
            this.base = base;
        }

        @Override
        public void visit(PoseStack.Pose pose, String partPath, int cubeIndex, ModelPart.Cube cube) {
            Matrix4f matrix = pose.pose();
            // Part transforms are in blocks but cube bounds are in model units, so scale first.
            for (int corner = 0; corner < 8; corner++) {
                float x = ((corner & 1) == 0 ? cube.minX : cube.maxX) / MODEL_UNITS_PER_BLOCK;
                float y = ((corner & 2) == 0 ? cube.minY : cube.maxY) / MODEL_UNITS_PER_BLOCK;
                float z = ((corner & 4) == 0 ? cube.minZ : cube.maxZ) / MODEL_UNITS_PER_BLOCK;
                matrix.transformPosition(x, y, z, scratch);

                float relY = scratch.y - base.y;
                if (relY > maxY) {
                    maxY = relY;
                }
                if (relY < minY) {
                    minY = relY;
                }

                float relX = scratch.x - base.x;
                float relZ = scratch.z - base.z;
                float radiusSq = relX * relX + relZ * relZ;
                if (radiusSq > maxRadiusSq) {
                    maxRadiusSq = radiusSq;
                }
            }
        }
    }
}

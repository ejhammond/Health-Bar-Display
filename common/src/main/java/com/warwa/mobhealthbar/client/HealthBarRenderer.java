package com.warwa.mobhealthbar.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.warwa.mobhealthbar.config.HealthBarConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.Random;

/**
 * Draws vanilla-style hearts above entities in world space.
 *
 * <p>This is called from a <em>clean</em> PoseStack — the one
 * {@code EntityRenderDispatcher} hands to {@code EntityRenderer.submit}, which carries
 * nothing but a translate to the entity's feet. That matters enormously: the model
 * PoseStack that render layers see has already had the entity's scale, per-renderer
 * {@code scale()} overrides and {@code setupRotations()} applied to it, which is what
 * previously flung the bar sideways off a beached cod, below a phantom, and eight blocks
 * above a wither. Rendering here, exactly like vanilla name tags do, sidesteps all of it.
 *
 * <p>In this space, after the camera billboard and {@code scale(s, -s, s)}:
 * +X is screen right and +Y is screen <em>down</em> (the vanilla name tag convention).
 * The bar is drawn with its bottom edge at the origin, growing upwards.
 */
public final class HealthBarRenderer {

    // Vanilla heart sprite identifiers (in GUI atlas)
    private static final Identifier HEART_CONTAINER = Identifier.withDefaultNamespace("hud/heart/container");
    private static final Identifier HEART_FULL = Identifier.withDefaultNamespace("hud/heart/full");
    private static final Identifier HEART_HALF = Identifier.withDefaultNamespace("hud/heart/half");
    private static final Identifier HEART_CONTAINER_BLINKING = Identifier.withDefaultNamespace("hud/heart/container_blinking");
    private static final Identifier HEART_FULL_BLINKING = Identifier.withDefaultNamespace("hud/heart/full_blinking");
    private static final Identifier HEART_HALF_BLINKING = Identifier.withDefaultNamespace("hud/heart/half_blinking");
    private static final Identifier HEART_ABSORBING_FULL = Identifier.withDefaultNamespace("hud/heart/absorbing_full");
    private static final Identifier HEART_ABSORBING_HALF = Identifier.withDefaultNamespace("hud/heart/absorbing_half");
    private static final Identifier HEART_POISONED_FULL = Identifier.withDefaultNamespace("hud/heart/poisoned_full");
    private static final Identifier HEART_POISONED_HALF = Identifier.withDefaultNamespace("hud/heart/poisoned_half");
    private static final Identifier HEART_WITHERED_FULL = Identifier.withDefaultNamespace("hud/heart/withered_full");
    private static final Identifier HEART_WITHERED_HALF = Identifier.withDefaultNamespace("hud/heart/withered_half");

    private static final int NO_OVERLAY = OverlayTexture.NO_OVERLAY;
    private static final Random SHAKE_RANDOM = new Random();

    private static final float HEART_SIZE = 9.0f;
    private static final float HEART_SPACING = 8.0f;
    private static final float ROW_HEIGHT = HEART_SIZE + 1.0f;

    /**
     * Depth between the stacked layers of a single heart, in local units.
     *
     * <p>A heart is the same quad drawn two or three times over: container, then optionally a
     * ghost, then the fill. Drawn coplanar they fight for the depth test, and the fight is
     * decided per pixel by rasteriser rounding rather than by draw order.
     *
     * <p>On an ordinary heart the fill quad has exactly the same corners as its container, so
     * both rasterise to bit-identical depths and the tie resolves consistently — which is why
     * only boss bars looked broken. A boss heart is cropped to a fraction of its width, giving
     * the fill quad a different right edge and therefore slightly different interpolated depth
     * across the overlap. The two then z-fight in a speckled band, and because the crop changes
     * every frame as health drains, the speckle crawls and flickers.
     *
     * <p>Separating the layers along the view axis removes the tie entirely. The offset points
     * straight at the camera, so it moves nothing on screen — at the default scale a step is
     * about 6mm of world depth.
     */
    private static final float LAYER_STEP = 0.25f;

    /** The empty heart outline every heart is drawn on top of. */
    private static final float Z_CONTAINER = 0.0f;
    /** The fading "health was here" heart, drawn between the container and the fill. */
    private static final float Z_GHOST = LAYER_STEP;
    /** The actual red (or poisoned, withered, absorbing) heart. */
    private static final float Z_FILL = LAYER_STEP * 2.0f;

    /** Base world-space size of one heart, matching the vanilla name tag scale. */
    public static final float BASE_SCALE = 0.025f;

    private HealthBarRenderer() {}

    /**
     * @param poseStack a clean pose translated to the entity's render origin
     * @param offsetX horizontal offset towards the viewer, for bars placed in front of the mob
     * @param anchorY world-space Y offset from that origin to the bar's bottom edge
     * @param offsetZ horizontal offset towards the viewer, for bars placed in front of the mob
     * @param barScale final heart scale, including the configured size and distance scaling
     * @param cameraOrientation the camera's rotation, for billboarding
     */
    public static void render(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            HealthBarData data,
            float offsetX,
            float anchorY,
            float offsetZ,
            float barScale,
            Quaternionf cameraOrientation
    ) {
        if (!data.shouldRender()) return;

        Minecraft mc = Minecraft.getInstance();
        TextureAtlas guiAtlas = mc.getAtlasManager().getAtlasOrThrow(AtlasIds.GUI);
        Sprites sprites = new Sprites(guiAtlas);

        Layout layout = Layout.of(data);

        poseStack.pushPose();
        poseStack.translate(offsetX, anchorY, offsetZ);
        poseStack.mulPose(cameraOrientation);

        float scale = barScale * layout.shrink;
        poseStack.scale(scale, -scale, scale);

        RenderType renderType = RenderTypes.entityTranslucent(sprites.atlas);
        submitNodeCollector.submitCustomGeometry(poseStack, renderType,
                (pose, buffer) -> draw(pose, buffer, data, sprites, layout));

        poseStack.popPose();
    }

    private static void draw(PoseStack.Pose pose, VertexConsumer buffer, HealthBarData data,
                             Sprites sprites, Layout layout) {
        if (data.boss()) {
            drawBossBar(pose, buffer, data, sprites, layout);
        } else {
            drawHearts(pose, buffer, data, sprites, layout);
        }
    }

    /**
     * A fixed row of hearts representing 100% of max health, draining smoothly.
     *
     * <p>A wither has 300 HP and a dragon 200, which drawn literally is 150 and 100 hearts
     * — illegible at any scale that fits on screen. Instead each heart is worth a tenth of
     * max health and is filled by a <em>fractional</em> amount, so a single point of damage
     * on a 300 HP wither visibly shaves a sliver off the last heart rather than doing
     * nothing until a half-heart boundary is crossed.
     */
    private static void drawBossBar(PoseStack.Pose pose, VertexConsumer buffer, HealthBarData data,
                                    Sprites sprites, Layout layout) {
        int hearts = HealthBarConfig.BOSS_HEART_COUNT;
        float fraction = data.healthFraction();
        float perHeart = 1.0f / hearts;

        TextureAtlasSprite full = sprites.fullFor(data);
        float y = -ROW_HEIGHT;

        for (int i = 0; i < hearts; i++) {
            float x = layout.startX + i * HEART_SPACING;
            renderHeart(pose, buffer, sprites.container, x, y, Z_CONTAINER, data.alpha());

            // How much of this heart is filled, 0..1, left to right.
            float fill = Mth.clamp((fraction - i * perHeart) / perHeart, 0.0f, 1.0f);
            if (fill > 0.0f) {
                renderHeartCropped(pose, buffer, full, x, y, Z_FILL, data.alpha(), fill);
            }
        }
    }

    /** The normal one-heart-per-2-HP display, mirroring vanilla's Gui heart logic. */
    private static void drawHearts(PoseStack.Pose pose, VertexConsumer buffer, HealthBarData data,
                                   Sprites sprites, Layout layout) {
        // Vanilla ceils health into half-hearts (Gui.java), so 0.1 HP still shows a half heart.
        final int health = Mth.ceil(data.currentHealth());
        final int absorption = Mth.ceil(data.absorption());
        final int previousHealth = Mth.ceil(data.previousHealth());
        final boolean blinking = data.damageFlashing() && (data.ticksSinceDamage() % 4) < 2;

        TextureAtlasSprite fullHeart = sprites.fullFor(data);
        TextureAtlasSprite halfHeart = sprites.halfFor(data);

        for (int i = 0; i < layout.totalHearts; i++) {
            float x = layout.xOf(i);
            float y = layout.yOf(i) + shake(data, i);

            renderHeart(pose, buffer, blinking ? sprites.containerBlink : sprites.container,
                    x, y, Z_CONTAINER, data.alpha());

            int halves = i * 2;

            // Ghost hearts: briefly show where health was before the hit.
            if (data.damageFlashing() && previousHealth > health && blinking
                    && halves < previousHealth && halves >= health) {
                boolean ghostHalf = (halves + 1 == previousHealth);
                renderHeart(pose, buffer, ghostHalf ? sprites.halfBlink : sprites.fullBlink,
                        x, y, Z_GHOST, data.alpha());
            }

            if (halves < health) {
                boolean isHalf = (halves + 1 == health);
                renderHeart(pose, buffer, isHalf ? halfHeart : fullHeart, x, y, Z_FILL, data.alpha());
            }
        }

        for (int i = 0; i < layout.absorptionHearts; i++) {
            int index = layout.totalHearts + i;
            float x = layout.xOf(index);
            float y = layout.yOf(index) + shake(data, index);

            renderHeart(pose, buffer, sprites.container, x, y, Z_CONTAINER, data.alpha());

            int halves = i * 2;
            if (halves < absorption) {
                boolean isHalf = (halves + 1 == absorption);
                renderHeart(pose, buffer, isHalf ? sprites.absorbHalf : sprites.absorbFull,
                        x, y, Z_FILL, data.alpha());
            }
        }
    }

    private static float shake(HealthBarData data, int index) {
        if (!data.shaking()) return 0.0f;
        SHAKE_RANDOM.setSeed(data.ticksSinceDamage() * 31L + index);
        return (SHAKE_RANDOM.nextFloat() - 0.5f) * 3.0f;
    }

    private static void renderHeart(PoseStack.Pose pose, VertexConsumer buffer, TextureAtlasSprite sprite,
                                    float x, float y, float z, float alpha) {
        renderHeartCropped(pose, buffer, sprite, x, y, z, alpha, 1.0f);
    }

    /**
     * Draws a heart, optionally cropped to {@code fill} of its width from the left.
     * Both the quad and the U range are scaled so the texture stays put rather than squashing.
     *
     * @param z depth offset towards the camera, separating this layer from the one beneath it
     *          (see {@link #LAYER_STEP})
     */
    private static void renderHeartCropped(PoseStack.Pose pose, VertexConsumer buffer, TextureAtlasSprite sprite,
                                           float x, float y, float z, float alpha, float fill) {
        Matrix4f matrix = pose.pose();

        float u0 = sprite.getU0();
        float u1 = Mth.lerp(fill, sprite.getU0(), sprite.getU1());
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        float x0 = x;
        float x1 = x + HEART_SIZE * fill;
        float y0 = y;                  // top edge
        float y1 = y + HEART_SIZE;     // bottom edge (+Y is screen down here)

        int light = 0xF000F0; // full brightness

        buffer.addVertex(matrix, x0, y0, z).setColor(1.0f, 1.0f, 1.0f, alpha)
                .setUv(u0, v0).setOverlay(NO_OVERLAY).setLight(light).setNormal(0, 0, 1);
        buffer.addVertex(matrix, x0, y1, z).setColor(1.0f, 1.0f, 1.0f, alpha)
                .setUv(u0, v1).setOverlay(NO_OVERLAY).setLight(light).setNormal(0, 0, 1);
        buffer.addVertex(matrix, x1, y1, z).setColor(1.0f, 1.0f, 1.0f, alpha)
                .setUv(u1, v1).setOverlay(NO_OVERLAY).setLight(light).setNormal(0, 0, 1);
        buffer.addVertex(matrix, x1, y0, z).setColor(1.0f, 1.0f, 1.0f, alpha)
                .setUv(u1, v0).setOverlay(NO_OVERLAY).setLight(light).setNormal(0, 0, 1);
    }

    /** Grid maths: how many hearts, over how many rows, and where each one lands. */
    private static final class Layout {
        final int totalHearts;
        final int absorptionHearts;
        final int heartsPerRow;
        final int rows;
        final float startX;
        final float shrink;

        private Layout(int totalHearts, int absorptionHearts, int heartsPerRow, int rows, float shrink) {
            this.totalHearts = totalHearts;
            this.absorptionHearts = absorptionHearts;
            this.heartsPerRow = heartsPerRow;
            this.rows = rows;
            this.shrink = shrink;

            int inWidestRow = Math.min(totalHearts + absorptionHearts, heartsPerRow);
            float width = inWidestRow * HEART_SPACING + (HEART_SIZE - HEART_SPACING);
            this.startX = -width / 2.0f;
        }

        static Layout of(HealthBarData data) {
            if (data.boss()) {
                int hearts = HealthBarConfig.BOSS_HEART_COUNT;
                return new Layout(hearts, 0, hearts, 1, 1.0f);
            }

            int hearts = Mth.ceil(data.maxHealth() / 2.0f);
            int absorption = Mth.ceil(data.absorption() / 2.0f);
            int perRow = HealthBarConfig.HEARTS_PER_ROW;
            int rows = Mth.ceil((float) (hearts + absorption) / perRow);

            float shrink = 1.0f;
            if (rows > HealthBarConfig.MAX_HEART_ROWS) {
                shrink = (float) HealthBarConfig.MAX_HEART_ROWS / rows;
                perRow = Mth.ceil((float) (hearts + absorption) / HealthBarConfig.MAX_HEART_ROWS);
                rows = HealthBarConfig.MAX_HEART_ROWS;
            }
            return new Layout(hearts, absorption, perRow, rows, shrink);
        }

        float xOf(int index) {
            return startX + (index % heartsPerRow) * HEART_SPACING;
        }

        /** Rows stack upwards from the anchor, so the bar's bottom edge sits at y = 0. */
        float yOf(int index) {
            int row = index / heartsPerRow;
            return -(rows - row) * ROW_HEIGHT;
        }
    }

    /** Resolves every heart sprite once per bar rather than per heart. */
    private static final class Sprites {
        final Identifier atlas;
        final TextureAtlasSprite container;
        final TextureAtlasSprite containerBlink;
        final TextureAtlasSprite full;
        final TextureAtlasSprite half;
        final TextureAtlasSprite fullBlink;
        final TextureAtlasSprite halfBlink;
        final TextureAtlasSprite absorbFull;
        final TextureAtlasSprite absorbHalf;
        final TextureAtlasSprite poisonFull;
        final TextureAtlasSprite poisonHalf;
        final TextureAtlasSprite witherFull;
        final TextureAtlasSprite witherHalf;

        Sprites(TextureAtlas guiAtlas) {
            container = guiAtlas.getSprite(HEART_CONTAINER);
            containerBlink = guiAtlas.getSprite(HEART_CONTAINER_BLINKING);
            full = guiAtlas.getSprite(HEART_FULL);
            half = guiAtlas.getSprite(HEART_HALF);
            fullBlink = guiAtlas.getSprite(HEART_FULL_BLINKING);
            halfBlink = guiAtlas.getSprite(HEART_HALF_BLINKING);
            absorbFull = guiAtlas.getSprite(HEART_ABSORBING_FULL);
            absorbHalf = guiAtlas.getSprite(HEART_ABSORBING_HALF);
            poisonFull = guiAtlas.getSprite(HEART_POISONED_FULL);
            poisonHalf = guiAtlas.getSprite(HEART_POISONED_HALF);
            witherFull = guiAtlas.getSprite(HEART_WITHERED_FULL);
            witherHalf = guiAtlas.getSprite(HEART_WITHERED_HALF);
            atlas = container.atlasLocation();
        }

        TextureAtlasSprite fullFor(HealthBarData data) {
            if (data.poisoned()) return poisonFull;
            if (data.withered()) return witherFull;
            return full;
        }

        TextureAtlasSprite halfFor(HealthBarData data) {
            if (data.poisoned()) return poisonHalf;
            if (data.withered()) return witherHalf;
            return half;
        }
    }
}

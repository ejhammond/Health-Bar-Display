package com.warwa.mobhealthbar.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.warwa.mobhealthbar.MobHealthBar;
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
import org.joml.Vector3f;

import java.util.Random;

/**
 * Core renderer that draws vanilla-style hearts above entities in world space.
 *
 * COORDINATE SYSTEM NOTES (from analyzing LivingEntityRenderer.submit()):
 * When a RenderLayer's submit() is called, the PoseStack has these transforms applied:
 *   1. scale(entityScale, entityScale, entityScale)
 *   2. mulPose(YP.rotationDegrees(180 - bodyRot))  [body yaw rotation]
 *   3. scale(-1, -1, 1)  [flip X and Y]
 *   4. translate(0, -1.501, 0)  [model origin offset]
 *
 * This means:
 *   - Y axis is INVERTED: positive Y = DOWN in world space
 *   - X axis is INVERTED: positive X = LEFT in world space
 *   - Origin is 1.501 units ABOVE entity feet (in world space)
 *   - Body yaw rotation is applied
 *
 * To render hearts above the entity head:
 *   1. Translate in the inverted space to above the head
 *   2. Undo the body rotation and flip for clean billboard
 *   3. Apply camera billboard (like vanilla name tags)
 *   4. Scale with (0.025, -0.025, 0.025) matching vanilla name tag convention
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

    // Vanilla model offset applied before layers are called
    private static final float MODEL_ORIGIN_OFFSET = 1.501f;

    private static boolean debugLogged = false;

    private HealthBarRenderer() {}

    /**
     * Renders vanilla-style hearts above an entity using the SubmitNodeCollector pipeline.
     *
     * @param yRot the entity's body rotation (passed to undo it for billboard)
     */
    public static void renderHealthBar(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            HealthBarData data,
            float entityHeight,
            int packedLight,
            float yRot
    ) {
        if (!data.shouldRender()) return;

        Minecraft mc = Minecraft.getInstance();

        float health = data.currentHealth();
        float maxHealth = data.maxHealth();
        float absorption = data.absorption();
        float alpha = data.alpha();
        float previousHealth = data.previousHealth();

        int totalHearts = Mth.ceil(maxHealth / 2.0f);
        int absorptionHearts = Mth.ceil(absorption / 2.0f);

        float heartScale = 1.0f;
        int heartsPerRow = HealthBarConfig.HEARTS_PER_ROW;
        int totalRows = Mth.ceil((float)(totalHearts + absorptionHearts) / heartsPerRow);
        if (totalRows > HealthBarConfig.MAX_HEART_ROWS) {
            heartScale = (float) HealthBarConfig.MAX_HEART_ROWS / totalRows;
            heartsPerRow = Mth.ceil((float)(totalHearts + absorptionHearts) / HealthBarConfig.MAX_HEART_ROWS);
        }

        poseStack.pushPose();

        // === ROBUST BILLBOARD APPROACH ===
        // Instead of trying to undo entity transforms (fragile, varies per entity type),
        // we compute the view-space position of "above the head" using the current matrix,
        // then RESET the matrix and apply a clean billboard from scratch.
        //
        // This is exactly how vanilla name tags work - they render AFTER popPose(),
        // with a clean PoseStack. We emulate this from inside a render layer.

        // STEP 1: Compute view-space position of "above head" using current (entity-transformed) matrix
        // In the entity's local space (Y flipped, origin offset by 1.501):
        //   Entity head top = local (0, MODEL_ORIGIN_OFFSET - entityHeight, 0)
        //   Above head       = local (0, MODEL_ORIGIN_OFFSET - entityHeight - HEIGHT_OFFSET, 0)
        float localY = MODEL_ORIGIN_OFFSET - entityHeight - HealthBarConfig.HEIGHT_OFFSET;
        Vector3f aboveHead = new Vector3f(0, localY, 0);

        // Transform this local point through the current matrix to get view-space position
        Matrix4f currentMatrix = poseStack.last().pose();
        currentMatrix.transformPosition(aboveHead);

        if (!debugLogged) {
            MobHealthBar.LOGGER.info("[MobHealthBar] entityHeight={}, localY={}, viewPos=({},{},{})",
                    entityHeight, localY, aboveHead.x, aboveHead.y, aboveHead.z);
        }

        // STEP 2: Reset pose to identity and place at the computed view-space position
        poseStack.last().pose().identity();
        poseStack.last().normal().identity();
        poseStack.translate(aboveHead.x, aboveHead.y, aboveHead.z);

        // STEP 3: Apply camera billboard rotation (exactly like vanilla NameTagFeatureRenderer)
        // Vanilla: poseStack.mulPose(camera.orientation)
        Quaternionf cameraOrientation = mc.gameRenderer.getMainCamera().rotation();
        poseStack.mulPose(cameraOrientation);

        // STEP 4: Apply scale (exactly like vanilla name tags: 0.025, -0.025, 0.025)
        // Positive X, negative Y = text convention where +Y goes down on screen
        float scale = HealthBarConfig.HEART_SCALE * heartScale;
        poseStack.scale(scale, -scale, scale);

        if (!debugLogged) {
            MobHealthBar.LOGGER.info("[MobHealthBar] Billboard applied. scale={}", scale);
            debugLogged = true;
        }

        // === STEP 5: Resolve sprites and render hearts ===
        TextureAtlas guiAtlas = mc.getAtlasManager().getAtlasOrThrow(AtlasIds.GUI);

        TextureAtlasSprite containerSpr = guiAtlas.getSprite(HEART_CONTAINER);
        TextureAtlasSprite fullSpr = guiAtlas.getSprite(HEART_FULL);
        TextureAtlasSprite halfSpr = guiAtlas.getSprite(HEART_HALF);
        TextureAtlasSprite containerBlinkSpr = guiAtlas.getSprite(HEART_CONTAINER_BLINKING);
        TextureAtlasSprite fullBlinkSpr = guiAtlas.getSprite(HEART_FULL_BLINKING);
        TextureAtlasSprite halfBlinkSpr = guiAtlas.getSprite(HEART_HALF_BLINKING);
        TextureAtlasSprite absorbFullSpr = guiAtlas.getSprite(HEART_ABSORBING_FULL);
        TextureAtlasSprite absorbHalfSpr = guiAtlas.getSprite(HEART_ABSORBING_HALF);
        TextureAtlasSprite poisonFullSpr = guiAtlas.getSprite(HEART_POISONED_FULL);
        TextureAtlasSprite poisonHalfSpr = guiAtlas.getSprite(HEART_POISONED_HALF);
        TextureAtlasSprite witherFullSpr = guiAtlas.getSprite(HEART_WITHERED_FULL);
        TextureAtlasSprite witherHalfSpr = guiAtlas.getSprite(HEART_WITHERED_HALF);

        // All sprites share the same atlas texture
        Identifier atlasTexture = containerSpr.atlasLocation();
        RenderType renderType = RenderTypes.entityTranslucent(atlasTexture);

        // Calculate centering
        int heartsInFirstRow = Math.min(totalHearts + absorptionHearts, heartsPerRow);
        float totalWidth = heartsInFirstRow * HEART_SPACING + (HEART_SIZE - HEART_SPACING);
        float startX = -totalWidth / 2.0f;

        boolean blinking = data.damageFlashing() && (data.ticksSinceDamage() % 4) < 2;
        long shakeSeed = data.ticksSinceDamage();

        // Convert to integer half-hearts using ceil, exactly like vanilla Gui.java:
        //   int currentHealth = Mth.ceil(player.getHealth());
        // This ensures even 0.1 HP shows as a half heart (ceil(0.1) = 1)
        final int intHealth = Mth.ceil(health);
        final int intAbsorption = Mth.ceil(absorption);
        final int intPreviousHealth = Mth.ceil(previousHealth);

        // Capture for lambda
        final int fTotalHearts = totalHearts;
        final int fAbsorptionHearts = absorptionHearts;
        final int fHeartsPerRow = heartsPerRow;
        final float fStartX = startX;
        final float fAlpha = alpha;
        final int fHealth = intHealth;
        final int fAbsorption = intAbsorption;
        final int fPreviousHealth = intPreviousHealth;
        final boolean fBlinking = blinking;
        final boolean fShaking = data.shaking();
        final boolean fDamageFlashing = data.damageFlashing();
        final boolean fPoisoned = data.poisoned();
        final boolean fWithered = data.withered();
        final long fShakeSeed = shakeSeed;

        // Submit all hearts as a single custom geometry batch
        submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            // In this space (after billboard + scale(s, -s, s)):
            //   +X = screen right
            //   +Y = screen DOWN (because of -scale on Y, matching vanilla name tag convention)
            //   Hearts at Y=0 are at the top, Y=9 is one heart lower
            //   Additional rows go further down (+Y)

            for (int i = 0; i < fTotalHearts; i++) {
                int row = i / fHeartsPerRow;
                int col = i % fHeartsPerRow;
                float x = fStartX + col * HEART_SPACING;
                float y = row * (HEART_SIZE + 1);

                float shakeY = 0;
                if (fShaking) {
                    SHAKE_RANDOM.setSeed(fShakeSeed * 31L + i);
                    shakeY = (SHAKE_RANDOM.nextFloat() - 0.5f) * 3.0f;
                }

                float heartY = y + shakeY;

                // Container
                TextureAtlasSprite container = fBlinking ? containerBlinkSpr : containerSpr;
                renderHeartQuad(pose, buffer, container, x, heartY, fAlpha);

                // Vanilla heart fill logic (from Gui.java):
                //   int halves = containerIndex * 2;
                //   if (halves < currentHealth) { halfHeart = halves + 1 == currentHealth; }
                int halves = i * 2;

                // Ghost hearts (damage flash) - show where health WAS
                if (fDamageFlashing && fPreviousHealth > fHealth) {
                    if (halves < fPreviousHealth && halves >= fHealth) {
                        if (fBlinking) {
                            boolean ghostHalf = (halves + 1 == fPreviousHealth);
                            renderHeartQuad(pose, buffer, ghostHalf ? halfBlinkSpr : fullBlinkSpr, x, heartY, fAlpha);
                        }
                    }
                }

                // Current health hearts (exactly like vanilla)
                TextureAtlasSprite fullHeart = fPoisoned ? poisonFullSpr : (fWithered ? witherFullSpr : fullSpr);
                TextureAtlasSprite halfHeart = fPoisoned ? poisonHalfSpr : (fWithered ? witherHalfSpr : halfSpr);

                if (halves < fHealth) {
                    boolean isHalf = (halves + 1 == fHealth);
                    renderHeartQuad(pose, buffer, isHalf ? halfHeart : fullHeart, x, heartY, fAlpha);
                }
            }

            // Absorption hearts
            for (int i = 0; i < fAbsorptionHearts; i++) {
                int idx = fTotalHearts + i;
                int row = idx / fHeartsPerRow;
                int col = idx % fHeartsPerRow;
                float x = fStartX + col * HEART_SPACING;
                float y = row * (HEART_SIZE + 1);

                float shakeY = 0;
                if (fShaking) {
                    SHAKE_RANDOM.setSeed(fShakeSeed * 31L + idx);
                    shakeY = (SHAKE_RANDOM.nextFloat() - 0.5f) * 3.0f;
                }

                float heartY = y + shakeY;

                renderHeartQuad(pose, buffer, containerSpr, x, heartY, fAlpha);

                // Absorption uses same integer-halves logic
                int absHalves = i * 2;
                if (absHalves < fAbsorption) {
                    boolean isHalf = (absHalves + 1 == fAbsorption);
                    renderHeartQuad(pose, buffer, isHalf ? absorbHalfSpr : absorbFullSpr, x, heartY, fAlpha);
                }
            }
        });

        poseStack.popPose();
    }

    /**
     * Renders a single heart sprite as a textured quad.
     * In the billboard space with scale(s, -s, s):
     *   +Y = screen down, so quad from (x, y) to (x+9, y+9) draws right-side-up heart going downward.
     */
    private static void renderHeartQuad(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            TextureAtlasSprite sprite,
            float x, float y,
            float alpha
    ) {
        Matrix4f matrix = pose.pose();

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        float x0 = x;
        float x1 = x + HEART_SIZE;
        float y0 = y;              // top of heart (screen up)
        float y1 = y + HEART_SIZE;  // bottom of heart (screen down)

        int light = 0xF000F0; // Full brightness

        // Standard quad winding: top-left, top-right, bottom-right, bottom-left
        // UV: v0 at top (y0), v1 at bottom (y1) = right-side up texture
        buffer.addVertex(matrix, x0, y0, 0).setColor(1.0f, 1.0f, 1.0f, alpha)
                .setUv(u0, v0).setOverlay(NO_OVERLAY).setLight(light).setNormal(0, 0, 1);
        buffer.addVertex(matrix, x0, y1, 0).setColor(1.0f, 1.0f, 1.0f, alpha)
                .setUv(u0, v1).setOverlay(NO_OVERLAY).setLight(light).setNormal(0, 0, 1);
        buffer.addVertex(matrix, x1, y1, 0).setColor(1.0f, 1.0f, 1.0f, alpha)
                .setUv(u1, v1).setOverlay(NO_OVERLAY).setLight(light).setNormal(0, 0, 1);
        buffer.addVertex(matrix, x1, y0, 0).setColor(1.0f, 1.0f, 1.0f, alpha)
                .setUv(u1, v0).setOverlay(NO_OVERLAY).setLight(light).setNormal(0, 0, 1);
    }
}

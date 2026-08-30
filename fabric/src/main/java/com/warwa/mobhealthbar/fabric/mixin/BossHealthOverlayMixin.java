package com.warwa.mobhealthbar.fabric.mixin;

import com.warwa.mobhealthbar.config.HealthBarConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides the vanilla boss health bar across the top of the screen when configured to.
 *
 * <p>In 26.2 the HUD is extract/submit based, so cancelling extraction is enough — nothing
 * downstream ever gets submitted. The boss events themselves are left untouched, so toggling
 * the option back on restores the bar immediately.
 */
@Mixin(BossHealthOverlay.class)
public class BossHealthOverlayMixin {

    @Inject(
            method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void mobhealthbar$hideVanillaBossBar(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (HealthBarConfig.get().hideVanillaBossBar) {
            ci.cancel();
        }
    }
}

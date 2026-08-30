package com.warwa.mobhealthbar.tracking;

import com.warwa.mobhealthbar.config.HealthBarConfig;

public class TrackedEntity {

    public enum FadeState {
        FADING_IN,
        VISIBLE,
        FADING_OUT,
        HIDDEN
    }

    private final int entityId;
    private boolean hostile;
    private int displayTicksRemaining;
    private FadeState fadeState;
    private float fadeProgress; // 0.0 = invisible, 1.0 = fully visible
    private int fadeTicksInState;

    // Damage tracking for heart animations
    private float lastHealth = -1;
    private float previousHealth = -1;
    private int damageTick = -1000; // tick when last damage occurred
    private int currentTick = 0;

    public TrackedEntity(int entityId, boolean hostile, int displayDuration) {
        this.entityId = entityId;
        this.hostile = hostile;
        this.displayTicksRemaining = displayDuration;
        this.fadeState = FadeState.FADING_IN;
        this.fadeProgress = 0.0f;
        this.fadeTicksInState = 0;
    }

    public void refresh(boolean hostile, int displayDuration) {
        this.hostile = hostile;
        // Never shorten an existing window: looking at a mob every tick must not
        // drag down a longer hit timer (you have to look at a mob to hit it).
        // The bar stays for the longer of the active reasons (look vs hit).
        this.displayTicksRemaining = Math.max(this.displayTicksRemaining, displayDuration);

        if (fadeState == FadeState.FADING_OUT) {
            // Reverse the fade - smoothly go back to visible
            fadeState = FadeState.FADING_IN;
            // Keep current fadeProgress so there's no pop
            fadeTicksInState = (int) (fadeProgress * HealthBarConfig.FADE_IN_DURATION);
        } else if (fadeState == FadeState.HIDDEN) {
            fadeState = FadeState.FADING_IN;
            fadeProgress = 0.0f;
            fadeTicksInState = 0;
        }
        // If FADING_IN or VISIBLE, just reset the timer (already done above)
    }

    public void tick() {
        currentTick++;
        fadeTicksInState++;

        switch (fadeState) {
            case FADING_IN -> {
                fadeProgress = (float) fadeTicksInState / HealthBarConfig.FADE_IN_DURATION;
                if (fadeProgress >= 1.0f) {
                    fadeProgress = 1.0f;
                    fadeState = FadeState.VISIBLE;
                    fadeTicksInState = 0;
                }
            }
            case VISIBLE -> {
                displayTicksRemaining--;
                if (displayTicksRemaining <= 0) {
                    fadeState = FadeState.FADING_OUT;
                    fadeTicksInState = 0;
                }
            }
            case FADING_OUT -> {
                fadeProgress = 1.0f - (float) fadeTicksInState / HealthBarConfig.FADE_OUT_DURATION;
                if (fadeProgress <= 0.0f) {
                    fadeProgress = 0.0f;
                    fadeState = FadeState.HIDDEN;
                }
            }
            case HIDDEN -> {
                // Will be removed by the tracker
            }
        }
    }

    public void updateHealth(float currentHealth) {
        if (lastHealth >= 0 && currentHealth < lastHealth) {
            // Damage occurred
            previousHealth = lastHealth;
            damageTick = currentTick;
        }
        lastHealth = currentHealth;
    }

    public float getAlpha(float partialTick) {
        float raw = fadeProgress;

        // Interpolate between ticks for smooth animation
        if (fadeState == FadeState.FADING_IN) {
            raw = Math.min(1.0f, (fadeTicksInState + partialTick) / HealthBarConfig.FADE_IN_DURATION);
        } else if (fadeState == FadeState.FADING_OUT) {
            raw = Math.max(0.0f, 1.0f - (fadeTicksInState + partialTick) / HealthBarConfig.FADE_OUT_DURATION);
        }

        // Smoothstep easing for natural feel
        return raw * raw * (3.0f - 2.0f * raw);
    }

    public boolean isHidden() {
        return fadeState == FadeState.HIDDEN;
    }

    public boolean isDamageFlashing() {
        return (currentTick - damageTick) < HealthBarConfig.DAMAGE_FLASH_DURATION;
    }

    public boolean isShaking() {
        return (currentTick - damageTick) < HealthBarConfig.SHAKE_DURATION;
    }

    public int getTicksSinceDamage() {
        return currentTick - damageTick;
    }

    public int getEntityId() { return entityId; }
    public boolean isHostile() { return hostile; }
    public float getLastHealth() { return lastHealth; }
    public float getPreviousHealth() { return previousHealth; }
    public FadeState getFadeState() { return fadeState; }
    public int getDamageTick() { return damageTick; }
    public int getCurrentTick() { return currentTick; }
}

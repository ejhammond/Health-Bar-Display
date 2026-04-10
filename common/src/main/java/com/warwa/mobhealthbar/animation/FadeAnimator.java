package com.warwa.mobhealthbar.animation;

public final class FadeAnimator {

    /**
     * Applies smoothstep easing to a linear value.
     * Produces a natural-feeling acceleration and deceleration.
     */
    public static float smoothstep(float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        return t * t * (3.0f - 2.0f * t);
    }

    /**
     * Lerp between two values.
     */
    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private FadeAnimator() {}
}

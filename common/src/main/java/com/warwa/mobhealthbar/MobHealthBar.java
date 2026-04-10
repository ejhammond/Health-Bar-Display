package com.warwa.mobhealthbar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MobHealthBar {

    public static final String MOD_ID = "mobhealthbar";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        LOGGER.info("Mob Health Bar initialized");
    }
}

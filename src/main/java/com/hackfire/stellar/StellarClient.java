package com.hackfire.stellar;

import com.hackfire.stellar.config.StellarConfig;
import com.hackfire.stellar.render.StellarSkyRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StellarClient implements ClientModInitializer {
    public static final String MOD_ID = "stellar";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static StellarConfig config;

    @Override
    public void onInitializeClient() {
        config = StellarConfig.load();
        StellarSkyRenderer renderer = new StellarSkyRenderer(() -> config);
        WorldRenderEvents.AFTER_SKY.register(renderer::render);
        LOGGER.info("Project Stellar {} initialized", "0.1.0");
    }

    public static StellarConfig config() { return config; }
}

package com.hackfire.stellar.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Small JSON config. Defaults intentionally favor low GPU cost. */
public final class StellarConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("stellar.json");

    public boolean enabled = true;
    public int starCount = 450;
    public float starBrightness = 0.82f;
    public boolean galaxyEnabled = true;
    public float galaxyBrightness = 0.75f;
    public boolean distantGalaxies = true;
    public float distantGalaxyBrightness = 0.65f;
    public boolean shootingStars = true;
    public float cometChancePerSecond = 0.012f;

    public static StellarConfig load() {
        try {
            if (Files.exists(FILE)) {
                StellarConfig loaded = GSON.fromJson(Files.readString(FILE), StellarConfig.class);
                if (loaded != null) { loaded.sanitize(); return loaded; }
            }
        } catch (Exception e) {
            System.err.println("[Stellar] Could not read config; using defaults: " + e.getMessage());
        }
        StellarConfig config = new StellarConfig();
        config.save();
        return config;
    }

    private void sanitize() {
        starCount = Math.max(0, Math.min(1800, starCount));
        starBrightness = Math.max(0.0f, Math.min(1.0f, starBrightness));
        galaxyBrightness = Math.max(0.0f, Math.min(1.0f, galaxyBrightness));
        distantGalaxyBrightness = Math.max(0.0f, Math.min(1.0f, distantGalaxyBrightness));
        cometChancePerSecond = Math.max(0.0f, Math.min(0.1f, cometChancePerSecond));
    }

    private void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(this));
        } catch (IOException e) {
            System.err.println("[Stellar] Could not save config: " + e.getMessage());
        }
    }
}
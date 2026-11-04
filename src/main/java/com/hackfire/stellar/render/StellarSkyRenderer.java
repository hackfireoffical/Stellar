package com.hackfire.stellar.render;

import com.hackfire.stellar.config.StellarConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

/**
 * Cached, geometry-only sky layer. No framebuffers, texture samplers,
 * post-processing, or per-frame random generation.
 */
public final class StellarSkyRenderer {
    private static final int MAX_STARS = 1800;
    private static final float RADIUS = 96.0f;
    private static final int GALAXY_SEGMENTS = 72;
    // x, y, z, half-size, brightness, red, green, blue
    private static final float[] STARS = new float[MAX_STARS * 8];
    private static int generatedCount = -1;
    private static final int NEBULA_COUNT = 32;
    // x, y, z, radius, red, green, blue, alpha
    private static final float[] NEBULAE = new float[NEBULA_COUNT * 8];
    private static boolean nebulaeGenerated;
    private static final int MAX_COMETS = 2;
    private static final float[] COMET_ANGLE = new float[MAX_COMETS];
    private static final float[] COMET_HEIGHT = new float[MAX_COMETS];
    private static final float[] COMET_LIFE = new float[MAX_COMETS];
    private static final float[] COMET_DURATION = new float[MAX_COMETS];
    private static final float[] COMET_SPEED = new float[MAX_COMETS];
    private static final Random COMET_RANDOM = Random.create(0xC04E7L);
    private static long lastCometUpdateNanos;

    private StellarSkyRenderer() { }

    private static void generateNebulae() {
        Random random = Random.create(0x4E8A1A5L);
        for (int i = 0; i < NEBULA_COUNT; i++) {
            double y = Math.max(-0.32, Math.min(0.32, random.nextGaussian() * 0.13));
            double angle = random.nextDouble() * Math.PI * 2.0;
            double ring = Math.sqrt(1.0 - y * y);
            int p = i * 8;
            NEBULAE[p] = (float) Math.cos(angle) * (float) ring;
            NEBULAE[p + 1] = (float) y;
            NEBULAE[p + 2] = (float) Math.sin(angle) * (float) ring;
            NEBULAE[p + 3] = 1.8f + random.nextFloat() * 3.8f;
            boolean violet = random.nextFloat() < 0.55f;
            NEBULAE[p + 4] = violet ? 112.0f : 66.0f;
            NEBULAE[p + 5] = violet ? 82.0f : 126.0f;
            NEBULAE[p + 6] = violet ? 190.0f : 220.0f;
            NEBULAE[p + 7] = 4.0f + random.nextFloat() * 5.0f;
        }
        nebulaeGenerated = true;
    }

    private static void addNebulae(BufferBuilder buffer, org.joml.Matrix4f matrix,
                                   float night, float strength) {
        if (!nebulaeGenerated) generateNebulae();
        for (int i = 0; i < NEBULA_COUNT; i++) {
            int p = i * 8;
            float dx = NEBULAE[p], dy = NEBULAE[p + 1], dz = NEBULAE[p + 2];
            float centerX = dx * (RADIUS - 1.0f);
            float centerY = dy * (RADIUS - 1.0f);
            float centerZ = dz * (RADIUS - 1.0f);
            // Tangent basis keeps each translucent patch facing the viewer at the sky dome.
            float rightX = -dz, rightY = 0.0f, rightZ = dx;
            float rightLength = (float) Math.sqrt(rightX * rightX + rightZ * rightZ);
            if (rightLength < 0.001f) { rightX = 1.0f; rightZ = 0.0f; rightLength = 1.0f; }
            rightX /= rightLength;
            rightZ /= rightLength;
            float upX = dy * rightZ;
            float upY = dz * rightX - dx * rightZ;
            float upZ = -dy * rightX;
            float size = NEBULAE[p + 3];
            int red = (int) NEBULAE[p + 4];
            int green = (int) NEBULAE[p + 5];
            int blue = (int) NEBULAE[p + 6];
            int alpha = (int) (NEBULAE[p + 7] * strength * night);

            // Two softly overlapping scales make a cloud-like patch from flat geometry.
            addNebulaQuad(buffer, matrix, centerX, centerY, centerZ,
                    rightX, rightY, rightZ, upX, upY, upZ, size, red, green, blue, alpha);
            addNebulaQuad(buffer, matrix, centerX, centerY, centerZ,
                    rightX, rightY, rightZ, upX, upY, upZ, size * 0.48f,
                    Math.min(255, red + 28), Math.min(255, green + 24),
                    Math.min(255, blue + 20), alpha + alpha / 2);
        }
    }

    private static void addNebulaQuad(BufferBuilder buffer, org.joml.Matrix4f matrix,
                                      float cx, float cy, float cz,
                                      float rx, float ry, float rz,
                                      float ux, float uy, float uz, float size,
                                      int red, int green, int blue, int alpha) {
        float rX = rx * size, rY = ry * size, rZ = rz * size;
        float uX = ux * size * 0.62f, uY = uy * size * 0.62f, uZ = uz * size * 0.62f;
        vertex(buffer, matrix, cx-rX-uX, cy-rY-uY, cz-rZ-uZ, red, green, blue, alpha);
        vertex(buffer, matrix, cx-rX+uX, cy-rY+uY, cz-rZ+uZ, red, green, blue, alpha);
        vertex(buffer, matrix, cx+rX+uX, cy+rY+uY, cz+rZ+uZ, red, green, blue, alpha);
        vertex(buffer, matrix, cx+rX-uX, cy+rY-uY, cz+rZ-uZ, red, green, blue, alpha);
    }

    private static void generate(int count) {
        Random random = Random.create(0x57E11A4L);
        for (int i = 0; i < count; i++) {
            boolean inBand = random.nextFloat() < 0.68f;
            double y = inBand ? random.nextGaussian() * 0.105 : random.nextDouble() * 2.0 - 1.0;
            y = Math.max(-0.98, Math.min(0.98, y));
            double angle = random.nextDouble() * Math.PI * 2.0;
            double ring = Math.sqrt(1.0 - y * y);
            float size = inBand
                    ? 0.045f + random.nextFloat() * 0.095f
                    : 0.075f + random.nextFloat() * 0.19f;
            float brightness = 0.22f + random.nextFloat() * 0.78f;
            float red = 0.70f + random.nextFloat() * 0.30f;
            float green = 0.76f + random.nextFloat() * 0.24f;
            float blue = 0.88f + random.nextFloat() * 0.12f;
            if (inBand && random.nextFloat() < 0.42f) {
                red = 0.72f;
                green = 0.78f;
                blue = 1.0f;
            }
            int p = i * 8;
            STARS[p] = (float) Math.cos(angle) * (float) ring * RADIUS;
            STARS[p + 1] = (float) y * RADIUS;
            STARS[p + 2] = (float) Math.sin(angle) * (float) ring * RADIUS;
            STARS[p + 3] = size;
            STARS[p + 4] = brightness;
            STARS[p + 5] = red;
            STARS[p + 6] = green;
            STARS[p + 7] = blue;
        }
        generatedCount = count;
    }

    public static void renderSky(MatrixStack matrices, StellarConfig config, float nightFactor, float weatherFactor) {
        if (config == null || !config.enabled || nightFactor <= 0.001f) return;

        int count = MathHelper.clamp(config.starCount, 0, MAX_STARS);
        if (generatedCount != count) generate(count);
        float night = MathHelper.clamp(nightFactor, 0.0f, 1.0f);
        float weather = MathHelper.clamp(weatherFactor, 0.0f, 1.0f);
        float brightness = MathHelper.clamp(config.starBrightness, 0.0f, 1.0f) * night * (0.85f + weather * 0.15f);
        updateComets(config, night);

        matrices.push();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        try {
            BufferBuilder buffer = Tessellator.getInstance().getBuffer();
            buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            var matrix = matrices.peek().getPositionMatrix();

            // A soft, segmented blue-violet galactic band. The nested strips
            // create a feathered haze without textures or extra draw calls.
            if (config.galaxyEnabled && config.galaxyBrightness > 0.001f) {
                float galaxyStrength = MathHelper.clamp(config.galaxyBrightness, 0.0f, 1.0f);
                addGalaxyBand(buffer, matrix, night, galaxyStrength);
                addNebulae(buffer, matrix, night * weather, galaxyStrength);
                if (config.distantGalaxies && config.distantGalaxyBrightness > 0.001f) {
                    addDistantGalaxies(buffer, matrix, night * weather,
                            MathHelper.clamp(config.distantGalaxyBrightness, 0.0f, 1.0f));
                }
            }

            for (int i = 0; i < count; i++) {
                int p = i * 8;
                float x = STARS[p], y = STARS[p + 1], z = STARS[p + 2];
                float half = STARS[p + 3];
                int alpha = (int) (255.0f * brightness * STARS[p + 4]);
                int red = (int) (255.0f * STARS[p + 5]);
                int green = (int) (255.0f * STARS[p + 6]);
                int blue = (int) (255.0f * STARS[p + 7]);
                vertex(buffer, matrix, x - half, y - half, z, red, green, blue, alpha);
                vertex(buffer, matrix, x - half, y + half, z, red, green, blue, alpha);
                vertex(buffer, matrix, x + half, y + half, z, red, green, blue, alpha);
                vertex(buffer, matrix, x + half, y - half, z, red, green, blue, alpha);
            }

            if (config.shootingStars) {
                renderComets(buffer, matrix, night);
            }
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            matrices.pop();
        }
    }

    private static void addGalaxyBand(BufferBuilder buffer, org.joml.Matrix4f matrix,
                                      float night, float strength) {
        final double step = Math.PI * 2.0 / GALAXY_SEGMENTS;
        final float[] widths = { 5.8f, 3.5f, 1.65f };
        final int[][] colors = {
                { 42, 58, 132, 10 },
                { 76, 83, 180, 13 },
                { 126, 112, 210, 15 }
        };
        for (int layer = 0; layer < widths.length; layer++) {
            float width = widths[layer];
            int[] c = colors[layer];
            int alpha = (int) (c[3] * strength * night);
            for (int i = 0; i < GALAXY_SEGMENTS; i++) {
                double a0 = i * step;
                double a1 = (i + 1) * step;
                addBandQuad(buffer, matrix, a0, a1, width, c[0], c[1], c[2], alpha);
            }
        }
    }

    private static void addBandQuad(BufferBuilder buffer, org.joml.Matrix4f matrix,
                                    double a0, double a1, float width,
                                    int red, int green, int blue, int alpha) {
        float y0 = -width, y1 = width;
        float r0 = (float) Math.sqrt(RADIUS * RADIUS - y0 * y0);
        float r1 = (float) Math.sqrt(RADIUS * RADIUS - y1 * y1);
        vertex(buffer, matrix, (float) Math.cos(a0) * r0, y0, (float) Math.sin(a0) * r0, red, green, blue, alpha);
        vertex(buffer, matrix, (float) Math.cos(a0) * r1, y1, (float) Math.sin(a0) * r1, red, green, blue, alpha);
        vertex(buffer, matrix, (float) Math.cos(a1) * r1, y1, (float) Math.sin(a1) * r1, red, green, blue, alpha);
        vertex(buffer, matrix, (float) Math.cos(a1) * r0, y0, (float) Math.sin(a1) * r0, red, green, blue, alpha);
    }


    /** Two tiny spiral galaxies, built from cached deterministic point geometry. */
    private static void addDistantGalaxies(BufferBuilder buffer, org.joml.Matrix4f matrix,
                                           float night, float strength) {
        addSpiralGalaxy(buffer, matrix, 1.05f, 0.16f, 0.82f, night, strength,
                118, 145, 235);
        addSpiralGalaxy(buffer, matrix, 4.12f, -0.12f, 0.58f, night, strength,
                174, 145, 238);
    }

    private static void addSpiralGalaxy(BufferBuilder buffer, org.joml.Matrix4f matrix,
                                       float centerAngle, float centerY, float scale,
                                       float night, float strength,
                                       int red, int green, int blue) {
        float horizontal = (float) Math.sqrt(1.0f - centerY * centerY);
        float dirX = (float) Math.cos(centerAngle) * horizontal;
        float dirY = centerY;
        float dirZ = (float) Math.sin(centerAngle) * horizontal;
        float rightX = -(float) Math.sin(centerAngle);
        float rightZ = (float) Math.cos(centerAngle);
        float upX = -centerY * (float) Math.cos(centerAngle);
        float upY = horizontal;
        float upZ = -centerY * (float) Math.sin(centerAngle);
        float cx = dirX * (RADIUS - 1.2f);
        float cy = dirY * (RADIUS - 1.2f);
        float cz = dirZ * (RADIUS - 1.2f);
        final int arms = 3;
        final int pointsPerArm = 22;
        for (int arm = 0; arm < arms; arm++) {
            for (int point = 0; point < pointsPerArm; point++) {
                float t = point / (float) (pointsPerArm - 1);
                float radius = (0.16f + t * 2.25f) * scale;
                float angle = arm * (float) (Math.PI * 2.0 / arms) + t * 5.2f;
                float localX = (float) Math.cos(angle) * radius;
                float localY = (float) Math.sin(angle) * radius * 0.58f;
                float x = cx + rightX * localX + upX * localY;
                float y = cy + upY * localY;
                float z = cz + rightZ * localX + upZ * localY;
                float size = (0.035f + (1.0f - t) * 0.025f) * scale;
                int alpha = (int) ((38.0f + (1.0f - t) * 42.0f) * night * strength);
                int shade = 72 + (int) (28.0f * (1.0f - t));
                int r = Math.min(255, red * shade / 100);
                int g = Math.min(255, green * shade / 100);
                int b = Math.min(255, blue * shade / 100);
                vertex(buffer, matrix, x-size, y-size, z, r, g, b, alpha);
                vertex(buffer, matrix, x-size, y+size, z, r, g, b, alpha);
                vertex(buffer, matrix, x+size, y+size, z, r, g, b, alpha);
                vertex(buffer, matrix, x+size, y-size, z, r, g, b, alpha);
            }
        }
        // Brighter central bulge.
        float core = 0.22f * scale;
        int coreAlpha = (int) (75.0f * night * strength);
        vertex(buffer, matrix, cx-core, cy-core, cz, 220, 225, 255, coreAlpha);
        vertex(buffer, matrix, cx-core, cy+core, cz, 220, 225, 255, coreAlpha);
        vertex(buffer, matrix, cx+core, cy+core, cz, 220, 225, 255, coreAlpha);
        vertex(buffer, matrix, cx+core, cy-core, cz, 220, 225, 255, coreAlpha);
    }

    /** Advances a tiny fixed pool of comets; no objects are allocated per frame. */
    private static float updateComets(StellarConfig config, float night) {
        long now = System.nanoTime();
        if (lastCometUpdateNanos == 0L) {
            lastCometUpdateNanos = now;
            return 0.0f;
        }
        float delta = Math.min(0.1f, (now - lastCometUpdateNanos) / 1_000_000_000.0f);
        lastCometUpdateNanos = now;
        if (night <= 0.01f || !config.shootingStars) {
            for (int i = 0; i < MAX_COMETS; i++) COMET_LIFE[i] = 0.0f;
            return delta;
        }

        float chance = MathHelper.clamp(config.cometChancePerSecond, 0.0f, 0.1f);
        if (COMET_RANDOM.nextFloat() < chance * delta) {
            for (int i = 0; i < MAX_COMETS; i++) {
                if (COMET_LIFE[i] <= 0.0f) {
                    COMET_ANGLE[i] = COMET_RANDOM.nextFloat() * (float) (Math.PI * 2.0);
                    COMET_HEIGHT[i] = (COMET_RANDOM.nextFloat() - 0.5f) * 34.0f;
                    COMET_DURATION[i] = 1.1f + COMET_RANDOM.nextFloat() * 1.2f;
                    COMET_LIFE[i] = COMET_DURATION[i];
                    COMET_SPEED[i] = 0.16f + COMET_RANDOM.nextFloat() * 0.12f;
                    break;
                }
            }
        }
        for (int i = 0; i < MAX_COMETS; i++) {
            if (COMET_LIFE[i] > 0.0f) {
                COMET_LIFE[i] -= delta;
                COMET_ANGLE[i] += COMET_SPEED[i] * delta;
            }
        }
        return delta;
    }

    private static void renderComets(BufferBuilder buffer, org.joml.Matrix4f matrix, float night) {
        for (int i = 0; i < MAX_COMETS; i++) {
            float remaining = COMET_LIFE[i];
            if (remaining <= 0.0f) continue;
            float progress = 1.0f - remaining / COMET_DURATION[i];
            float fadeIn = Math.min(1.0f, progress * 5.0f);
            float fadeOut = Math.min(1.0f, remaining * 2.5f);
            int alpha = (int) (220.0f * night * fadeIn * fadeOut);
            float angle = COMET_ANGLE[i];
            float y = COMET_HEIGHT[i];
            float ring = (float) Math.sqrt(RADIUS * RADIUS - y * y);
            float headX = (float) Math.cos(angle) * ring;
            float headZ = (float) Math.sin(angle) * ring;
            float tailLength = 2.2f + progress * 2.2f;
            float tailX = (float) Math.sin(angle) * tailLength;
            float tailZ = -(float) Math.cos(angle) * tailLength;
            float halfWidth = 0.11f;
            // Bright head and a slim cool-white tail in the same sky batch.
            vertex(buffer, matrix, headX - halfWidth, y, headZ, 205, 225, 255, alpha);
            vertex(buffer, matrix, headX + halfWidth, y, headZ, 205, 225, 255, alpha);
            vertex(buffer, matrix, headX + tailX + halfWidth * 0.25f, y, headZ + tailZ, 112, 164, 255, alpha / 3);
            vertex(buffer, matrix, headX + tailX - halfWidth * 0.25f, y, headZ + tailZ, 112, 164, 255, alpha / 3);
        }
    }

    private static void vertex(BufferBuilder buffer, org.joml.Matrix4f matrix,
                               float x, float y, float z,
                               int red, int green, int blue, int alpha) {
        buffer.vertex(matrix, x, y, z).color(red, green, blue, alpha).next();
    }
}
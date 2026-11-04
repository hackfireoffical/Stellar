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

    private StellarSkyRenderer() { }

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

    public static void renderSky(MatrixStack matrices, StellarConfig config, float nightFactor) {
        if (config == null || !config.enabled || nightFactor <= 0.001f) return;

        int count = MathHelper.clamp(config.starCount, 0, MAX_STARS);
        if (generatedCount != count) generate(count);
        float night = MathHelper.clamp(nightFactor, 0.0f, 1.0f);
        float brightness = MathHelper.clamp(config.starBrightness, 0.0f, 1.0f) * night;

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
                addGalaxyBand(buffer, matrix, night,
                        MathHelper.clamp(config.galaxyBrightness, 0.0f, 1.0f));
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

    private static void vertex(BufferBuilder buffer, org.joml.Matrix4f matrix,
                               float x, float y, float z,
                               int red, int green, int blue, int alpha) {
        buffer.vertex(matrix, x, y, z).color(red, green, blue, alpha).next();
    }
}
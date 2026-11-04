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
 * Cached, geometry-only sky layer. It deliberately avoids framebuffers,
 * shaders with texture samplers, and per-frame random generation.
 */
public final class StellarSkyRenderer {
    private static final int MAX_STARS = 1800;
    private static final float RADIUS = 96.0f;
    // x, y, z, half-size, brightness, red, green, blue
    private static final float[] STARS = new float[MAX_STARS * 8];
    private static int generatedCount = -1;

    private StellarSkyRenderer() { }

    private static void generate(int count) {
        Random random = Random.create(0x57E11A4L);
        for (int i = 0; i < count; i++) {
            // Most stars cluster around a tilted galactic band; the rest fill deep space.
            boolean inBand = random.nextFloat() < 0.68f;
            double y = inBand
                    ? (random.nextGaussian() * 0.105)
                    : (random.nextDouble() * 2.0 - 1.0);
            y = Math.max(-0.98, Math.min(0.98, y));
            double angle = random.nextDouble() * Math.PI * 2.0;
            double ring = Math.sqrt(1.0 - y * y);
            double x = Math.cos(angle) * ring;
            double z = Math.sin(angle) * ring;

            float size = inBand
                    ? 0.045f + random.nextFloat() * 0.095f
                    : 0.075f + random.nextFloat() * 0.19f;
            float brightness = 0.22f + random.nextFloat() * 0.78f;
            float red = 0.70f + random.nextFloat() * 0.30f;
            float green = 0.76f + random.nextFloat() * 0.24f;
            float blue = 0.88f + random.nextFloat() * 0.12f;

            // Subtle cool/lavender tint inside the galactic band.
            if (inBand && random.nextFloat() < 0.42f) {
                red = 0.72f;
                green = 0.78f;
                blue = 1.0f;
            }

            int p = i * 8;
            STARS[p] = (float) x * RADIUS;
            STARS[p + 1] = (float) y * RADIUS;
            STARS[p + 2] = (float) z * RADIUS;
            STARS[p + 3] = size;
            STARS[p + 4] = brightness;
            STARS[p + 5] = red;
            STARS[p + 6] = green;
            STARS[p + 7] = blue;
        }
        generatedCount = count;
    }

    public static void renderSky(MatrixStack matrices, StellarConfig config) {
        if (config == null || !config.enabled || config.starCount <= 0) return;

        int count = MathHelper.clamp(config.starCount, 0, MAX_STARS);
        if (generatedCount != count) generate(count);

        float brightness = MathHelper.clamp(config.starBrightness, 0.0f, 1.0f);
        matrices.push();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        var matrix = matrices.peek().getPositionMatrix();

        for (int i = 0; i < count; i++) {
            int p = i * 8;
            float x = STARS[p];
            float y = STARS[p + 1];
            float z = STARS[p + 2];
            float half = STARS[p + 3];
            int alpha = (int) (255.0f * brightness * STARS[p + 4]);
            int red = (int) (255.0f * STARS[p + 5]);
            int green = (int) (255.0f * STARS[p + 6]);
            int blue = (int) (255.0f * STARS[p + 7]);

            // Tiny quads are cheaper than texture sampling or a post-process.
            vertex(buffer, matrix, x - half, y - half, z, red, green, blue, alpha);
            vertex(buffer, matrix, x - half, y + half, z, red, green, blue, alpha);
            vertex(buffer, matrix, x + half, y + half, z, red, green, blue, alpha);
            vertex(buffer, matrix, x + half, y - half, z, red, green, blue, alpha);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private static void vertex(BufferBuilder buffer, org.joml.Matrix4f matrix,
                               float x, float y, float z, int red, int green, int blue, int alpha) {
        buffer.vertex(matrix, x, y, z).color(red, green, blue, alpha).next();
    }
}

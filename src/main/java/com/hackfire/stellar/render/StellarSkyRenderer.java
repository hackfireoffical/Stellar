package com.hackfire.stellar.render;

import com.hackfire.stellar.config.StellarConfig;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.function.Supplier;

/**
 * Minimal additive star pass. Geometry is generated once and cached; no per-frame
 * object creation or framebuffer/post-processing is used.
 */
public final class StellarSkyRenderer {
    private static final int MAX_STARS = 1800;
    private static final float RADIUS = 96.0f;
    private final Supplier<StellarConfig> config;
    private final float[] stars = new float[MAX_STARS * 5]; // x,y,z,size,brightness
    private int generatedCount = -1;

    public StellarSkyRenderer(Supplier<StellarConfig> config) { this.config = config; }

    private void generate(int count) {
        Random random = Random.create(0x57E11A4L);
        for (int i = 0; i < count; i++) {
            // Uniform directions on a sphere; a mild galactic band adds structure.
            double y = random.nextDouble() * 2.0 - 1.0;
            double angle = random.nextDouble() * Math.PI * 2.0;
            double ring = Math.sqrt(1.0 - y * y);
            double x = Math.cos(angle) * ring;
            double z = Math.sin(angle) * ring;
            double band = Math.pow(Math.max(0.0, 1.0 - Math.abs(y) * 1.65), 2.0);
            float size = 0.10f + random.nextFloat() * 0.22f;
            float brightness = 0.35f + random.nextFloat() * 0.65f + (float) band * 0.22f;
            int p = i * 5;
            stars[p] = (float) x * RADIUS;
            stars[p + 1] = (float) y * RADIUS;
            stars[p + 2] = (float) z * RADIUS;
            stars[p + 3] = size;
            stars[p + 4] = Math.min(1.0f, brightness);
        }
        generatedCount = count;
    }

    public void render(WorldRenderContext context) {
        StellarConfig cfg = config.get();
        if (cfg == null || !cfg.enabled || cfg.starCount <= 0) return;
        int count = Math.min(MAX_STARS, cfg.starCount);
        if (generatedCount != count) generate(count);

        MatrixStack matrices = context.matrixStack();
        matrices.push();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        var matrix = matrices.peek().getPositionMatrix();
        float global = MathHelper.clamp(cfg.starBrightness, 0.0f, 1.0f);
        for (int i = 0; i < count; i++) {
            int p = i * 5;
            float x = stars[p], y = stars[p + 1], z = stars[p + 2];
            float half = stars[p + 3];
            int alpha = (int) (255.0f * global * stars[p + 4]);
            // Small camera-facing diamond/square, intentionally no bloom pass.
            vertex(buffer, matrix, x - half, y - half, z, alpha);
            vertex(buffer, matrix, x - half, y + half, z, alpha);
            vertex(buffer, matrix, x + half, y + half, z, alpha);
            vertex(buffer, matrix, x + half, y - half, z, alpha);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private static void vertex(BufferBuilder buffer, org.joml.Matrix4f matrix,
                               float x, float y, float z, int alpha) {
        buffer.vertex(matrix, x, y, z).color(190, 215, 255, alpha).next();
    }
}

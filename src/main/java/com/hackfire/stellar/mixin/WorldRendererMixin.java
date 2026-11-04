package com.hackfire.stellar.mixin;

import com.hackfire.stellar.StellarClient;
import com.hackfire.stellar.render.StellarSkyRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds Stellar's geometry after vanilla has drawn the overworld sky. */
@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Inject(
        method = "renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V",
        at = @At("TAIL")
    )
    private void stellar$renderSky(MatrixStack matrices, Matrix4f projectionMatrix,
                                   float tickDelta, Camera camera, boolean thickFog,
                                   Runnable fogCallback, CallbackInfo callbackInfo) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        float time = (client.world.getTimeOfDay() % 24000L) + tickDelta;
        float night;
        if (time < 12000.0f || time > 23000.0f) {
            night = 0.0f;
        } else if (time < 14000.0f) {
            night = (time - 12000L) / 2000.0f;
        } else if (time <= 22000.0f) {
            night = 1.0f;
        } else {
            night = (23000L - time) / 1000.0f;
        }
        float rain = MathHelper.clamp(client.world.getRainGradient(tickDelta), 0.0f, 1.0f);
        float thunder = MathHelper.clamp(client.world.getThunderGradient(tickDelta), 0.0f, 1.0f);
        // Thunderstorms suppress the distant sky more strongly than ordinary rain.
        float weatherVisibility = 1.0f - Math.max(rain * 0.68f, thunder * 0.88f);
        float sunset = time >= 11000.0f && time <= 15000.0f
                ? 1.0f - Math.abs(time - 13000L) / 2000.0f : 0.0f;
        float sunrise = time >= 22000.0f
                ? 1.0f - Math.abs(time - 23000L) / 1000.0f
                 : (time <= 1000.0f ? 1.0f - Math.abs(time - 0L) / 1000.0f : 0.0f);
        float twilight = MathHelper.clamp(Math.max(sunset, sunrise), 0.0f, 1.0f);
        StellarSkyRenderer.renderSky(matrices, StellarClient.config(), night, weatherVisibility, twilight);
    }
}
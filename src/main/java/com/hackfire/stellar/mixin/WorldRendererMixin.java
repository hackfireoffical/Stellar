package com.hackfire.stellar.mixin;

import com.hackfire.stellar.StellarClient;
import com.hackfire.stellar.render.StellarSkyRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
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
        long time = client.world.getTimeOfDay() % 24000L;
        float night;
        if (time < 12000L || time > 23000L) {
            night = 0.0f;
        } else if (time < 14000L) {
            night = (time - 12000L) / 2000.0f;
        } else if (time <= 22000L) {
            night = 1.0f;
        } else {
            night = (23000L - time) / 1000.0f;
        }
        StellarSkyRenderer.renderSky(matrices, StellarClient.config(), night);
    }
}
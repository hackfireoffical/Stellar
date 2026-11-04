package com.hackfire.stellar.mixin;

import com.hackfire.stellar.StellarClient;
import com.hackfire.stellar.render.StellarSkyRenderer;
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
        StellarSkyRenderer.renderSky(matrices, StellarClient.config());
    }
}

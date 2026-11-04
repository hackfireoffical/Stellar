package com.hackfire.stellar.mixin;

import com.hackfire.stellar.StellarClient;
import com.hackfire.stellar.config.StellarConfig;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Gently harmonizes biome water tints without changing vanilla transparency or render layers. */
@Mixin(BiomeColors.class)
public abstract class WaterColorMixin {
    @Inject(method = "getWaterColor(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/util/math/BlockPos;)I",
            at = @At("RETURN"), cancellable = true)
    private static void stellar$tintWater(BlockRenderView world, BlockPos pos,
                                          CallbackInfoReturnable<Integer> cir) {
        StellarConfig config = StellarClient.config();
        if (config == null || !config.enabled || !config.waterTintEnabled) return;

        int original = cir.getReturnValue();
        float strength = Math.max(0.0f, Math.min(1.0f, config.waterTintStrength));
        if (strength <= 0.0f) return;

        // A restrained blue-cyan tint; biome identity remains visible underneath.
        int targetRed = 54;
        int targetGreen = 139;
        int targetBlue = 190;
        int red = (int) (((original >> 16) & 255) * (1.0f - strength) + targetRed * strength);
        int green = (int) (((original >> 8) & 255) * (1.0f - strength) + targetGreen * strength);
        int blue = (int) ((original & 255) * (1.0f - strength) + targetBlue * strength);
        cir.setReturnValue((red << 16) | (green << 8) | blue);
    }
}

package io.github.caddmix.lightupdate.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.caddmix.lightupdate.light.BlockEntityLight;
import net.minecraft.client.renderer.blockentity.BrightnessCombiner;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Double chests and beds. method_24193 is the lambda in acceptDouble that rereads raw light for both halves
 * and rounds it down. Optional, a missing target only costs smooth double chests.
 */
@Mixin(value = BrightnessCombiner.class, priority = 900)
public abstract class BrightnessCombinerMixin {
	@ModifyExpressionValue(method = "method_24193", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I", ordinal = 0), require = 0)
	private static int lightupdate$smoothFirst(int light, @Local(argsOnly = true, ordinal = 0) BlockEntity first) {
		return BlockEntityLight.smooth(first, light);
	}

	@ModifyExpressionValue(method = "method_24193", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I", ordinal = 1), require = 0)
	private static int lightupdate$smoothSecond(int light, @Local(argsOnly = true, ordinal = 1) BlockEntity second) {
		return BlockEntityLight.smooth(second, light);
	}

	@ModifyReturnValue(method = "method_24193", at = @At("RETURN"), require = 0)
	private static int lightupdate$combine(int original, @Local(argsOnly = true, ordinal = 0) BlockEntity first, @Local(argsOnly = true, ordinal = 1) BlockEntity second) {
		return BlockEntityLight.combine(first, second, original);
	}
}

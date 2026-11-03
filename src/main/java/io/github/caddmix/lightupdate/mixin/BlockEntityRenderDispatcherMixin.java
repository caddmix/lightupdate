package io.github.caddmix.lightupdate.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.caddmix.lightupdate.light.BlockEntityLight;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Same priority reasoning as {@link EntityRendererMixin}. Sodium renders block entities through here too. */
@Mixin(value = BlockEntityRenderDispatcher.class, priority = 900)
public abstract class BlockEntityRenderDispatcherMixin {
	@ModifyExpressionValue(
			method = "setupAndRender",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I")
	)
	private static int lightupdate$smooth(int light, @Local(argsOnly = true) BlockEntity blockEntity) {
		return BlockEntityLight.smooth(blockEntity, light);
	}
}

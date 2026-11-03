package io.github.caddmix.lightupdate.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.github.caddmix.lightupdate.light.EntityLight;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Priority below the default 1000 so mods that adjust entity light after vanilla (flicker, emissive
 * overrides) apply on top of the smoothed value instead of getting smoothed away.
 */
@Mixin(value = EntityRenderer.class, priority = 900)
public abstract class EntityRendererMixin<T extends Entity> {
	/** keeps the probe vanilla already computed, so the mod never calls it a second time */
	@WrapOperation(
			method = "getPackedLightCoords",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getLightProbePosition(F)Lnet/minecraft/world/phys/Vec3;")
	)
	private Vec3 lightupdate$captureProbe(Entity entity, float partialTick, Operation<Vec3> original, @Share("probe") LocalRef<Vec3> probe) {
		Vec3 position = original.call(entity, partialTick);
		probe.set(position);
		return position;
	}

	@ModifyReturnValue(method = "getPackedLightCoords", at = @At("RETURN"))
	private int lightupdate$smooth(int original, @Local(argsOnly = true) T entity, @Share("probe") LocalRef<Vec3> probe) {
		return EntityLight.smooth(entity, probe.get(), original);
	}
}

package io.github.caddmix.lightupdate.mixin;

import io.github.caddmix.lightupdate.light.LightEpochs;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The light engine reports changed sections here, vanilla and scalablelux both do. */
@Mixin(ClientChunkCache.class)
public abstract class ClientChunkCacheMixin {
	@Inject(method = "onLightUpdate", at = @At("HEAD"))
	private void lightupdate$sectionChanged(LightLayer layer, SectionPos pos, CallbackInfo ci) {
		LightEpochs.sectionChanged(pos.x(), pos.y(), pos.z());
	}
}

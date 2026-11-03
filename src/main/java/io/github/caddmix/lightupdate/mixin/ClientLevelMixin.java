package io.github.caddmix.lightupdate.mixin;

import io.github.caddmix.lightupdate.light.LightEpochs;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Light data the server sends goes into the light engine without the onLightUpdate callback, but every
 * received section gets marked dirty here, so the caches near it resample too.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
	@Inject(method = "setSectionDirtyWithNeighbors", at = @At("HEAD"))
	private void lightupdate$sectionChanged(int sectionX, int sectionY, int sectionZ, CallbackInfo ci) {
		LightEpochs.sectionChanged(sectionX, sectionY, sectionZ);
	}
}

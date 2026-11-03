package io.github.caddmix.lightupdate.mixin;

import io.github.caddmix.lightupdate.light.FrameClock;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void lightupdate$beginFrame(CallbackInfo ci) {
		FrameClock.beginFrame();
	}
}

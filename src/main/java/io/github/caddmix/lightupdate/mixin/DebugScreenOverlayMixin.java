package io.github.caddmix.lightupdate.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.caddmix.lightupdate.LightUpdate;
import io.github.caddmix.lightupdate.debug.LightStats;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenOverlayMixin {
	@ModifyReturnValue(method = "getGameInformation", at = @At("RETURN"), require = 0)
	private List<String> lightupdate$addStats(List<String> lines) {
		if (!LightUpdate.config().debugStats) {
			return lines;
		}

		List<String> result = lines instanceof ArrayList<String> ? lines : new ArrayList<>(lines);
		result.add(LightUpdate.enabled() ? LightStats.line() : "lightupdate off");
		return result;
	}
}

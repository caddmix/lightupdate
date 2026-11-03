package io.github.caddmix.lightupdate.light;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.caddmix.lightupdate.LightUpdate;
import io.github.caddmix.lightupdate.access.BlockEntityLightHolder;
import io.github.caddmix.lightupdate.debug.LightStats;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Block entities sit still, so their light only changes when the world light changes. Those changes fade
 * instead of snapping. In between values from other mods (lambdynamiclights writes some) pass through.
 */
public final class BlockEntityLight {
	private BlockEntityLight() {
	}

	public static int smooth(BlockEntity blockEntity, int original) {
		if (!LightUpdate.blockEntitiesActive() || !RenderSystem.isOnRenderThread()) {
			return original;
		}

		FadeState state = ((BlockEntityLightHolder) blockEntity).lightupdate$blockEntityLight();
		int frame = FrameClock.frame();
		if (state.frame == frame) {
			return state.packed;
		}

		long now = FrameClock.nanos();
		if (state.settled && original == state.original && FrameClock.continuous(state.nanos)) {
			state.frame = frame;
			state.nanos = now;
			return state.packed;
		}

		float targetBlock = LightMath.blockLevel(original);
		float targetSky = LightMath.skyLevel(original);
		state.fadeTowards(targetBlock, targetSky, frame, now, false);
		state.original = original;
		if (state.settled) {
			// hand back the exact bits so emissive and modded values stay untouched
			state.packed = original;
		}

		LightStats.blockEntity();
		return state.packed;
	}

	/**
	 * Double chests and beds light both halves with the brighter one. Vanilla recomputes that from raw light,
	 * this takes the per channel max of the two faded values instead.
	 */
	public static int combine(BlockEntity first, BlockEntity second, int original) {
		if (!LightUpdate.blockEntitiesActive() || !RenderSystem.isOnRenderThread()) {
			return original;
		}

		FadeState a = ((BlockEntityLightHolder) first).lightupdate$blockEntityLight();
		FadeState b = ((BlockEntityLightHolder) second).lightupdate$blockEntityLight();
		int frame = FrameClock.frame();
		if (a.frame != frame || b.frame != frame) {
			return original;
		}

		LightStats.doubleBlockMerge();
		return Math.max(LightMath.blockUnits(a.packed), LightMath.blockUnits(b.packed))
				| Math.max(LightMath.skyUnits(a.packed), LightMath.skyUnits(b.packed)) << 16;
	}
}

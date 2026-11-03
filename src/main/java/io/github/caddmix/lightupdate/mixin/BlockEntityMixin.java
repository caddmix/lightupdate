package io.github.caddmix.lightupdate.mixin;

import io.github.caddmix.lightupdate.access.BlockEntityLightHolder;
import io.github.caddmix.lightupdate.light.FadeState;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin implements BlockEntityLightHolder {
	@Unique
	private FadeState lightupdate$light;

	@Override
	public FadeState lightupdate$blockEntityLight() {
		FadeState state = lightupdate$light;
		if (state == null) {
			state = new FadeState();
			lightupdate$light = state;
		}

		return state;
	}
}

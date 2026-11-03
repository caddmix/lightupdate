package io.github.caddmix.lightupdate.mixin;

import io.github.caddmix.lightupdate.access.EntityLightHolder;
import io.github.caddmix.lightupdate.light.EntityLightState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class EntityMixin implements EntityLightHolder {
	@Unique
	private EntityLightState lightupdate$light;

	@Override
	public EntityLightState lightupdate$entityLight() {
		EntityLightState state = lightupdate$light;
		if (state == null) {
			state = new EntityLightState();
			lightupdate$light = state;
		}

		return state;
	}
}

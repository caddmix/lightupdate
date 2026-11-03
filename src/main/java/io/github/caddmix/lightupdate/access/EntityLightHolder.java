package io.github.caddmix.lightupdate.access;

import io.github.caddmix.lightupdate.light.EntityLightState;

/** Implemented on Entity by a mixin, the state lives in a field so lookups cost one field read. */
public interface EntityLightHolder {
	EntityLightState lightupdate$entityLight();
}

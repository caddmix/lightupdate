package io.github.caddmix.lightupdate.access;

import io.github.caddmix.lightupdate.light.FadeState;

/** Implemented on BlockEntity by a mixin, the state lives in a field so lookups cost one field read. */
public interface BlockEntityLightHolder {
	FadeState lightupdate$blockEntityLight();
}

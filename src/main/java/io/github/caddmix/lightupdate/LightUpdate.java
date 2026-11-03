package io.github.caddmix.lightupdate;

import io.github.caddmix.lightupdate.config.LightUpdateConfig;
import io.github.caddmix.lightupdate.debug.LightStats;
import io.github.caddmix.lightupdate.light.FrameClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LightUpdate {
	public static final String MOD_ID = "lightupdate";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static LightUpdateConfig config = new LightUpdateConfig();
	/** runtime switch, starts from the config and flips with the toggle key */
	private static boolean enabled = true;

	private LightUpdate() {
	}

	public static LightUpdateConfig config() {
		return config;
	}

	public static void applyConfig(LightUpdateConfig newConfig) {
		config = newConfig;
		enabled = newConfig.enabled;
		FrameClock.setFadeMillis(newConfig.fadeMs);
		LightStats.setEnabled(newConfig.debugStats);
	}

	public static boolean enabled() {
		return enabled;
	}

	public static boolean toggle() {
		enabled = !enabled;
		return enabled;
	}

	public static boolean entitiesActive() {
		return enabled && config.entities;
	}

	public static boolean blockEntitiesActive() {
		return enabled && config.blockEntities;
	}
}

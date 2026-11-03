package io.github.caddmix.lightupdate.debug;

import java.util.Locale;

/** Per second counters for the f3 line. Everything is a no op while debug_stats is off. */
public final class LightStats {
	private static final long WINDOW_NANOS = 1_000_000_000L;

	private static boolean enabled;
	private static long windowStart;
	private static long entityUpdates;
	private static long entitySamples;
	private static long entityNanos;
	private static long blockEntityUpdates;
	private static long doubleBlockMerges;
	private static String line = "lightupdate stats warming up";

	private LightStats() {
	}

	public static void setEnabled(boolean value) {
		enabled = value;
	}

	public static long begin() {
		return enabled ? System.nanoTime() : 0L;
	}

	public static void entity(long started, boolean sampled) {
		if (!enabled) {
			return;
		}

		entityUpdates++;
		if (sampled) {
			entitySamples++;
		}

		entityNanos += System.nanoTime() - started;
	}

	public static void blockEntity() {
		if (enabled) {
			blockEntityUpdates++;
		}
	}

	public static void doubleBlockMerge() {
		if (enabled) {
			doubleBlockMerges++;
		}
	}

	public static void frame(long now) {
		if (!enabled) {
			return;
		}

		if (windowStart == 0L) {
			windowStart = now;
			return;
		}

		if (now - windowStart < WINDOW_NANOS) {
			return;
		}

		double seconds = (now - windowStart) / 1.0e9;
		line = String.format(Locale.ROOT, "lightupdate %d entity updates/s, %.1f%% resampled, %d ns each, %d block entity updates/s, %d double block merges/s",
				Math.round(entityUpdates / seconds),
				entityUpdates == 0 ? 0.0 : 100.0 * entitySamples / entityUpdates,
				entityUpdates == 0 ? 0 : entityNanos / entityUpdates,
				Math.round(blockEntityUpdates / seconds),
				Math.round(doubleBlockMerges / seconds));
		windowStart = now;
		entityUpdates = 0;
		entitySamples = 0;
		entityNanos = 0;
		blockEntityUpdates = 0;
		doubleBlockMerges = 0;
	}

	public static String line() {
		return line;
	}
}

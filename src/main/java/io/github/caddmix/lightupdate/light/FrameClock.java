package io.github.caddmix.lightupdate.light;

import io.github.caddmix.lightupdate.debug.LightStats;

/**
 * One tick per rendered frame. Every fade in a frame shares the same time step, so the exp() runs once per
 * frame instead of once per entity.
 */
public final class FrameClock {
	/** a state not updated for longer than this snaps to its target instead of fading from an old value */
	private static final long MAX_GAP_NANOS = 250_000_000L;

	private static int frame;
	private static long nanos;
	private static float frameAlpha = 1f;
	private static float tauSeconds = 0.25f / 3f;

	private FrameClock() {
	}

	/** called at the start of every LevelRenderer.renderLevel */
	public static void beginFrame() {
		long now = System.nanoTime();
		long step = nanos == 0L ? 0L : now - nanos;
		nanos = now;
		frame++;
		frameAlpha = LightMath.fadeAlpha(step * 1.0e-9f, tauSeconds);
		LightStats.frame(now);
	}

	public static int frame() {
		return frame;
	}

	public static long nanos() {
		return nanos;
	}

	/** fade share for a state that was updated exactly one frame ago */
	public static float frameAlpha() {
		return frameAlpha;
	}

	public static float alphaSince(long lastNanos) {
		return LightMath.fadeAlpha((nanos - lastNanos) * 1.0e-9f, tauSeconds);
	}

	public static boolean continuous(long lastNanos) {
		return nanos - lastNanos <= MAX_GAP_NANOS;
	}

	/** fadeMillis is the time a fade needs to settle, about three time constants */
	public static void setFadeMillis(int fadeMillis) {
		tauSeconds = Math.max(0, fadeMillis) / 3000f;
	}
}

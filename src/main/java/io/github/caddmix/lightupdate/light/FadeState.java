package io.github.caddmix.lightupdate.light;

/** Per object light state for the time fade. One instance per rendered entity or block entity, made once. */
public class FadeState {
	private static final int NEVER = Integer.MIN_VALUE;

	/** frame of the last update, the result is reused for repeated calls in the same frame */
	public int frame = NEVER;
	public long nanos;
	public float block;
	public float sky;
	public int packed;

	/** moves the faded levels toward the target and stores the packed result for this frame */
	public final void fadeTowards(float targetBlock, float targetSky, int currentFrame, long now, boolean snap) {
		if (snap || frame == NEVER || !FrameClock.continuous(nanos)) {
			block = targetBlock;
			sky = targetSky;
		} else {
			float alpha = frame == currentFrame - 1 ? FrameClock.frameAlpha() : FrameClock.alphaSince(nanos);
			block = LightMath.fade(block, targetBlock, alpha);
			sky = LightMath.fade(sky, targetSky, alpha);
		}

		frame = currentFrame;
		nanos = now;
		packed = LightMath.pack(block, sky);
	}
}

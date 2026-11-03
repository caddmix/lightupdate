package io.github.caddmix.lightupdate.light;

/** Fade state plus the cached light of the 8 blocks around the entity light probe. */
public final class EntityLightState extends FadeState {
	public static final long NO_CELL = Long.MIN_VALUE;

	/** BlockPos.asLong of corner 0 */
	public long cell = NO_CELL;
	/** light epoch of the corner 0 section at sample time */
	public int epoch;
	public long sampledNanos;
	/** 8 corners, block light in the low nibble and sky light in the high nibble of each byte */
	public long corners;
	/** corners that can hold light, solid blocks with zero light drop out */
	public int validMask;
	/** a fresh sample already confirmed that a renderer overrides the light here, skip more checks */
	public boolean overrideConfirmed;
}

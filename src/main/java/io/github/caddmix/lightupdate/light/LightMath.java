package io.github.caddmix.lightupdate.light;

/**
 * Pure light math with no game classes, so unit tests run without minecraft.
 *
 * Packed light uses the vanilla lightmap layout (block in the low 16 bits, sky in the high 16 bits,
 * 16 units per light level) but keeps the in between values that vanilla rounds away.
 */
public final class LightMath {
	public static final int UNITS_PER_LEVEL = 16;
	public static final int MAX_UNITS = 15 * UNITS_PER_LEVEL;
	/** returned by {@link #blend} when none of the valid corners carries weight */
	public static final long NO_BLEND = -1L;
	/** below this distance a fade snaps onto its target, a quarter of the 1/16 level the lightmap can show */
	private static final float SNAP = 1f / 64f;

	private LightMath() {
	}

	public static int pack(float block, float sky) {
		return units(block) | units(sky) << 16;
	}

	private static int units(float level) {
		int units = Math.round(level * UNITS_PER_LEVEL);
		return units < 0 ? 0 : Math.min(units, MAX_UNITS);
	}

	public static int blockUnits(int packed) {
		return packed & 0xFFFF;
	}

	public static int skyUnits(int packed) {
		return packed >>> 16;
	}

	public static float blockLevel(int packed) {
		return blockUnits(packed) / (float) UNITS_PER_LEVEL;
	}

	public static float skyLevel(int packed) {
		return skyUnits(packed) / (float) UNITS_PER_LEVEL;
	}

	/** stores one corner as a byte, block light in the low nibble and sky light in the high nibble */
	public static long putCorner(long corners, int index, int block, int sky) {
		return corners | (long) ((block & 0xF) | (sky & 0xF) << 4) << (index << 3);
	}

	public static int cornerBlock(long corners, int index) {
		return (int) (corners >>> (index << 3)) & 0xF;
	}

	public static int cornerSky(long corners, int index) {
		return (int) (corners >>> ((index << 3) + 4)) & 0xF;
	}

	/**
	 * Trilinear blend of the 8 corner lights around a point. Corner index bits are x | y << 1 | z << 2 and
	 * fx, fy, fz are the offsets of the point from corner 0, each in [0, 1). Corners with a clear bit in
	 * validMask drop out and the weights of the rest get renormalized, so solid blocks next to an entity
	 * do not drag its light down.
	 *
	 * @return block and sky level as float bits, see {@link #first} and {@link #second}, or {@link #NO_BLEND}
	 */
	public static long blend(long corners, int validMask, float fx, float fy, float fz) {
		float sumWeight = 0f;
		float sumBlock = 0f;
		float sumSky = 0f;

		for (int i = 0; i < 8; i++) {
			if ((validMask >>> i & 1) == 0) {
				continue;
			}

			float weight = ((i & 1) != 0 ? fx : 1f - fx)
					* ((i & 2) != 0 ? fy : 1f - fy)
					* ((i & 4) != 0 ? fz : 1f - fz);
			int corner = (int) (corners >>> (i << 3)) & 0xFF;
			sumWeight += weight;
			sumBlock += weight * (corner & 0xF);
			sumSky += weight * (corner >>> 4);
		}

		if (sumWeight < 1.0e-4f) {
			return NO_BLEND;
		}

		float inverse = 1f / sumWeight;
		return twoFloats(sumBlock * inverse, sumSky * inverse);
	}

	public static long twoFloats(float first, float second) {
		return Float.floatToRawIntBits(first) & 0xFFFFFFFFL | (long) Float.floatToRawIntBits(second) << 32;
	}

	public static float first(long two) {
		return Float.intBitsToFloat((int) two);
	}

	public static float second(long two) {
		return Float.intBitsToFloat((int) (two >>> 32));
	}

	/**
	 * Keeps renderer overrides intact. originalUnits is what vanilla (and any mod before us) returned for one
	 * channel, worldLevel is the plain world light in the block the probe sits in and smooth is the blended level.
	 * Equal means nothing overrode the light, so the smooth value wins. A brighter override (blazes, glow squids,
	 * dynamic light mods) wins whenever it beats the smooth value and a darker one whenever it undercuts it.
	 */
	public static float resolve(int originalUnits, int worldLevel, float smooth) {
		int worldUnits = worldLevel * UNITS_PER_LEVEL;
		if (originalUnits == worldUnits) {
			return smooth;
		}

		float original = originalUnits / (float) UNITS_PER_LEVEL;
		return originalUnits > worldUnits ? Math.max(original, smooth) : Math.min(original, smooth);
	}

	/** share of the remaining distance a fade covers in dtSeconds, the same at any frame rate */
	public static float fadeAlpha(float dtSeconds, float tauSeconds) {
		if (tauSeconds <= 0f) {
			return 1f;
		}

		if (dtSeconds <= 0f) {
			return 0f;
		}

		return 1f - (float) Math.exp(-dtSeconds / tauSeconds);
	}

	public static float fade(float current, float target, float alpha) {
		float next = current + (target - current) * alpha;
		return Math.abs(target - next) < SNAP ? target : next;
	}
}

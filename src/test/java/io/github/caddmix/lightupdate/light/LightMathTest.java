package io.github.caddmix.lightupdate.light;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LightMathTest {
	private static final float EPSILON = 1.0e-4f;

	private static long uniform(int block, int sky) {
		long corners = 0L;
		for (int i = 0; i < 8; i++) {
			corners = LightMath.putCorner(corners, i, block, sky);
		}
		return corners;
	}

	@Test
	void wholeLevelsPackLikeVanilla() {
		for (int block = 0; block <= 15; block++) {
			for (int sky = 0; sky <= 15; sky++) {
				// LightTexture.pack in 1.21.1
				int vanilla = block << 4 | sky << 20;
				assertEquals(vanilla, LightMath.pack(block, sky));
				assertEquals(block, LightMath.blockLevel(vanilla), EPSILON);
				assertEquals(sky, LightMath.skyLevel(vanilla), EPSILON);
			}
		}
	}

	@Test
	void packKeepsSixteenthsAndClamps() {
		assertEquals(120, LightMath.blockUnits(LightMath.pack(7.5f, 0f)));
		assertEquals(61, LightMath.skyUnits(LightMath.pack(0f, 3.8f)));
		assertEquals(240, LightMath.blockUnits(LightMath.pack(99f, 0f)));
		assertEquals(0, LightMath.skyUnits(LightMath.pack(0f, -3f)));
	}

	@Test
	void cornersRoundTrip() {
		long corners = 0L;
		for (int i = 0; i < 8; i++) {
			corners = LightMath.putCorner(corners, i, i, 15 - i);
		}
		for (int i = 0; i < 8; i++) {
			assertEquals(i, LightMath.cornerBlock(corners, i));
			assertEquals(15 - i, LightMath.cornerSky(corners, i));
		}
	}

	@Test
	void uniformLightBlendsToItself() {
		long blended = LightMath.blend(uniform(9, 13), 0xFF, 0.3f, 0.7f, 0.1f);
		assertEquals(9f, LightMath.first(blended), EPSILON);
		assertEquals(13f, LightMath.second(blended), EPSILON);
	}

	@Test
	void blendIsTrilinear() {
		// x = 0 side has block 4, x = 1 side has block 12
		long corners = 0L;
		for (int i = 0; i < 8; i++) {
			corners = LightMath.putCorner(corners, i, (i & 1) == 0 ? 4 : 12, 15);
		}
		assertEquals(4f, LightMath.first(LightMath.blend(corners, 0xFF, 0f, 0.5f, 0.5f)), EPSILON);
		assertEquals(8f, LightMath.first(LightMath.blend(corners, 0xFF, 0.5f, 0.5f, 0.5f)), EPSILON);
		assertEquals(10f, LightMath.first(LightMath.blend(corners, 0xFF, 0.75f, 0.2f, 0.9f)), EPSILON);
	}

	@Test
	void solidCornersDoNotDarken() {
		// floor blocks (y = 0 corners) are solid with zero light, the air above has block light 10
		long corners = 0L;
		for (int i = 0; i < 8; i++) {
			if ((i & 2) != 0) {
				corners = LightMath.putCorner(corners, i, 10, 0);
			}
		}
		int validMask = 0b1100_1100;
		assertEquals(10f, LightMath.first(LightMath.blend(corners, validMask, 0.4f, 0.1f, 0.6f)), EPSILON);
	}

	@Test
	void noValidCornerMeansNoBlend() {
		assertEquals(LightMath.NO_BLEND, LightMath.blend(0L, 0, 0.5f, 0.5f, 0.5f));
		// only corner 7 is valid and the point sits on corner 0, so it carries no weight
		assertEquals(LightMath.NO_BLEND, LightMath.blend(uniform(5, 5), 0b1000_0000, 0f, 0f, 0f));
	}

	/** the case from the user clip, walking along a light gradient of one level per block */
	@Test
	void walkingAcrossBlocksHasNoJumps() {
		float previous = Float.NaN;
		float biggestStep = 0f;
		for (int step = 0; step <= 400; step++) {
			double x = 2.0 + step * 0.01; // probe x from 2.0 to 6.0
			double qx = x - 0.5;
			int bx = (int) Math.floor(qx);
			// block light at block coordinate n is n
			long corners = 0L;
			for (int i = 0; i < 8; i++) {
				corners = LightMath.putCorner(corners, i, bx + (i & 1), 15);
			}
			float level = LightMath.first(LightMath.blend(corners, 0xFF, (float) (qx - bx), 0.5f, 0.5f));
			if (!Float.isNaN(previous)) {
				biggestStep = Math.max(biggestStep, Math.abs(level - previous));
			}
			previous = level;
		}
		// vanilla jumps a full level at each border, the blend moves by the walked distance
		assertTrue(biggestStep < 0.011f, "biggest step " + biggestStep);
	}

	@Test
	void resolveKeepsOverrides() {
		// no override
		assertEquals(6.4f, LightMath.resolve(6 << 4, 6, 6.4f), EPSILON);
		// blaze and burning entities force 15
		assertEquals(15f, LightMath.resolve(15 << 4, 4, 4.7f), EPSILON);
		// glow item frame keeps max(5, world)
		assertEquals(5f, LightMath.resolve(5 << 4, 2, 2.6f), EPSILON);
		assertEquals(5.3f, LightMath.resolve(5 << 4, 4, 5.3f), EPSILON);
		// a darkening override wins when it undercuts
		assertEquals(3f, LightMath.resolve(3 << 4, 8, 7.6f), EPSILON);
		// in between values from other mods pass through as overrides
		assertEquals(9.5f, LightMath.resolve(152, 4, 4.2f), EPSILON);
	}

	@Test
	void fadeIsFrameRateIndependent() {
		float tau = 0.25f / 3f;
		float at60 = runFade(60, 0.15f, tau);
		float at240 = runFade(240, 0.15f, tau);
		float exact = 15f * (1f - (float) Math.exp(-0.15 / tau));
		assertEquals(exact, at60, 0.02f);
		assertEquals(exact, at240, 0.02f);
	}

	private static float runFade(int fps, float seconds, float tau) {
		float value = 0f;
		float alpha = LightMath.fadeAlpha(1f / fps, tau);
		int frames = Math.round(seconds * fps);
		for (int i = 0; i < frames; i++) {
			value = LightMath.fade(value, 15f, alpha);
		}
		return value;
	}

	@Test
	void fadeSnapsWhenClose() {
		assertEquals(10f, LightMath.fade(9.99f, 10f, 0.1f), 0f);
		assertEquals(1f, LightMath.fadeAlpha(0.016f, 0f), 0f);
		assertEquals(0f, LightMath.fadeAlpha(0f, 0.1f), 0f);
	}
}

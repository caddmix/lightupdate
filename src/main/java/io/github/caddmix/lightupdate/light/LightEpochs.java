package io.github.caddmix.lightupdate.light;

/**
 * A counter per chunk section (hashed into a fixed table) that goes up whenever light in or next to that
 * section changes. Entity caches remember the counter of the section their corner 0 sits in and resample
 * when it moves. Hash collisions only cause extra resamples.
 *
 * Plain int writes are fine here, a lost increment only delays a resample until the next change or the
 * periodic backstop in {@link EntityLight}.
 */
public final class LightEpochs {
	private static final int BITS = 12;
	private static final int MASK = (1 << BITS) - 1;
	private static final int[] EPOCHS = new int[1 << BITS];

	private LightEpochs() {
	}

	private static int slot(int sectionX, int sectionY, int sectionZ) {
		int hash = sectionX * 0x9E3779B1 ^ sectionY * 0x85EBCA77 ^ sectionZ * 0xC2B2AE3D;
		return (hash ^ hash >>> 15) & MASK;
	}

	public static int get(int sectionX, int sectionY, int sectionZ) {
		return EPOCHS[slot(sectionX, sectionY, sectionZ)];
	}

	/**
	 * Light changed in a section. The 8 corners of an entity cache reach from its corner 0 one block in the
	 * positive direction, so caches anchored in this section or one section below it on any axis can see the
	 * change. Vanilla also reports neighbor sections, scalablelux only reports the changed ones, so the
	 * mod covers the neighbors itself.
	 */
	public static void sectionChanged(int sectionX, int sectionY, int sectionZ) {
		for (int dx = 0; dx <= 1; dx++) {
			for (int dy = 0; dy <= 1; dy++) {
				for (int dz = 0; dz <= 1; dz++) {
					EPOCHS[slot(sectionX - dx, sectionY - dy, sectionZ - dz)]++;
				}
			}
		}
	}
}

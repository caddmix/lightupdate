package io.github.caddmix.lightupdate.light;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.caddmix.lightupdate.LightUpdate;
import io.github.caddmix.lightupdate.access.EntityLightHolder;
import io.github.caddmix.lightupdate.debug.LightStats;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

/**
 * Smooth light for entities. Vanilla reads one light value from the block the eyes sit in, so the light
 * jumps a whole level at every block border. This blends the 8 blocks around the probe instead and fades
 * changes the blend cannot explain, like a torch placed next to a mob.
 */
public final class EntityLight {
	/** resample at least this often, a backstop for light engines that skip the change callback */
	private static final long RESAMPLE_NANOS = 2_000_000_000L;
	/** render thread only, see the thread check in {@link #smooth} */
	private static final BlockPos.MutableBlockPos POS = new BlockPos.MutableBlockPos();

	private EntityLight() {
	}

	/**
	 * @param probe    the light probe position vanilla used, usually the eye position
	 * @param original packed light vanilla computed, renderer overrides included
	 */
	public static int smooth(Entity entity, Vec3 probe, int original) {
		if (!LightUpdate.entitiesActive() || probe == null || !RenderSystem.isOnRenderThread()) {
			return original;
		}

		EntityLightState state = ((EntityLightHolder) entity).lightupdate$entityLight();
		int frame = FrameClock.frame();
		if (state.frame == frame) {
			return state.packed;
		}

		long started = LightStats.begin();
		long now = FrameClock.nanos();
		Level level = entity.level();

		// block centers sit at integer coordinates after this shift, so corner 0 is the floor of the point
		double qx = probe.x - 0.5;
		double qy = probe.y - 0.5;
		double qz = probe.z - 0.5;
		int bx = Mth.floor(qx);
		int by = Mth.floor(qy);
		int bz = Mth.floor(qz);
		long cell = BlockPos.asLong(bx, by, bz);
		int epoch = LightEpochs.get(bx >> 4, by >> 4, bz >> 4);
		long previousCell = state.cell;

		// standing still with nothing changed around it, the last result still holds
		if (state.settled && probe.x == state.probeX && probe.y == state.probeY && probe.z == state.probeZ
				&& original == state.original && epoch == state.epoch && cell == previousCell
				&& now - state.sampledNanos <= RESAMPLE_NANOS && FrameClock.continuous(state.nanos)) {
			state.frame = frame;
			state.nanos = now;
			LightStats.entity(started, false);
			return state.packed;
		}

		boolean sampled = false;
		if (cell != previousCell || epoch != state.epoch || now - state.sampledNanos > RESAMPLE_NANOS) {
			sample(state, level, bx, by, bz, cell, epoch, now);
			sampled = true;
		}

		// the block vanilla read is one of the corners
		int center = (Mth.floor(probe.x) - bx) | (Mth.floor(probe.y) - by) << 1 | (Mth.floor(probe.z) - bz) << 2;
		int originalBlock = LightMath.blockUnits(original);
		int originalSky = LightMath.skyUnits(original);
		int worldBlock = LightMath.cornerBlock(state.corners, center);
		int worldSky = LightMath.cornerSky(state.corners, center);

		// a mismatch is either a renderer override or a stale cache, one fresh sample tells them apart
		if ((originalBlock != worldBlock << 4 || originalSky != worldSky << 4) && !sampled && !state.overrideConfirmed) {
			sample(state, level, bx, by, bz, cell, epoch, now);
			state.overrideConfirmed = true;
			sampled = true;
			worldBlock = LightMath.cornerBlock(state.corners, center);
			worldSky = LightMath.cornerSky(state.corners, center);
		}

		float smoothBlock = worldBlock;
		float smoothSky = worldSky;
		if (LightUpdate.config().spatialBlend) {
			long blended = LightMath.blend(state.corners, state.validMask, (float) (qx - bx), (float) (qy - by), (float) (qz - bz));
			if (blended != LightMath.NO_BLEND) {
				smoothBlock = LightMath.first(blended);
				smoothSky = LightMath.second(blended);
			}
		}

		float targetBlock = LightMath.resolve(originalBlock, worldBlock, smoothBlock);
		float targetSky = LightMath.resolve(originalSky, worldSky, smoothSky);
		boolean teleported = previousCell != EntityLightState.NO_CELL && farApart(previousCell, cell);
		state.fadeTowards(targetBlock, targetSky, frame, now, teleported);
		state.probeX = probe.x;
		state.probeY = probe.y;
		state.probeZ = probe.z;
		state.original = original;

		LightStats.entity(started, sampled);
		return state.packed;
	}

	private static void sample(EntityLightState state, Level level, int bx, int by, int bz, long cell, int epoch, long now) {
		BlockPos.MutableBlockPos pos = POS;
		long corners = 0L;
		for (int i = 0; i < 8; i++) {
			pos.set(bx + (i & 1), by + (i >> 1 & 1), bz + (i >> 2 & 1));
			corners = LightMath.putCorner(corners, i, level.getBrightness(LightLayer.BLOCK, pos), level.getBrightness(LightLayer.SKY, pos));
		}

		// dark corners only need a block check when something else is lit, otherwise the result is 0 anyway
		int validMask = 0xFF;
		if (corners != 0L) {
			for (int i = 0; i < 8; i++) {
				if ((corners >>> (i << 3) & 0xFFL) == 0L) {
					pos.set(bx + (i & 1), by + (i >> 1 & 1), bz + (i >> 2 & 1));
					if (level.getBlockState(pos).isSolidRender(level, pos)) {
						validMask &= ~(1 << i);
					}
				}
			}
		}

		state.corners = corners;
		state.validMask = validMask;
		state.cell = cell;
		state.epoch = epoch;
		state.sampledNanos = now - state.resampleJitter;
		state.overrideConfirmed = false;
	}

	private static boolean farApart(long a, long b) {
		return Math.abs(BlockPos.getX(a) - BlockPos.getX(b)) > 2
				|| Math.abs(BlockPos.getY(a) - BlockPos.getY(b)) > 2
				|| Math.abs(BlockPos.getZ(a) - BlockPos.getZ(b)) > 2;
	}
}

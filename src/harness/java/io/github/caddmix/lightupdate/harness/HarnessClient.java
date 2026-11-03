package io.github.caddmix.lightupdate.harness;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import io.github.caddmix.lightupdate.LightUpdate;
import io.github.caddmix.lightupdate.access.BlockEntityLightHolder;
import io.github.caddmix.lightupdate.debug.LightStats;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * Scripted scenes for checking lightupdate in a real client. Active only with -Dlightupdate.harness=<scene>,
 * scenes are candle (the walk from the bug clip), lamp (block entity next to a light that turns on and off)
 * and crowd (cost with many entities). Results go to run-harness/harness/*.csv, then the client quits.
 */
public final class HarnessClient implements ClientModInitializer {
	private static final String SCENE = System.getProperty("lightupdate.harness", "");
	private static final ByteBuffer PIXELS = BufferUtils.createByteBuffer(256 * 256 * 4);

	private final List<String> rows = new ArrayList<>();
	private final List<Runnable> queue = new ArrayList<>();
	private int ticksInState;
	private State state = State.WAIT_TITLE;
	private String phase = "setup";
	private int frame;
	private int walkLeg;
	private int toggles;
	private Path outDir;

	private enum State { WAIT_TITLE, WAIT_WORLD, SETUP, RUN, DONE }

	@Override
	public void onInitializeClient() {
		if (SCENE.isEmpty()) {
			return;
		}

		ClientTickEvents.END_CLIENT_TICK.register(this::tick);
		WorldRenderEvents.END.register(context -> this.frame());
		log("scene " + SCENE);
	}

	private static void log(String message) {
		LightUpdate.LOGGER.info("[harness] {}", message);
	}

	private void tick(Minecraft mc) {
		ticksInState++;
		switch (state) {
			case WAIT_TITLE -> {
				if (mc.screen instanceof TitleScreen) {
					outDir = mc.gameDirectory.toPath().resolve("harness");
					createWorld(mc);
					next(State.WAIT_WORLD);
				}
			}
			case WAIT_WORLD -> {
				if (mc.level != null && mc.player != null && mc.getSingleplayerServer() != null && ticksInState > 40) {
					setupScene(mc);
					next(State.SETUP);
				}
			}
			case SETUP -> {
				if (ticksInState > 100) {
					next(State.RUN);
				}
			}
			case RUN -> runScene(mc);
			case DONE -> {
				if (ticksInState == 20) {
					finish(mc);
				}
			}
		}
	}

	private void next(State newState) {
		state = newState;
		ticksInState = 0;
	}

	private static void createWorld(Minecraft mc) {
		GameRules rules = new GameRules();
		rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
		rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
		rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
		rules.getRule(GameRules.RULE_RANDOMTICKING).set(0, null);
		LevelSettings settings = new LevelSettings("harness-" + SCENE + "-" + System.currentTimeMillis(), GameType.CREATIVE, false,
				Difficulty.PEACEFUL, true, rules, new WorldDataConfiguration(DataPackConfig.DEFAULT, FeatureFlags.DEFAULT_FLAGS));
		mc.createWorldOpenFlows().createFreshLevel(settings.levelName(), settings, new WorldOptions(42L, false, false),
				registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
				mc.screen);
		log("creating flat world");
	}

	private static void run(Minecraft mc, String... commands) {
		MinecraftServer server = mc.getSingleplayerServer();
		server.execute(() -> {
			for (String command : commands) {
				server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
			}
		});
	}

	// the flat world surface is y = -60, a stone tunnel along +x with a torch at the far end
	private static final int FLOOR = -60;

	private void setupScene(Minecraft mc) {
		mc.options.hideGui = true;
		mc.options.pauseOnLostFocus = false;
		run(mc, "time set midnight", "weather clear");
		switch (SCENE) {
			case "candle" -> {
				run(mc,
						"fill -2 -61 -4 34 -55 4 minecraft:stone",
						"fill 0 -60 -3 32 -57 3 minecraft:air",
						"setblock 32 -60 0 minecraft:torch",
						"tp @p 2.5 -60 0.5 -90 0");
				mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			}
			case "lamp" -> {
				run(mc,
						"fill -6 -61 14 6 -55 26 minecraft:stone",
						"fill -5 -60 15 5 -56 25 minecraft:air",
						"setblock 0 -60 22 minecraft:chest[facing=south]",
						"setblock 1 -60 22 minecraft:chest[facing=south,type=left]",
						"setblock 0 -60 22 minecraft:chest[facing=south,type=right]",
						"setblock -2 -59 25 minecraft:oak_wall_sign[facing=north]",
						"setblock 2 -60 20 minecraft:torch",
						"gamemode spectator @p",
						"tp @p 0.5 -58.3 17.5 0 25");
				mc.options.setCameraType(CameraType.FIRST_PERSON);
			}
			case "crowd" -> {
				List<String> commands = new ArrayList<>();
				commands.add("fill -20 -60 -20 20 -60 40 minecraft:air");
				for (int x = -12; x <= 12; x++) {
					for (int z = 6; z <= 36; z++) {
						commands.add("summon minecraft:zombie " + x + ".5 -60 " + z + ".5 {NoAI:1b,Silent:1b,PersistenceRequired:1b}");
					}
				}
				for (int x = -12; x <= 12; x += 6) {
					commands.add("setblock " + x + " -60 20 minecraft:torch");
				}
				commands.add("tp @p 0.5 -54 -4 0 30");
				run(mc, commands.toArray(String[]::new));
				mc.player.getAbilities().flying = true;
				mc.player.onUpdateAbilities();
				mc.options.setCameraType(CameraType.FIRST_PERSON);
			}
			default -> log("unknown scene " + SCENE);
		}
		log("scene built");
	}

	private void runScene(Minecraft mc) {
		switch (SCENE) {
			case "candle" -> runCandle(mc);
			case "lamp" -> runLamp(mc);
			case "crowd" -> runCrowd(mc);
			default -> next(State.DONE);
		}
	}

	/** walks the player to the torch and back, once with vanilla light and once with lightupdate */
	private void runCandle(Minecraft mc) {
		if (ticksInState == 1) {
			startPhase(mc, "vanilla", false);
			walkLeg = 0;
		}

		if (phase.equals("vanilla") || phase.equals("smooth")) {
			double x = mc.player.getX();
			if (walkLeg == 0) {
				walk(mc, -90f);
				if (x >= 27.0) {
					walkLeg = 1;
				}
			} else if (walkLeg == 1) {
				walk(mc, 90f);
				if (x <= 3.0) {
					walkLeg = 2;
					mc.options.keyUp.setDown(false);
					if (phase.equals("vanilla")) {
						phase = "pause";
						ticksInState = 1000;
					} else {
						endScene(mc);
					}
				}
			}
		} else if (phase.equals("pause") && ticksInState == 1040) {
			run(mc, "tp @p 2.5 -60 0.5 -90 0");
		} else if (phase.equals("pause") && ticksInState == 1080) {
			startPhase(mc, "smooth", true);
			walkLeg = 0;
		}
	}

	private static void walk(Minecraft mc, float yaw) {
		mc.player.setYRot(yaw);
		mc.player.setXRot(0f);
		mc.options.keyUp.setDown(true);
	}

	/** a torch next to a double chest turns on and off every 2 seconds */
	private void runLamp(Minecraft mc) {
		if (ticksInState == 1) {
			startPhase(mc, "vanilla", false);
			toggles = 0;
		}

		if (ticksInState % 40 == 0) {
			boolean on = toggles % 2 == 0;
			run(mc, on ? "setblock 2 -60 20 minecraft:air" : "setblock 2 -60 20 minecraft:torch");
			toggles++;
			if (toggles == 6) {
				if (phase.equals("vanilla")) {
					startPhase(mc, "smooth", true);
					toggles = 0;
				} else {
					endScene(mc);
				}
			}
		}
	}

	/** 20 seconds without and 20 seconds with lightupdate in front of 775 zombies */
	private void runCrowd(Minecraft mc) {
		if (ticksInState == 1) {
			LightUpdate.config().debugStats = true;
			LightStats.setEnabled(true);
			startPhase(mc, "vanilla", false);
		}

		if (ticksInState % 20 == 0) {
			log(String.format(Locale.ROOT, "%s fps %d entities %d %s", phase, mc.getFps(), mc.level.getEntityCount(), LightStats.line()));
		}

		if (ticksInState == 400) {
			startPhase(mc, "smooth", true);
		} else if (ticksInState == 800) {
			endScene(mc);
		}
	}

	private void startPhase(Minecraft mc, String name, boolean modOn) {
		if (LightUpdate.enabled() != modOn) {
			LightUpdate.toggle();
		}

		phase = name;
		log("phase " + name + " mod " + (modOn ? "on" : "off"));
		Screenshot.grab(mc.gameDirectory, "harness-" + SCENE + "-" + name + ".png", mc.getMainRenderTarget(), component -> { });
	}

	private void endScene(Minecraft mc) {
		phase = "done";
		mc.options.keyUp.setDown(false);
		next(State.DONE);
	}

	private void finish(Minecraft mc) {
		try {
			Files.createDirectories(outDir);
			Path file = outDir.resolve(SCENE + ".csv");
			Files.write(file, rows);
			log("wrote " + rows.size() + " rows to " + file);
		} catch (IOException e) {
			log("could not write results: " + e);
		}

		log("done");
		mc.stop();
	}

	/** one row per rendered frame while a phase runs */
	private void frame() {
		Minecraft mc = Minecraft.getInstance();
		if (state != State.RUN || mc.player == null || mc.level == null || !(phase.equals("vanilla") || phase.equals("smooth"))) {
			return;
		}

		frame++;
		float partialTick = mc.getTimer().getGameTimeDeltaPartialTick(false);
		RenderTarget target = mc.getMainRenderTarget();
		int width = target.width;
		int height = target.height;

		if (rows.isEmpty()) {
			rows.add("phase,frame,nanos,x,block_units,sky_units,luma");
		}

		switch (SCENE) {
			case "candle" -> {
				int light = mc.getEntityRenderDispatcher().getPackedLightCoords(mc.player, partialTick);
				// torso of the player in third person, a little below the screen center
				double luma = luma(target, width / 2 - width / 60, height / 2 - height / 6, width / 30, height / 14);
				rows.add(String.format(Locale.ROOT, "%s,%d,%d,%.4f,%d,%d,%.3f", phase, frame, System.nanoTime(), mc.player.getX(),
						light & 0xFFFF, light >>> 16, luma));
			}
			case "lamp" -> {
				BlockPos pos = new BlockPos(0, FLOOR, 22);
				BlockEntity chest = mc.level.getBlockEntity(pos);
				int light = phase.equals("smooth") && chest != null
						? ((BlockEntityLightHolder) chest).lightupdate$blockEntityLight().packed
						: LevelRenderer.getLightColor(mc.level, pos);
				double luma = luma(target, width / 2 - width / 40, height / 2 - height / 10, width / 20, height / 20);
				rows.add(String.format(Locale.ROOT, "%s,%d,%d,%.4f,%d,%d,%.3f", phase, frame, System.nanoTime(), 0.0,
						light & 0xFFFF, light >>> 16, luma));
			}
			default -> {
			}
		}
	}

	/** mean luma of a window region, x and y from the bottom left like glReadPixels */
	private static double luma(RenderTarget target, int x, int y, int w, int h) {
		w = Math.min(w, 256);
		h = Math.min(h, 256);
		GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, target.frameBufferId);
		PIXELS.clear();
		GL11.glReadPixels(x, y, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, PIXELS);
		double sum = 0.0;
		for (int i = 0; i < w * h; i++) {
			int r = PIXELS.get(i * 4) & 0xFF;
			int g = PIXELS.get(i * 4 + 1) & 0xFF;
			int b = PIXELS.get(i * 4 + 2) & 0xFF;
			sum += 0.2126 * r + 0.7152 * g + 0.0722 * b;
		}
		return sum / (w * h);
	}
}

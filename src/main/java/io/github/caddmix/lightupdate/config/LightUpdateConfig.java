package io.github.caddmix.lightupdate.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import io.github.caddmix.lightupdate.LightUpdate;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

/** config/lightupdate.json, read once at startup */
public final class LightUpdateConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	@SerializedName("enabled")
	public boolean enabled = true;
	@SerializedName("entities")
	public boolean entities = true;
	@SerializedName("block_entities")
	public boolean blockEntities = true;
	/** blend the 8 blocks around an entity, off means only the time fade runs */
	@SerializedName("spatial_blend")
	public boolean spatialBlend = true;
	/** time a light change needs to settle, 0 turns the fade off */
	@SerializedName("fade_ms")
	public int fadeMs = 250;
	/** swap the lightmap lookup in vanilla entity shaders, needs a resource reload to change */
	@SerializedName("patch_shaders")
	public boolean patchShaders = true;
	/** adds an f3 line with timings, costs two nanoTime calls per entity while on */
	@SerializedName("debug_stats")
	public boolean debugStats = false;

	public static LightUpdateConfig load(Path file) {
		if (Files.isRegularFile(file)) {
			try (Reader reader = Files.newBufferedReader(file)) {
				LightUpdateConfig config = GSON.fromJson(reader, LightUpdateConfig.class);
				if (config != null) {
					config.sanitize();
					// writes back keys added in newer versions
					config.save(file);
					return config;
				}
			} catch (IOException | JsonParseException e) {
				// keep the broken file so the user can fix it
				LightUpdate.LOGGER.warn("could not read {}, using defaults: {}", file, e.getMessage());
				return new LightUpdateConfig();
			}
		}

		LightUpdateConfig config = new LightUpdateConfig();
		config.save(file);
		return config;
	}

	private void sanitize() {
		fadeMs = Math.max(0, Math.min(fadeMs, 5000));
	}

	public void save(Path file) {
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(this));
		} catch (IOException e) {
			LightUpdate.LOGGER.warn("could not write {}: {}", file, e.getMessage());
		}
	}
}

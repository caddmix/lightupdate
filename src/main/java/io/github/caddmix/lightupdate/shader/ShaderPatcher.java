package io.github.caddmix.lightupdate.shader;

import io.github.caddmix.lightupdate.LightUpdate;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vanilla 1.21.1 entity shaders read the lightmap with texelFetch(Sampler2, UV2 / 16, 0), which drops
 * everything between whole light levels, while terrain shaders read it filtered. This swaps in a filtered
 * read centered on lightmap texels, so whole levels give the exact vanilla color and in between values blend.
 *
 * Only exact vanilla lines get replaced. A resource pack that rewrote one of these shaders keeps its version.
 * Iris shader packs use their own programs and read the light coordinates as floats already.
 */
public final class ShaderPatcher {
	public static final String VANILLA_LOOKUP = "texelFetch(Sampler2, UV2 / 16, 0)";
	public static final String SMOOTH_LOOKUP = "texture(Sampler2, clamp((vec2(UV2) + 8.0) / 256.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0)))";

	/** every 1.21.1 core vertex shader that renders entity, block entity or name tag light with texelFetch */
	private static final Set<String> TARGETS = Set.of(
			"rendertype_armor_cutout_no_cull",
			"rendertype_breeze_wind",
			"rendertype_entity_cutout",
			"rendertype_entity_cutout_no_cull",
			"rendertype_entity_cutout_no_cull_z_offset",
			"rendertype_entity_decal",
			"rendertype_entity_no_outline",
			"rendertype_entity_smooth_cutout",
			"rendertype_entity_solid",
			"rendertype_entity_translucent",
			"rendertype_entity_translucent_cull",
			"rendertype_item_entity_translucent_cull",
			"rendertype_text",
			"rendertype_text_background",
			"rendertype_text_intensity"
	);

	private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();

	private ShaderPatcher() {
	}

	/** @param name core shader program name, like rendertype_entity_cutout */
	public static String patch(String name, String source) {
		if (source == null || name == null || !LightUpdate.config().patchShaders || !TARGETS.contains(name)) {
			return source;
		}

		String patched = rewrite(source);
		if (patched == source) {
			if (REPORTED.add(name)) {
				LightUpdate.LOGGER.info("shader {} has no vanilla lightmap lookup, probably replaced by a resource pack, left untouched", name);
			}
		} else if (REPORTED.add(name)) {
			LightUpdate.LOGGER.info("smooth lightmap lookup in {}", name);
		}

		return patched;
	}

	public static String rewrite(String source) {
		return source.contains(VANILLA_LOOKUP) ? source.replace(VANILLA_LOOKUP, SMOOTH_LOOKUP) : source;
	}

	public static boolean isTarget(String name) {
		return TARGETS.contains(name);
	}
}

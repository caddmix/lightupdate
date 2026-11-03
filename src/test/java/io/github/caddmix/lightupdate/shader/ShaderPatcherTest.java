package io.github.caddmix.lightupdate.shader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShaderPatcherTest {
	private static final String LINE = "    lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);";

	@Test
	void replacesTheVanillaLookup() {
		String patched = ShaderPatcher.patch("rendertype_entity_cutout", "void main() {\n" + LINE + "\n}\n");
		assertFalse(patched.contains(ShaderPatcher.VANILLA_LOOKUP));
		assertTrue(patched.contains("lightMapColor = " + ShaderPatcher.SMOOTH_LOOKUP + ";"));
	}

	@Test
	void leavesOtherShadersAlone() {
		String source = "void main() {\n" + LINE + "\n}\n";
		assertSame(source, ShaderPatcher.patch("particle", source));
		assertSame(source, ShaderPatcher.patch("rendertype_leash", source));
		assertSame(source, ShaderPatcher.patch("somemod:rendertype_entity_cutout", source));
	}

	@Test
	void leavesRewrittenPackShadersAlone() {
		String custom = "void main() {\n    lightMapColor = texelFetch(Sampler2, UV2 >> 4, 0);\n}\n";
		assertSame(custom, ShaderPatcher.patch("rendertype_entity_solid", custom));
	}

	/** reads the real 1.21.1 shaders from the game jar on the test classpath, nothing gets copied into the repo */
	@Test
	void everyTargetHasTheVanillaLookup() throws IOException {
		for (String name : List.of("rendertype_armor_cutout_no_cull", "rendertype_breeze_wind", "rendertype_entity_cutout",
				"rendertype_entity_cutout_no_cull", "rendertype_entity_cutout_no_cull_z_offset", "rendertype_entity_decal",
				"rendertype_entity_no_outline", "rendertype_entity_smooth_cutout", "rendertype_entity_solid",
				"rendertype_entity_translucent", "rendertype_entity_translucent_cull", "rendertype_item_entity_translucent_cull",
				"rendertype_text", "rendertype_text_background", "rendertype_text_intensity")) {
			assertTrue(ShaderPatcher.isTarget(name), name);
			try (InputStream in = ShaderPatcherTest.class.getClassLoader().getResourceAsStream("assets/minecraft/shaders/core/" + name + ".vsh")) {
				assertNotNull(in, name);
				String source = new String(in.readAllBytes(), StandardCharsets.UTF_8);
				assertTrue(source.contains(ShaderPatcher.VANILLA_LOOKUP), name);
				String patched = ShaderPatcher.rewrite(source);
				assertFalse(patched.contains("texelFetch(Sampler2"), name);
				assertEquals(source.length() - ShaderPatcher.VANILLA_LOOKUP.length() + ShaderPatcher.SMOOTH_LOOKUP.length(), patched.length(), name);
			}
		}
	}

	/** whole light levels must land on texel centers so vanilla light keeps its exact color */
	@Test
	void wholeLevelsHitTexelCenters() {
		for (int level = 0; level <= 15; level++) {
			double coordinate = (level * 16 + 8) / 256.0;
			assertEquals((level + 0.5) / 16.0, coordinate, 1.0e-9);
		}
	}
}

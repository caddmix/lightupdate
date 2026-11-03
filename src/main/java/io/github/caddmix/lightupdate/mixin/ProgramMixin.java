package io.github.caddmix.lightupdate.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.shaders.Program;
import io.github.caddmix.lightupdate.shader.ShaderPatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Rewrites core shader sources right after they are read, before imports get resolved. */
@Mixin(Program.class)
public abstract class ProgramMixin {
	@ModifyExpressionValue(
			method = "compileShaderInternal",
			at = @At(value = "INVOKE", target = "Lorg/apache/commons/io/IOUtils;toString(Ljava/io/InputStream;Ljava/nio/charset/Charset;)Ljava/lang/String;", remap = false),
			require = 0
	)
	private static String lightupdate$smoothLightmapLookup(String source, @Local(argsOnly = true) Program.Type type, @Local(argsOnly = true, ordinal = 0) String name) {
		return type == Program.Type.VERTEX ? ShaderPatcher.patch(name, source) : source;
	}
}

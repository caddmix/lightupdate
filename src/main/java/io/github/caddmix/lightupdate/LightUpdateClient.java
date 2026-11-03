package io.github.caddmix.lightupdate;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.caddmix.lightupdate.config.LightUpdateConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

public final class LightUpdateClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		LightUpdate.applyConfig(LightUpdateConfig.load(FabricLoader.getInstance().getConfigDir().resolve("lightupdate.json")));

		// unbound by default, for quick before and after checks
		KeyMapping toggle = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.lightupdate.toggle", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.lightupdate"));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggle.consumeClick()) {
				boolean on = LightUpdate.toggle();
				if (client.player != null) {
					client.player.displayClientMessage(Component.translatable(on ? "message.lightupdate.on" : "message.lightupdate.off"), true);
				}
			}
		});
	}
}

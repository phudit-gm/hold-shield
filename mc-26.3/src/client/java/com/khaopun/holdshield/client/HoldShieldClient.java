package com.khaopun.holdshield.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class HoldShieldClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath("holdshield", "main"));

	private static KeyMapping holdKey;
	private static KeyMapping settingsKey;
	private static final ShieldSwapper SWAPPER = new ShieldSwapper();

	/** Used by the render mixins to ask what the first-person hands should show. */
	public static ShieldSwapper swapper() {
		return SWAPPER;
	}

	@Override
	public void onInitializeClient() {
		holdKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.holdshield.hold", InputConstants.Type.KEYBOARD, InputConstants.KEY_V, CATEGORY));

		// Unbound by default; Mod Menu support can be added later.
		settingsKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.holdshield.settings", InputConstants.UNKNOWN.getValue(), CATEGORY));

		// Start of tick, so the swap and the "use item" happen before vanilla handles input this tick.
		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			SWAPPER.tick(client, holdKey.isDown());
			while (settingsKey.consumeClick()) {
				client.setScreenAndShow(new HoldShieldConfigScreen(client.gui.screen()));
			}
		});
	}
}

package com.khaopun.holdshield.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class HoldShieldClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath("holdshield", "main"));

	private static KeyMapping holdKey;
	private static final ShieldSwapper SWAPPER = new ShieldSwapper();

	@Override
	public void onInitializeClient() {
		holdKey = KeyBindingHelper.registerKeyBinding(
				new KeyMapping("key.holdshield.hold", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY));

		// Start of tick, so the swap and the "use item" happen before vanilla handles input this tick.
		ClientTickEvents.START_CLIENT_TICK.register(client -> SWAPPER.tick(client, holdKey.isDown()));
	}
}

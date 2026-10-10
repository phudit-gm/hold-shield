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
	private static KeyMapping settingsKey;
	private static final ShieldSwapper SWAPPER = new ShieldSwapper();
	/** Toggle mode: the shield should be up until the next press. */
	private static boolean toggledOn;

	@Override
	public void onInitializeClient() {
		holdKey = KeyBindingHelper.registerKeyBinding(
				new KeyMapping("key.holdshield.hold", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY));

		// Unbound by default; the settings screen is also reachable from Mod Menu.
		settingsKey = KeyBindingHelper.registerKeyBinding(
				new KeyMapping("key.holdshield.settings", InputConstants.UNKNOWN.getValue(), CATEGORY));

		// Start of tick, so the swap and the "use item" happen before vanilla handles input this tick.
		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			boolean wantShield;
			if (HoldShieldConfig.get().activation == HoldShieldConfig.Activation.TOGGLE) {
				while (holdKey.consumeClick()) {
					toggledOn = !toggledOn;
				}
				wantShield = toggledOn;
			} else {
				while (holdKey.consumeClick()) {
					// Hold mode only looks at isDown(); drop the queued presses.
				}
				toggledOn = false;
				wantShield = holdKey.isDown();
			}
			SWAPPER.tick(client, wantShield);
			if (toggledOn && !SWAPPER.isActive()) {
				// The round did not start or was cancelled (no shield, screen, shield broke...): next press raises.
				toggledOn = false;
			}
			while (settingsKey.consumeClick()) {
				client.setScreen(new HoldShieldConfigScreen(client.screen));
			}
		});
	}
}

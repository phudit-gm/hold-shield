package com.khaopun.holdshield.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Adds the "Configure" button for Hold Shield in Mod Menu. Only loaded when Mod Menu is installed. */
public class HoldShieldModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return HoldShieldConfigScreen::new;
	}
}

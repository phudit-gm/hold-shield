package com.khaopun.holdshield.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Plain vanilla settings screen. Saved when the screen is closed. */
public class HoldShieldConfigScreen extends Screen {
	private final Screen parent;
	private final HoldShieldConfig config = HoldShieldConfig.get();

	public HoldShieldConfigScreen(Screen parent) {
		super(Component.translatable("holdshield.config.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int x = this.width / 2 - 100;
		int y = this.height / 4;

		addRenderableWidget(CycleButton.onOffBuilder(config.enabled).create(
				x, y, 200, 20, Component.translatable("holdshield.config.enabled"),
				(button, value) -> config.enabled = value));

		addRenderableWidget(CycleButton.<HoldShieldConfig.Priority>builder(
						p -> Component.translatable("holdshield.config.priority." + p.name().toLowerCase(Locale.ROOT)),
						config.priority)
				.withValues(HoldShieldConfig.Priority.values())
				.create(x, y + 24, 200, 20, Component.translatable("holdshield.config.priority"),
						(button, value) -> config.priority = value));

		addRenderableWidget(CycleButton.onOffBuilder(config.debugLogging).create(
				x, y + 48, 200, 20, Component.translatable("holdshield.config.debug"),
				(button, value) -> config.debugLogging = value));

		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
				.bounds(x, y + 84, 200, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.centeredText(this.font, this.title, this.width / 2, this.height / 4 - 24, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		config.save();
		this.minecraft.setScreenAndShow(parent);
	}
}

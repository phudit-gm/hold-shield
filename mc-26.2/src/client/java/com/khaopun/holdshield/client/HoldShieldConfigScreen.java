package com.khaopun.holdshield.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** Plain vanilla settings screen, two columns, grouped, with tooltips. Saved when the screen is closed. */
public class HoldShieldConfigScreen extends Screen {
	private static final int VALID_TEXT = 0xFFE0E0E0;
	private static final int INVALID_TEXT = 0xFFFF5555;

	private final Screen parent;
	private final HoldShieldConfig config = HoldShieldConfig.get();
	private int top;

	public HoldShieldConfigScreen(Screen parent) {
		super(Component.translatable("holdshield.config.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int left = this.width / 2 - 155;
		int right = this.width / 2 + 5;
		top = Math.max(24, this.height / 2 - 106);

		// General (group label at top + 0, drawn in extractRenderState)
		toggle(left, top + 12, "holdshield.config.enabled", config.enabled, v -> config.enabled = v);
		cycle(right, top + 12, 150, "holdshield.config.activation", HoldShieldConfig.Activation.values(),
				config.activation, v -> config.activation = v);
		toggle(left, top + 36, "holdshield.config.messages", config.actionBarMessages, v -> config.actionBarMessages = v);
		toggle(right, top + 36, "holdshield.config.debug", config.debugLogging, v -> config.debugLogging = v);

		// Shield Selection (label at top + 64)
		cycle(left, top + 76, 310, "holdshield.config.priority", HoldShieldConfig.Priority.values(),
				config.priority, v -> config.priority = v);
		number(left, top + 100, "holdshield.config.min_durability", config.minDurabilityPercent, 100,
				v -> config.minDurabilityPercent = v);
		number(right, top + 100, "holdshield.config.low_warning", config.lowDurabilityWarningPercent, 100,
				v -> config.lowDurabilityWarningPercent = v);

		// Hand & Timing (label at top + 128)
		cycle(left, top + 140, 150, "holdshield.config.hand", HoldShieldConfig.Hand.values(),
				config.hand, v -> config.hand = v);
		cycle(right, top + 140, 150, "holdshield.config.totem", HoldShieldConfig.TotemProtection.values(),
				config.totemProtection, v -> config.totemProtection = v);
		number(left, top + 164, "holdshield.config.delay", config.raiseDelayTicks,
				HoldShieldConfig.MAX_RAISE_DELAY_TICKS, v -> config.raiseDelayTicks = v);

		addRenderableWidget(Button.builder(Component.translatable("holdshield.config.reset"), button -> {
			config.resetToDefaults();
			config.save();
			rebuildWidgets();
		}).bounds(left, top + 192, 150, 20).build());

		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
				.bounds(right, top + 192, 150, 20).build());
	}

	private void toggle(int x, int y, String key, boolean value, Consumer<Boolean> setter) {
		addRenderableWidget(CycleButton.onOffBuilder(value)
				.withTooltip(v -> Tooltip.create(Component.translatable(key + ".tooltip")))
				.create(x, y, 150, 20, Component.translatable(key), (button, v) -> setter.accept(v)));
	}

	/** Cycle button whose tooltip is the option's description plus the description of the shown value. */
	private <T extends Enum<T>> void cycle(int x, int y, int width, String key, T[] values, T value, Consumer<T> setter) {
		addRenderableWidget(CycleButton.<T>builder(v -> Component.translatable(valueKey(key, v)), value)
				.withValues(values)
				.withTooltip(v -> Tooltip.create(Component.translatable(key + ".tooltip")
						.append("\n\n").append(Component.translatable(valueKey(key, v) + ".tooltip"))))
				.create(x, y, width, 20, Component.translatable(key), (button, v) -> setter.accept(v)));
	}

	/** Label + text field that only accepts a whole number from 0 to max; invalid text turns red and is not saved. */
	private void number(int x, int y, String key, int value, int max, IntConsumer setter) {
		Tooltip tooltip = Tooltip.create(Component.translatable(key + ".tooltip"));
		StringWidget label = new StringWidget(x, y, 106, 20, Component.translatable(key), this.font);
		label.setTooltip(tooltip);
		addRenderableWidget(label);

		EditBox box = new EditBox(this.font, x + 108, y, 42, 20, Component.translatable(key));
		box.setMaxLength(3);
		box.setValue(Integer.toString(value));
		box.setTooltip(tooltip);
		box.setResponder(text -> {
			int parsed = parse(text, max);
			box.setTextColor(parsed < 0 ? INVALID_TEXT : VALID_TEXT);
			if (parsed >= 0) {
				setter.accept(parsed);
			}
		});
		addRenderableWidget(box);
	}

	/** The number in text if it is only digits and at most max, else -1. */
	private static int parse(String text, int max) {
		if (text.isEmpty() || text.length() > 3 || !text.chars().allMatch(Character::isDigit)) {
			return -1;
		}
		int value = Integer.parseInt(text);
		return value <= max ? value : -1;
	}

	private static String valueKey(String key, Enum<?> value) {
		return key + "." + value.name().toLowerCase(Locale.ROOT);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		int cx = this.width / 2;
		graphics.centeredText(this.font, this.title, cx, top - 16, 0xFFFFFFFF);
		graphics.centeredText(this.font, Component.translatable("holdshield.config.group.general"), cx, top, 0xFFAAAAAA);
		graphics.centeredText(this.font, Component.translatable("holdshield.config.group.selection"), cx, top + 64, 0xFFAAAAAA);
		graphics.centeredText(this.font, Component.translatable("holdshield.config.group.hand_timing"), cx, top + 128, 0xFFAAAAAA);
	}

	@Override
	public void onClose() {
		config.save();
		this.minecraft.setScreenAndShow(parent);
	}
}

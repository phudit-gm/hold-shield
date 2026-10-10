package com.khaopun.holdshield.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** User settings, stored as JSON in the Fabric config directory (config/holdshield.json). */
public final class HoldShieldConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger("holdshield");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static HoldShieldConfig instance;

	/** Which shield to pick when several are in the inventory. */
	public enum Priority {
		/** Most remaining durability, wherever it is (default). Older configs called this DURABILITY. */
		@SerializedName(value = "HIGHEST_DURABILITY", alternate = {"DURABILITY"})
		HIGHEST_DURABILITY,
		/** Least remaining durability, wherever it is. */
		LOWEST_DURABILITY,
		/** A shield in the hotbar wins; durability only breaks ties inside a group. */
		HOTBAR_FIRST,
		/** A shield in the main inventory wins, so the selected hotbar slot never has to change. */
		INVENTORY_FIRST,
		/** The first shield found (hotbar 0-8, then inventory 9-35); durability is ignored. */
		FIRST_FOUND
	}

	/** Which hand raises the shield. */
	public enum Hand {
		/** The shield is swapped into the off hand; the main hand item stays (default). */
		OFFHAND,
		/** The shield is selected in / swapped into the selected hotbar slot. */
		MAIN_HAND
	}

	/** What to do when the off hand holds a Totem of Undying and the shield would replace it. */
	public enum TotemProtection {
		/** Swap the totem out like any other item (default, same as before). */
		OFF,
		/** Raise the shield with the main hand instead, so the totem stays in the off hand. */
		USE_MAIN_HAND,
		/** Do not raise the shield at all. */
		DONT_RAISE
	}

	/** How the hold key works. */
	public enum Activation {
		/** Raised while the key is held (default). */
		HOLD,
		/** Press once to raise, press again to lower. */
		TOGGLE
	}

	public static final int MAX_RAISE_DELAY_TICKS = 20;

	public boolean enabled = true;
	public Activation activation = Activation.HOLD;
	public Priority priority = Priority.HIGHEST_DURABILITY;
	public Hand hand = Hand.OFFHAND;
	public TotemProtection totemProtection = TotemProtection.OFF;
	/** Shields with less durability left than this percentage are never picked (0 = no limit). */
	public int minDurabilityPercent = 0;
	/** Action bar warning once per raise when the raised shield is at or below this percentage (0 = off). */
	public int lowDurabilityWarningPercent = 0;
	/** Action bar message when the key is pressed but no usable shield is found or the totem blocks the raise. */
	public boolean actionBarMessages = true;
	/** Log every swap / restore decision (for bug reports). */
	public boolean debugLogging = false;
	/** Ticks to wait after moving the shield before raising it (other mods may play an equip animation). */
	public int raiseDelayTicks = 1;

	/** Back to the shipped defaults (the caller saves). */
	public void resetToDefaults() {
		HoldShieldConfig defaults = new HoldShieldConfig();
		enabled = defaults.enabled;
		activation = defaults.activation;
		priority = defaults.priority;
		hand = defaults.hand;
		totemProtection = defaults.totemProtection;
		minDurabilityPercent = defaults.minDurabilityPercent;
		lowDurabilityWarningPercent = defaults.lowDurabilityWarningPercent;
		actionBarMessages = defaults.actionBarMessages;
		debugLogging = defaults.debugLogging;
		raiseDelayTicks = defaults.raiseDelayTicks;
	}

	/** Missing or unknown values (older or hand-edited files) fall back to defaults; numbers are clamped. */
	private void sanitize() {
		HoldShieldConfig defaults = new HoldShieldConfig();
		if (activation == null) {
			activation = defaults.activation;
		}
		if (priority == null) {
			priority = defaults.priority;
		}
		if (hand == null) {
			hand = defaults.hand;
		}
		if (totemProtection == null) {
			totemProtection = defaults.totemProtection;
		}
		minDurabilityPercent = clamp(minDurabilityPercent, 0, 100);
		lowDurabilityWarningPercent = clamp(lowDurabilityWarningPercent, 0, 100);
		raiseDelayTicks = clamp(raiseDelayTicks, 0, MAX_RAISE_DELAY_TICKS);
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	public static HoldShieldConfig get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("holdshield.json");
	}

	private static HoldShieldConfig load() {
		Path file = path();
		if (Files.exists(file)) {
			try {
				HoldShieldConfig loaded = GSON.fromJson(Files.readString(file), HoldShieldConfig.class);
				if (loaded != null) {
					loaded.sanitize();
					return loaded;
				}
			} catch (IOException | RuntimeException e) {
				LOGGER.warn("Could not read {}, using defaults", file, e);
			}
		}
		HoldShieldConfig fresh = new HoldShieldConfig();
		fresh.save();
		return fresh;
	}

	public void save() {
		try {
			Files.writeString(path(), GSON.toJson(this));
		} catch (IOException e) {
			LOGGER.warn("Could not write {}", path(), e);
		}
	}
}

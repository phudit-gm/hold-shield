package com.khaopun.holdshield.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
		/** Most remaining durability, wherever it is (default). */
		DURABILITY,
		/** A shield in the hotbar wins; durability only breaks ties inside a group. */
		HOTBAR_FIRST,
		/** A shield in the main inventory wins, so the selected hotbar slot never has to change. */
		INVENTORY_FIRST
	}

	public boolean enabled = true;
	public Priority priority = Priority.DURABILITY;
	/**
	 * First person only: show the item you were holding in the right hand and the shield in the left hand,
	 * instead of the shield replacing your item. Purely visual.
	 */
	public boolean visualOffhand = true;
	/** Log every swap / restore decision (for bug reports). */
	public boolean debugLogging = false;

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
					if (loaded.priority == null) {
						loaded.priority = Priority.DURABILITY;
					}
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

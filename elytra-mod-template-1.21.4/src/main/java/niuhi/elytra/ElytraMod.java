package niuhi.elytra;

import net.fabricmc.api.ModInitializer;
import niuhi.elytra.config.ModConfig;
import niuhi.elytra.detection.ModEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElytraMod implements ModInitializer {
	public static final String MOD_ID = "elytra-mod";
	public static final ModConfig CONFIG = ModConfig.load();

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		System.out.println("Elytra Boost Mod is loading...");
		ModEvents.register();
		LOGGER.info("Mod Testing");
		CONFIG.save(); // Ensure the config file is created on first launch
	}
}
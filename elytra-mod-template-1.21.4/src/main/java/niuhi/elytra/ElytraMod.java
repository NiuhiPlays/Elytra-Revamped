package niuhi.elytra;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import niuhi.elytra.detection.ModEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElytraMod implements ModInitializer {
	public static final String MOD_ID = "elytra-revamped";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final boolean ACCESSORIES_LOADED = FabricLoader.getInstance().isModLoaded("accessories");
	public static final boolean YACL_LOADED = FabricLoader.getInstance().isModLoaded("yet_another_config_lib_v3");

	@Override
	public void onInitialize() {
		LOGGER.info("Elytra Revamped is loading...");
		if (ACCESSORIES_LOADED) {
			LOGGER.info("Accessories mod detected! Enabling additional elytra detection in accessory slots.");
		}
		if (YACL_LOADED) {
			LOGGER.info("YACL Detected, Enabling in-game config page.");
		}
		ModEvents.register();
	}
}
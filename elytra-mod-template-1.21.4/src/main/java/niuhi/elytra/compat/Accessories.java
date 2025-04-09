package niuhi.elytra.compat;

import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import niuhi.elytra.config.DebugLogger;
import niuhi.elytra.config.ModConfig; // Import for config access
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles compatibility with the Accessories mod
 * It's designed to be safe to load even when the mod isn't present
 */
public class Accessories {
    private static final Logger LOGGER = LoggerFactory.getLogger("elytra-revamped-compat");
    private static boolean hasWarned = false;
    private static final ModConfig config = ModConfig.init(); // Access the config

    /**
     * Safely checks if a player has an elytra in any accessories slot
     *
     * @param player The player to check
     * @return true if the player has an elytra in accessories
     */
    public static boolean hasElytraAccessories(ServerPlayerEntity player) {
        if (config.debug.enabled && config.debug.Accessories) {
            DebugLogger.debug("Accessories", "Checking Accessories for player %s", player.getName().getString());
        }

        try {
            Class.forName("io.wispforest.accessories.api.AccessoriesCapability");

            var capability = io.wispforest.accessories.api.AccessoriesCapability.get(player);
            if (capability == null) {
                if (config.debug.enabled && config.debug.Accessories) {
                    DebugLogger.debug("Accessories", "Player %s: AccessoriesCapability is null, no accessories found", player.getName().getString());
                }
                return false;
            }

            boolean hasElytra = capability.isEquipped(
                    io.wispforest.accessories.api.caching.ItemStackBasedPredicate.ofItem(Items.ELYTRA)
            );

            if (config.debug.enabled && config.debug.Accessories) {
                DebugLogger.debug("Accessories", "Player %s: Accessories check result - hasElytra=%b", player.getName().getString(), hasElytra);
            }
            return hasElytra;

        } catch (ClassNotFoundException e) {
            if (!hasWarned) {
                LOGGER.info("Accessories mod not found, disabling accessories integration");
                hasWarned = true;
            }
            if (config.debug.enabled && config.debug.Accessories) {
                DebugLogger.debug("Accessories", "Player %s: Accessories mod not present, skipping check", player.getName().getString());
            }
            return false;
        } catch (Exception e) {
            if (!hasWarned) {
                LOGGER.error("Error checking for elytra accessories: {}", e.getMessage());
                hasWarned = true;
            }
            if (config.debug.enabled && config.debug.Accessories) {
                DebugLogger.debug("Accessories", "Player %s: Exception during accessories check - message=%s", player.getName().getString(), e.getMessage());
            }
            return false;
        }
    }
}
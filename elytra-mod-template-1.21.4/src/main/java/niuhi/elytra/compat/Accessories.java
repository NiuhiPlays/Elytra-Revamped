package niuhi.elytra.compat;

import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles compatibility with the Accessories mod
 * It's designed to be safe to load even when the mod isn't present
 */
public class Accessories {
    private static final Logger LOGGER = LoggerFactory.getLogger("elytra-revamped-compat");
    private static boolean hasWarned = false;

    /**
     * Safely checks if a player has an elytra in any accessories slot
     *
     * @param player The player to check
     * @return true if the player has an elytra in accessories
     */
    public static boolean hasElytraAccessories(ServerPlayerEntity player) {
        try {
            Class.forName("io.wispforest.accessories.api.AccessoriesCapability");

            var capability = io.wispforest.accessories.api.AccessoriesCapability.get(player);
            if (capability == null) {
                return false;
            }

            return capability.isEquipped(
                    io.wispforest.accessories.api.caching.ItemStackBasedPredicate.ofItem(Items.ELYTRA)
            );
        } catch (ClassNotFoundException e) {
            if (!hasWarned) {
                LOGGER.info("Accessories mod not found, disabling accessories integration");
                hasWarned = true;
            }
            return false;
        } catch (Exception e) {
            // Only log once to avoid spam
            if (!hasWarned) {
                LOGGER.error("Error checking for elytra accessories: {}", e.getMessage());
                hasWarned = true;
            }
            return false;
        }
    }
}
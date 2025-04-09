package niuhi.elytra.detection;

import net.fabricmc.loader.api.FabricLoader;
import niuhi.elytra.compat.Accessories;
import niuhi.elytra.config.ModConfig;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

public class ElytraFlightDetector {
    private final ModConfig config;
    private final boolean accessoriesLoaded;

    public ElytraFlightDetector(ModConfig config) {
        this.config = config;
        this.accessoriesLoaded = FabricLoader.getInstance().isModLoaded("accessories");
    }

    /**
     * Determines if a player is actively flying with elytra
     * @param player The player to check
     * @return true if the player is flying with elytra
     */
    public boolean isFlying(ServerPlayerEntity player) {
        return isWearingElytra(player)
                && !player.isOnGround()
                && (player.getVelocity().y != 0 ||
                Math.abs(player.getVelocity().x) + Math.abs(player.getVelocity().z) > 0.1);
    }

    /**
     * Checks if the player is wearing an elytra
     * @param player The player to check
     * @return true if the player has elytra equipped
     */
    public boolean isWearingElytra(ServerPlayerEntity player) {
        boolean hasElytra = player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);

        if (accessoriesLoaded) {
            try {
                return hasElytra || Accessories.hasElytraAccessories(player);
            } catch (Exception e) {
                // Fallback to vanilla behavior if anything goes wrong
                return false;
            }
        }
        return hasElytra;
    }
}
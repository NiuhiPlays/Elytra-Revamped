package niuhi.elytra.detection;

import niuhi.elytra.config.ModConfig;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

public class ElytraFlightDetector {
    private final ModConfig config;

    public ElytraFlightDetector(ModConfig config) {
        this.config = config;
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
                Math.abs(player.getVelocity().x) + Math.abs(player.getVelocity().z) > config.mechanics.minHorizontalVelocity);
    }

    /**
     * Checks if the player is wearing an elytra
     * @param player The player to check
     * @return true if the player has elytra equipped
     */
    public boolean isWearingElytra(ServerPlayerEntity player) {
        return player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
    }
}
package niuhi.elytra.detection;

import net.fabricmc.loader.api.FabricLoader;
import niuhi.elytra.compat.Accessories;
import niuhi.elytra.config.ModConfig;
import niuhi.elytra.config.DebugLogger;
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

    public boolean isFlying(ServerPlayerEntity player) {
        boolean isGliding = player.isGliding();
        boolean isWearingElytra = isWearingElytra(player);
        boolean result = isGliding && isWearingElytra;

        DebugLogger.debug("FlightDetector", "Player %s: isFlying check - isGliding=%b, isWearingElytra=%b, result=%b",
                player.getName().getString(), isGliding, isWearingElytra, result);

        return result;
    }

    public boolean isWearingElytra(ServerPlayerEntity player) {
        boolean hasElytraInChest = player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
        boolean result = hasElytraInChest;

        if (accessoriesLoaded) {
            try {
                boolean hasAccessoryElytra = Accessories.hasElytraAccessories(player);
                result = hasElytraInChest || hasAccessoryElytra;

                DebugLogger.debug("FlightDetector", "Player %s: Accessories check - chestElytra=%b, accessoryElytra=%b, result=%b",
                        player.getName().getString(), hasElytraInChest, hasAccessoryElytra, result);
            } catch (Exception e) {
                DebugLogger.debug("FlightDetector", "Player %s: Accessories check failed - exception=%s, falling back to chest check only",
                        player.getName().getString(), e.getMessage());
                result = hasElytraInChest;
            }
        } else {
            DebugLogger.debug("FlightDetector", "Player %s: No Accessories mod - chestElytra=%b, result=%b",
                    player.getName().getString(), hasElytraInChest, result);
        }

        return result;
    }
}
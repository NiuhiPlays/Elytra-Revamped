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

    public boolean isFlying(ServerPlayerEntity player) {
        return player.isGliding() && isWearingElytra(player);
    }

    public boolean isWearingElytra(ServerPlayerEntity player) {
        boolean hasElytra = player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);

        if (accessoriesLoaded) {
            try {
                return hasElytra || Accessories.hasElytraAccessories(player);
            } catch (Exception e) {
                return false; // Fallback to vanilla behavior if Accessories fails
            }
        }
        return hasElytra;
    }
}
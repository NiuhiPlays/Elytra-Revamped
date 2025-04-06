package niuhi.elytra.compat;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.caching.ItemStackBasedPredicate;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

public class Accessories {

    public static boolean hasElytraAccessories(ServerPlayerEntity player) {
        AccessoriesCapability capability = AccessoriesCapability.get(player);
        if (capability == null){
            return false;
        }

        return capability.getAllEquipped().stream()
                .anyMatch(slotEntryReference -> capability.isEquipped(ItemStackBasedPredicate.ofItem(Items.ELYTRA)));
        //return hasElytraAccessories(player);
    }
}

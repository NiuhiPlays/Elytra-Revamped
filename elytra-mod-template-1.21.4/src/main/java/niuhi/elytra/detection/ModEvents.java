package niuhi.elytra.detection;

import niuhi.elytra.config.ModConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Set;

public class ModEvents {
    private static final ModConfig config = ModConfig.load();
    private static final Set<ServerPlayerEntity> boostedPlayers = new HashSet<>(); // Track boosted players

    public static void register() {
        // Prevent Firework Use When Flying (if enabled in config)
        if (config.disableFireworks) {
            UseItemCallback.EVENT.register((player, world, hand) -> {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    ItemStack itemStack = player.getStackInHand(hand);

                    if (isActuallyFlying(serverPlayer) && itemStack.isOf(Items.FIREWORK_ROCKET)) {
                        return ActionResult.FAIL; // Block firework use
                    }
                }
                return ActionResult.PASS; // Allow normal use otherwise
            });
        }

        // Elytra Fire Boosting & Soul Campfire Pull
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (isActuallyFlying(player)) {
                    if (isAboveFire(player)) {
                        applyBoostIfNearFire(player);
                    } else {
                        boostedPlayers.remove(player); // Reset boost availability as soon as player moves away
                    }
                    applySoulCampfirePull(player);
                } else {
                    boostedPlayers.remove(player); // Ensure reset when not flying
                }
            }
        });
    }

    // Detect if the player is flying with Elytra
    private static boolean isActuallyFlying(ServerPlayerEntity player) {
        return isWearingElytra(player)
                && !player.isOnGround()
                && (player.getVelocity().y != 0 || Math.abs(player.getVelocity().x) + Math.abs(player.getVelocity().z) > 0.1);
    }

    /**
     * Checks if the player is above a fire, lit campfire, or soul campfire.
     */
    private static boolean isAboveFire(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        BlockPos playerPos = player.getBlockPos();

        for (int y = 0; y <= config.hayFireHeight; y++) {
            BlockPos checkPos = playerPos.down(y);
            BlockState blockState = world.getBlockState(checkPos);

            if (isFireOrCampfire(blockState)) {
                boolean isLit = blockState.contains(CampfireBlock.LIT) && blockState.get(CampfireBlock.LIT);
                if (isLit) {
                    return true;
                }
            }
        }
        return false;
    }

    // Check if Elytra is equipped
    private static boolean isWearingElytra(ServerPlayerEntity player) {
        return player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
    }

    private static void applyBoostIfNearFire(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        BlockPos playerPos = player.getBlockPos();

        // If the player has already been boosted, don't boost again until they leave the fire area
        if (boostedPlayers.contains(player)) {
            return;
        }

        double boostAmount = 0.0;

        // Check for fire sources within configurable heights
        for (int y = 0; y <= config.hayFireHeight; y++) {
            BlockPos checkPos = playerPos.down(y);
            BlockState blockState = world.getBlockState(checkPos);

            if (isFireOrCampfire(blockState)) {
                boolean isLit = blockState.contains(CampfireBlock.LIT) && blockState.get(CampfireBlock.LIT);
                boolean hasHayBale = world.getBlockState(checkPos.down()).isOf(Blocks.HAY_BLOCK);
                boolean isSoulCampfire = blockState.isOf(Blocks.SOUL_CAMPFIRE);

                if (isLit) {
                    if (isSoulCampfire) {
                        boostAmount = -config.basePull; // Downward pull for soul campfires
                    } else {
                        boostAmount = hasHayBale ? config.hayBoost : config.baseBoost; // Different boost for hay or general fire
                    }

                    // Adjust boost based on vertical distance
                    double verticalDistance = player.getY() - checkPos.getY();
                    if (verticalDistance < 5) {
                        boostAmount *= 0.8;
                    } else if (verticalDistance >= 5 && verticalDistance < 10) {
                        boostAmount *= 0.6;
                    } else if (verticalDistance >= 10) {
                        boostAmount *= 0.4;
                    }

                    // Mark player as boosted to prevent stacking
                    boostedPlayers.add(player);
                    break; // Apply only the first detected boost
                }
            }
        }

        if (boostAmount != 0) {
            applyBoost(player, boostAmount);
        }
    }

    // Apply downward pull when flying over a soul campfire
    private static void applySoulCampfirePull(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        BlockPos playerPos = player.getBlockPos();

        for (int i = 0; i < config.normalFireHeight; i++) { // Only check within fire detection range
            BlockPos belowPos = playerPos.down(i);
            BlockState blockState = world.getBlockState(belowPos);

            if (blockState.isOf(Blocks.SOUL_CAMPFIRE) && blockState.contains(CampfireBlock.LIT) && blockState.get(CampfireBlock.LIT)) {
                // Check if there's hay nearby and apply appropriate pull
                boolean hasHayBale = world.getBlockState(belowPos.down()).isOf(Blocks.HAY_BLOCK);
                if (hasHayBale) {
                    applyBoost(player, -config.hayPull); // Use the hayPull for a stronger downward pull if there's hay
                } else {
                    applyBoost(player, -config.basePull); // Use the basePull for soul campfires without hay
                }
                return;
            }
        }
    }

    // Apply the calculated boost to the player
    private static void applyBoost(ServerPlayerEntity player, double boostAmount) {
        Vec3d velocity = player.getVelocity();
        Vec3d boostVector = new Vec3d(0, boostAmount, 0); // Apply vertical force

        player.setVelocity(velocity.add(boostVector));
        player.velocityModified = true;
    }

    // Check if the given block is a fire or campfire
    private static boolean isFireOrCampfire(BlockState state) {
        return state.isOf(Blocks.FIRE) || state.isOf(Blocks.CAMPFIRE) || state.isOf(Blocks.SOUL_CAMPFIRE);
    }
}

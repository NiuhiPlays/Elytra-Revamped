package niuhi.elytra.detection;

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
    private static final Set<ServerPlayerEntity> boostedPlayers = new HashSet<>(); // To track boosted players

    public static void register() {
        // Prevent Firework Use When Flying
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                ItemStack itemStack = player.getStackInHand(hand);

                if (isActuallyFlying(serverPlayer) && itemStack.isOf(Items.FIREWORK_ROCKET)) {
                    return ActionResult.FAIL; // Block firework use
                }
            }
            return ActionResult.PASS; // Allow normal use otherwise
        });

        // Elytra Fire Boosting
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (isActuallyFlying(player) && isAboveCampfire(player)) {
                    applyFireBoost(player);
                } else {
                    // If player is no longer above a fire source, reset the boosted state
                    boostedPlayers.remove(player);
                }

                // Check for soul campfires and apply downward pull
                if (isAboveSoulCampfire(player)) {
                    applySoulCampfirePull(player);
                }
            }
        });
    }

    // More accurate Elytra flight detection
    private static boolean isActuallyFlying(ServerPlayerEntity player) {
        return isWearingElytra(player)
                && !player.isOnGround()
                && (player.getVelocity().y != 0 || Math.abs(player.getVelocity().x) + Math.abs(player.getVelocity().z) > 0.1); // Detects real flight
    }

    // Check if Elytra is equipped
    private static boolean isWearingElytra(ServerPlayerEntity player) {
        return player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
    }

    // Check if the player is flying over a fire source or a campfire on a hay bale
    private static boolean isAboveCampfire(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        BlockPos playerPos = player.getBlockPos();

        // Check for normal fires or campfires (10 block check)
        for (int i = 0; i < 10; i++) { // Checking up to 10 blocks below for regular fires/campfires
            BlockPos belowPos = playerPos.down(i);
            BlockState blockState = world.getBlockState(belowPos);

            // Check if the campfire is lit (normal or soul campfire)
            if (blockState.isOf(Blocks.CAMPFIRE) && blockState.get(CampfireBlock.LIT) || blockState.isOf(Blocks.SOUL_CAMPFIRE) && blockState.get(CampfireBlock.LIT)) {
                return true; // Apply boost for lit campfire or soul campfire
            }
            if (blockState.isOf(Blocks.FIRE) || blockState.isOf(Blocks.SOUL_FIRE)) {
                return true; // Apply boost for regular fire
            }
        }

        // Check for campfires with hay bales (25 block check)
        for (int i = 0; i < 25; i++) { // Checking up to 25 blocks below for campfires with hay bales
            BlockPos belowPos = playerPos.down(i);
            BlockState blockState = world.getBlockState(belowPos);

            // Check if it's a lit campfire on top of a hay bale
            if (blockState.isOf(Blocks.CAMPFIRE) && blockState.get(CampfireBlock.LIT)) {
                BlockPos underCampfire = belowPos.down();
                if (world.getBlockState(underCampfire).isOf(Blocks.HAY_BLOCK)) {
                    return true; // Extra boost if hay bale is underneath
                }
            }
        }

        return false;
    }

    // Check if the player is flying over a lit soul campfire
    private static boolean isAboveSoulCampfire(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        BlockPos playerPos = player.getBlockPos();

        // Check for soul campfires (10 block check)
        for (int i = 0; i < 10; i++) { // Checking up to 10 blocks below for soul campfires
            BlockPos belowPos = playerPos.down(i);
            BlockState blockState = world.getBlockState(belowPos);

            // Check if the soul campfire is lit
            if (blockState.isOf(Blocks.SOUL_CAMPFIRE) && blockState.get(CampfireBlock.LIT)) {
                return true; // Apply downward pull for lit soul campfire
            }
        }

        return false;
    }

    // Apply downward pull when flying over a soul campfire
    private static void applySoulCampfirePull(ServerPlayerEntity player) {
        // Only apply pull when the player is flying with Elytra
        if (isActuallyFlying(player)) {
            Vec3d currentVelocity = player.getVelocity();
            Vec3d pull = new Vec3d(0, -0.5, 0); // Downward pull (negative Y velocity)

            // Apply downward pull to the player's current velocity
            player.setVelocity(currentVelocity.add(pull));
            player.velocityModified = true; // Ensure velocity updates immediately
        }
    }

    // Apply velocity boost when flying over a fire source
    private static void applyFireBoost(ServerPlayerEntity player) {
        // Don't apply boost if already boosted during this tick
        if (boostedPlayers.contains(player)) {
            return; // Player is already boosted, no need to apply boost again
        }

        ServerWorld world = player.getServerWorld();
        BlockPos playerPos = player.getBlockPos();

        // Default boost power (start with 0.4)
        double boostPower = 0.6;

        // Check if the player is above a hay bale + campfire (25 blocks check)
        for (int i = 0; i < 25; i++) { // Checking up to 25 blocks below
            BlockPos belowPos = playerPos.down(i);
            BlockState blockState = world.getBlockState(belowPos);

            // Adjust boost based on proximity to lit campfire with hay bale
            if (blockState.isOf(Blocks.CAMPFIRE) && blockState.get(CampfireBlock.LIT)) {
                BlockPos underCampfire = belowPos.down();
                if (world.getBlockState(underCampfire).isOf(Blocks.HAY_BLOCK)) {
                    boostPower = 1.2; // Stronger boost if hay bale is underneath
                }

                // Gradual boost based on vertical distance (if the player is higher above the campfire)
                double verticalDistance = player.getY() - belowPos.getY();
                if (verticalDistance < 5) {
                    boostPower = 1.0; // Moderate boost if close (within 5 blocks above)
                } else if (verticalDistance >= 5 && verticalDistance < 10) {
                    boostPower = 0.8; // Slightly weaker boost (between 5 to 10 blocks above)
                } else if (verticalDistance >= 10) {
                    boostPower = 0.4; // Further distance, so weaker boost
                }
                break; // Stop once we've applied the boost logic for campfire
            }
        }

        // Apply boost by adding a constant velocity change (we add to the existing velocity)
        Vec3d currentVelocity = player.getVelocity();
        Vec3d boost = new Vec3d(0, boostPower, 0); // Only affecting the Y-axis (upward)

        // Add the boost to the player's current velocity
        player.setVelocity(currentVelocity.add(boost));
        player.velocityModified = true; // Ensure velocity updates immediately

        // Mark the player as boosted
        boostedPlayers.add(player);
    }
}

package niuhi.elytra.detection;

import niuhi.elytra.config.ModConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

public class FireBoostHandler {
    private final ModConfig config;
    private final Map<ServerPlayerEntity, Integer> boostedPlayers = new HashMap<>(); // Track boosted players and their cooldowns

    public FireBoostHandler(ModConfig config) {
        this.config = config;
    }

    /**
     * Process tick for campfire boost features
     * @param player The player to process
     */
    public void processTick(ServerPlayerEntity player) {
        if (!config.fireBoost.enabled) return;

        // Update cooldowns
        if (boostedPlayers.containsKey(player)) {
            int remainingCooldown = boostedPlayers.get(player);
            if (remainingCooldown > 0) {
                boostedPlayers.put(player, remainingCooldown - 1);
                return; // Skip processing if still on cooldown
            }
        }

        if (isAboveCampfire(player)) {
            applyBoostIfEligible(player);
        }
    }

    /**
     * Reset player state when they stop flying
     * @param player The player to reset
     */
    public void resetPlayer(ServerPlayerEntity player) {
        boostedPlayers.remove(player);
    }

    /**
     * Check for hay bale in area to determine detection range
     * @param world The server world
     * @param pos The position to check below
     * @return true if hay bale is present
     */
    private boolean hasHayBaleUnderneathArea(ServerWorld world, BlockPos pos) {
        // Check directly below
        if (world.getBlockState(pos.down()).isOf(Blocks.HAY_BLOCK)) {
            return true;
        }

        // Check a 3x3 area below (optional, for wider detection)
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue; // Skip center (already checked)

                if (world.getBlockState(pos.down().add(x, 0, z)).isOf(Blocks.HAY_BLOCK)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks if the player is above a campfire
     * @param player The player to check
     * @return true if player is above campfire
     */
    private boolean isAboveCampfire(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        BlockPos playerPos = player.getBlockPos();

        // First, check if there are hay bales in the detection area
        // This determines our detection height
        boolean hasHayBale = false;
        for (int y = 0; y <= Math.max(config.fireBoost.detectionHeight, config.fireBoost.hayDetectionHeight); y++) {
            BlockPos checkPos = playerPos.down(y);
            if (hasHayBaleUnderneathArea(world, checkPos)) {
                hasHayBale = true;
                break;
            }
        }

        // Determine maximum detection height based on hay bale presence
        int maxDetectionHeight = hasHayBale ?
                config.fireBoost.hayDetectionHeight :
                config.fireBoost.detectionHeight;

        // Now check for campfires within the determined range
        for (int y = 0; y <= maxDetectionHeight; y++) {
            BlockPos checkPos = playerPos.down(y);
            BlockState blockState = world.getBlockState(checkPos);

            FireType fireType = FireType.fromState(blockState);
            if (fireType != null && !fireType.isSoulFire()) {
                boolean isLit = blockState.contains(CampfireBlock.LIT) && blockState.get(CampfireBlock.LIT);
                if (isLit) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Apply boost to player if eligible
     * @param player The player to boost
     */
    private void applyBoostIfEligible(ServerPlayerEntity player) {
        // If the player is still on cooldown, don't boost
        if (boostedPlayers.containsKey(player) && boostedPlayers.get(player) > 0) {
            return;
        }

        ServerWorld world = player.getServerWorld();
        BlockPos playerPos = player.getBlockPos();
        double boostAmount = 0.0;

        // First determine if hay detection should be used
        boolean hasHayInArea = false;
        for (int y = 0; y <= Math.max(config.fireBoost.detectionHeight, config.fireBoost.hayDetectionHeight); y++) {
            BlockPos checkPos = playerPos.down(y);
            if (hasHayBaleUnderneathArea(world, checkPos)) {
                hasHayInArea = true;
                break;
            }
        }

        // Determine max detection height
        int maxDetectionHeight = hasHayInArea ?
                config.fireBoost.hayDetectionHeight :
                config.fireBoost.detectionHeight;

        // Find the first valid campfire
        for (int y = 0; y <= maxDetectionHeight; y++) {
            BlockPos checkPos = playerPos.down(y);
            BlockState blockState = world.getBlockState(checkPos);

            FireType fireType = FireType.fromState(blockState);
            if (fireType != null && !fireType.isSoulFire()) {
                boolean isLit = blockState.contains(CampfireBlock.LIT) && blockState.get(CampfireBlock.LIT);
                boolean directHayBale = world.getBlockState(checkPos.down()).isOf(Blocks.HAY_BLOCK);

                if (isLit) {
                    // Determine base boost amount
                    boostAmount = directHayBale ? config.fireBoost.hayBoost : config.fireBoost.baseBoost;

                    // Apply distance modifier
                    double distance = player.getY() - checkPos.getY();
                    if (distance < 5) {
                        boostAmount *= config.fireBoost.distanceMultiplierClose;
                    } else if (distance >= 5 && distance < 10) {
                        boostAmount *= config.fireBoost.distanceMultiplierMedium;
                    } else {
                        boostAmount *= config.fireBoost.distanceMultiplierFar;
                    }

                    // Set cooldown if configured
                    if (config.fireBoost.boostCooldownTicks > 0) {
                        boostedPlayers.put(player, config.fireBoost.boostCooldownTicks);
                    } else {
                        boostedPlayers.put(player, 0); // Just mark as boosted with no cooldown
                    }
                    break;
                }
            }
        }

        // Apply the boost if found
        if (boostAmount > 0) {
            applyBoost(player, boostAmount);
        }
    }

    /**
     * Apply vertical boost to player
     * @param player The player to boost
     * @param boostAmount The amount of boost to apply
     */
    private void applyBoost(ServerPlayerEntity player, double boostAmount) {
        Vec3d velocity = player.getVelocity();
        Vec3d boostVector = new Vec3d(0, boostAmount, 0);

        player.setVelocity(velocity.add(boostVector));
        player.velocityModified = true;
    }
}
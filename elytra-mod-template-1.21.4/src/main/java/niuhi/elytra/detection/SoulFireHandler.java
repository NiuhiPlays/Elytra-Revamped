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

public class SoulFireHandler {
    private final ModConfig config;
    private final FeedbackHandler feedbackHandler;
    private final Map<ServerPlayerEntity, Integer> pulledPlayers = new HashMap<>(); // Track players and their cooldowns

    public SoulFireHandler(ModConfig config, FeedbackHandler feedbackHandler) {
        this.config = config;
        this.feedbackHandler = feedbackHandler;
    }

    /**
     * Process tick for soul fire pull features
     * @param player The player to process
     */
    public void processTick(ServerPlayerEntity player) {
        // Check if soul fire is enabled
        if (!config.soulFire.enabled) return;

        // Update cooldowns
        if (pulledPlayers.containsKey(player)) {
            int remainingCooldown = pulledPlayers.get(player);
            if (remainingCooldown > 0) {
                pulledPlayers.put(player, remainingCooldown - 1);
                return; // Skip processing if still on cooldown
            }
        }

        applySoulCampfirePull(player);
    }

    /**
     * Reset player state when they stop flying
     * @param player The player to reset
     */
    public void resetPlayer(ServerPlayerEntity player) {
        pulledPlayers.remove(player);
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
     * Apply downward pull when flying over a soul campfire
     * @param player The player to apply pull to
     */
    private void applySoulCampfirePull(ServerPlayerEntity player) {
        // If the player is still on cooldown, don't apply pull
        if (pulledPlayers.containsKey(player) && pulledPlayers.get(player) > 0) {
            return;
        }

        ServerWorld world = player.getServerWorld();
        BlockPos playerPos = player.getBlockPos();

        // First determine if hay detection should be used
        boolean hasHayInArea = false;
        for (int y = 0; y <= Math.max(config.soulFire.detectionHeight, config.soulFire.hayDetectionHeight); y++) {
            BlockPos checkPos = playerPos.down(y);
            if (hasHayBaleUnderneathArea(world, checkPos)) {
                hasHayInArea = true;
                break;
            }
        }

        // Determine max detection height
        int maxDetectionHeight = hasHayInArea ?
                config.soulFire.hayDetectionHeight :
                config.soulFire.detectionHeight;

        for (int y = 0; y < maxDetectionHeight; y++) {
            BlockPos checkPos = playerPos.down(y);
            BlockState blockState = world.getBlockState(checkPos);

            FireType fireType = FireType.fromState(blockState);
            if (fireType != null && fireType.isSoulFire()) {
                boolean isLit = blockState.contains(CampfireBlock.LIT) && blockState.get(CampfireBlock.LIT);

                if (isLit) {
                    // Check if there's hay directly below and apply appropriate pull
                    boolean directHayBale = world.getBlockState(checkPos.down()).isOf(Blocks.HAY_BLOCK);
                    double pullAmount = directHayBale ? config.soulFire.hayPull : config.soulFire.basePull;

                    // Apply automatic scaling based on height if enabled
                    if (config.soulFire.autoScaleWithHeight) {
                        double distance = player.getY() - checkPos.getY();
                        double maxHeight = maxDetectionHeight;
                        // Calculate scale factor based on height (1.0 at close range, scaling down to 0.3 at max height)
                        double scaleFactor = 1.0 - (0.7 * (distance / maxHeight));
                        // Ensure scale factor is within reasonable bounds
                        scaleFactor = Math.min(1.0, Math.max(0.3, scaleFactor));
                        pullAmount *= scaleFactor;
                    }

                    // Set cooldown if configured
                    if (config.soulFire.pullCooldownTicks > 0) {
                        pulledPlayers.put(player, config.soulFire.pullCooldownTicks);
                    } else {
                        pulledPlayers.put(player, 0); // Just mark as pulled with no cooldown
                    }

                    applyPull(player, pullAmount);

                    // Play feedback effects
                    feedbackHandler.playSoulFirePullFeedback(player);
                    return;
                }
            }
        }
    }

    /**
     * Apply downward pull to player
     * @param player The player to pull
     * @param pullAmount The amount of pull to apply
     */
    private void applyPull(ServerPlayerEntity player, double pullAmount) {
        Vec3d velocity = player.getVelocity();
        Vec3d pullVector = new Vec3d(0, -pullAmount, 0); // Negative for downward pull

        player.setVelocity(velocity.add(pullVector));
        player.velocityModified = true;
    }
}
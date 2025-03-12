package niuhi.elytra.detection;

import niuhi.elytra.config.ModConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class SoulFireHandler {
    private final ModConfig config;

    public SoulFireHandler(ModConfig config) {
        this.config = config;
    }

    /**
     * Process tick for soul fire pull features
     * @param player The player to process
     */
    public void processTick(ServerPlayerEntity player) {
        if (!config.soulFire.enabled) return;

        applySoulCampfirePull(player);
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

                    applyPull(player, pullAmount);
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
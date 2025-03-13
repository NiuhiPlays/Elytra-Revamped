package niuhi.elytra.detection;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import niuhi.elytra.config.ModConfig;

public class DragHandler {
    private final ModConfig config;
    private final Random random = Random.create();
    private final FeedbackHandler feedbackHandler;

    public DragHandler(ModConfig config, FeedbackHandler feedbackHandler) {
        this.config = config;
        this.feedbackHandler = feedbackHandler;
    }

    /**
     * Apply drag to player's velocity when sneaking during flight
     * @param player The player to process
     */
    public void processTick(ServerPlayerEntity player) {
        if (!config.drag.enabled) return;

        if (!config.drag.requireSneaking || player.isSneaking()) {
            applyDrag(player);
            playDragFeedback(player);
        }
    }

    /**
     * Apply drag effect to player velocity
     * @param player The player to apply drag to
     */
    private void applyDrag(ServerPlayerEntity player) {
        Vec3d velocity = player.getVelocity();

        // Apply drag factor to horizontal velocity components (x and z)
        double draggedX = velocity.x * config.drag.dragFactor;
        double draggedZ = velocity.z * config.drag.dragFactor;

        // Leave y velocity as is to not interfere with vertical movement
        Vec3d draggedVelocity = new Vec3d(draggedX, velocity.y, draggedZ);

        // Only apply if the velocity actually changed
        if (!draggedVelocity.equals(velocity)) {
            player.setVelocity(draggedVelocity);
            player.velocityModified = true;
        }
    }

    /**
     * Play particle and sound effects for drag
     * @param player The player to play effects for
     */
    private void playDragFeedback(ServerPlayerEntity player) {
        if (!config.feedback.enableParticles && !config.feedback.enableSounds) {
            return; // Both effects disabled
        }

        ServerWorld world = player.getServerWorld();
        Vec3d pos = player.getPos();
        Vec3d velocity = player.getVelocity();

        // Only play effects if the player is moving with some speed
        double horizSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (horizSpeed < 0.3) return;

        // Create particles
        if (config.feedback.enableParticles) {
            // Spawn white dust particles in player's wake
            for (int i = 0; i < 5; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 0.5;
                double offsetY = (random.nextDouble() - 0.5) * 0.5;
                double offsetZ = (random.nextDouble() - 0.5) * 0.5;

                // Calculate position behind player based on velocity
                Vec3d particlePos = pos.subtract(
                        velocity.normalize().multiply(0.5 + random.nextDouble() * 0.5)
                ).add(offsetX, offsetY, offsetZ);

                world.spawnParticles(
                        ParticleTypes.CLOUD, // White cloud particles for "air resistance" effect
                        particlePos.x,
                        particlePos.y,
                        particlePos.z,
                        1, // count
                        0.05, // offset X
                        0.05, // offset Y
                        0.05, // offset Z
                        0.02 // speed - slow moving particles
                );
            }
        }

        // Play sound occasionally (not every tick to avoid sound spam)
        if (config.feedback.enableSounds && random.nextInt(20) == 0) {
            world.playSound(
                    null, // Player to exclude, null to include all
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.ENTITY_BREEZE_LAND, // Breeze land sound as requested
                    SoundCategory.PLAYERS,
                    config.feedback.soundVolume * 0.4f, // Lower volume to not be annoying
                    config.feedback.soundPitch * 1.2f + (random.nextFloat() * 0.1f) // Slightly higher pitch
            );
        }
    }
}
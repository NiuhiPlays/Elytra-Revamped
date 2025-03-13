package niuhi.elytra.detection;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import niuhi.elytra.config.ModConfig;

public class FeedbackHandler {
    private final ModConfig config;
    private final Random random = Random.create();

    public FeedbackHandler(ModConfig config) {
        this.config = config;
    }

    /**
     * Play fire boost feedback effects when a player gets boosted
     * @param player The player to play effects for
     */
    public void playFireBoostFeedback(ServerPlayerEntity player) {
        if (!config.feedback.enableParticles && !config.feedback.enableSounds) {
            return; // Both effects disabled
        }

        ServerWorld world = player.getServerWorld();
        Vec3d pos = player.getPos();

        // Create particles
        if (config.feedback.enableParticles) {
            // Spawn flame particles in a small area around the player
            for (int i = 0; i < 15; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 0.5;
                double offsetY = (random.nextDouble() - 0.5) * 0.5;
                double offsetZ = (random.nextDouble() - 0.5) * 0.5;

                world.spawnParticles(
                        ParticleTypes.FLAME,
                        pos.getX() + offsetX,
                        pos.getY() + offsetY,
                        pos.getZ() + offsetZ,
                        1, // count
                        0.0, // offset X
                        0.2, // offset Y (mostly upward)
                        0.0, // offset Z
                        0.05 // speed
                );
            }
        }

        // Play sound
        if (config.feedback.enableSounds) {
            world.playSound(
                    null, // Player to exclude, null to include all
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.ENTITY_BREEZE_IDLE_GROUND, // Breeze idle sound as requested
                    SoundCategory.PLAYERS,
                    config.feedback.soundVolume,
                    config.feedback.soundPitch + (random.nextFloat() * 0.2f - 0.1f) // Slight random pitch variation
            );
        }
    }

    /**
     * Play soul fire pull feedback effects when a player gets pulled down
     * @param player The player to play effects for
     */
    public void playSoulFirePullFeedback(ServerPlayerEntity player) {
        if (!config.feedback.enableParticles && !config.feedback.enableSounds) {
            return; // Both effects disabled
        }

        ServerWorld world = player.getServerWorld();
        Vec3d pos = player.getPos();

        // Create particles
        if (config.feedback.enableParticles) {
            // Spawn soul flame particles in a small area around the player
            for (int i = 0; i < 15; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 0.5;
                double offsetY = (random.nextDouble() - 0.5) * 0.5;
                double offsetZ = (random.nextDouble() - 0.5) * 0.5;

                world.spawnParticles(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        pos.getX() + offsetX,
                        pos.getY() + offsetY,
                        pos.getZ() + offsetZ,
                        1, // count
                        0.0, // offset X
                        -0.2, // offset Y (mostly downward)
                        0.0, // offset Z
                        0.05 // speed
                );
            }
        }

        // Play sound
        if (config.feedback.enableSounds) {
            world.playSound(
                    null, // Player to exclude, null to include all
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.ENTITY_BREEZE_IDLE_AIR, // A different breeze sound for soul fire
                    SoundCategory.PLAYERS,
                    config.feedback.soundVolume,
                    config.feedback.soundPitch * 0.7f + (random.nextFloat() * 0.1f) // Lower pitch for soul fire
            );
        }
    }
}
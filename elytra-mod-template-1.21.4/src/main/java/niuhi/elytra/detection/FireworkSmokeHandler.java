package niuhi.elytra.detection;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import niuhi.elytra.config.ModConfig;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FireworkSmokeHandler {
    private final ModConfig config;
    private final Random random = Random.create();

    // Track active smoke effects
    private final Map<ServerPlayerEntity, SmokeEffectData> activeEffects = new HashMap<>();

    // Base duration in ticks (20 ticks = 1 second)
    private static final int BASE_DURATION = 40; // 2 seconds base duration

    public FireworkSmokeHandler(ModConfig config) {
        this.config = config;
    }

    /**
     * Start a smoke particle effect when a player tries to use a firework while flying
     * @param player The player who tried to use the firework
     * @param rocketStrength The strength/level of the firework rocket (1-3)
     */
    public void playFireworkSmokeEffect(ServerPlayerEntity player, int rocketStrength) {
        // Skip if smoke effect is disabled
        if (!config.mechanics.enableFireworkSmoke) {
            return;
        }

        // Play initial sound if sounds are enabled
        if (config.feedback.enableSounds) {
            player.getServerWorld().playSound(
                    null, // Player to exclude, null to include all
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.ENTITY_BREEZE_WIND_BURST,
                    SoundCategory.PLAYERS,
                    config.feedback.soundVolume * 0.5f, // Slightly quieter than normal effects
                    config.feedback.soundPitch + (random.nextFloat() * 0.2f - 0.1f) // Slight random pitch variation
            );
        }

        // Calculate duration based on rocket strength
        // Clamping strength to valid range (1-3)
        int clampedStrength = Math.max(1, Math.min(3, rocketStrength));

        // Duration scales with rocket strength: 40, 60, or 80 ticks (2s, 3s, or 4s)
        int duration = BASE_DURATION + ((clampedStrength - 1) * 20);

        // Create or refresh smoke effect data with the calculated duration
        activeEffects.put(player, new SmokeEffectData(duration));
    }

    /**
     * Overloaded method for backward compatibility - uses default strength of 1
     * @param player The player who tried to use the firework
     */
    public void playFireworkSmokeEffect(ServerPlayerEntity player) {
        playFireworkSmokeEffect(player, 1);
    }

    /**
     * Process all active smoke effects, called every tick
     */
    public void processTick() {
        if (!config.mechanics.enableFireworkSmoke) {
            activeEffects.clear();
            return;
        }

        Iterator<Map.Entry<ServerPlayerEntity, SmokeEffectData>> iterator = activeEffects.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<ServerPlayerEntity, SmokeEffectData> entry = iterator.next();
            ServerPlayerEntity player = entry.getKey();
            SmokeEffectData effectData = entry.getValue();

            // Remove effect if player is offline or effect has expired
            if (!player.isAlive() || player.isRemoved() || !effectData.update()) {
                iterator.remove();
                continue;
            }

            // Spawn particles for this tick
            spawnFollowingParticles(player, effectData);
        }
    }

    /**
     * Spawn particles that follow the player
     */
    private void spawnFollowingParticles(ServerPlayerEntity player, SmokeEffectData effectData) {
        ServerWorld world = player.getServerWorld();
        Vec3d pos = player.getPos();
        Vec3d lookVec = player.getRotationVector().normalize();

        // Determine how many particles to spawn based on remaining duration
        // More particles at the beginning, fewer as the effect fades
        int particleCount = Math.max(1, Math.min(5, (int)(effectData.getRemainingPercentage() * 5)));

        for (int i = 0; i < particleCount; i++) {
            // Randomize position for a trailing effect
            double offsetX = (random.nextDouble() - 0.5) * 0.3;
            double offsetY = (random.nextDouble() - 0.5) * 0.3;
            double offsetZ = (random.nextDouble() - 0.5) * 0.3;

            // Position behind the player based on look direction
            // The multiplier creates distance behind the player
            Vec3d particlePos = pos.add(
                    lookVec.multiply(-0.5 - random.nextDouble() * 0.5) // Behind player with randomness
            ).add(offsetX, offsetY, offsetZ);

            // Create a small upward drift for the smoke
            double upDrift = 0.02 + random.nextDouble() * 0.03;

            // Particle velocity - very slight movement mainly upward
            world.spawnParticles(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    particlePos.x,
                    particlePos.y,
                    particlePos.z,
                    1, // count per spawn
                    offsetX * 0.02,
                    upDrift,  // slight upward drift
                    offsetZ * 0.02,
                    0.01 // speed
            );
        }
    }

    /**
     * Clear effects for a player when they stop flying
     */
    public void resetPlayer(ServerPlayerEntity player) {
        activeEffects.remove(player);
    }

    /**
     * Helper class to track effect duration and properties
     */
    private static class SmokeEffectData {
        private int remainingTicks;
        private final int totalDuration;

        public SmokeEffectData(int duration) {
            this.remainingTicks = duration;
            this.totalDuration = duration;
        }

        /**
         * Update the effect timer
         * @return true if the effect is still active, false if it has expired
         */
        public boolean update() {
            return --remainingTicks > 0;
        }

        /**
         * Get the percentage of time remaining
         * @return value between 0.0 and 1.0
         */
        public float getRemainingPercentage() {
            return (float) remainingTicks / totalDuration;
        }
    }
}
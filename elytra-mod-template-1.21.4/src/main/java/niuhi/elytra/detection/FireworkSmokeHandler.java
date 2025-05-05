package niuhi.elytra.detection;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import niuhi.elytra.config.DebugLogger;
import niuhi.elytra.config.ModConfig;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FireworkSmokeHandler {
    private final ModConfig config;
    private final Random random = Random.create();

    // Track active smoke effects
    private final Map<ServerPlayerEntity, SmokeEffectData> activeEffects = new HashMap<>();

    public FireworkSmokeHandler(ModConfig config) {
        this.config = config;
    }

    /**
     * Start a smoke particle effect when a player tries to use a firework while flying
     * @param player The player who tried to use the firework
     * @param flightDuration The flight duration of the firework rocket (1-3)
     */
    public void playFireworkSmokeEffect(ServerPlayerEntity player, int flightDuration) {
        // Skip if smoke effect is disabled
        if (!config.mechanics.enableFireworkSmoke) {
            return;
        }

        // Clamp flight duration to valid range (1-3)
        int clampedDuration = Math.max(1, Math.min(3, flightDuration));

        // Map flight duration to smoke effect duration: 20, 40, or 60 ticks
        int smokeDuration = clampedDuration * 20;

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

        // Create or refresh smoke effect data with the calculated duration
        activeEffects.put(player, new SmokeEffectData(smokeDuration));

        if (config.debug.enabled && config.debug.fireworkSmokeHandler) {
            DebugLogger.debug("FireworkSmokeHandler", "Player %s triggered firework smoke effect, flightDuration=%d, smokeDuration=%d ticks",
                    player.getName().getString(), clampedDuration, smokeDuration);
        }
    }

    /**
     * Overloaded method to extract flight duration from a firework rocket item stack
     * @param player The player who tried to use the firework
     * @param fireworkStack The firework rocket item stack
     */
    public void playFireworkSmokeEffect(ServerPlayerEntity player, ItemStack fireworkStack) {
        int flightDuration = getFlightDuration(fireworkStack);
        playFireworkSmokeEffect(player, flightDuration);
    }

    /**
     * Overloaded method for backward compatibility - uses default flight duration of 1
     * @param player The player who tried to use the firework
     */
    public void playFireworkSmokeEffect(ServerPlayerEntity player) {
        playFireworkSmokeEffect(player, 1);
    }

    /**
     * Helper method to extract flight duration from a firework rocket item stack
     * @param stack The firework rocket item stack
     * @return The flight duration (1-3), defaults to 1 if invalid or not a firework
     */
    private int getFlightDuration(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() != Items.FIREWORK_ROCKET) {
            if (config.debug.enabled && config.debug.fireworkSmokeHandler) {
                DebugLogger.debug("FireworkSmokeHandler", "Invalid or non-firework item stack: %s, defaulting to flight duration 1",
                        stack == null ? "null" : stack.toString());
            }
            return 1;
        }

        // Access the FireworksComponent using the Data Component API
        FireworksComponent fireworksComponent = stack.get(DataComponentTypes.FIREWORKS);
        if (fireworksComponent == null) {
            if (config.debug.enabled && config.debug.fireworkSmokeHandler) {
                DebugLogger.debug("FireworkSmokeHandler", "No Fireworks component for stack: %s, defaulting to flight duration 1",
                        stack.toString());
            }
            return 1;
        }

        // Get the flight duration from the FireworksComponent
        int flightDuration = fireworksComponent.flightDuration();
        if (flightDuration < 1 || flightDuration > 3) {
            if (config.debug.enabled && config.debug.fireworkSmokeHandler) {
                DebugLogger.debug("FireworkSmokeHandler", "Invalid flight duration %d for stack: %s, defaulting to 1",
                        flightDuration, stack.toString());
            }
            return 1;
        }

        if (config.debug.enabled && config.debug.fireworkSmokeHandler) {
            DebugLogger.debug("FireworkSmokeHandler", "Extracted flight duration %d for stack: %s",
                    flightDuration, stack.toString());
        }
        return flightDuration;
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
                if (config.debug.enabled && config.debug.fireworkSmokeHandler) {
                    DebugLogger.debug("FireworkSmokeHandler", "Player %s smoke effect ended", player.getName().getString());
                }
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
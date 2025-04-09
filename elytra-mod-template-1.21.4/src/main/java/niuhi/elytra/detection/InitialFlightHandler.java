package niuhi.elytra.detection;

import net.minecraft.server.network.ServerPlayerEntity;
import niuhi.elytra.config.DebugLogger;
import niuhi.elytra.config.ModConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InitialFlightHandler {
    private final ModConfig config;
    private final Map<UUID, Boolean> hasUsedInitialFirework = new HashMap<>();
    private final Map<UUID, Integer> flightDurationTicks = new HashMap<>();

    public InitialFlightHandler(ModConfig config) {
        this.config = config;
    }

    /**
     * Process tick to track flight duration
     * @param player The player to track
     */
    public void processTick(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();

        if (player.isGliding()) {
            // Increment flight duration counter
            int currentDuration = flightDurationTicks.getOrDefault(playerId, 0);
            flightDurationTicks.put(playerId, currentDuration + 1);
            if (config.debug.enabled && config.debug.initialFlightHandler) {
                DebugLogger.debug("InitialFlightHandler", "Player %s flight duration: %s ticks",
                        player.getName().getString(), currentDuration + 1);
            }
        }
    }

    /**
     * Check if a player should be allowed to use an initial firework
     * @param player The player to check
     * @return true if player can use initial firework
     */
    public boolean canUseInitialFirework(ServerPlayerEntity player) {
        if (!config.mechanics.allowInitialFirework) {
            return false;
        }

        UUID playerId = player.getUuid();

        // Player can't use initial firework if they've already used one
        if (hasUsedInitialFirework.getOrDefault(playerId, false)) {
            return false;
        }

        // Check if they're still within the grace period
        int flightDuration = flightDurationTicks.getOrDefault(playerId, 0);
        return flightDuration <= config.mechanics.initialFireworkGraceTicks;
    }

    /**
     * Mark player as having used their initial firework
     * @param player The player who used a firework
     */
    public void markInitialFireworkUsed(ServerPlayerEntity player) {
        hasUsedInitialFirework.put(player.getUuid(), true);
    }

    /**
     * Reset player tracking when they stop flying
     * @param player The player who stopped flying
     */
    public void resetPlayer(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();

        // Only reset when the player has landed completely
        if (!player.isGliding()) {
            hasUsedInitialFirework.remove(playerId);
            flightDurationTicks.remove(playerId);
        }
    }
}
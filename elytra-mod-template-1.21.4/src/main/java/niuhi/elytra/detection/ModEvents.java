package niuhi.elytra.detection;

import niuhi.elytra.config.ModConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

public class ModEvents {
    private static final ModConfig config = ModConfig.load();
    private static final ElytraFlightDetector flightDetector = new ElytraFlightDetector(config);
    private static final FeedbackHandler feedbackHandler = new FeedbackHandler(config);
    private static final FireBoostHandler fireBoostHandler = new FireBoostHandler(config, feedbackHandler);
    private static final SoulFireHandler soulFireHandler = new SoulFireHandler(config, feedbackHandler);
    private static final FireworkSmokeHandler fireworkSmokeHandler = new FireworkSmokeHandler(config);
    private static final DragHandler dragHandler = new DragHandler(config, feedbackHandler);

    public static void register() {
        // Register firework prevention event if enabled in config
        if (config.mechanics.disableFireworks) {
            registerFireworkPrevention();
        }

        // Register tick event for boost handlers
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Process smoke effects every tick
            fireworkSmokeHandler.processTick();

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (flightDetector.isFlying(player)) {
                    fireBoostHandler.processTick(player);
                    soulFireHandler.processTick(player);
                    dragHandler.processTick(player);
                } else {
                    fireBoostHandler.resetPlayer(player);
                    soulFireHandler.resetPlayer(player);
                    fireworkSmokeHandler.resetPlayer(player); // Clear smoke effects when player stops flying
                }
            }
        });
    }

    /**
     * Registers the event handler for preventing firework use while flying
     */
    private static void registerFireworkPrevention() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                ItemStack itemStack = player.getStackInHand(hand);

                if (flightDetector.isFlying(serverPlayer) && itemStack.isOf(Items.FIREWORK_ROCKET)) {
                    // Start smoke effect when firework use is prevented
                    fireworkSmokeHandler.playFireworkSmokeEffect(serverPlayer);

                    return ActionResult.FAIL; // Block firework use
                }
            }
            return ActionResult.PASS; // Allow normal use otherwise
        });
    }
}
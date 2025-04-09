package niuhi.elytra.detection;

import niuhi.elytra.ElytraMod;
import niuhi.elytra.config.ModConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

public class ModEvents {
    public static final ModConfig config = ModConfig.init();
    private static final ElytraFlightDetector flightDetector = new ElytraFlightDetector(config);
    private static final FeedbackHandler feedbackHandler = new FeedbackHandler(config);
    private static final FireBoostHandler fireBoostHandler = new FireBoostHandler(config, feedbackHandler);
    private static final SoulFireHandler soulFireHandler = new SoulFireHandler(config, feedbackHandler);
    private static final FireworkSmokeHandler fireworkSmokeHandler = new FireworkSmokeHandler(config);
    private static final DragHandler dragHandler = new DragHandler(config, feedbackHandler);
    private static final InitialFlightHandler initialFlightHandler = new InitialFlightHandler(config);

    public static void register() {
        registerFireworkPrevention();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (config == null) {
                ElytraMod.LOGGER.error("Config is null in ModEvents tick handler!");
                return;
            }
            fireworkSmokeHandler.processTick();

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                initialFlightHandler.processTick(player);

                boolean isCurrentlyFlying = flightDetector.isFlying(player);

                if (isCurrentlyFlying) {
                    fireBoostHandler.processTick(player);
                    soulFireHandler.processTick(player);
                    dragHandler.processTick(player);
                } else {
                    fireBoostHandler.resetPlayer(player);
                    soulFireHandler.resetPlayer(player);
                    fireworkSmokeHandler.resetPlayer(player);
                    initialFlightHandler.resetPlayer(player);
                }
            }
        });
    }

    private static void registerFireworkPrevention() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                ItemStack itemStack = player.getStackInHand(hand);
                if (config != null && config.mechanics.disableFireworks &&
                        player.isGliding() &&
                        itemStack.isOf(Items.FIREWORK_ROCKET)) {

                    // Check if this is their initial firework use
                    if (initialFlightHandler.canUseInitialFirework(serverPlayer)) {
                        // Let this firework use pass through
                        initialFlightHandler.markInitialFireworkUsed(serverPlayer);
                        return ActionResult.PASS;
                    }

                    // Otherwise, show smoke effect
                    fireworkSmokeHandler.playFireworkSmokeEffect(serverPlayer);
                    return ActionResult.FAIL;
                }
            }
            return ActionResult.PASS;
        });
    }
}
package niuhi.elytra.detection;

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

    public static void register() {
        // Always register firework prevention, but check config dynamically
        registerFireworkPrevention();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            fireworkSmokeHandler.processTick();
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (flightDetector.isFlying(player)) {
                    fireBoostHandler.processTick(player);
                    soulFireHandler.processTick(player);
                    dragHandler.processTick(player);
                } else {
                    fireBoostHandler.resetPlayer(player);
                    soulFireHandler.resetPlayer(player);
                    fireworkSmokeHandler.resetPlayer(player);
                }
            }
        });
    }

    private static void registerFireworkPrevention() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                ItemStack itemStack = player.getStackInHand(hand);
                // Check config dynamically here
                if (config.mechanics.disableFireworks &&
                        flightDetector.isFlying(serverPlayer) &&
                        itemStack.isOf(Items.FIREWORK_ROCKET)) {
                    fireworkSmokeHandler.playFireworkSmokeEffect(serverPlayer);
                    return ActionResult.FAIL;
                }
            }
            return ActionResult.PASS;
        });
    }
}
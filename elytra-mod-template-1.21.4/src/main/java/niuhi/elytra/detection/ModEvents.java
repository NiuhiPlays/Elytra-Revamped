package niuhi.elytra.detection;

import niuhi.elytra.config.ModConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Set;

public class ModEvents {
    private static final ModConfig config = ModConfig.load();
    private static final Set<ServerPlayerEntity> boostedPlayers = new HashSet<>(); // Track boosted players

    public static void register() {
        // Prevent Firework Use When Flying (if enabled in config)
        if (config.disableFireworks) {
            UseItemCallback.EVENT.register((player, world, hand) -> {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    ItemStack itemStack = player.getStackInHand(hand);
                    if (isActuallyFlying(serverPlayer) && itemStack.isOf(Items.FIREWORK_ROCKET)) {
                        return ActionResult.FAIL; // Block firework use
                    }
                }
                return ActionResult.PASS;
            });
        }

        // Elytra Fire Boosting & Soul Campfire Pull
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (isActuallyFlying(player)) {
                    if (isAboveCampfire(player)) {
                        applyBoostIfNearCampfire(player);
                    } else {
                        boostedPlayers.remove(player);
                    }
                    applySoulCampfirePull(player);
                } else {
                    boostedPlayers.remove(player);
                }
            }
        });
    }

    private static boolean isActuallyFlying(ServerPlayerEntity player) {
        Vec3d velocity = player.getVelocity();
        return isWearingElytra(player) && !player.isOnGround() && (velocity.y != 0 || Math.abs(velocity.x) + Math.abs(velocity.z) > 0.1);
    }

    private static boolean isAboveCampfire(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        BlockPos playerPos = player.getBlockPos();

        for (int y = 0; y <= config.hayFireHeight; y++) {
            BlockPos checkPos = playerPos.down(y);
            BlockState blockState = world.getBlockState(checkPos);

            if (isLitCampfire(blockState)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isWearingElytra(ServerPlayerEntity player) {
        return player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
    }

    private static void applyBoostIfNearCampfire(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        BlockPos playerPos = player.getBlockPos();

        if (boostedPlayers.contains(player)) {
            return;
        }

        double boostAmount = 0.0;

        for (int y = 0; y <= config.hayFireHeight; y++) {
            BlockPos checkPos = playerPos.down(y);
            BlockState blockState = world.getBlockState(checkPos);

            if (isLitCampfire(blockState)) {
                boolean hasHayBale = world.getBlockState(checkPos.down()).isOf(Blocks.HAY_BLOCK);
                boostAmount = hasHayBale ? config.hayBoost : config.baseBoost;

                double verticalDistance = player.getY() - checkPos.getY();
                boostAmount *= (verticalDistance < 5) ? 0.8 : (verticalDistance < 10) ? 0.6 : 0.4;

                boostedPlayers.add(player);
                break;
            }
        }

        if (boostAmount != 0) {
            applyBoost(player, boostAmount);
        }
    }

    private static void applySoulCampfirePull(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        BlockPos playerPos = player.getBlockPos();

        for (int i = 0; i < config.normalFireHeight; i++) {
            BlockPos belowPos = playerPos.down(i);
            BlockState blockState = world.getBlockState(belowPos);

            if (blockState.isOf(Blocks.SOUL_CAMPFIRE) && blockState.contains(CampfireBlock.LIT) && blockState.get(CampfireBlock.LIT)) {
                boolean hasHayBale = world.getBlockState(belowPos.down()).isOf(Blocks.HAY_BLOCK);
                applyBoost(player, hasHayBale ? -config.hayPull : -config.basePull);
                return;
            }
        }
    }

    private static void applyBoost(ServerPlayerEntity player, double boostAmount) {
        Vec3d velocity = player.getVelocity();
        player.setVelocity(velocity.add(0, boostAmount, 0));
        player.velocityModified = true;
    }

    private static boolean isLitCampfire(BlockState state) {
        return (state.isOf(Blocks.CAMPFIRE) || state.isOf(Blocks.SOUL_CAMPFIRE)) && state.getOrEmpty(CampfireBlock.LIT).orElse(false);
    }
}

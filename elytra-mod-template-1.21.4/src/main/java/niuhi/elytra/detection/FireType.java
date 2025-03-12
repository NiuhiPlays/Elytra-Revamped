package niuhi.elytra.detection;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

public enum FireType {
    NORMAL_FIRE(Blocks.FIRE, 0.3),
    CAMPFIRE(Blocks.CAMPFIRE, 0.3),
    SOUL_CAMPFIRE(Blocks.SOUL_CAMPFIRE, -0.3); // Negative for pull effect

    private final Block block;
    private final double baseEffect;

    FireType(Block block, double baseEffect) {
        this.block = block;
        this.baseEffect = baseEffect;
    }

    public boolean matches(BlockState state) {
        return state.isOf(block);
    }

    public double getBaseEffect() {
        return baseEffect;
    }

    public boolean isSoulFire() {
        return this == SOUL_CAMPFIRE;
    }

    public static FireType fromState(BlockState state) {
        for (FireType type : values()) {
            if (type.matches(state)) {
                return type;
            }
        }
        return null;
    }
}
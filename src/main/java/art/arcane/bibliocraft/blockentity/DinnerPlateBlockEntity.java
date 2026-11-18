package art.arcane.bibliocraft.blockentity;

import art.arcane.bibliocraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DinnerPlateBlockEntity extends FurnitureInventoryBlockEntity {
    public static final int SLOT_COUNT = 3;
    private static final int BITES_PER_MEAL = 4;
    private static final int BITE_TIMEOUT = 60;
    private final Map<UUID, Bite> bites = new HashMap<>();

    public DinnerPlateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DINNER_PLATE.get(), pos, state, SLOT_COUNT);
    }

    public static boolean isFood(ItemStack stack) {
        return !stack.isEmpty() && stack.isEdible();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isFood(stack);
    }

    public boolean finishBite(Player player) {
        long tick = player.level().getGameTime();
        bites.values().removeIf(bite -> tick - bite.tick() > BITE_TIMEOUT);
        UUID id = player.getUUID();
        Bite prior = bites.get(id);
        int count = prior == null ? 1 : prior.count() + 1;
        if (count == BITES_PER_MEAL) {
            bites.remove(id);
            return true;
        }
        bites.put(id, new Bite(tick, count));
        return false;
    }

    public void stopEating(Player player) {
        bites.remove(player.getUUID());
    }

    private record Bite(long tick, int count) {}
}

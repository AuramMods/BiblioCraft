package art.arcane.bibliocraft.item;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.jetbrains.annotations.Nullable;

public final class DrillSelection {
    private DrillSelection() {}

    public static @Nullable BlockPos select(UseOnContext context, String key, String prompt) {
        var player = context.getPlayer();
        ItemStack tool = context.getItemInHand();
        CompoundTag previous = tool.getTagElement(key);
        String dimension = context.getLevel().dimension().location().toString();
        if (previous != null && previous.hasUUID("Player") && previous.getUUID("Player").equals(player.getUUID())
                && previous.getString("Dimension").equals(dimension)) {
            tool.removeTagKey(key);
            return BlockPos.of(previous.getLong("Pos"));
        }
        CompoundTag selection = tool.getOrCreateTagElement(key);
        selection.putLong("Pos", context.getClickedPos().asLong());
        selection.putUUID("Player", player.getUUID());
        selection.putString("Dimension", dimension);
        player.displayClientMessage(Component.translatable(prompt), true);
        return null;
    }
}

package art.arcane.bibliocraft.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ColoredBlockItem extends BlockItem {
    public ColoredBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static DyeColor getColor(ItemStack stack) {
        CompoundTag state = stack.getTagElement(BLOCK_STATE_TAG);
        return state == null ? DyeColor.WHITE : DyeColor.byName(state.getString("color"), DyeColor.WHITE);
    }

    public static ItemStack applyColor(ItemStack stack, DyeColor color) {
        stack.getOrCreateTagElement(BLOCK_STATE_TAG).putString("color", color.getSerializedName());
        return stack;
    }

    public void addVariants(CreativeModeTab.Output output) {
        for (DyeColor color : DyeColor.values()) {
            output.accept(applyColor(new ItemStack(this), color));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable(getDescriptionId() + ".color." + getColor(stack).getSerializedName()));
    }
}

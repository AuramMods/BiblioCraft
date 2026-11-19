package art.arcane.bibliocraft.item;

import art.arcane.bibliocraft.painting.PaintingArt;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class PaintingCanvasItem extends Item {
    public PaintingCanvasItem() {
        super(new Properties());
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return PaintingArt.from(stack).isBlank() ? super.getMaxStackSize(stack) : 1;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        PaintingArt art = PaintingArt.from(stack);
        if (!art.isBlank()) {
            tooltip.add(art.name().copy().withStyle(ChatFormatting.GRAY));
        }
    }
}

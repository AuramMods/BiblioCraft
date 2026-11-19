package art.arcane.bibliocraft.item;

import art.arcane.bibliocraft.blockentity.FancySignLayout;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class FancySignItem extends FurnitureBlockItem {
    public FancySignItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        var data = stack.getTagElement("BlockEntityTag");
        if (data != null && (data.contains(FancySignLayout.TAG, Tag.TAG_COMPOUND)
                || !data.getList("Items", Tag.TAG_COMPOUND).isEmpty())) {
            tooltip.add(Component.translatable("item.bibliocraft.fancy_sign.contents").withStyle(ChatFormatting.GRAY));
        }
    }
}

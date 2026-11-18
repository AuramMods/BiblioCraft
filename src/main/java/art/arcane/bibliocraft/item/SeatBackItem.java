package art.arcane.bibliocraft.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class SeatBackItem extends Item {
    private final int style;

    public SeatBackItem(int style) {
        super(new Properties());
        this.style = style;
    }

    public int style() {
        return style;
    }

    @Override
    public Component getName(ItemStack stack) {
        return FurnitureBlockItem.materialName(stack, super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        FurnitureBlockItem.appendPanelTooltip(stack, tooltip);
    }

    public void addVariants(CreativeModeTab.Output output) {
        FurnitureBlockItem.addMaterialVariants(this, output);
    }
}

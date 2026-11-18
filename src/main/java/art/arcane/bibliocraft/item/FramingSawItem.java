package art.arcane.bibliocraft.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class FramingSawItem extends Item {
    public FramingSawItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return stack.copyWithCount(1);
    }
}

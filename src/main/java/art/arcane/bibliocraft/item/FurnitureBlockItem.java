package art.arcane.bibliocraft.item;

import art.arcane.bibliocraft.block.FurnitureMaterial;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class FurnitureBlockItem extends BlockItem {
    public FurnitureBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        FurnitureMaterial.Wood wood = FurnitureMaterial.fromItem(stack).wood();
        return Component.translatable("item.bibliocraft.furniture_name",
                Component.translatable("wood.bibliocraft." + wood.id()), super.getName(stack));
    }

    public void addVariants(CreativeModeTab.Output output) {
        for (FurnitureMaterial.Wood wood : FurnitureMaterial.Wood.values()) {
            output.accept(new FurnitureMaterial(wood, null).applyTo(new ItemStack(this)));
        }
    }
}

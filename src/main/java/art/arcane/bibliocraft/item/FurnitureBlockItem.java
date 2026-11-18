package art.arcane.bibliocraft.item;

import art.arcane.bibliocraft.block.FurnitureMaterial;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.List;

public class FurnitureBlockItem extends BlockItem {
    public FurnitureBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return materialName(stack, super.getName(stack));
    }

    static Component materialName(ItemStack stack, Component itemName) {
        FurnitureMaterial.Wood wood = FurnitureMaterial.fromItem(stack).wood();
        return Component.translatable("item.bibliocraft.furniture_name",
                Component.translatable("wood.bibliocraft." + wood.id()), itemName);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        appendPanelTooltip(stack, tooltip);
    }

    static void appendPanelTooltip(ItemStack stack, List<Component> tooltip) {
        var panel = FurnitureMaterial.fromItem(stack).panel();
        if (panel != null) {
            Component name = BuiltInRegistries.BLOCK.getOptional(panel).map(Block::getName)
                    .orElseGet(() -> Component.literal(panel.toString()));
            tooltip.add(Component.translatable("item.bibliocraft.furniture_panel", name).withStyle(ChatFormatting.GRAY));
        }
    }

    public void addVariants(CreativeModeTab.Output output) {
        addMaterialVariants(this, output);
    }

    static void addMaterialVariants(Item item, CreativeModeTab.Output output) {
        for (FurnitureMaterial.Wood wood : FurnitureMaterial.Wood.values()) {
            output.accept(new FurnitureMaterial(wood, null).applyTo(new ItemStack(item)));
        }
    }
}

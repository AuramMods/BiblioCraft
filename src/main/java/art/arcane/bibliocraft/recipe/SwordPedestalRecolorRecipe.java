package art.arcane.bibliocraft.recipe;

import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.item.ColoredBlockItem;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModRecipes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class SwordPedestalRecolorRecipe extends CustomRecipe {
    public SwordPedestalRecolorRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return BiblioConfig.COMMON.enableSwordPedestal.get() && !assemble(container, level.registryAccess()).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registries) {
        ItemStack pedestal = ItemStack.EMPTY;
        DyeColor color = null;
        for (ItemStack stack : container.getItems()) {
            if (stack.isEmpty()) {
                continue;
            }
            if (pedestal.isEmpty() && stack.is(ModBlocks.SWORD_PEDESTAL.get().asItem())) {
                pedestal = stack;
            } else if (color == null && stack.getItem() instanceof DyeItem dye) {
                color = dye.getDyeColor();
            } else {
                return ItemStack.EMPTY;
            }
        }
        if (pedestal.isEmpty() || color == null || color == ColoredBlockItem.getColor(pedestal)) {
            return ItemStack.EMPTY;
        }
        return ColoredBlockItem.applyColor(pedestal.copyWithCount(1), color);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SWORD_PEDESTAL_RECOLOR.get();
    }
}

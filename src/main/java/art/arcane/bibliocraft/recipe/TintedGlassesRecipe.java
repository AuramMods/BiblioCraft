package art.arcane.bibliocraft.recipe;

import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.item.ReadingGlassesItem;
import art.arcane.bibliocraft.registry.ModItems;
import art.arcane.bibliocraft.registry.ModRecipes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.Tags;

public final class TintedGlassesRecipe extends CustomRecipe {
    public TintedGlassesRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return BiblioConfig.COMMON.enableReadingglasses.get() && !ingredients(container).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registries) {
        return ReadingGlassesItem.withVariant(ingredients(container), ReadingGlassesItem.Variant.TINTED);
    }

    private static ItemStack ingredients(CraftingContainer container) {
        ItemStack glasses = ItemStack.EMPTY;
        boolean dye = false;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (glasses.isEmpty() && stack.is(ModItems.BIBLIO_GLASSES.get())
                    && ReadingGlassesItem.variant(stack) == ReadingGlassesItem.Variant.READING) {
                glasses = stack;
            } else if (!dye && stack.is(Tags.Items.DYES_GRAY)) {
                dye = true;
            } else {
                return ItemStack.EMPTY;
            }
        }
        return dye ? glasses : ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.TINTED_GLASSES.get();
    }
}

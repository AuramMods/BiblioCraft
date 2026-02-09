package art.arcane.bibliocraft.recipe;

import art.arcane.bibliocraft.config.BiblioFeatureToggles;
import art.arcane.bibliocraft.registry.ModItems;
import art.arcane.bibliocraft.registry.ModRecipes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public class EnchantedAtlasRecipe extends CustomRecipe {
    private static final int CENTER_SLOT = 4;
    private static final int[] CORNER_SLOTS = {0, 2, 6, 8};
    private static final int[] EDGE_SLOTS = {1, 3, 5, 7};

    public EnchantedAtlasRecipe(ResourceLocation recipeId, CraftingBookCategory category) {
        super(recipeId, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        if (!BiblioFeatureToggles.isEnchantedAtlasRecipeEnabled()) {
            return false;
        }
        if (container.getContainerSize() < 9) {
            return false;
        }
        if (!container.getItem(CENTER_SLOT).is(ModItems.ATLAS_BOOK.get())) {
            return false;
        }

        for (int slot : CORNER_SLOTS) {
            if (!container.getItem(slot).is(Items.ENDER_PEARL)) {
                return false;
            }
        }

        int compassCount = 0;
        int enchantedBookCount = 0;
        for (int slot : EDGE_SLOTS) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(ModItems.WAYPOINT_COMPASS.get())) {
                compassCount++;
                continue;
            }
            if (stack.is(Items.ENCHANTED_BOOK)) {
                enchantedBookCount++;
                continue;
            }
            return false;
        }

        if (compassCount != 2 || enchantedBookCount != 2) {
            return false;
        }

        for (int slot = 9; slot < container.getContainerSize(); slot++) {
            if (!container.getItem(slot).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        ItemStack atlas = container.getItem(CENTER_SLOT);
        if (!atlas.is(ModItems.ATLAS_BOOK.get())) {
            return ItemStack.EMPTY;
        }

        ItemStack result = atlas.copyWithCount(1);
        CompoundTag tag = result.getOrCreateTag();
        // Placeholder parity marker for old death-compass enchanted atlas behavior.
        tag.putBoolean("bibliocraft:death_compass_enabled", true);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        ItemStack result = new ItemStack(ModItems.ATLAS_BOOK.get());
        result.getOrCreateTag().putBoolean("bibliocraft:death_compass_enabled", true);
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ENCHANTED_ATLAS_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.ENCHANTED_ATLAS_RECIPE_TYPE.get();
    }
}

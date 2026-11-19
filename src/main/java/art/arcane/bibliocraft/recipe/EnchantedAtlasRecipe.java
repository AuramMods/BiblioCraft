package art.arcane.bibliocraft.recipe;

import art.arcane.bibliocraft.config.BiblioFeatureToggles;
import art.arcane.bibliocraft.enchantment.DeathCompassEnchantment;
import art.arcane.bibliocraft.registry.ModItems;
import art.arcane.bibliocraft.registry.ModRecipes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class EnchantedAtlasRecipe extends CustomRecipe {
    private static final int CENTER_SLOT = 4;
    private static final int[] CORNER_SLOTS = {0, 2, 6, 8};

    public EnchantedAtlasRecipe(ResourceLocation recipeId, CraftingBookCategory category) {
        super(recipeId, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        if (!BiblioFeatureToggles.isEnchantedAtlasRecipeEnabled()) {
            return false;
        }
        if (!canCraftInDimensions(container.getWidth(), container.getHeight())) {
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

        boolean verticalCompasses = container.getItem(1).is(ModItems.WAYPOINT_COMPASS.get());
        var vertical = verticalCompasses ? ModItems.WAYPOINT_COMPASS.get() : Items.ENCHANTED_BOOK;
        var horizontal = verticalCompasses ? Items.ENCHANTED_BOOK : ModItems.WAYPOINT_COMPASS.get();
        return container.getItem(1).is(vertical) && container.getItem(7).is(vertical)
                && container.getItem(3).is(horizontal) && container.getItem(5).is(horizontal);
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        ItemStack atlas = container.getItem(CENTER_SLOT);
        if (!atlas.is(ModItems.ATLAS_BOOK.get())) {
            return ItemStack.EMPTY;
        }

        return DeathCompassEnchantment.apply(atlas.copyWithCount(1));
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width == 3 && height == 3;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return DeathCompassEnchantment.apply(new ItemStack(ModItems.ATLAS_BOOK.get()));
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ENCHANTED_ATLAS_SERIALIZER.get();
    }

}

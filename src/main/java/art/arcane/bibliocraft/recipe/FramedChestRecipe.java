package art.arcane.bibliocraft.recipe;

import art.arcane.bibliocraft.block.FurnitureMaterial;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModRecipes;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

public final class FramedChestRecipe extends ShapedRecipe {
    private final FurnitureMaterial.Wood wood;

    private FramedChestRecipe(ShapedRecipe recipe) {
        super(recipe.getId(), recipe.getGroup(), recipe.category(), recipe.getWidth(), recipe.getHeight(),
                recipe.getIngredients(), recipe.getResultItem(RegistryAccess.EMPTY), recipe.showNotification());
        wood = FurnitureMaterial.fromItem(recipe.getResultItem(RegistryAccess.EMPTY)).wood();
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        if (!super.matches(container, level)) {
            return false;
        }
        ItemStack label = findLabel(container);
        return !label.isEmpty() && FurnitureMaterial.fromItem(label).wood() == wood;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registries) {
        return FurnitureMaterial.fromItem(findLabel(container)).applyTo(super.assemble(container, registries));
    }

    private static ItemStack findLabel(CraftingContainer container) {
        for (ItemStack stack : container.getItems()) {
            if (stack.is(ModBlocks.LABEL.get().asItem())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.FRAMED_CHEST.get();
    }

    public static final class Serializer implements RecipeSerializer<FramedChestRecipe> {
        private final ShapedRecipe.Serializer shaped = new ShapedRecipe.Serializer();

        @Override
        public FramedChestRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new FramedChestRecipe(shaped.fromJson(id, json));
        }

        @Override
        public FramedChestRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new FramedChestRecipe(shaped.fromNetwork(id, buffer));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, FramedChestRecipe recipe) {
            shaped.toNetwork(buffer, recipe);
        }
    }
}

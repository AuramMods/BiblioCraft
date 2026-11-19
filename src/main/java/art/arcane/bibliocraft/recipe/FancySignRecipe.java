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
import net.minecraft.world.item.crafting.ShapelessRecipe;

public final class FancySignRecipe extends ShapelessRecipe {
    private FancySignRecipe(ShapelessRecipe recipe) {
        super(recipe.getId(), recipe.getGroup(), recipe.category(), recipe.getResultItem(RegistryAccess.EMPTY),
                recipe.getIngredients());
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registries) {
        ItemStack result = super.assemble(container, registries);
        for (ItemStack stack : container.getItems()) {
            if (stack.is(ModBlocks.LABEL.get().asItem())) {
                return FurnitureMaterial.fromItem(stack).applyTo(result);
            }
        }
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.FANCY_SIGN.get();
    }

    public static final class Serializer implements RecipeSerializer<FancySignRecipe> {
        private final ShapelessRecipe.Serializer vanilla = new ShapelessRecipe.Serializer();

        @Override
        public FancySignRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new FancySignRecipe(vanilla.fromJson(id, json));
        }

        @Override
        public FancySignRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new FancySignRecipe(vanilla.fromNetwork(id, buffer));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, FancySignRecipe recipe) {
            vanilla.toNetwork(buffer, recipe);
        }
    }
}

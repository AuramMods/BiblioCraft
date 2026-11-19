package art.arcane.bibliocraft.recipe;

import art.arcane.bibliocraft.block.FurnitureMaterial;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

public final class MaterialShapedRecipe extends ShapedRecipe {
    private final FurnitureMaterial.Wood wood;
    private final Serializer serializer;

    private MaterialShapedRecipe(ShapedRecipe recipe, Serializer serializer) {
        super(recipe.getId(), recipe.getGroup(), recipe.category(), recipe.getWidth(), recipe.getHeight(),
                recipe.getIngredients(), recipe.getResultItem(RegistryAccess.EMPTY), recipe.showNotification());
        this.serializer = serializer;
        wood = FurnitureMaterial.fromItem(recipe.getResultItem(RegistryAccess.EMPTY)).wood();
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        if (!super.matches(container, level)) {
            return false;
        }
        ItemStack material = findMaterial(container);
        return !material.isEmpty() && FurnitureMaterial.fromItem(material).wood() == wood;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registries) {
        return FurnitureMaterial.fromItem(findMaterial(container)).applyTo(super.assemble(container, registries));
    }

    private ItemStack findMaterial(CraftingContainer container) {
        for (ItemStack stack : container.getItems()) {
            if (stack.is(serializer.materialSource.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return serializer;
    }

    public static final class Serializer implements RecipeSerializer<MaterialShapedRecipe> {
        private final ShapedRecipe.Serializer shaped = new ShapedRecipe.Serializer();
        private final Supplier<Item> materialSource;

        public Serializer(Supplier<Item> materialSource) {
            this.materialSource = materialSource;
        }

        @Override
        public MaterialShapedRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new MaterialShapedRecipe(shaped.fromJson(id, json), this);
        }

        @Override
        public MaterialShapedRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new MaterialShapedRecipe(shaped.fromNetwork(id, buffer), this);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, MaterialShapedRecipe recipe) {
            shaped.toNetwork(buffer, recipe);
        }
    }
}

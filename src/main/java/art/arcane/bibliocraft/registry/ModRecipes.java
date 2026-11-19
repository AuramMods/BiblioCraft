package art.arcane.bibliocraft.registry;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.recipe.EnchantedAtlasRecipe;
import art.arcane.bibliocraft.recipe.ColoredBlockRecolorRecipe;
import art.arcane.bibliocraft.recipe.FancySignRecipe;
import art.arcane.bibliocraft.recipe.FramedChestRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, BiblioCraft.MODID);

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, BiblioCraft.MODID);

    public static final RegistryObject<RecipeSerializer<EnchantedAtlasRecipe>> ENCHANTED_ATLAS_SERIALIZER =
            RECIPE_SERIALIZERS.register("enchantedatlas", () -> new SimpleCraftingRecipeSerializer<>(EnchantedAtlasRecipe::new));
    public static final RegistryObject<RecipeSerializer<ColoredBlockRecolorRecipe>> SWORD_PEDESTAL_RECOLOR =
            RECIPE_SERIALIZERS.register("sword_pedestal_recolor",
                    () -> new ColoredBlockRecolorRecipe.Serializer(() -> ModBlocks.SWORD_PEDESTAL.get().asItem()));
    public static final RegistryObject<RecipeSerializer<ColoredBlockRecolorRecipe>> COLORED_BLOCK_RECOLOR =
            RECIPE_SERIALIZERS.register("colored_block_recolor", ColoredBlockRecolorRecipe.Serializer::new);
    public static final RegistryObject<RecipeSerializer<FancySignRecipe>> FANCY_SIGN =
            RECIPE_SERIALIZERS.register("fancy_sign", FancySignRecipe.Serializer::new);
    public static final RegistryObject<RecipeSerializer<FramedChestRecipe>> FRAMED_CHEST =
            RECIPE_SERIALIZERS.register("framed_chest", FramedChestRecipe.Serializer::new);

    public static final RegistryObject<RecipeType<EnchantedAtlasRecipe>> ENCHANTED_ATLAS_RECIPE_TYPE =
            RECIPE_TYPES.register("enchantedatlas", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return BiblioCraft.MODID + ":enchantedatlas";
                }
            });

    private ModRecipes() {}
}

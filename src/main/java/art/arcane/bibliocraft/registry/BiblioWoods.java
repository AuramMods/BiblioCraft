package art.arcane.bibliocraft.registry;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.FurnitureMaterial;
import art.arcane.bibliocraft.config.BiblioFeatureToggles;
import art.arcane.bibliocraft.item.FurnitureBlockItem;
import art.arcane.bibliocraft.item.SeatBackItem;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public final class BiblioWoods {
    private BiblioWoods() {}

    public static void registerTabs(DeferredRegister<CreativeModeTab> tabs) {
        var resource = Objects.requireNonNull(BiblioWoods.class.getResourceAsStream("/bibliocraft/bibliowoods.json"));
        try (var reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
            JsonParser.parseReader(reader).getAsJsonObject().asMap().forEach((namespace, entries) -> {
                List<ResourceLocation> planks = entries.getAsJsonArray().asList().stream()
                        .map(JsonElement::getAsString)
                        .map(wood -> ResourceLocation.fromNamespaceAndPath(namespace, wood + "_planks"))
                        .toList();
                String tab = "bibliowoods_" + namespace;
                tabs.register(tab, () -> CreativeModeTab.builder()
                        .title(Component.translatable("itemGroup." + BiblioCraft.MODID + '.' + tab))
                        .icon(() -> materials(planks).findFirst().orElse(FurnitureMaterial.OAK)
                                .applyTo(new ItemStack(ModBlocks.BOOKCASE.get())))
                        .displayItems((params, output) -> addVariants(output, planks))
                        .build());
            });
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read the BiblioWoods material list", exception);
        }
    }

    private static void addVariants(CreativeModeTab.Output output, List<ResourceLocation> planks) {
        List<Item> items = Stream.concat(
                        ModBlocks.getTabItems().stream().filter(item -> BiblioFeatureToggles.isBlockItemEnabled(item.getId())),
                        ModItems.getTabItems().stream().filter(item -> BiblioFeatureToggles.isItemEnabled(item.getId())))
                .map(RegistryObject::get)
                .filter(item -> item instanceof FurnitureBlockItem || item instanceof SeatBackItem)
                .toList();
        materials(planks).forEach(material -> items.forEach(item -> output.accept(material.applyTo(new ItemStack(item)))));
    }

    private static Stream<FurnitureMaterial> materials(List<ResourceLocation> planks) {
        return planks.stream().filter(BuiltInRegistries.BLOCK::containsKey)
                .map(plank -> new FurnitureMaterial(FurnitureMaterial.Wood.FRAMED, plank));
    }
}

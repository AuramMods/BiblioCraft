package art.arcane.bibliocraft;

import art.arcane.bibliocraft.network.ModNetwork;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import art.arcane.bibliocraft.registry.ModEnchantments;
import art.arcane.bibliocraft.registry.ModEntities;
import art.arcane.bibliocraft.registry.ModItems;
import art.arcane.bibliocraft.registry.ModMenus;
import art.arcane.bibliocraft.registry.ModRecipes;
import art.arcane.bibliocraft.registry.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(BiblioCraft.MODID)
public class BiblioCraft {
    public static final String MODID = "bibliocraft";
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<CreativeModeTab> TAB = CREATIVE_TABS.register(
            MODID + "_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.bibliocraft.bibliocraft_tab"))
                    .icon(() -> new ItemStack(ModBlocks.BOOKCASE.get()))
                    .displayItems((params, output) -> {
                        ModBlocks.getTabItems().forEach(item -> output.accept(item.get()));
                        ModItems.getTabItems().forEach(item -> output.accept(item.get()));
                    })
                    .build()
    );

    public BiblioCraft(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        ModNetwork.register();
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModEnchantments.ENCHANTMENTS.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        ModRecipes.RECIPE_TYPES.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}

package art.arcane.bibliocraft;

import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModItems;
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
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}

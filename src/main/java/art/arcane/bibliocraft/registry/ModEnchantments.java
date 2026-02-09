package art.arcane.bibliocraft.registry;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.enchantment.PlaceholderEnchantment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, BiblioCraft.MODID);

    public static final RegistryObject<Enchantment> DEATH_COMPASS =
            register("deathcompassench");
    public static final RegistryObject<Enchantment> READING =
            register("readingench");

    private ModEnchantments() {}

    private static RegistryObject<Enchantment> register(String name) {
        return ENCHANTMENTS.register(
                name,
                () -> new PlaceholderEnchantment(
                        Enchantment.Rarity.UNCOMMON,
                        EnchantmentCategory.BREAKABLE,
                        EquipmentSlot.MAINHAND
                )
        );
    }
}

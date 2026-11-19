package art.arcane.bibliocraft.registry;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.enchantment.DeathCompassEnchantment;
import art.arcane.bibliocraft.enchantment.ReadingEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, BiblioCraft.MODID);

    public static final RegistryObject<Enchantment> DEATH_COMPASS =
            ENCHANTMENTS.register("deathcompassench", DeathCompassEnchantment::new);
    public static final RegistryObject<Enchantment> READING =
            ENCHANTMENTS.register("readingench", ReadingEnchantment::new);

    private ModEnchantments() {}
}

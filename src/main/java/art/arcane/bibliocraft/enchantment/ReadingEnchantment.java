package art.arcane.bibliocraft.enchantment;

import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.item.ReadingGlassesItem;
import art.arcane.bibliocraft.registry.ModEnchantments;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public final class ReadingEnchantment extends Enchantment {
    public ReadingEnchantment() {
        super(Rarity.UNCOMMON, EnchantmentCategory.ARMOR_HEAD, new EquipmentSlot[] {EquipmentSlot.HEAD});
    }

    @Override
    public int getMinCost(int level) {
        return 10;
    }

    @Override
    public int getMaxCost(int level) {
        return super.getMinCost(level) + 50;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return enabled() && category.canEnchant(stack.getItem());
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return enabled() && stack.is(Items.BOOK);
    }

    @Override
    public boolean isDiscoverable() {
        return enabled();
    }

    @Override
    public boolean isTradeable() {
        return enabled();
    }

    public static boolean canRead(ItemStack head) {
        return enabled() && (head.getItem() instanceof ReadingGlassesItem
                || head.getEnchantmentLevel(ModEnchantments.READING.get()) > 0);
    }

    private static boolean enabled() {
        return BiblioConfig.COMMON.enableReadingglasses.get();
    }
}

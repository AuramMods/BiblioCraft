package art.arcane.bibliocraft.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public final class PortableInventory {
    private PortableInventory() {}

    public static ItemStack read(ItemStack container) {
        if (container.getTag() != null) {
            ListTag inventory = container.getTag().getList("Inventory", Tag.TAG_COMPOUND);
            for (int index = 0; index < inventory.size(); index++) {
                CompoundTag item = inventory.getCompound(index);
                if (item.contains("Slot", Tag.TAG_ANY_NUMERIC) && item.getInt("Slot") == 0) {
                    return ItemStack.of(item);
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public static void write(ItemStack container, ItemStack stack) {
        ListTag inventory = new ListTag();
        if (!stack.isEmpty()) {
            CompoundTag saved = stack.save(new CompoundTag());
            saved.putByte("Slot", (byte) 0);
            inventory.add(saved);
        }
        container.getOrCreateTag().put("Inventory", inventory);
    }
}

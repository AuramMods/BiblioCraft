package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.blockentity.ArmorStandBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class ArmorStandMenu extends FurnitureInventoryMenu {
    private static final int STAND_END = ArmorStandBlockEntity.SLOT_COUNT;
    private static final int WORN_START = STAND_END + 36;

    public ArmorStandMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(STAND_END));
    }

    public ArmorStandMenu(int id, Inventory inventory, Container stand) {
        super(ModMenus.ARMOR_STAND.get(), id, stand, STAND_END);
        for (int index = 0; index < STAND_END; index++) {
            int slot = index;
            addSlot(new Slot(stand, slot, 80, 8 + slot * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return ArmorStandBlockEntity.accepts(slot, stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        addPlayerSlots(inventory);
        for (int index = 0; index < STAND_END; index++) {
            addWornSlot(inventory, index);
        }
    }

    private void addWornSlot(Inventory inventory, int index) {
        EquipmentSlot equipment = ArmorStandBlockEntity.equipmentSlot(index);
        addSlot(new Slot(inventory, 39 - index, 126, 8 + index * 18) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.canEquip(equipment, inventory.player);
            }

            @Override
            public boolean mayPickup(Player player) {
                return player.isCreative() || !EnchantmentHelper.hasBindingCurse(getItem());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void setByPlayer(ItemStack stack) {
                inventory.player.onEquipItem(equipment, getItem(), stack);
                super.setByPlayer(stack);
            }
        });
    }

    @Override
    public boolean stillValid(Player player) {
        return BiblioConfig.COMMON.enableArmorstand.get() && super.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player) || !stillValid(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved;
        if (index < STAND_END) {
            moved = moveItemStackTo(stack, WORN_START + index, WORN_START + index + 1, false)
                    || moveItemStackTo(stack, STAND_END, WORN_START, true);
        } else if (index >= WORN_START) {
            moved = moveItemStackTo(stack, 0, STAND_END, false)
                    || moveItemStackTo(stack, STAND_END, WORN_START, true);
        } else {
            moved = moveItemStackTo(stack, WORN_START, slots.size(), false)
                    || moveItemStackTo(stack, 0, STAND_END, false);
        }
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return original;
    }
}

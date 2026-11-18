package art.arcane.bibliocraft.blockentity;

import art.arcane.bibliocraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class ArmorStandBlockEntity extends FurnitureInventoryBlockEntity {
    public static final int SLOT_COUNT = 4;
    private static final EquipmentSlot[] EQUIPMENT = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public ArmorStandBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARMOR_STAND.get(), pos, state, SLOT_COUNT);
    }

    public static EquipmentSlot equipmentSlot(int slot) {
        return EQUIPMENT[slot];
    }

    public static boolean accepts(int slot, ItemStack stack) {
        return !stack.isEmpty() && LivingEntity.getEquipmentSlotForItem(stack) == equipmentSlot(slot);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return accepts(slot, stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    public void swapWornArmor(Player player, int slot) {
        EquipmentSlot equipment = equipmentSlot(slot);
        ItemStack worn = player.getItemBySlot(equipment);
        ItemStack displayed = getItem(slot);
        if ((worn.isEmpty() && displayed.isEmpty())
                || (!worn.isEmpty() && (!accepts(slot, worn) || (!player.isCreative() && EnchantmentHelper.hasBindingCurse(worn))))
                || (!displayed.isEmpty() && !displayed.canEquip(equipment, player))) {
            return;
        }
        player.setItemSlot(equipment, displayed);
        setItem(slot, worn);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && level.getBlockState(worldPosition.above()).isAir()) {
            level.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).expandTowards(0, 1, 0).inflate(0.25);
    }
}

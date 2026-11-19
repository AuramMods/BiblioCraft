package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.block.FramedChestBlock;
import art.arcane.bibliocraft.blockentity.FramedChestBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

public final class FramedChestMenu extends FurnitureInventoryMenu {
    private final Container storage;
    @Nullable
    private final FramedChestBlockEntity left;
    @Nullable
    private final FramedChestBlockEntity right;
    private final int rows;

    public FramedChestMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, new SimpleContainer(FramedChestBlockEntity.SLOTS * (buffer.readBoolean() ? 2 : 1)), null, null);
    }

    public FramedChestMenu(int id, Inventory inventory, FramedChestBlockEntity left, @Nullable FramedChestBlockEntity right) {
        this(id, inventory, right == null ? left : new CompoundContainer(left, right), left, right);
    }

    private FramedChestMenu(int id, Inventory inventory, Container storage, @Nullable FramedChestBlockEntity left,
                            @Nullable FramedChestBlockEntity right) {
        super(ModMenus.FRAMED_CHEST.get(), id, storage, storage.getContainerSize());
        this.storage = storage;
        this.left = left;
        this.right = right;
        rows = storage.getContainerSize() / 9;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(storage, row * 9 + column, 8 + column * 18, 18 + row * 18));
            }
        }
        addPlayerSlots(inventory, 8, 31 + rows * 18);
        storage.startOpen(inventory.player);
    }

    public int rows() {
        return rows;
    }

    public boolean containsChest(FramedChestBlockEntity chest) {
        return chest == left || chest == right;
    }

    @Override
    public boolean stillValid(Player player) {
        return left == null || (BiblioConfig.COMMON.enableFramedChest.get() && super.stillValid(player)
                && FramedChestBlock.partner(player.level(), left.getBlockPos(), left.getBlockState()) == right);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        storage.stopOpen(player);
    }
}

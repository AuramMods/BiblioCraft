package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.block.ClockBlock;
import art.arcane.bibliocraft.blockentity.ClockBlockEntity;
import art.arcane.bibliocraft.blockentity.ClockSettings;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ClockMenu extends AbstractContainerMenu {
    @Nullable
    private final ClockBlockEntity clicked;
    @Nullable
    private final ClockBlockEntity clock;
    private final ClockSettings opened;
    private boolean saved;

    public ClockMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, null, null, ClockSettings.read(buffer));
    }

    public ClockMenu(int id, ClockBlockEntity clicked, ClockBlockEntity clock) {
        this(id, clicked, clock, clock.getSettings());
    }

    private ClockMenu(int id, @Nullable ClockBlockEntity clicked, @Nullable ClockBlockEntity clock, ClockSettings opened) {
        super(ModMenus.CLOCK.get(), id);
        this.clicked = clicked;
        this.clock = clock;
        this.opened = opened;
    }

    public ClockSettings openedSettings() {
        return opened;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return clock == null || (BiblioConfig.COMMON.enableClock.get() && clicked.canAccess(player)
                && clock.canAccess(player) && Container.stillValidBlockEntity(clicked, player)
                && ClockBlock.mainClock(player.level(), clicked.getBlockPos()) == clock);
    }

    public boolean save(Player player, ClockSettings edit) {
        if (clock == null || saved || player.containerMenu != this || !stillValid(player)) {
            return false;
        }
        saved = true;
        clock.setSettings(clock.getSettings().mergeEdits(opened, edit));
        clock.wind();
        return true;
    }
}

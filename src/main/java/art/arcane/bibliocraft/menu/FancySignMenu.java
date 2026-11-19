package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.blockentity.FancySignBlockEntity;
import art.arcane.bibliocraft.blockentity.FancySignLayout;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

public final class FancySignMenu extends FurnitureInventoryMenu {
    @Nullable
    private final FancySignBlockEntity sign;
    private final FancySignLayout openedLayout;

    public FancySignMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, new SimpleContainer(2), null, FancySignLayout.read(data));
    }

    public FancySignMenu(int id, Inventory inventory, FancySignBlockEntity sign) {
        this(id, inventory, sign, sign, sign.getLayout());
    }

    private FancySignMenu(int id, Inventory inventory, Container contents, @Nullable FancySignBlockEntity sign,
                          FancySignLayout openedLayout) {
        super(ModMenus.FANCY_SIGN.get(), id, contents, 2);
        this.sign = sign;
        this.openedLayout = openedLayout;
        for (int slot = 0; slot < 2; slot++) {
            addSlot(new Slot(contents, slot, 194 + slot * 28, 159) {
                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        addPlayerSlots(inventory, 20, 164);
    }

    public FancySignLayout openedLayout() {
        return openedLayout;
    }

    public boolean save(Player player, FancySignLayout edit) {
        if (sign == null || player.containerMenu != this || !stillValid(player)) {
            return false;
        }
        sign.setLayout(sign.getLayout().mergeEdits(openedLayout, edit));
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return BiblioConfig.COMMON.enableFancySign.get() && super.stillValid(player);
    }
}

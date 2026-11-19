package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.blockentity.PaintingFrameBlockEntity;
import art.arcane.bibliocraft.blockentity.PaintingPressBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.painting.PaintingArt;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public final class PaintingPressMenu extends FurnitureInventoryMenu {
    private final @Nullable PaintingPressBlockEntity press;
    private final PaintingArt initial;

    public PaintingPressMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, new SimpleContainer(1),
                inventory.player.level().getBlockEntity(data.readBlockPos()) instanceof PaintingPressBlockEntity found
                        ? found : null, PaintingArt.read(data));
    }

    public PaintingPressMenu(int id, Inventory inventory, PaintingPressBlockEntity press) {
        this(id, inventory, press, press, press.selection());
    }

    private PaintingPressMenu(int id, Inventory inventory, Container contents,
                              @Nullable PaintingPressBlockEntity press, PaintingArt initial) {
        super(ModMenus.PAINTING_PRESS.get(), id, contents, 1);
        this.press = press;
        this.initial = initial;
        addSlot(new Slot(contents, 0, 120, 139) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return PaintingFrameBlockEntity.accepts(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addPlayerSlots(inventory, 48, 159);
    }

    public static void open(ServerPlayer player, PaintingPressBlockEntity press, Component title) {
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (id, inventory, visitor) -> new PaintingPressMenu(id, inventory, press), title), data -> {
            data.writeBlockPos(press.getBlockPos());
            press.selection().write(data);
        });
    }

    public PaintingArt selection() {
        return press == null ? initial : press.selection();
    }

    public boolean transfer(Player player, PaintingArt art, boolean start) {
        if (press == null || player.containerMenu != this || !stillValid(player)) {
            return false;
        }
        press.select(art);
        return !start || press.startTransfer();
    }

    @Override
    public boolean stillValid(Player player) {
        return BiblioConfig.COMMON.enablePainting.get() && super.stillValid(player);
    }
}

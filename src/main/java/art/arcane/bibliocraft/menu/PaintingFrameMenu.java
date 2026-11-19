package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.blockentity.PaintingFrameBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.painting.PaintingSettings;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

public final class PaintingFrameMenu extends FurnitureInventoryMenu {
    private static final int DATA_COUNT = 8;
    private final ContainerData data;
    private final boolean borderless;

    public PaintingFrameMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, new SimpleContainer(1), data.readBoolean());
    }

    public PaintingFrameMenu(int id, Inventory inventory, PaintingFrameBlockEntity frame) {
        this(id, inventory, frame, frame.borderless());
    }

    private PaintingFrameMenu(int id, Inventory inventory, Container contents, boolean borderless) {
        super(ModMenus.PAINTING.get(), id, contents, 1);
        this.borderless = borderless;
        data = contents instanceof PaintingFrameBlockEntity frame ? new SimpleContainerData(DATA_COUNT) {
            @Override
            public int get(int index) {
                var settings = frame.settings();
                return switch (index) {
                    case 0 -> settings.corner();
                    case 1 -> settings.scale();
                    case 2 -> settings.resolution();
                    case 3 -> settings.aspect();
                    case 4 -> settings.rotation();
                    case 5 -> settings.customWidth();
                    case 6 -> settings.customHeight();
                    default -> settings.hidden() ? 1 : 0;
                };
            }
        } : new SimpleContainerData(DATA_COUNT);
        addDataSlots(data);
        addSlot(new Slot(contents, 0, 80, 89) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return PaintingFrameBlockEntity.accepts(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addPlayerSlots(inventory, 8, 110);
    }

    public static void open(ServerPlayer player, PaintingFrameBlockEntity frame, Component title) {
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (id, inventory, visitor) -> new PaintingFrameMenu(id, inventory, frame), title),
                data -> data.writeBoolean(frame.borderless()));
    }

    public boolean borderless() {
        return borderless;
    }

    public PaintingSettings settings() {
        return new PaintingSettings(data.get(0), data.get(1), data.get(2), data.get(3), data.get(4),
                data.get(5), data.get(6), data.get(7) != 0);
    }

    public boolean edit(Player player, PaintingSettings previous, PaintingSettings desired) {
        if (!(getSlot(0).container instanceof PaintingFrameBlockEntity frame)
                || player.containerMenu != this || !stillValid(player)) {
            return false;
        }
        PaintingSettings current = frame.settings();
        frame.edit(new PaintingSettings(
                previous.corner() == desired.corner() ? current.corner() : desired.corner(),
                previous.scale() == desired.scale() ? current.scale() : desired.scale(),
                previous.resolution() == desired.resolution() ? current.resolution() : desired.resolution(),
                previous.aspect() == desired.aspect() ? current.aspect() : desired.aspect(),
                previous.rotation() == desired.rotation() ? current.rotation() : desired.rotation(),
                previous.customWidth() == desired.customWidth() ? current.customWidth() : desired.customWidth(),
                previous.customHeight() == desired.customHeight() ? current.customHeight() : desired.customHeight(),
                previous.hidden() == desired.hidden() ? current.hidden() : desired.hidden()));
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return BiblioConfig.COMMON.enablePainting.get() && super.stillValid(player);
    }
}

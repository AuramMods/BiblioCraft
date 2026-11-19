package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.blockentity.MapFrameBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.map.MapPin;
import art.arcane.bibliocraft.registry.ModItems;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.SharedConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class MapFrameMenu extends AbstractContainerMenu {
    private final @Nullable MapFrameBlockEntity frame;
    private final InteractionHand hand;
    private final ItemStack tool;
    private final ItemStack map;
    private final View view;

    public MapFrameMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, null, InteractionHand.MAIN_HAND, ItemStack.EMPTY, View.read(buffer));
    }

    public MapFrameMenu(int id, @Nullable MapFrameBlockEntity frame, InteractionHand hand, ItemStack tool, View view) {
        super(ModMenus.MAP_FRAME.get(), id);
        this.frame = frame;
        this.hand = hand;
        this.tool = tool;
        this.map = frame == null ? ItemStack.EMPTY : frame.getItem(MapFrameBlockEntity.MAP);
        this.view = view;
    }

    public static void open(ServerPlayer player, MapFrameBlockEntity frame, InteractionHand hand, MapPin.Point point,
                            MapItemSavedData data) {
        MapPin existing = frame.pinAt(point);
        MapPin selected = existing == null ? new MapPin(UUID.randomUUID(), point, "", DyeColor.RED) : existing;
        var target = selected.target(data);
        var view = new View(selected, existing != null, frame.pins().size() + 1, target.x(), target.z());
        var tool = player.getItemInHand(hand);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (id, inventory, owner) -> new MapFrameMenu(id, frame, hand, tool, view),
                Component.translatable("gui.bibliocraft.map_pin.title")), view::write);
    }

    @Override
    public boolean stillValid(Player player) {
        return frame == null || BiblioConfig.COMMON.enableMapFrame.get() && frame.stillValid(player)
                && player.getItemInHand(hand) == tool && tool.is(ModItems.MAP_TOOL.get())
                && !map.isEmpty() && frame.getItem(MapFrameBlockEntity.MAP) == map;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    public View view() {
        return view;
    }

    public boolean save(Player player, String name, DyeColor color, boolean remove) {
        if (frame == null || player.containerMenu != this || !stillValid(player)) {
            return false;
        }
        MapPin original = view.pin();
        String title = SharedConstants.filterText(name).strip();
        var next = new MapPin(original.id(), original.point(), title.isEmpty() ? "Waypoint " + view.number() : title,
                color);
        if (view.editing()) {
            return frame.changePin(original, remove ? null : next);
        }
        return !remove && frame.addPin(next);
    }

    public record View(MapPin pin, boolean editing, int number, int x, int z) {
        private void write(FriendlyByteBuf buffer) {
            pin.write(buffer);
            buffer.writeBoolean(editing);
            buffer.writeVarInt(number).writeInt(x).writeInt(z);
        }

        private static View read(FriendlyByteBuf buffer) {
            return new View(MapPin.read(buffer), buffer.readBoolean(), buffer.readVarInt(), buffer.readInt(),
                    buffer.readInt());
        }
    }
}

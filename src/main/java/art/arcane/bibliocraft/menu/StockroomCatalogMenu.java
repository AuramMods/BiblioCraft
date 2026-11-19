package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.catalog.CatalogContents;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.item.StockroomCatalogItem;
import art.arcane.bibliocraft.item.WaypointCompassItem;
import art.arcane.bibliocraft.network.ModNetwork;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.SharedConstants;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class StockroomCatalogMenu extends HeldItemMenu {
    public static final int COMPASS_LIMIT = 8;

    private CatalogContents.Snapshot snapshot = new CatalogContents.Snapshot(List.of(), 0);
    private final List<CatalogContents.Source> incoming = new ArrayList<>();
    private List<CatalogContents.Entry> entries = List.of();
    private int sourceCount;
    private int unavailable;
    private int revision;
    private long lastRefresh = Long.MIN_VALUE;

    public StockroomCatalogMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory.player, buffer.readEnum(InteractionHand.class));
    }

    public StockroomCatalogMenu(int id, Player owner, InteractionHand hand) {
        super(ModMenus.STOCKROOM_CATALOG.get(), id, owner, hand);
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(player) && heldStack.getItem() instanceof StockroomCatalogItem
                && BiblioConfig.COMMON.enableStockroomCatalog.get();
    }

    public boolean rename(Player player, String name) {
        if (!stillValid(player)) {
            return false;
        }
        String title = CatalogContents.bounded(SharedConstants.filterText(name).strip(), CatalogContents.TITLE_LENGTH);
        if (title.isEmpty()) {
            heldStack.resetHoverName();
        } else {
            heldStack.setHoverName(Component.literal(title));
        }
        player.getInventory().setChanged();
        return true;
    }

    public static List<Integer> compassSlots(Inventory inventory) {
        if (!BiblioConfig.COMMON.enableWaypointCompass.get()) {
            return List.of();
        }
        List<Integer> result = new ArrayList<>(COMPASS_LIMIT);
        for (int slot = 0; slot < inventory.getContainerSize() && result.size() < COMPASS_LIMIT; slot++) {
            if (inventory.getItem(slot).getItem() instanceof WaypointCompassItem) {
                result.add(slot);
            }
        }
        return result;
    }

    public boolean assign(ServerPlayer player, int slot, GlobalPos location, String key) {
        if (!stillValid(player) || !compassSlots(player.getInventory()).contains(slot)) {
            return false;
        }
        var source = snapshot.sources().stream().filter(entry -> entry.location().equals(location)
                && entry.counts().containsKey(key)).findFirst().orElse(null);
        if (source == null || !CatalogContents.canAssign(player.serverLevel(), player, location)) {
            return false;
        }
        String name = Component.translatable("gui.bibliocraft.catalog.item_in_container",
                Component.translatable(key), source.name().component()).getString();
        ItemStack compass = player.getInventory().getItem(slot);
        WaypointCompassItem.setTarget(compass, WaypointCompassItem.at(location, name));
        player.getInventory().setChanged();
        player.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, slot, compass));
        return true;
    }

    public void refresh(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (!stillValid(player) || now < lastRefresh + 10) {
            return;
        }
        lastRefresh = now;
        snapshot = CatalogContents.scan(player.serverLevel(), player, heldStack);
        var target = PacketDistributor.PLAYER.with(() -> player);
        ModNetwork.CHANNEL.send(target, new ModNetwork.StockLogPacket(containerId, snapshot.sources().size(),
                snapshot.unavailable(), false, null));
        for (var source : snapshot.sources()) {
            for (var batch : source.batches()) {
                ModNetwork.CHANNEL.send(target, new ModNetwork.StockLogPacket(containerId, 0, 0, false, batch));
            }
        }
        ModNetwork.CHANNEL.send(target, new ModNetwork.StockLogPacket(containerId, 0, 0, true, null));
    }

    public void accept(ModNetwork.StockLogPacket update) {
        if (update.finished()) {
            entries = new CatalogContents.Snapshot(incoming, unavailable).entries();
            incoming.clear();
            revision++;
        } else if (update.source() == null) {
            sourceCount = update.sourceCount();
            unavailable = update.unavailable();
            incoming.clear();
        } else {
            incoming.add(update.source());
        }
    }

    public List<CatalogContents.Entry> entries() {
        return entries;
    }

    public int revision() {
        return revision;
    }

    public int sourceCount() {
        return sourceCount;
    }

    public int unavailable() {
        return unavailable;
    }
}

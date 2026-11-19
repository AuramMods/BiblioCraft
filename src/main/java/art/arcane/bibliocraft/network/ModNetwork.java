package art.arcane.bibliocraft.network;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.block.FurnitureReading;
import art.arcane.bibliocraft.catalog.CatalogContents;
import art.arcane.bibliocraft.book.BigBookContent;
import art.arcane.bibliocraft.book.ClipboardContent;
import art.arcane.bibliocraft.book.SlottedBookContents;
import art.arcane.bibliocraft.client.BigBookScreen;
import art.arcane.bibliocraft.client.ClipboardScreen;
import art.arcane.bibliocraft.client.ReadingOverlay;
import art.arcane.bibliocraft.client.StockroomCatalogScreen;
import art.arcane.bibliocraft.menu.BigBookMenu;
import art.arcane.bibliocraft.menu.ClipboardMenu;
import art.arcane.bibliocraft.menu.SlottedBookMenu;
import art.arcane.bibliocraft.menu.RedstoneBookMenu;
import art.arcane.bibliocraft.client.RecipeBookScreen;
import art.arcane.bibliocraft.client.TypesettingTableScreen;
import art.arcane.bibliocraft.menu.TypesettingTableMenu;
import art.arcane.bibliocraft.menu.PaintingFrameMenu;
import art.arcane.bibliocraft.menu.PaintingPressMenu;
import art.arcane.bibliocraft.menu.MapFrameMenu;
import art.arcane.bibliocraft.menu.AtlasMenu;
import art.arcane.bibliocraft.map.MapPin;
import net.minecraft.world.item.DyeColor;
import art.arcane.bibliocraft.painting.PaintingArt;
import art.arcane.bibliocraft.painting.PaintingSettings;
import art.arcane.bibliocraft.crafting.RecordedRecipe;
import art.arcane.bibliocraft.menu.RecipeBookMenu;
import art.arcane.bibliocraft.item.WaypointCompassItem;
import art.arcane.bibliocraft.menu.StockroomCatalogMenu;
import art.arcane.bibliocraft.menu.WaypointCompassMenu;
import art.arcane.bibliocraft.blockentity.ClockSettings;
import art.arcane.bibliocraft.blockentity.FancySignLayout;
import art.arcane.bibliocraft.menu.DeskBookMenu;
import art.arcane.bibliocraft.menu.ClockMenu;
import art.arcane.bibliocraft.menu.FancySignMenu;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Supplier;

public final class ModNetwork {
    private static final String PROTOCOL = "15";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private static int packetId = 0;
    private static boolean initialized = false;
    private static final WeakHashMap<ServerPlayer, Long> LAST_READING_REQUEST = new WeakHashMap<>();

    private ModNetwork() {}

    public static void register() {
        if (initialized) {
            return;
        }
        initialized = true;

        CHANNEL.messageBuilder(AtlasPinPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AtlasPinPacket::encode).decoder(AtlasPinPacket::decode)
                .consumerMainThread((message, context) -> {
                    var player = context.get().getSender();
                    if (player != null && player.containerMenu instanceof AtlasMenu menu && menu.containerId == message.menuId()) {
                        menu.edit(player, message.action(), message.slot(), message.mapId(), message.pin(), message.point(),
                                message.name(), message.color(), message.compass());
                    }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(TypesettingActionPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TypesettingActionPacket::encode).decoder(TypesettingActionPacket::decode)
                .consumerMainThread((message, context) -> {
                    var player = context.get().getSender();
                    if (player != null && player.containerMenu instanceof TypesettingTableMenu menu
                            && menu.containerId == message.menuId()) {
                        menu.apply(player, message.action(), message.book());
                    }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(McbEditPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(McbEditPacket::encode).decoder(McbEditPacket::decode)
                .consumerMainThread((message, context) -> {
                    var player = context.get().getSender();
                    if (player != null && player.containerMenu instanceof DeskBookMenu menu && menu.containerId == message.menuId()) {
                        menu.save(player, message.pages(), message.title(), message.page());
                    }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(McbPagePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(McbPagePacket::encode).decoder(McbPagePacket::decode)
                .consumerMainThread((message, context) -> {
                    var player = context.get().getSender();
                    if (player != null && player.containerMenu instanceof DeskBookMenu menu && menu.containerId == message.menuId()) {
                        menu.turnPage(player, message.page());
                    }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(BigBookVisitPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(BigBookVisitPacket::encode).decoder(BigBookVisitPacket::decode)
                .consumerMainThread((message, context) -> {
                    var player = context.get().getSender();
                    if (player != null && player.containerMenu instanceof BigBookMenu menu && menu.containerId == message.menuId()) {
                        menu.visit(player, message.index()).ifPresent(page -> CHANNEL.send(
                                PacketDistributor.PLAYER.with(() -> player), new BigBookPagePacket(menu.containerId, message.index(), page)));
                    }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(BigBookEditPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(BigBookEditPacket::encode).decoder(BigBookEditPacket::decode)
                .consumerMainThread((message, context) -> {
                    var player = context.get().getSender();
                    if (player != null && player.containerMenu instanceof BigBookMenu menu && menu.containerId == message.menuId()) {
                        menu.stage(player, message.index(), message.page());
                    }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(BigBookFinishPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(BigBookFinishPacket::encode).decoder(BigBookFinishPacket::decode)
                .consumerMainThread((message, context) -> {
                    var player = context.get().getSender();
                    if (player != null && player.containerMenu instanceof BigBookMenu menu && menu.containerId == message.menuId()) {
                        menu.finish(player, message.index(), message.chapters(), message.title());
                    }
                    context.get().setPacketHandled(true);
                }).add();
        registerServerbound(PanelerPacket.class, PanelerPacket::new);
        CHANNEL.messageBuilder(StockTitlePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(StockTitlePacket::encode).decoder(StockTitlePacket::decode)
                .consumerMainThread((message, context) -> {
                    var player = context.get().getSender();
                    if (player != null && player.containerMenu instanceof StockroomCatalogMenu menu
                            && menu.containerId == message.menuId()) {
                        menu.rename(player, message.title());
                    }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(StockCompassPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(StockCompassPacket::encode).decoder(StockCompassPacket::decode)
                .consumerMainThread((message, context) -> {
                    var player = context.get().getSender();
                    if (player != null && player.containerMenu instanceof StockroomCatalogMenu menu
                            && menu.containerId == message.menuId()
                            && !menu.assign(player, message.slot(), message.location(), message.item())) {
                        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                                "message.bibliocraft.catalog.assignment_unavailable"), false);
                    }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(StockRefreshPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, buffer) -> buffer.writeVarInt(message.menuId()))
                .decoder(buffer -> new StockRefreshPacket(buffer.readVarInt()))
                .consumerMainThread((message, context) -> {
                    var player = context.get().getSender();
                    if (player != null && player.containerMenu instanceof StockroomCatalogMenu menu
                            && menu.containerId == message.menuId()) {
                        menu.refresh(player);
                    }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(WaypointEditPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(WaypointEditPacket::encode).decoder(WaypointEditPacket::decode)
                .consumerMainThread((message, context) -> {
                    var player = context.get().getSender();
                    if (player != null && player.containerMenu instanceof WaypointCompassMenu menu
                            && menu.containerId == message.menuId()) {
                        menu.save(player, message.x(), message.z(), message.name(), message.currentDimension());
                    }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(ClipboardVisit.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ClipboardVisit::encode).decoder(ClipboardVisit::decode)
                .consumerMainThread((message, context) -> {
                    var player = context.get().getSender();
                    if (player != null && player.containerMenu instanceof ClipboardMenu menu
                            && menu.containerId == message.menuId()) {
                        var view = menu.visit(player, message.page());
                        if (view != null) {
                            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                                    new ClipboardPage(menu.containerId, view));
                        }
                    }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(ClipboardEdit.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ClipboardEdit::encode).decoder(ClipboardEdit::decode)
                .consumerMainThread((message, context) -> {
                    var player = context.get().getSender();
                    if (player != null && player.containerMenu instanceof ClipboardMenu menu
                            && menu.containerId == message.menuId()) {
                        menu.edit(player, message.page(), message.content());
                    }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(SlottedBookEdit.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SlottedBookEdit::encode).decoder(SlottedBookEdit::decode)
                .consumerMainThread((message, context) -> {
                    var player = context.get().getSender();
                    if (player != null && player.containerMenu instanceof SlottedBookMenu menu
                            && menu.containerId == message.menuId()) {
                        menu.saveLabel(player, message.label());
                    }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(RedstoneBookRename.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RedstoneBookRename::encode).decoder(RedstoneBookRename::decode)
                .consumerMainThread((message, context) -> {
                    var player = context.get().getSender();
                    if (player != null && player.containerMenu instanceof RedstoneBookMenu menu
                            && menu.containerId == message.menuId()) {
                        menu.rename(player, message.title());
                    }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(MapPinPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(MapPinPacket::encode).decoder(MapPinPacket::decode)
                .consumerMainThread((message, context) -> {
                    var player = context.get().getSender();
                    if (player != null && player.containerMenu instanceof MapFrameMenu menu
                            && menu.containerId == message.menuId() && menu.stillValid(player)) {
                        if (menu.save(player, message.name(), message.color(), message.remove())) {
                            player.closeContainer();
                        } else {
                            player.displayClientMessage(Component.translatable("message.bibliocraft.map_pin.conflict"), false);
                        }
                    }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(RecipeBookEditPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RecipeBookEditPacket::encode).decoder(RecipeBookEditPacket::decode)
                .consumerMainThread((message, context) -> {
                    var player = context.get().getSender();
                    if (player != null && player.containerMenu instanceof RecipeBookMenu menu
                            && menu.containerId == message.menuId()) {
                        menu.annotate(player, message.lines(), message.sign());
                    }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(SignPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SignPacket::encode).decoder(SignPacket::decode)
                .consumerMainThread((message, context) -> {
                    var player = context.get().getSender();
                    if (player != null && player.containerMenu instanceof FancySignMenu menu && menu.containerId == message.menuId()) {
                        menu.save(player, message.layout());
                    }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(ClockPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ClockPacket::encode).decoder(ClockPacket::decode)
                .consumerMainThread((message, context) -> {
                    var player = context.get().getSender();
                    if (player != null && player.containerMenu instanceof ClockMenu menu && menu.containerId == message.menuId()) {
                        menu.save(player, message.settings());
                    }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(PaintPressPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PaintPressPacket::encode).decoder(PaintPressPacket::decode)
                .consumerMainThread((message, context) -> {
                    var player = context.get().getSender();
                    if (player != null && player.containerMenu instanceof PaintingPressMenu menu
                            && menu.containerId == message.menuId()) {
                        menu.transfer(player, message.art(), message.start());
                    }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(PaintingPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PaintingPacket::encode).decoder(PaintingPacket::decode)
                .consumerMainThread((message, context) -> {
                    var player = context.get().getSender();
                    if (player != null && player.containerMenu instanceof PaintingFrameMenu menu
                            && menu.containerId == message.menuId()) {
                        menu.edit(player, message.previous(), message.desired());
                    }
                    context.get().setPacketHandled(true);
                }).add();

        registerClientbound(DrillTextPacket.class, DrillTextPacket::new);
        registerClientbound(PanelerClientPacket.class, PanelerClientPacket::new);
        CHANNEL.messageBuilder(RecipeTextPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RecipeTextPacket::encode).decoder(RecipeTextPacket::decode)
                .consumerMainThread((message, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> RecipeBookScreen.receive(message));
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(StockLogPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(StockLogPacket::encode).decoder(StockLogPacket::decode)
                .consumerMainThread((message, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> StockroomCatalogScreen.receive(message));
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(BigBookPagePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BigBookPagePacket::encode).decoder(BigBookPagePacket::decode)
                .consumerMainThread((message, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BigBookScreen.receive(message));
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(TypesettingPagePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TypesettingPagePacket::encode).decoder(TypesettingPagePacket::decode)
                .consumerMainThread((message, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> TypesettingTableScreen.receive(message));
                    context.get().setPacketHandled(true);
                }).add();
        registerClientbound(OpenBookPacket.class, OpenBookPacket::new);
        CHANNEL.messageBuilder(ClipboardPage.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClipboardPage::encode).decoder(ClipboardPage::decode)
                .consumerMainThread((message, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClipboardScreen.accept(message));
                    context.get().setPacketHandled(true);
                }).add();
        registerClientbound(DeskOpenGuiPacket.class, DeskOpenGuiPacket::new);
        registerClientbound(SoundPlayerPacket.class, SoundPlayerPacket::new);
        CHANNEL.messageBuilder(ReadingRequest.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, buffer) -> buffer.writeVarInt(message.sequence()))
                .decoder(buffer -> new ReadingRequest(buffer.readVarInt()))
                .consumerMainThread((message, context) -> {
                    var player = context.get().getSender();
                    if (player != null) {
                        replyToReadingRequest(player, message);
                    }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(ReadingResponse.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ReadingResponse::encode).decoder(ReadingResponse::decode)
                .consumerMainThread((message, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ReadingOverlay.receive(message));
                    context.get().setPacketHandled(true);
                }).add();
    }

    private static void replyToReadingRequest(ServerPlayer player, ReadingRequest request) {
        long tick = player.server.overworld().getGameTime();
        Long last = LAST_READING_REQUEST.get(player);
        if (last != null && tick - last < 3) {
            return;
        }
        LAST_READING_REQUEST.put(player, tick);

        // The server resolves the aim and range without accepting a client-supplied inventory address.
        var hit = player.pick(player.getBlockReach(), 1, false);
        var target = hit instanceof BlockHitResult blockHit ? FurnitureReading.target(player.level(), blockHit) : null;
        var text = target == null ? List.<Component>of() : FurnitureReading.read(player, target);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new ReadingResponse(request.sequence(), text.isEmpty() ? Optional.empty() : Optional.of(target), text));
    }

    public record ReadingRequest(int sequence) {}

    public record ReadingResponse(int sequence, Optional<FurnitureReading.Target> target, List<Component> text) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(sequence);
            buffer.writeOptional(target, (buf, value) -> {
                buf.writeBlockPos(value.pos());
                buf.writeVarInt(value.slot());
            });
            buffer.writeCollection(text, FriendlyByteBuf::writeComponent);
        }

        private static ReadingResponse decode(FriendlyByteBuf buffer) {
            return new ReadingResponse(buffer.readVarInt(),
                    buffer.readOptional(buf -> new FurnitureReading.Target(buf.readBlockPos(), buf.readVarInt())),
                    buffer.readList(FriendlyByteBuf::readComponent));
        }
    }

    private static <T extends EmptyPacket> void registerServerbound(Class<T> type, Supplier<T> factory) {
        register(type, factory, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    private static <T extends EmptyPacket> void registerClientbound(Class<T> type, Supplier<T> factory) {
        register(type, factory, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    private static <T extends EmptyPacket> void register(
            Class<T> type,
            Supplier<T> factory,
            Optional<NetworkDirection> direction
    ) {
        CHANNEL.registerMessage(
                packetId++,
                type,
                EmptyPacket::encode,
                buf -> factory.get(),
                (message, contextSupplier) -> {
                    contextSupplier.get().setPacketHandled(true);
                },
                direction
        );
    }

    public abstract static class EmptyPacket {
        public static void encode(EmptyPacket message, FriendlyByteBuf buffer) {
            // Placeholder packet has no payload during breadth-first porting.
        }
    }

    public record AtlasPinPacket(int menuId, AtlasMenu.Edit action, int slot, int mapId,
                                 @org.jetbrains.annotations.Nullable UUID pin, MapPin.Point point, String name,
                                 DyeColor color, int compass) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(menuId).writeEnum(action).writeVarInt(slot).writeVarInt(mapId);
            buffer.writeNullable(pin, FriendlyByteBuf::writeUUID);
            buffer.writeDouble(point.u()).writeDouble(point.v());
            buffer.writeUtf(name, MapPin.NAME_LENGTH).writeEnum(color).writeVarInt(compass);
        }

        private static AtlasPinPacket decode(FriendlyByteBuf buffer) {
            return new AtlasPinPacket(buffer.readVarInt(), buffer.readEnum(AtlasMenu.Edit.class), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readNullable(FriendlyByteBuf::readUUID),
                    new MapPin.Point(buffer.readDouble(), buffer.readDouble()), buffer.readUtf(MapPin.NAME_LENGTH),
                    buffer.readEnum(DyeColor.class), buffer.readVarInt());
        }
    }
    public record TypesettingActionPacket(int menuId, TypesettingTableMenu.Action action, UUID book) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(menuId).writeEnum(action).writeNullable(book, FriendlyByteBuf::writeUUID);
        }

        private static TypesettingActionPacket decode(FriendlyByteBuf buffer) {
            return new TypesettingActionPacket(buffer.readVarInt(), buffer.readEnum(TypesettingTableMenu.Action.class),
                    buffer.readNullable(FriendlyByteBuf::readUUID));
        }
    }

    public record TypesettingPagePacket(int menuId, TypesettingTableMenu.Page page) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(menuId);
            page.write(buffer);
        }

        private static TypesettingPagePacket decode(FriendlyByteBuf buffer) {
            return new TypesettingPagePacket(buffer.readVarInt(), TypesettingTableMenu.Page.read(buffer));
        }
    }
    public record McbEditPacket(int menuId, int page, List<String> pages, Optional<String> title) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(menuId);
            buffer.writeVarInt(page);
            buffer.writeCollection(pages, (buf, text) -> buf.writeUtf(text, DeskBookMenu.MAX_PAGE_LENGTH));
            buffer.writeOptional(title, (buf, text) -> buf.writeUtf(text, DeskBookMenu.MAX_TITLE_LENGTH));
        }

        private static McbEditPacket decode(FriendlyByteBuf buffer) {
            int menuId = buffer.readVarInt();
            int page = buffer.readVarInt();
            List<String> pages = buffer.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, DeskBookMenu.MAX_PAGES),
                    buf -> buf.readUtf(DeskBookMenu.MAX_PAGE_LENGTH));
            return new McbEditPacket(menuId, page, pages, buffer.readOptional(buf -> buf.readUtf(DeskBookMenu.MAX_TITLE_LENGTH)));
        }
    }

    public record McbPagePacket(int menuId, int page) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(menuId);
            buffer.writeVarInt(page);
        }

        private static McbPagePacket decode(FriendlyByteBuf buffer) {
            return new McbPagePacket(buffer.readVarInt(), buffer.readVarInt());
        }
    }
    public record BigBookVisitPacket(int menuId, int index) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(menuId).writeByte(index);
        }

        private static BigBookVisitPacket decode(FriendlyByteBuf buffer) {
            return new BigBookVisitPacket(buffer.readVarInt(), buffer.readUnsignedByte());
        }
    }

    public record BigBookPagePacket(int menuId, int index, BigBookContent.Page page) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(menuId).writeByte(index);
            page.write(buffer);
        }

        private static BigBookPagePacket decode(FriendlyByteBuf buffer) {
            return new BigBookPagePacket(buffer.readVarInt(), buffer.readUnsignedByte(), BigBookContent.Page.read(buffer));
        }
    }

    public record BigBookEditPacket(int menuId, int index, BigBookContent.Page page) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(menuId).writeByte(index);
            page.write(buffer);
        }

        private static BigBookEditPacket decode(FriendlyByteBuf buffer) {
            return new BigBookEditPacket(buffer.readVarInt(), buffer.readUnsignedByte(), BigBookContent.Page.read(buffer));
        }
    }

    public record BigBookFinishPacket(int menuId, int index, List<BigBookContent.Chapter> chapters, Optional<String> title) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(menuId).writeByte(index);
            BigBookContent.Chapter.writeAll(buffer, chapters);
            buffer.writeOptional(title, (out, text) -> out.writeUtf(text, BigBookContent.TITLE_LENGTH));
        }

        private static BigBookFinishPacket decode(FriendlyByteBuf buffer) {
            return new BigBookFinishPacket(buffer.readVarInt(), buffer.readUnsignedByte(),
                    BigBookContent.Chapter.readAll(buffer), buffer.readOptional(in -> in.readUtf(BigBookContent.TITLE_LENGTH)));
        }
    }

    public static final class PanelerPacket extends EmptyPacket {}
    public record StockTitlePacket(int menuId, String title) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(menuId);
            buffer.writeUtf(title, CatalogContents.TITLE_LENGTH);
        }

        private static StockTitlePacket decode(FriendlyByteBuf buffer) {
            return new StockTitlePacket(buffer.readVarInt(), buffer.readUtf(CatalogContents.TITLE_LENGTH));
        }
    }

    public record StockCompassPacket(int menuId, int slot, GlobalPos location, String item) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(menuId);
            buffer.writeVarInt(slot);
            buffer.writeGlobalPos(location);
            buffer.writeUtf(item, CatalogContents.NAME_LENGTH);
        }

        private static StockCompassPacket decode(FriendlyByteBuf buffer) {
            return new StockCompassPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readGlobalPos(),
                    buffer.readUtf(CatalogContents.NAME_LENGTH));
        }
    }

    public record StockRefreshPacket(int menuId) {}

    public record WaypointEditPacket(int menuId, int x, int z, String name, boolean currentDimension) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(menuId);
            buffer.writeInt(x);
            buffer.writeInt(z);
            buffer.writeUtf(name, WaypointCompassItem.MAX_NAME_LENGTH);
            buffer.writeBoolean(currentDimension);
        }

        private static WaypointEditPacket decode(FriendlyByteBuf buffer) {
            return new WaypointEditPacket(buffer.readVarInt(), buffer.readInt(), buffer.readInt(),
                    buffer.readUtf(WaypointCompassItem.MAX_NAME_LENGTH), buffer.readBoolean());
        }
    }

    public record ClipboardVisit(int menuId, int page) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(menuId);
            buffer.writeVarInt(page);
        }

        private static ClipboardVisit decode(FriendlyByteBuf buffer) {
            return new ClipboardVisit(buffer.readVarInt(), buffer.readVarInt());
        }
    }

    public record ClipboardEdit(int menuId, int page, ClipboardContent.Page content) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(menuId);
            buffer.writeVarInt(page);
            content.write(buffer);
        }

        private static ClipboardEdit decode(FriendlyByteBuf buffer) {
            return new ClipboardEdit(buffer.readVarInt(), buffer.readVarInt(), ClipboardContent.Page.read(buffer));
        }
    }

    public record ClipboardPage(int menuId, ClipboardContent.View view) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(menuId);
            view.write(buffer);
        }

        private static ClipboardPage decode(FriendlyByteBuf buffer) {
            return new ClipboardPage(buffer.readVarInt(), ClipboardContent.View.read(buffer));
        }
    }
    public record SlottedBookEdit(int menuId, SlottedBookContents.Label label) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(menuId);
            label.write(buffer);
        }

        private static SlottedBookEdit decode(FriendlyByteBuf buffer) {
            return new SlottedBookEdit(buffer.readVarInt(), SlottedBookContents.Label.read(buffer));
        }
    }

    public record RedstoneBookRename(int menuId, String title) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(menuId);
            buffer.writeUtf(title, RedstoneBookMenu.TITLE_LENGTH);
        }

        private static RedstoneBookRename decode(FriendlyByteBuf buffer) {
            return new RedstoneBookRename(buffer.readVarInt(), buffer.readUtf(RedstoneBookMenu.TITLE_LENGTH));
        }
    }
    public record MapPinPacket(int menuId, String name, DyeColor color, boolean remove) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(menuId).writeUtf(name, MapPin.NAME_LENGTH).writeEnum(color).writeBoolean(remove);
        }

        private static MapPinPacket decode(FriendlyByteBuf buffer) {
            return new MapPinPacket(buffer.readVarInt(), buffer.readUtf(MapPin.NAME_LENGTH),
                    buffer.readEnum(DyeColor.class), buffer.readBoolean());
        }
    }
    public record RecipeBookEditPacket(int menuId, List<String> lines, boolean sign) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(menuId);
            buffer.writeCollection(lines, (out, line) -> out.writeUtf(line, RecordedRecipe.LINE_LENGTH));
            buffer.writeBoolean(sign);
        }

        private static RecipeBookEditPacket decode(FriendlyByteBuf buffer) {
            return new RecipeBookEditPacket(buffer.readVarInt(),
                    buffer.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, RecordedRecipe.TEXT_LINES),
                            in -> in.readUtf(RecordedRecipe.LINE_LENGTH)), buffer.readBoolean());
        }
    }
    public record SignPacket(int menuId, FancySignLayout layout) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(menuId);
            layout.write(buffer);
        }

        private static SignPacket decode(FriendlyByteBuf buffer) {
            return new SignPacket(buffer.readVarInt(), FancySignLayout.read(buffer));
        }
    }
    public record ClockPacket(int menuId, ClockSettings settings) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(menuId);
            settings.write(buffer);
        }

        private static ClockPacket decode(FriendlyByteBuf buffer) {
            return new ClockPacket(buffer.readVarInt(), ClockSettings.read(buffer));
        }
    }
    public record PaintPressPacket(int menuId, PaintingArt art, boolean start) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(menuId);
            art.write(buffer);
            buffer.writeBoolean(start);
        }

        private static PaintPressPacket decode(FriendlyByteBuf buffer) {
            return new PaintPressPacket(buffer.readVarInt(), PaintingArt.read(buffer), buffer.readBoolean());
        }
    }

    public record PaintingPacket(int menuId, PaintingSettings previous, PaintingSettings desired) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(menuId);
            previous.write(buffer);
            desired.write(buffer);
        }

        private static PaintingPacket decode(FriendlyByteBuf buffer) {
            return new PaintingPacket(buffer.readVarInt(), PaintingSettings.read(buffer), PaintingSettings.read(buffer));
        }
    }

    public static final class DrillTextPacket extends EmptyPacket {}
    public static final class PanelerClientPacket extends EmptyPacket {}
    public record RecipeTextPacket(int menuId, Component message) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(menuId);
            buffer.writeComponent(message);
        }

        private static RecipeTextPacket decode(FriendlyByteBuf buffer) {
            return new RecipeTextPacket(buffer.readVarInt(), buffer.readComponent());
        }
    }
    public record StockLogPacket(int menuId, int sourceCount, int unavailable, boolean finished,
                                 @org.jetbrains.annotations.Nullable CatalogContents.Source source) {
        void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(menuId);
            buffer.writeVarInt(sourceCount);
            buffer.writeVarInt(unavailable);
            buffer.writeBoolean(finished);
            buffer.writeNullable(source, (out, value) -> value.write(out));
        }

        static StockLogPacket decode(FriendlyByteBuf buffer) {
            return new StockLogPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readBoolean(), buffer.readNullable(CatalogContents.Source::read));
        }
    }

    public static final class OpenBookPacket extends EmptyPacket {}
    public static final class DeskOpenGuiPacket extends EmptyPacket {}
    public static final class SoundPlayerPacket extends EmptyPacket {}
}

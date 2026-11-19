package art.arcane.bibliocraft.network;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.catalog.CatalogContents;
import art.arcane.bibliocraft.book.BigBookContent;
import art.arcane.bibliocraft.client.BigBookScreen;
import art.arcane.bibliocraft.client.StockroomCatalogScreen;
import art.arcane.bibliocraft.menu.BigBookMenu;
import art.arcane.bibliocraft.client.RecipeBookScreen;
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
import java.util.function.Supplier;

public final class ModNetwork {
    private static final String PROTOCOL = "7";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private static int packetId = 0;
    private static boolean initialized = false;

    private ModNetwork() {}

    public static void register() {
        if (initialized) {
            return;
        }
        initialized = true;

        registerServerbound(AtlasWptPacket.class, AtlasWptPacket::new);
        registerServerbound(TypePacket.class, TypePacket::new);
        registerServerbound(TypeFlagPacket.class, TypeFlagPacket::new);
        registerServerbound(TypeDeletePacket.class, TypeDeletePacket::new);
        registerServerbound(TypeUpdatePacket.class, TypeUpdatePacket::new);
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
        registerServerbound(ClipboardPacket.class, ClipboardPacket::new);
        registerServerbound(UpdateInvPacket.class, UpdateInvPacket::new);
        registerServerbound(AtlasPacket.class, AtlasPacket::new);
        registerServerbound(MeasurePacket.class, MeasurePacket::new);
        registerServerbound(MapPinPacket.class, MapPinPacket::new);
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
        registerServerbound(PaintPressPacket.class, PaintPressPacket::new);
        registerServerbound(PaintingPacket.class, PaintingPacket::new);
        registerServerbound(PaintingClearPacket.class, PaintingClearPacket::new);

        registerClientbound(DrillTextPacket.class, DrillTextPacket::new);
        registerClientbound(AtlasClientPacket.class, AtlasClientPacket::new);
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
        registerClientbound(OpenBookPacket.class, OpenBookPacket::new);
        registerClientbound(DeskOpenGuiPacket.class, DeskOpenGuiPacket::new);
        registerClientbound(AtlasSwpClientPacket.class, AtlasSwpClientPacket::new);
        registerClientbound(AtlasTGuiPacket.class, AtlasTGuiPacket::new);
        registerClientbound(SoundPlayerPacket.class, SoundPlayerPacket::new);
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

    public static final class AtlasWptPacket extends EmptyPacket {}
    public static final class TypePacket extends EmptyPacket {}
    public static final class TypeFlagPacket extends EmptyPacket {}
    public static final class TypeDeletePacket extends EmptyPacket {}
    public static final class TypeUpdatePacket extends EmptyPacket {}
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

    public static final class ClipboardPacket extends EmptyPacket {}
    public static final class UpdateInvPacket extends EmptyPacket {}
    public static final class AtlasPacket extends EmptyPacket {}
    public static final class MeasurePacket extends EmptyPacket {}
    public static final class MapPinPacket extends EmptyPacket {}
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
    public static final class PaintPressPacket extends EmptyPacket {}
    public static final class PaintingPacket extends EmptyPacket {}
    public static final class PaintingClearPacket extends EmptyPacket {}

    public static final class DrillTextPacket extends EmptyPacket {}
    public static final class AtlasClientPacket extends EmptyPacket {}
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
    public static final class AtlasSwpClientPacket extends EmptyPacket {}
    public static final class AtlasTGuiPacket extends EmptyPacket {}
    public static final class SoundPlayerPacket extends EmptyPacket {}
}

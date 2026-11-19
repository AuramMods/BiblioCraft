package art.arcane.bibliocraft.network;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.blockentity.FancySignLayout;
import art.arcane.bibliocraft.menu.DeskBookMenu;
import art.arcane.bibliocraft.menu.FancySignMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public final class ModNetwork {
    private static final String PROTOCOL = "2";
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
        registerServerbound(PanelerPacket.class, PanelerPacket::new);
        registerServerbound(RecipeCraftPacket.class, RecipeCraftPacket::new);
        registerServerbound(StockTitlePacket.class, StockTitlePacket::new);
        registerServerbound(StockCompassPacket.class, StockCompassPacket::new);
        registerServerbound(ClipboardPacket.class, ClipboardPacket::new);
        registerServerbound(UpdateInvPacket.class, UpdateInvPacket::new);
        registerServerbound(AtlasPacket.class, AtlasPacket::new);
        registerServerbound(MeasurePacket.class, MeasurePacket::new);
        registerServerbound(MapPinPacket.class, MapPinPacket::new);
        registerServerbound(RBookPacket.class, RBookPacket::new);
        registerServerbound(RBookLoadPacket.class, RBookLoadPacket::new);
        CHANNEL.messageBuilder(SignPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SignPacket::encode).decoder(SignPacket::decode)
                .consumerMainThread((message, context) -> {
                    var player = context.get().getSender();
                    if (player != null && player.containerMenu instanceof FancySignMenu menu && menu.containerId == message.menuId()) {
                        menu.save(player, message.layout());
                    }
                    context.get().setPacketHandled(true);
                }).add();
        registerServerbound(ClockPacket.class, ClockPacket::new);
        registerServerbound(PaintPressPacket.class, PaintPressPacket::new);
        registerServerbound(PaintingPacket.class, PaintingPacket::new);
        registerServerbound(PaintingClearPacket.class, PaintingClearPacket::new);

        registerClientbound(DrillTextPacket.class, DrillTextPacket::new);
        registerClientbound(AtlasClientPacket.class, AtlasClientPacket::new);
        registerClientbound(PanelerClientPacket.class, PanelerClientPacket::new);
        registerClientbound(RecipeTextPacket.class, RecipeTextPacket::new);
        registerClientbound(StockLogPacket.class, StockLogPacket::new);
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
    public static final class PanelerPacket extends EmptyPacket {}
    public static final class RecipeCraftPacket extends EmptyPacket {}
    public static final class StockTitlePacket extends EmptyPacket {}
    public static final class StockCompassPacket extends EmptyPacket {}
    public static final class ClipboardPacket extends EmptyPacket {}
    public static final class UpdateInvPacket extends EmptyPacket {}
    public static final class AtlasPacket extends EmptyPacket {}
    public static final class MeasurePacket extends EmptyPacket {}
    public static final class MapPinPacket extends EmptyPacket {}
    public static final class RBookPacket extends EmptyPacket {}
    public static final class RBookLoadPacket extends EmptyPacket {}
    public record SignPacket(int menuId, FancySignLayout layout) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(menuId);
            layout.write(buffer);
        }

        private static SignPacket decode(FriendlyByteBuf buffer) {
            return new SignPacket(buffer.readVarInt(), FancySignLayout.read(buffer));
        }
    }
    public static final class ClockPacket extends EmptyPacket {}
    public static final class PaintPressPacket extends EmptyPacket {}
    public static final class PaintingPacket extends EmptyPacket {}
    public static final class PaintingClearPacket extends EmptyPacket {}

    public static final class DrillTextPacket extends EmptyPacket {}
    public static final class AtlasClientPacket extends EmptyPacket {}
    public static final class PanelerClientPacket extends EmptyPacket {}
    public static final class RecipeTextPacket extends EmptyPacket {}
    public static final class StockLogPacket extends EmptyPacket {}
    public static final class OpenBookPacket extends EmptyPacket {}
    public static final class DeskOpenGuiPacket extends EmptyPacket {}
    public static final class AtlasSwpClientPacket extends EmptyPacket {}
    public static final class AtlasTGuiPacket extends EmptyPacket {}
    public static final class SoundPlayerPacket extends EmptyPacket {}
}

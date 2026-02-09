package art.arcane.bibliocraft.network;

import art.arcane.bibliocraft.BiblioCraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

public final class ModNetwork {
    private static final String PROTOCOL = "1";
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
        registerServerbound(McbEditPacket.class, McbEditPacket::new);
        registerServerbound(McbPagePacket.class, McbPagePacket::new);
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
        registerServerbound(SignPacket.class, SignPacket::new);
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
    public static final class McbEditPacket extends EmptyPacket {}
    public static final class McbPagePacket extends EmptyPacket {}
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
    public static final class SignPacket extends EmptyPacket {}
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

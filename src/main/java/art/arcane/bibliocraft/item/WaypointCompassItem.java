package art.arcane.bibliocraft.item;

import art.arcane.bibliocraft.catalog.CatalogContents;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.menu.WaypointCompassMenu;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class WaypointCompassItem extends Item {
    public static final int MAX_COORDINATE = 29_999_984;
    public static final int MAX_NAME_LENGTH = 160;
    private static final String DEFAULT_NAME = "World Origin";
    public static final Target ORIGIN = new Target(0, 0, DEFAULT_NAME, null);

    public WaypointCompassItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!BiblioConfig.COMMON.enableWaypointCompass.get()) {
            return InteractionResultHolder.pass(stack);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            Target target = getTarget(stack);
            if (player.isShiftKeyDown()) {
                NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                        (id, inventory, owner) -> new WaypointCompassMenu(id, owner, hand, target), getDescription()),
                        buffer -> {
                            buffer.writeEnum(hand);
                            target.write(buffer);
                        });
            } else {
                player.displayClientMessage(target.description(), false);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(getTarget(stack).description());
    }

    public static Target getTarget(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("XCoord", Tag.TAG_ANY_NUMERIC) || !tag.contains("ZCoord", Tag.TAG_ANY_NUMERIC)) {
            return ORIGIN;
        }
        return new Target(tag.getInt("XCoord"), tag.getInt("ZCoord"), tag.getString("WaypointName"),
                tag.contains("WaypointDimension", Tag.TAG_STRING) ? ResourceLocation.tryParse(tag.getString("WaypointDimension")) : null);
    }

    public static void setTarget(ItemStack stack, Target target) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt("XCoord", target.x());
        tag.putInt("ZCoord", target.z());
        tag.putString("WaypointName", target.name());
        if (target.dimension() == null) {
            tag.remove("WaypointDimension");
        } else {
            tag.putString("WaypointDimension", target.dimension().toString());
        }
    }

    public static Target at(GlobalPos location, String name) {
        return new Target(location.pos().getX(), location.pos().getZ(), name, location.dimension().location());
    }

    public static boolean validCoordinate(int value) {
        return value >= -MAX_COORDINATE && value <= MAX_COORDINATE;
    }

    public static int needlePart(double x, double z, float yaw, Target target) {
        double heading = Math.toDegrees(Math.atan2(target.z() + 0.5 - z, target.x() + 0.5 - x));
        double faceAngle = heading - yaw - 180.0;
        // The red tip of OBJ part 36 is at about -140 degrees in the model's XZ plane.
        int part = Math.floorMod((int) Math.round((faceAngle + 140.0) / 10.0), 36);
        return part == 0 ? 36 : part;
    }

    public record Target(int x, int z, String name, @Nullable ResourceLocation dimension) {
        public Target {
            name = CatalogContents.bounded(name, MAX_NAME_LENGTH);
        }

        public Component label() {
            return DEFAULT_NAME.equals(name) ? Component.translatable("gui.bibliocraft.waypoint.origin")
                    : Component.literal(name);
        }

        public Component description() {
            Component coordinates = Component.translatable("gui.bibliocraft.waypoint.coordinates", label(), x, z);
            return dimension == null ? coordinates
                    : coordinates.copy().append(" (" + dimension + ")");
        }

        public void write(FriendlyByteBuf buffer) {
            buffer.writeInt(x);
            buffer.writeInt(z);
            buffer.writeUtf(name, MAX_NAME_LENGTH);
            buffer.writeNullable(dimension, FriendlyByteBuf::writeResourceLocation);
        }

        public static Target read(FriendlyByteBuf buffer) {
            return new Target(buffer.readInt(), buffer.readInt(), buffer.readUtf(MAX_NAME_LENGTH),
                    buffer.readNullable(FriendlyByteBuf::readResourceLocation));
        }
    }
}

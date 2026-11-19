package art.arcane.bibliocraft.item;

import art.arcane.bibliocraft.block.MarkerPoleBlock;
import art.arcane.bibliocraft.block.TableBlock;
import art.arcane.bibliocraft.blockentity.MarkerPoleBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class TapeMeasureItem extends Item {
    private static final String START = "TapeStart";
    private static final String ABSOLUTE = "TapeAbsolute";

    public TapeMeasureItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();
        if (!BiblioConfig.COMMON.enableTapemeasure.get() || player == null || player.isSpectator()) {
            return InteractionResult.PASS;
        }
        // Keep the table's display slot on top and the tape's original rotation controls on the sides.
        if (context.getHand() == InteractionHand.MAIN_HAND
                && BiblioConfig.COMMON.enableTable.get() && level.getBlockState(pos).getBlock() instanceof TableBlock) {
            return InteractionResult.PASS;
        }
        if (player.isUsingItem()) {
            return InteractionResult.CONSUME;
        }
        player.startUsingItem(context.getHand());
        if (level instanceof ServerLevel server) {
            Start start = readStart(stack);
            if (start != null) {
                if (start.marker() != null) {
                    ServerLevel origin = server.getServer().getLevel(start.origin().dimension());
                    if (origin != null) {
                        MarkerPoleBlockEntity.remove(origin, start.origin().pos().relative(start.face()), start.marker());
                    }
                }
                stack.removeTagKey(START);
                if (server.dimension().equals(start.origin().dimension())) {
                    finish(player, stack, Offset.between(start.origin().pos(), pos, face));
                    server.playSound(null, pos, ModSounds.TAPECLOSE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                    return InteractionResult.CONSUME;
                }
            }
            UUID marker = placeMarker(server, player, stack, pos.relative(face), face);
            saveStart(stack, new Start(GlobalPos.of(server.dimension(), pos), face, marker));
            player.displayClientMessage(Component.translatable("message.bibliocraft.tape.start"), false);
            server.playSound(null, pos, ModSounds.TAPEOPEN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!BiblioConfig.COMMON.enableTapemeasure.get()) {
            return InteractionResultHolder.pass(stack);
        }
        if (player.isUsingItem()) {
            return InteractionResultHolder.consume(stack);
        }
        player.startUsingItem(hand);
        if (!level.isClientSide) {
            boolean absolute = !absolute(stack);
            if (absolute) {
                stack.getOrCreateTag().putBoolean(ABSOLUTE, true);
            } else {
                stack.removeTagKey(ABSOLUTE);
            }
            player.displayClientMessage(Component.translatable(absolute
                    ? "message.bibliocraft.tape.mode_absolute" : "message.bibliocraft.tape.mode_axes"), false);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public boolean canContinueUsing(ItemStack previous, ItemStack current) {
        // Syncing the start or mode must not end the press before the vanilla release packet.
        return previous.is(this) && current.is(this);
    }

    private static @Nullable UUID placeMarker(ServerLevel level, Player player, ItemStack tape, BlockPos pos, Direction face) {
        if (!level.isInWorldBounds(pos) || !level.getWorldBorder().isWithinBounds(pos) || !level.hasChunkAt(pos)
                || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, face, tape) || !level.isEmptyBlock(pos)) {
            return null;
        }
        var state = ((MarkerPoleBlock) ModBlocks.MARKER_POLE.get()).attachedTo(face).setValue(MarkerPoleBlock.TEMPORARY, true);
        var snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        if (!level.setBlock(pos, state, Block.UPDATE_KNOWN_SHAPE)) {
            return null;
        }
        if (ForgeEventFactory.onBlockPlace(player, snapshot, face)) {
            snapshot.restore(true);
            return null;
        }
        if (!(level.getBlockEntity(pos) instanceof MarkerPoleBlockEntity pole) || level.getBlockState(pos) != state) {
            return null;
        }
        UUID id = UUID.randomUUID();
        pole.bind(level, player.getUUID(), id);
        level.markAndNotifyBlock(pos, level.getChunkAt(pos), snapshot.getReplacedBlock(), state, Block.UPDATE_ALL, Block.UPDATE_LIMIT);
        return id;
    }

    private static void finish(Player player, ItemStack stack, Offset offset) {
        if (absolute(stack)) {
            player.displayClientMessage(Component.translatable("message.bibliocraft.tape.absolute", offset.distance()), false);
            return;
        }
        if (offset.south() == 1 && offset.east() == 1 && offset.height() == 0) {
            player.displayClientMessage(Component.translatable("message.bibliocraft.tape.empty"), false);
            return;
        }
        if (offset.south() != 1) {
            player.displayClientMessage(Component.translatable("message.bibliocraft.tape.north_south", offset.south()), false);
        }
        if (offset.east() != 1) {
            player.displayClientMessage(Component.translatable("message.bibliocraft.tape.east_west", offset.east()), false);
        }
        if (offset.height() != 0) {
            player.displayClientMessage(Component.translatable("message.bibliocraft.tape.height", offset.height()), false);
        }
    }

    public static int liveDistance(Player player, ItemStack stack) {
        var start = readStart(stack);
        return start == null || !start.origin().dimension().equals(player.level().dimension())
                ? 0 : Offset.between(start.origin().pos(), player.blockPosition(), Direction.UP).distance();
    }

    public static boolean hasMarker(ItemStack stack, UUID id) {
        CompoundTag start = stack.getTagElement(START);
        return stack.getItem() instanceof TapeMeasureItem && start != null && start.hasUUID("Marker")
                && id.equals(start.getUUID("Marker"));
    }

    private static boolean absolute(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().getBoolean(ABSOLUTE);
    }

    private static @Nullable Start readStart(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(START);
        if (!(stack.getItem() instanceof TapeMeasureItem) || tag == null || !tag.contains("Origin", Tag.TAG_LONG)) {
            return null;
        }
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("Dimension"));
        Direction face = Direction.byName(tag.getString("Face"));
        if (dimension == null || face == null) {
            return null;
        }
        return new Start(GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dimension), BlockPos.of(tag.getLong("Origin"))),
                face, tag.hasUUID("Marker") ? tag.getUUID("Marker") : null);
    }

    private static void saveStart(ItemStack stack, Start start) {
        var tag = new CompoundTag();
        tag.putLong("Origin", start.origin().pos().asLong());
        tag.putString("Dimension", start.origin().dimension().location().toString());
        tag.putString("Face", start.face().getSerializedName());
        if (start.marker() != null) {
            tag.putUUID("Marker", start.marker());
        }
        stack.addTagElement(START, tag);
    }

    private record Start(GlobalPos origin, Direction face, @Nullable UUID marker) {}

    public record Offset(int east, int height, int south) {
        public static Offset between(BlockPos first, BlockPos last, Direction face) {
            return new Offset(Math.abs(last.getX() - first.getX()) + 1,
                    Math.abs(last.getY() - first.getY()) + (face.getAxis().isHorizontal() ? 1 : 0),
                    Math.abs(last.getZ() - first.getZ()) + 1);
        }

        public int distance() {
            return (int) Math.sqrt((double) east * east + (double) height * height + (double) south * south);
        }
    }
}

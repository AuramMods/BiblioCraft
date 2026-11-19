package art.arcane.bibliocraft.item;

import art.arcane.bibliocraft.block.ArmorStandBlock;
import art.arcane.bibliocraft.block.ClockBlock;
import art.arcane.bibliocraft.block.DeskBlock;
import art.arcane.bibliocraft.block.DrillAdjustable;
import art.arcane.bibliocraft.block.FramedChestBlock;
import art.arcane.bibliocraft.block.PaintingFrameBlock;
import art.arcane.bibliocraft.block.SeatBlock;
import art.arcane.bibliocraft.block.TableBlock;
import art.arcane.bibliocraft.blockentity.FurnitureBlockEntity;
import art.arcane.bibliocraft.config.BiblioFeatureToggles;
import art.arcane.bibliocraft.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraftforge.registries.ForgeRegistries;

public final class FurnitureDrillItem extends Item {
    private final boolean manual;

    public FurnitureDrillItem(Properties properties, boolean manual) {
        super(properties);
        this.manual = manual;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        var player = context.getPlayer();
        if (player == null || !BiblioFeatureToggles.isItemEnabled(ForgeRegistries.ITEMS.getKey(this))
                || !BiblioFeatureToggles.isBlockEnabled(ForgeRegistries.BLOCKS.getKey(state.getBlock()))) {
            return InteractionResult.PASS;
        }
        var block = state.getBlock();
        boolean sneaking = player.isShiftKeyDown();
        boolean paired = sneaking && (block instanceof DeskBlock || block instanceof ArmorStandBlock);
        if (!(block instanceof DrillAdjustable || block instanceof TableBlock || block instanceof SeatBlock
                || block instanceof ClockBlock || block instanceof FramedChestBlock || block instanceof PaintingFrameBlock)
                && !paired) {
            return rotateVanilla(context, state, sneaking);
        }
        var furniture = FurnitureBlockEntity.findAt(level, pos);
        if (furniture != null && !furniture.canAccess(player)) {
            return FurnitureBlockEntity.denyAccess(player);
        }
        var result = adjustFurniture(context, state, sneaking);
        if (!level.isClientSide && result.consumesAction()) {
            playSound(context);
        }
        return result;
    }

    private InteractionResult adjustFurniture(UseOnContext context, BlockState state, boolean sneaking) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var block = state.getBlock();
        if (block instanceof DeskBlock desk) {
            return desk.connectWithDrill(context);
        }
        if (block instanceof ArmorStandBlock stand) {
            if (!level.isClientSide) {
                stand.rotateStand(level, pos, state);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (block instanceof TableBlock table) {
            return table.adjustWithDrill(context, sneaking);
        }
        if (block instanceof SeatBlock seat) {
            return seat.adjustWithDrill(context, sneaking);
        }
        if (block instanceof ClockBlock clock) {
            return clock.adjustWithDrill(context, sneaking);
        }
        if (block instanceof FramedChestBlock chest) {
            return chest.adjustWithDrill(context, sneaking);
        }
        if (block instanceof PaintingFrameBlock frame) {
            return frame.adjustWithDrill(context, sneaking);
        }
        var adjustable = (DrillAdjustable) block;
        var adjusted = adjustable.adjustWithDrill(state, sneaking);
        if (adjusted == state) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && level.setBlock(pos, adjusted, Block.UPDATE_ALL)) {
            adjustable.afterDrill(level, pos, state, adjusted);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private InteractionResult rotateVanilla(UseOnContext context, BlockState state, boolean sneaking) {
        if (!supportedVanilla(state, sneaking)) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        var pos = context.getClickedPos();
        var rotated = state.rotate(Rotation.CLOCKWISE_90);
        if (rotated == state || !rotated.canSurvive(level, pos)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                var otherPos = pos.relative(ChestBlock.getConnectedDirection(state));
                var other = level.getBlockState(otherPos);
                if (other.is(state.getBlock()) && other.getValue(ChestBlock.TYPE) == state.getValue(ChestBlock.TYPE).getOpposite()
                        && otherPos.relative(ChestBlock.getConnectedDirection(other)).equals(pos)) {
                    level.setBlock(otherPos, other.setValue(ChestBlock.TYPE, ChestType.SINGLE),
                            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                }
                rotated = rotated.setValue(ChestBlock.TYPE, ChestType.SINGLE);
            }
            if (level.setBlock(pos, rotated, Block.UPDATE_ALL)) {
                playSound(context);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean supportedVanilla(BlockState state, boolean sneaking) {
        Block block = state.getBlock();
        if (!"minecraft".equals(ForgeRegistries.BLOCKS.getKey(block).getNamespace())) {
            return false;
        }
        if (block instanceof ChestBlock || block instanceof DispenserBlock || block instanceof AbstractFurnaceBlock
                || state.is(Blocks.ENDER_CHEST) || state.is(Blocks.COMPARATOR)) {
            return true;
        }
        if (state.is(Blocks.PISTON) || state.is(Blocks.STICKY_PISTON)) {
            return sneaking && !state.getValue(BlockStateProperties.EXTENDED);
        }
        return sneaking && (state.is(Blocks.LEVER) || state.is(Blocks.REPEATER) || state.is(BlockTags.ANVIL)
                || state.is(BlockTags.DOORS) || state.is(BlockTags.TRAPDOORS));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack drill = player.getItemInHand(hand);
        if (player.isShiftKeyDown() || !BiblioFeatureToggles.isItemEnabled(ForgeRegistries.ITEMS.getKey(this))
                || !DrillSelection.hasSelection(drill)) {
            return InteractionResultHolder.pass(drill);
        }
        if (!level.isClientSide) {
            DrillSelection.clear(drill);
            player.displayClientMessage(Component.translatable("message.bibliocraft.drill.cleared"), true);
        }
        return InteractionResultHolder.sidedSuccess(drill, level.isClientSide);
    }

    private void playSound(UseOnContext context) {
        context.getLevel().playSound(null, context.getClickedPos(), (manual ? ModSounds.DRILL : ModSounds.SCREW).get(),
                SoundSource.BLOCKS, manual ? 0.8F : 0.7F, 1);
    }
}

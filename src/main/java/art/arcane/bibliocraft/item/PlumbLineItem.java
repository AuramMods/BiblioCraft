package art.arcane.bibliocraft.item;

import art.arcane.bibliocraft.config.BiblioConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;

import java.util.OptionalInt;

public final class PlumbLineItem extends Item {
    public PlumbLineItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!BiblioConfig.COMMON.enablePlumbLine.get()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            BlockPos top = player.blockPosition().relative(player.getDirection()).below();
            OptionalInt surface = surfaceHeight(level, top);
            player.displayClientMessage(surface.isPresent()
                    ? Component.translatable("message.bibliocraft.plumb_line.depth",
                            top.getY() - surface.getAsInt() + 1, surface.getAsInt())
                    : Component.translatable("message.bibliocraft.plumb_line.no_surface"), false);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static OptionalInt surfaceHeight(Level level, BlockPos top) {
        var probe = top.mutable();
        for (int y = Math.min(top.getY(), level.getMaxBuildHeight() - 1); y >= level.getMinBuildHeight(); y--) {
            var state = level.getBlockState(probe.setY(y));
            if (!state.isAir() && !(state.getBlock() instanceof LiquidBlock)) {
                return OptionalInt.of(y + 1);
            }
        }
        return OptionalInt.empty();
    }
}

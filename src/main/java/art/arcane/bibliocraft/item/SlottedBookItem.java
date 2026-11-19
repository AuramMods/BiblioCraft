package art.arcane.bibliocraft.item;

import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.menu.SlottedBookMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class SlottedBookItem extends Item {
    public SlottedBookItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!BiblioConfig.COMMON.enableSlottedBook.get()) {
            return InteractionResultHolder.pass(stack);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            SlottedBookMenu.open(serverPlayer, hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String author = stack.getTag() == null ? "" : stack.getTag().getString("authorName");
        tooltip.add((author.isBlank() ? Component.translatable("item.bibliocraft.slotted_book.author")
                : Component.literal(author)).withStyle(ChatFormatting.GRAY));
    }
}

package art.arcane.bibliocraft.item;

import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.menu.RedstoneBookMenu;
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

public final class RedstoneBookItem extends Item {
    public RedstoneBookItem() {
        super(new Properties());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!BiblioConfig.COMMON.enableRedstonebook.get()) {
            return InteractionResultHolder.pass(stack);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            RedstoneBookMenu.open(serverPlayer, hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String author = stack.getTag() == null ? "" : stack.getTag().getString("redstonebook");
        tooltip.add((author.isBlank() ? Component.translatable("item.bibliocraft.biblio_red_book.author")
                : Component.literal(author)).withStyle(ChatFormatting.GRAY));
    }
}

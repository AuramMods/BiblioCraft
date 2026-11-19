package art.arcane.bibliocraft.item;

import art.arcane.bibliocraft.book.BigBookContent;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.menu.BigBookMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
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

public final class BigBookItem extends Item {
    public BigBookItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!BiblioConfig.COMMON.enableBigBook.get()) {
            return InteractionResultHolder.pass(stack);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            BigBookMenu.openHeld(serverPlayer, hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public Component getName(ItemStack book) {
        if (book.hasTag() && !book.getTag().getString("title").isEmpty()) {
            return Component.literal(book.getTag().getString("title"));
        }
        return super.getName(book);
    }

    @Override
    public boolean isFoil(ItemStack book) {
        return BigBookContent.signed(book) || super.isFoil(book);
    }

    @Override
    public int getEnchantmentValue() {
        return 1;
    }

    @Override
    public void appendHoverText(ItemStack book, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        var tag = book.getTag();
        if (tag != null && !tag.getString("author").isEmpty()) {
            tooltip.add(Component.translatable("gui.bibliocraft.big_book.author", tag.getString("author"))
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public @Nullable CompoundTag getShareTag(ItemStack book) {
        CompoundTag tag = super.getShareTag(book);
        if (tag != null && tag.sizeInBytes() > FriendlyByteBuf.DEFAULT_NBT_QUOTA - 32_768) {
            // The book menu can still stream the full document from the server, one page at a time.
            tag = tag.copy();
            BigBookContent.stripPageData(tag);
        }
        return tag;
    }
}

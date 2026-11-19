package art.arcane.bibliocraft.item;

import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.menu.ItemTesterMenu;
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

public final class NameTesterItem extends Item {
    public NameTesterItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack tester = player.getItemInHand(hand);
        if (!BiblioConfig.COMMON.enableTesterItem.get()) {
            return InteractionResultHolder.pass(tester);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (tester.getCount() > 1) {
                ItemStack extra = tester.split(tester.getCount() - 1);
                extra.removeTagKey("Inventory");
                player.getInventory().placeItemBackInInventory(extra);
            }
            ItemTesterMenu.open(serverPlayer, hand);
        }
        return InteractionResultHolder.sidedSuccess(tester, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.bibliocraft.tester_item.info").withStyle(ChatFormatting.GRAY));
    }
}

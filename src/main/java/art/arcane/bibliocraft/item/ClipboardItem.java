package art.arcane.bibliocraft.item;

import art.arcane.bibliocraft.book.ClipboardContent;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.menu.ClipboardMenu;
import art.arcane.bibliocraft.registry.ModBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public final class ClipboardItem extends BlockItem {
    public ClipboardItem() {
        super(ModBlocks.CLIPBOARD.get(), new Properties().stacksTo(1));
    }

    @Override
    public String getDescriptionId() {
        return getOrCreateDescriptionId();
    }

    @Override
    public void registerBlocks(Map<Block, Item> map, Item item) {
        // The separate clipboard block item still owns the block's registry mapping.
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        return BiblioConfig.COMMON.enableClipboard.get() && player != null && player.isShiftKeyDown()
                && context.getClickedFace().getAxis().isHorizontal() ? super.useOn(context) : InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!BiblioConfig.COMMON.enableClipboard.get() || player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            ClipboardMenu.openHeld(serverPlayer, hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String title = ClipboardContent.View.of(stack).page().title();
        if (!title.isBlank()) {
            tooltip.add(Component.literal(title));
        }
    }
}

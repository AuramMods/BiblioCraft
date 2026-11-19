package art.arcane.bibliocraft.item;

import art.arcane.bibliocraft.catalog.CatalogContents;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.menu.StockroomCatalogMenu;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

public final class StockroomCatalogItem extends Item {
    public StockroomCatalogItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null || !player.isShiftKeyDown() || !BiblioConfig.COMMON.enableStockroomCatalog.get()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            // The server is authoritative for vanilla and modded inventories without a client capability.
            return InteractionResult.SUCCESS;
        }
        var container = CatalogContents.findContainer(level, context.getClickedPos());
        var location = GlobalPos.of(level.dimension(), container == null ? context.getClickedPos() : container.getBlockPos());
        boolean known = CatalogContents.locations(stack, level.dimension()).contains(location);
        if (!known && (container == null || !CatalogContents.canAccess(container, player))) {
            player.displayClientMessage(Component.translatable("message.bibliocraft.catalog.unavailable"), true);
            return InteractionResult.CONSUME;
        }
        boolean added = CatalogContents.toggle(stack, location);
        player.getInventory().setChanged();
        Component name = container == null ? level.getBlockState(context.getClickedPos()).getBlock().getName()
                : CatalogContents.nameOf(container).component();
        player.displayClientMessage(Component.translatable(
                added ? "message.bibliocraft.catalog.added" : "message.bibliocraft.catalog.removed", name), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() || !BiblioConfig.COMMON.enableStockroomCatalog.get()) {
            return InteractionResultHolder.pass(stack);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            var title = stack.hasCustomHoverName()
                    ? Component.literal(CatalogContents.bounded(stack.getHoverName().getString(), CatalogContents.TITLE_LENGTH))
                    : stack.getHoverName();
            NetworkHooks.openScreen(serverPlayer,
                    new SimpleMenuProvider((id, inventory, owner) -> new StockroomCatalogMenu(id, owner, hand), title),
                    buffer -> buffer.writeEnum(hand));
            if (serverPlayer.containerMenu instanceof StockroomCatalogMenu menu) {
                menu.refresh(serverPlayer);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}

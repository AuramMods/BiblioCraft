package art.arcane.bibliocraft.item;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.menu.PlaceholderMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

public class MenuOpeningItem extends Item {
    private final Supplier<MenuType<PlaceholderMenu>> menuType;
    private final String menuPath;

    public MenuOpeningItem(Properties properties, Supplier<MenuType<PlaceholderMenu>> menuType, String menuPath) {
        super(properties);
        this.menuType = menuType;
        this.menuPath = menuPath;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(held);
        }

        if (level.isClientSide()) {
            return InteractionResultHolder.success(held);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            MenuType<PlaceholderMenu> type = menuType.get();
            MenuProvider provider = new SimpleMenuProvider(
                    (containerId, inventory, menuPlayer) -> new PlaceholderMenu(type, containerId),
                    Component.translatable("menu." + BiblioCraft.MODID + "." + menuPath)
            );
            NetworkHooks.openScreen(serverPlayer, provider);
        }

        return InteractionResultHolder.consume(held);
    }
}

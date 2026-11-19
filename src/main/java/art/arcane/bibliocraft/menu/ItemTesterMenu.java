package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.registry.ModItems;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.network.NetworkHooks;

public final class ItemTesterMenu extends SingleSlotHeldItemMenu {
    public ItemTesterMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readEnum(InteractionHand.class));
    }

    public ItemTesterMenu(int id, Inventory inventory, InteractionHand hand) {
        super(ModMenus.TESTER.get(), id, inventory, hand, ModItems.TESTER_ITEM.get(),
                BiblioConfig.COMMON.enableTesterItem::get);
    }

    public static void open(ServerPlayer player, InteractionHand hand) {
        NetworkHooks.openScreen(player, new SimpleMenuProvider((id, inventory, owner) ->
                new ItemTesterMenu(id, inventory, hand), Component.translatable(ModItems.TESTER_ITEM.get().getDescriptionId())),
                buffer -> buffer.writeEnum(hand));
    }
}

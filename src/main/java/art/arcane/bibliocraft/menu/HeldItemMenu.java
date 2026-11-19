package art.arcane.bibliocraft.menu;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

abstract class HeldItemMenu extends AbstractContainerMenu {
    protected final Player owner;
    protected final ItemStack heldStack;
    private final InteractionHand hand;

    protected HeldItemMenu(MenuType<?> type, int id, Player owner, InteractionHand hand) {
        super(type, id);
        this.owner = owner;
        this.hand = hand;
        this.heldStack = owner.getItemInHand(hand);
    }

    @Override
    public boolean stillValid(Player player) {
        return owner == player && player.isAlive() && !heldStack.isEmpty() && heldStack == player.getItemInHand(hand);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }
}

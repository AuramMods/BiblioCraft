package art.arcane.bibliocraft.item;

import art.arcane.bibliocraft.blockentity.FurnitureBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class FurnitureLockItem extends Item {
    public FurnitureLockItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        var player = context.getPlayer();
        var level = context.getLevel();
        var furniture = FurnitureBlockEntity.findAt(level, context.getClickedPos());
        if (player == null || furniture == null
                || (!furniture.isLocked() && !BiblioConfig.COMMON.enableLock.get())) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (!furniture.toggleLock(player)) {
                return FurnitureBlockEntity.denyAccess(player);
            }
            String message = furniture.isLocked() ? "message.bibliocraft.lock.locked" : "message.bibliocraft.lock.unlocked";
            player.displayClientMessage(Component.translatable(message), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.bibliocraft.biblio_creative_lock.tooltip").withStyle(ChatFormatting.GRAY));
    }
}

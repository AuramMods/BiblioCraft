package art.arcane.bibliocraft.item;

import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.crafting.RecordedRecipe;
import art.arcane.bibliocraft.menu.RecipeBookMenu;
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

import java.util.LinkedHashMap;
import java.util.List;

public final class RecipeBookItem extends Item {
    public RecipeBookItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack book = player.getItemInHand(hand);
        if (!BiblioConfig.COMMON.enableFancyWorkbench.get()) {
            return InteractionResultHolder.pass(book);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            RecipeBookMenu.open(serverPlayer, null, hand, book);
        }
        return InteractionResultHolder.sidedSuccess(book, level.isClientSide);
    }

    @Override
    public Component getName(ItemStack stack) {
        ItemStack output = RecordedRecipe.read(stack).output();
        return output.isEmpty() ? super.getName(stack)
                : Component.translatable("item.bibliocraft.recipe_book.named", output.getHoverName());
    }

    @Override
    public boolean isFoil(ItemStack book) {
        return RecordedRecipe.isSigned(book) || super.isFoil(book);
    }

    @Override
    public void appendHoverText(ItemStack book, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        var recipe = RecordedRecipe.read(book);
        if (recipe.isRecorded()) {
            tooltip.add(Component.translatable("gui.bibliocraft.recipe_book.ingredients"));
            var totals = new LinkedHashMap<Component, Integer>();
            for (ItemStack stack : recipe.grid()) {
                if (!stack.isEmpty()) {
                    totals.merge(stack.getHoverName(), 1, Integer::sum);
                }
            }
            totals.forEach((name, count) -> tooltip.add(Component.translatable(
                    "gui.bibliocraft.recipe_book.ingredient", count, name)));
        }
    }
}

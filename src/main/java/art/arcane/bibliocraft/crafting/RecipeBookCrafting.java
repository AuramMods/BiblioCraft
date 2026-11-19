package art.arcane.bibliocraft.crafting;

import art.arcane.bibliocraft.config.BiblioConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

public final class RecipeBookCrafting {
    private RecipeBookCrafting() {}

    static boolean same(ItemStack left, ItemStack right) {
        return ItemStack.isSameItemSameTags(left, right);
    }

    public static Optional<CraftingRecipe> resolve(RecordedRecipe recorded, CraftingContainer grid, Level level) {
        if (!recorded.isRecorded()) {
            return Optional.empty();
        }
        var manager = level.getRecipeManager();
        if (recorded.recipeId() == null) {
            return manager.getRecipeFor(RecipeType.CRAFTING, grid, level);
        }
        return manager.byKey(recorded.recipeId())
                .filter(recipe -> recipe instanceof CraftingRecipe crafting && crafting.matches(grid, level))
                .map(CraftingRecipe.class::cast);
    }

    public static Component craft(Player player, ItemStack book) {
        if (!BiblioConfig.COMMON.enableRecipeBookCrafting.get()) {
            return message("disabled");
        }
        var saved = RecordedRecipe.read(book);
        var grid = recordedGrid(saved);
        Level level = player.level();
        var recipe = resolve(saved, grid, level).orElse(null);
        var award = new ResultContainer();
        if (recipe == null || !applyRecipe(award, player, recipe)) {
            return message("invalid");
        }
        var stock = new Stock(player, book);
        for (ItemStack ingredient : saved.grid()) {
            if (!ingredient.isEmpty() && !stock.take(ingredient, 1)) {
                return message("missing");
            }
        }
        ItemStack output = recipe.assemble(grid, level.registryAccess());
        if (output.isEmpty() || !output.isItemEnabled(level.enabledFeatures())) {
            return message("invalid");
        }
        net.minecraft.core.NonNullList<ItemStack> remainders;
        ForgeHooks.setCraftingPlayer(player);
        try {
            remainders = recipe.getRemainingItems(grid);
        } finally {
            ForgeHooks.setCraftingPlayer(null);
        }
        stock.commit(true);
        output.onCraftedBy(level, player, output.getCount());
        ForgeEventFactory.firePlayerCraftingEvent(player, output, grid);
        award.awardUsedRecipes(player, grid.getItems());
        Component name = output.getHoverName();
        player.getInventory().placeItemBackInInventory(output);
        remainders.forEach(stack -> {
            if (!stack.isEmpty()) {
                player.getInventory().placeItemBackInInventory(stack);
            }
        });
        return Component.translatable("message.bibliocraft.recipe_book.crafted", name);
    }

    public static boolean applyRecipe(ResultContainer result, Player player, CraftingRecipe recipe) {
        if (player instanceof ServerPlayer serverPlayer) {
            return result.setRecipeUsed(player.level(), serverPlayer, recipe);
        }
        result.setRecipeUsed(recipe);
        return true;
    }

    public static boolean isAvailable(RecordedRecipe saved, Player player) {
        var grid = recordedGrid(saved);
        var recipe = resolve(saved, grid, player.level()).orElse(null);
        if (recipe == null || !applyRecipe(new ResultContainer(), player, recipe)) {
            return false;
        }
        var output = recipe.assemble(grid, player.level().registryAccess());
        return !output.isEmpty() && output.isItemEnabled(player.level().enabledFeatures());
    }

    private static Grid recordedGrid(RecordedRecipe saved) {
        var grid = new Grid();
        for (int slot = 0; slot < grid.getContainerSize(); slot++) {
            grid.setItem(slot, saved.grid().get(slot).copy());
        }
        return grid;
    }

    public static Optional<List<ItemStack>> load(Player player, RecordedRecipe saved, List<ItemStack> current) {
        var stock = new Stock(player, ItemStack.EMPTY);
        List<ItemStack> preview = new ArrayList<>(RecordedRecipe.GRID_SIZE);
        int target = 64;
        var requirements = new LinkedHashMap<ItemStack, List<Integer>>();
        for (int slot = 0; slot < RecordedRecipe.GRID_SIZE; slot++) {
            ItemStack pattern = saved.grid().get(slot);
            ItemStack existing = current.get(slot);
            if (!existing.isEmpty() && (pattern.isEmpty() || !same(pattern, existing))) {
                return Optional.empty();
            }
            if (!pattern.isEmpty()) {
                ItemStack key = requirements.keySet().stream().filter(item -> same(item, pattern)).findFirst().orElse(pattern);
                requirements.computeIfAbsent(key, ignored -> new ArrayList<>()).add(slot);
                target = Math.min(target, pattern.getMaxStackSize());
            }
        }
        while (target > 0 && !fits(requirements, current, stock, target)) {
            target--;
        }
        if (target == 0 || requirements.isEmpty()) {
            return Optional.empty();
        }
        for (int slot = 0; slot < RecordedRecipe.GRID_SIZE; slot++) {
            ItemStack pattern = saved.grid().get(slot);
            ItemStack existing = current.get(slot);
            int amount = Math.max(0, target - existing.getCount());
            if (pattern.isEmpty() || amount == 0) {
                preview.add(existing.copy());
            } else {
                stock.take(pattern, amount);
                preview.add(pattern.copyWithCount(existing.getCount() + amount));
            }
        }
        stock.commit(false);
        return Optional.of(preview);
    }

    private static boolean fits(LinkedHashMap<ItemStack, List<Integer>> requirements, List<ItemStack> grid, Stock stock, int target) {
        return requirements.entrySet().stream().allMatch(entry -> {
            int deficit = entry.getValue().stream().mapToInt(slot -> Math.max(0, target - grid.get(slot).getCount())).sum();
            return stock.count(entry.getKey()) >= deficit;
        });
    }

    private static Component message(String key) {
        return Component.translatable("message.bibliocraft.recipe_book." + key);
    }

    private static final class Stock {
        private final Player player;
        private final int[] remaining;

        private Stock(Player player, ItemStack excluded) {
            this.player = player;
            var items = player.getInventory().items;
            remaining = new int[items.size()];
            for (int slot = 0; slot < remaining.length; slot++) {
                ItemStack item = items.get(slot);
                remaining[slot] = item == excluded && !excluded.isEmpty() ? -1 : item.getCount();
            }
        }

        private int count(ItemStack needle) {
            int total = 0;
            for (int slot = 0; slot < remaining.length; slot++) {
                if (remaining[slot] > 0 && same(needle, player.getInventory().items.get(slot))) {
                    total += remaining[slot];
                }
            }
            return total;
        }

        private boolean take(ItemStack needle, int requested) {
            for (int slot = 0; slot < remaining.length && requested > 0; slot++) {
                if (remaining[slot] > 0 && same(needle, player.getInventory().items.get(slot))) {
                    int amount = Math.min(requested, remaining[slot]);
                    remaining[slot] -= amount;
                    requested -= amount;
                }
            }
            return requested == 0;
        }

        private void commit(boolean sync) {
            var items = player.getInventory().items;
            for (int slot = 0; slot < remaining.length; slot++) {
                ItemStack stack = items.get(slot);
                if (remaining[slot] >= 0 && remaining[slot] != stack.getCount()) {
                    ItemStack updated = stack.copyWithCount(remaining[slot]);
                    items.set(slot, updated);
                    if (sync && player instanceof ServerPlayer serverPlayer && serverPlayer.connection != null) {
                        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, slot, updated));
                    }
                }
            }
            player.getInventory().setChanged();
        }
    }

    private static final class Grid extends SimpleContainer implements CraftingContainer {
        private Grid() {
            super(RecordedRecipe.GRID_SIZE);
        }

        @Override
        public int getWidth() {
            return 3;
        }

        @Override
        public int getHeight() {
            return 3;
        }

        @Override
        public List<ItemStack> getItems() {
            return java.util.stream.IntStream.range(0, getContainerSize()).mapToObj(this::getItem).toList();
        }
    }
}

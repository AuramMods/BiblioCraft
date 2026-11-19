package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.block.FancyWorkbenchBlock;
import art.arcane.bibliocraft.blockentity.BookcaseBlockEntity;
import art.arcane.bibliocraft.blockentity.FancyWorkbenchBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.crafting.RecipeBookCrafting;
import art.arcane.bibliocraft.crafting.RecordedRecipe;
import art.arcane.bibliocraft.registry.ModItems;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Nullable;

public final class FancyWorkbenchMenu extends FurnitureInventoryMenu {
    public static final int SAVE_RECIPE = 0;
    public static final int LOAD_RECIPE = 1;
    public static final int RESULT = FancyWorkbenchBlockEntity.SLOT_COUNT;
    public static final int GRID_START = RESULT + 1;
    public static final int GRID_END = GRID_START + RecordedRecipe.GRID_SIZE;
    private final TransientCraftingContainer grid = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer result = new ResultContainer();
    private final Player viewer;
    private final @Nullable FancyWorkbenchBlockEntity bench;
    private final @Nullable Container left;
    private final @Nullable Container right;
    private final int playerStart;
    private boolean loading;

    public FancyWorkbenchMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, null, new SimpleContainer(FancyWorkbenchBlockEntity.SLOT_COUNT),
                buffer.readBoolean() ? new SimpleContainer(BookcaseBlockEntity.SLOT_COUNT) : null,
                buffer.readBoolean() ? new SimpleContainer(BookcaseBlockEntity.SLOT_COUNT) : null);
    }

    public FancyWorkbenchMenu(int id, Inventory inventory, FancyWorkbenchBlockEntity bench,
                              @Nullable BookcaseBlockEntity left, @Nullable BookcaseBlockEntity right) {
        this(id, inventory, bench, bench, left, right);
    }

    private FancyWorkbenchMenu(int id, Inventory inventory, @Nullable FancyWorkbenchBlockEntity bench,
                               Container contents, @Nullable Container left, @Nullable Container right) {
        super(ModMenus.FANCY_WORKBENCH.get(), id, contents, FancyWorkbenchBlockEntity.SLOT_COUNT);
        viewer = inventory.player;
        this.bench = bench;
        this.left = left;
        this.right = right;
        addSlot(new Slot(contents, 0, 8, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return FancyWorkbenchBlockEntity.acceptsBook(stack); }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int slot = 1; slot < FancyWorkbenchBlockEntity.SLOT_COUNT; slot++) {
            addSlot(new Slot(contents, slot, 17 + (slot - 1) * 18, 83) {
                @Override public boolean mayPlace(ItemStack stack) { return FancyWorkbenchBlockEntity.acceptsBook(stack); }
            });
        }
        addSlot(new ResultSlot(viewer, grid, result, 0, 124, 35));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new Slot(grid, row * 3 + column, 30 + column * 18, 17 + row * 18));
            }
        }
        addBookcase(left, -48);
        addBookcase(right, 187);
        playerStart = slots.size();
        addPlayerSlots(inventory, 8, 110);
    }

    private void addBookcase(@Nullable Container bookcase, int x) {
        if (bookcase == null) {
            return;
        }
        for (int column = 0; column < 2; column++) {
            for (int row = 0; row < 8; row++) {
                // Bookcases store upper and lower halves in separate rows.
                int physicalSlot = column * 8 + row;
                addSlot(new Slot(bookcase, physicalSlot, x + column * 21, 29 + row * 18) {
                    @Override public boolean mayPlace(ItemStack stack) { return BookcaseBlockEntity.acceptsBook(stack); }
                });
            }
        }
    }

    public boolean hasLeftBookcase() { return left != null; }
    public boolean hasRightBookcase() { return right != null; }

    @Override
    public boolean stillValid(Player player) {
        return bench == null || (viewer == player && BiblioConfig.COMMON.enableFancyWorkbench.get()
                && super.stillValid(player)
                && (left == null || FancyWorkbenchBlock.sideBookcase(bench, player, true) == left)
                && (right == null || FancyWorkbenchBlock.sideBookcase(bench, player, false) == right));
    }

    @Override
    public void slotsChanged(Container inventory) {
        if (inventory != grid || loading || viewer.level().isClientSide) {
            return;
        }
        var level = viewer.level();
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).orElse(null);
        ItemStack output = ItemStack.EMPTY;
        result.setRecipeUsed(null);
        if (recipe != null && RecipeBookCrafting.applyRecipe(result, viewer, recipe)) {
            ItemStack assembled = recipe.assemble(grid, level.registryAccess());
            if (assembled.isItemEnabled(level.enabledFeatures())) {
                output = assembled;
            }
        }
        result.setItem(0, output);
        if (viewer instanceof ServerPlayer serverPlayer && serverPlayer.connection != null) {
            setRemoteSlot(RESULT, output);
            serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), RESULT, output));
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (bench == null || !stillValid(player)) {
            return false;
        }
        ItemStack book = bench.getItem(FancyWorkbenchBlockEntity.ACTIVE_BOOK);
        if (button == SAVE_RECIPE && FancyWorkbenchBlockEntity.acceptsBook(book) && !RecordedRecipe.isSigned(book)) {
            slotsChanged(grid);
            var recipe = result.getRecipeUsed();
            ItemStack output = result.getItem(0);
            if (recipe != null && !output.isEmpty()) {
                bench.setItem(FancyWorkbenchBlockEntity.ACTIVE_BOOK, RecordedRecipe.create(grid.getItems(), output, recipe.getId()));
                return true;
            }
        }
        if (button == LOAD_RECIPE) {
            var recorded = RecordedRecipe.read(book);
            if (RecipeBookCrafting.isAvailable(recorded, player)) {
                var filled = RecipeBookCrafting.load(player, recorded, grid.getItems());
                if (filled.isPresent()) {
                    loading = true;
                    try {
                        for (int slot = 0; slot < grid.getContainerSize(); slot++) {
                            grid.setItem(slot, filled.get().get(slot));
                        }
                    } finally {
                        loading = false;
                    }
                    slotsChanged(grid);
                    return true;
                }
            }
            player.displayClientMessage(Component.translatable("message.bibliocraft.recipe_book.load_failed"), true);
        }
        return false;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != result && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = getSlot(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved;
        if (index == RESULT) {
            stack.getItem().onCraftedBy(stack, player.level(), player);
            moved = moveItemStackTo(stack, playerStart, slots.size(), true);
            if (moved) {
                slot.onQuickCraft(stack, original);
            }
        } else if (index == 0) {
            moved = moveItemStackTo(stack, 1, RESULT, false)
                    || moveItemStackTo(stack, playerStart, slots.size(), true);
        } else if (index < RESULT) {
            moved = moveItemStackTo(stack, 0, 1, false)
                    || moveItemStackTo(stack, playerStart, slots.size(), true);
        } else if (index < playerStart) {
            moved = moveItemStackTo(stack, playerStart, slots.size(), true);
        } else {
            moved = (stack.is(ModItems.RECIPE_BOOK.get()) && moveItemStackTo(stack, 0, RESULT, false))
                    || (stack.is(Items.BOOK) && moveItemStackTo(stack, 1, RESULT, false))
                    || (BookcaseBlockEntity.acceptsBook(stack) && moveItemStackTo(stack, GRID_END, playerStart, false));
            if (!moved) {
                int hotbar = slots.size() - Inventory.getSelectionSize();
                moved = index < hotbar ? moveItemStackTo(stack, hotbar, slots.size(), false)
                        : moveItemStackTo(stack, playerStart, hotbar, false);
            }
        }
        if (!moved || stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        if (index == RESULT && !stack.isEmpty()) {
            player.drop(stack, false);
        }
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            loading = true;
            try {
                clearContainer(player, grid);
            } finally {
                loading = false;
            }
        }
    }
}

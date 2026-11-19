package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.blockentity.DeskBlockEntity;
import art.arcane.bibliocraft.catalog.CatalogContents;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.crafting.RecipeBookCrafting;
import art.arcane.bibliocraft.crafting.RecordedRecipe;
import art.arcane.bibliocraft.network.ModNetwork;
import art.arcane.bibliocraft.registry.ModItems;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.world.SimpleMenuProvider;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class RecipeBookMenu extends AbstractContainerMenu {
    public static final int CRAFT = 0;
    private final Player viewer;
    private final @Nullable DeskBlockEntity desk;
    private final @Nullable InteractionHand hand;
    private final boolean portableCrafting;
    private final ItemStack original;
    private final ItemStack opened;
    private Component feedback = Component.empty();

    public RecipeBookMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory.player, null, null, ItemStack.EMPTY, buffer.readBoolean());
    }

    private RecipeBookMenu(int id, Player viewer, @Nullable DeskBlockEntity desk, @Nullable InteractionHand hand,
                           ItemStack original, boolean portableCrafting) {
        super(ModMenus.RECIPE_BOOK.get(), id);
        this.viewer = viewer;
        this.desk = desk;
        this.hand = hand;
        this.portableCrafting = portableCrafting;
        this.original = original;
        opened = original.copy();
        // A regular menu slot sends the full book through normal inventory synchronization.
        addSlot(new Slot(new SimpleContainer(opened), 0, -1000, -1000) {
            @Override public boolean mayPickup(Player player) { return false; }
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean isActive() { return false; }
        });
    }

    public static void open(ServerPlayer player, @Nullable DeskBlockEntity desk, @Nullable InteractionHand hand,
                            ItemStack stack) {
        boolean portable = hand != null && BiblioConfig.COMMON.enableRecipeBookCrafting.get();
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (id, inventory, viewer) -> new RecipeBookMenu(id, viewer, desk, hand, stack, portable),
                Component.translatable("item.bibliocraft.recipe_book")), buffer -> buffer.writeBoolean(portable));
    }

    public ItemStack book() {
        return getSlot(0).getItem();
    }

    public boolean portableCrafting() {
        return portableCrafting;
    }

    public Component feedback() {
        return feedback;
    }

    public void receiveFeedback(Component feedback) {
        this.feedback = feedback;
    }

    @Override
    public boolean stillValid(Player player) {
        if (player != viewer || !player.isAlive() || !BiblioConfig.COMMON.enableFancyWorkbench.get()) {
            return false;
        }
        if (player.level().isClientSide) {
            return true;
        }
        if (desk != null) {
            return BiblioConfig.COMMON.enableWritingdesk.get() && desk.stillValid(player)
                    && desk.getItem(DeskBlockEntity.OPEN_BOOK) == original && ItemStack.matches(opened, original);
        }
        return hand != null && player.getItemInHand(hand) == original && ItemStack.matches(opened, original);
    }

    public boolean annotate(Player player, List<String> lines, boolean sign) {
        if (!stillValid(player) || !original.is(ModItems.RECIPE_BOOK.get()) || RecordedRecipe.isSigned(original)
                || lines.size() != RecordedRecipe.TEXT_LINES) {
            return false;
        }
        var expected = new ListTag();
        for (String line : lines) {
            String sanitized = CatalogContents.bounded(SharedConstants.filterText(line), RecordedRecipe.LINE_LENGTH);
            expected.add(StringTag.valueOf(sanitized));
        }
        var edited = original.copy();
        var tag = edited.getOrCreateTag();
        tag.put("text", expected);
        tag.putBoolean("edited", true);
        if (sign) {
            tag.putBoolean("signed", true);
            tag.putString("author", player.getGameProfile().getName());
        }
        if (desk != null) {
            desk.setItem(DeskBlockEntity.OPEN_BOOK, edited);
        } else if (hand != null) {
            player.setItemInHand(hand, edited);
            player.getInventory().setChanged();
        }
        return true;
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (button != CRAFT || !portableCrafting || hand == null || !stillValid(player)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        Component message = RecipeBookCrafting.craft(player, original);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                new ModNetwork.RecipeTextPacket(containerId, message));
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }
}

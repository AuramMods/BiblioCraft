package art.arcane.bibliocraft.blockentity;

import art.arcane.bibliocraft.book.PrintingLibrary;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.item.PrintingPlateItem;
import art.arcane.bibliocraft.map.AtlasContents;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import art.arcane.bibliocraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class TypesettingTableBlockEntity extends FurnitureInventoryBlockEntity {
    public static final int SOURCE = 0;
    public static final int CHASES = 1;
    public static final int RESULT = 2;
    public static final ModelProperty<Integer> VISIBLE_PARTS = new ModelProperty<>();
    @Nullable private UUID selected;
    @Nullable private UUID savedSource;
    private int lastPower;
    private int sampleTicks;
    private int visibleParts;

    public TypesettingTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TYPESETTING_TABLE.get(), pos, state, 3);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SOURCE -> PrintingLibrary.isPublishable(stack) || stack.is(ModItems.PRINT_PLATE.get())
                    || BiblioConfig.COMMON.enableAtlas.get() && stack.is(ModItems.ATLAS_BOOK.get())
                    || stack.is(Items.ENCHANTED_BOOK) && !EnchantedBookItem.getEnchantments(stack).isEmpty();
            case CHASES -> stack.is(ModItems.BIBLIO_CHASE.get());
            default -> false;
        };
    }

    @Override
    protected int getSlotLimit(int slot) {
        return slot == CHASES ? 64 : 1;
    }

    @Override
    public void setChanged() {
        if (getItem(SOURCE).isEmpty()) {
            savedSource = null;
        }
        super.setChanged();
    }

    public void select(@Nullable UUID id) {
        selected = id;
        setChanged();
    }

    public @Nullable UUID selected() {
        return selected;
    }

    public void tick(ServerLevel server) {
        if (++sampleTicks < 3) {
            return;
        }
        sampleTicks = 0;
        int power = server.getBestNeighborSignal(worldPosition);
        if (power == lastPower) {
            return;
        }
        boolean rising = power > lastPower;
        lastPower = power;
        if (rising && BiblioConfig.COMMON.enablePrintpressTypeMachine.get() && hasSupplies()) {
            var book = PrintingLibrary.get(server).find(selected);
            if (book != null) {
                stamp(PrintingPlateItem.forBook(book));
            }
        }
        setChanged();
    }

    public boolean processSource(Player player) {
        if (!(level instanceof ServerLevel server) || !stillValid(player)
                || !BiblioConfig.COMMON.enablePrintpressTypeMachine.get()) {
            return false;
        }
        ItemStack source = getItem(SOURCE);
        if (source.is(ModItems.PRINT_PLATE.get())) {
            ItemStack chases = getItem(CHASES);
            ItemStack chase = new ItemStack(ModItems.BIBLIO_CHASE.get());
            setItem(SOURCE, chases.getCount() < chase.getMaxStackSize() ? ItemStack.EMPTY : chase);
            if (chases.getCount() < chase.getMaxStackSize()) {
                setItem(CHASES, chase.copyWithCount(chases.getCount() + 1));
            }
            return true;
        }
        if (source.is(Items.ENCHANTED_BOOK)) {
            int cost = enchantmentCost(source);
            if (!hasSupplies() || EnchantedBookItem.getEnchantments(source).isEmpty()) {
                return false;
            }
            if (!player.isCreative() && player.experienceLevel < cost) {
                player.displayClientMessage(Component.translatable("message.bibliocraft.typesetting.levels", cost), true);
                return false;
            }
            stamp(PrintingPlateItem.forEnchantments(source));
            setItem(SOURCE, ItemStack.EMPTY);
            if (!player.isCreative()) {
                player.giveExperienceLevels(-cost);
            }
            return true;
        }
        if (source.is(ModItems.ATLAS_BOOK.get()) && BiblioConfig.COMMON.enableAtlas.get()) {
            int sheets = new AtlasContents(source).filledMapCount();
            if (!hasSupplies() || sheets == 0) {
                return false;
            }
            int cost = atlasCost(sheets);
            if (!player.isCreative() && player.experienceLevel < cost) {
                player.displayClientMessage(Component.translatable("message.bibliocraft.typesetting.levels", cost), true);
                return false;
            }
            stamp(PrintingPlateItem.forAtlas(source));
            if (!player.isCreative()) {
                player.giveExperienceLevels(-cost);
            }
            return true;
        }
        var book = PrintingLibrary.get(server).publish(source, player);
        if (book == null) {
            return false;
        }
        savedSource = book.id();
        setChanged();
        return true;
    }

    private boolean hasSupplies() {
        return !getItem(CHASES).isEmpty() && getItem(RESULT).isEmpty();
    }

    private void stamp(ItemStack plate) {
        removeItem(CHASES, 1);
        setItem(RESULT, plate);
    }

    public static int enchantmentCost(ItemStack book) {
        var stored = EnchantedBookItem.getEnchantments(book);
        double total = 0;
        for (int i = 0; i < stored.size(); i++) {
            CompoundTag entry = stored.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(entry.getString("id"));
            var enchantment = id == null ? null : ForgeRegistries.ENCHANTMENTS.getValue(id);
            int maxLevel = enchantment == null ? 1 : enchantment.getMaxLevel();
            int effective = Math.max(0, entry.getShort("lvl")) + Math.max(0, 5 - maxLevel);
            total += (effective * (double) effective + 15) * (i == 0 ? 1 : 0.5 / i);
        }
        return (int) (total * BiblioConfig.COMMON.enchantmentMultiplyer.get() / 10);
    }

    public int sourceCost() {
        ItemStack source = getItem(SOURCE);
        if (source.is(Items.ENCHANTED_BOOK)) {
            return enchantmentCost(source);
        }
        if (source.is(ModItems.ATLAS_BOOK.get()) && BiblioConfig.COMMON.enableAtlas.get()) {
            return atlasCost(new AtlasContents(source).filledMapCount());
        }
        return 0;
    }

    private static int atlasCost(int sheets) {
        int level = 0;
        int xp = 7;
        // The original charges the level index of the bar containing 32 XP per sheet.
        while (xp < sheets * 32) {
            level++;
            xp += level < 16 ? 2 * level + 7 : 5 * level - 38;
        }
        return level;
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(VISIBLE_PARTS, visibleParts).build();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        selected = tag.hasUUID("SelectedBook") ? tag.getUUID("SelectedBook") : null;
        savedSource = tag.hasUUID("SavedSource") ? tag.getUUID("SavedSource") : null;
        lastPower = tag.getInt("LastPower");
        int mask = tag.getInt("VisibleParts");
        if (mask != visibleParts) {
            visibleParts = mask;
            refreshModel();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (selected != null) {
            tag.putUUID("SelectedBook", selected);
        }
        if (savedSource != null) {
            tag.putUUID("SavedSource", savedSource);
        }
        tag.putInt("LastPower", lastPower);
    }

    @Override
    public CompoundTag getUpdateTag() {
        var tag = new CompoundTag();
        saveFurnitureData(tag);
        ItemStack source = getItem(SOURCE);
        int mask = source.isEmpty() ? 0 : source.is(ModItems.PRINT_PLATE.get()) ? 1 << 7
                : source.is(Items.ENCHANTED_BOOK) ? 1 << 2 : savedSource == null ? 1 : 1 << 1;
        int chaseLayers = (getItem(CHASES).getCount() + 15) / 16;
        mask |= ((1 << chaseLayers) - 1) << 3;
        if (!getItem(RESULT).isEmpty()) {
            mask |= 1 << 8;
        }
        tag.putInt("VisibleParts", mask);
        return tag;
    }
}

package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.blockentity.DisplayCaseBlockEntity;
import art.arcane.bibliocraft.blockentity.FramedChestBlockEntity;
import art.arcane.bibliocraft.blockentity.FurnitureBlockEntity;
import art.arcane.bibliocraft.blockentity.FurnitureInventoryBlockEntity;
import art.arcane.bibliocraft.blockentity.FurniturePanelerBlockEntity;
import art.arcane.bibliocraft.blockentity.TypewriterBlockEntity;
import art.arcane.bibliocraft.blockentity.TypesettingTableBlockEntity;
import art.arcane.bibliocraft.book.PrintingLibrary;
import art.arcane.bibliocraft.catalog.CatalogContents;
import art.arcane.bibliocraft.enchantment.ReadingEnchantment;
import art.arcane.bibliocraft.menu.FurniturePanelerMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class FurnitureReading {
    private static final int NO_SLOT = -1;
    private static final int FIRST_ITEM = -2;
    private static final int CHEST_LABEL = -3;
    private static final int TYPEWRITER_ITEM = -4;
    private static final int SELECTED_TITLE = -5;
    private static final int MAX_LINES = 10;
    private static final int TEXT_LENGTH = 128;

    private FurnitureReading() {}

    public static @Nullable Target target(BlockGetter level, BlockHitResult hit) {
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        var state = level.getBlockState(pos);
        var block = state.getBlock();
        Direction side = hit.getDirection();
        var point = hit.getLocation();
        int slot = NO_SLOT;
        if (block instanceof ShelfBlock && side == state.getValue(ShelfBlock.FACING).getClockWise()) {
            slot = ShelfBlock.slotAt(pos, point, side);
        } else if (block instanceof BookcaseBlock && side == BookcaseBlock.opening(state)) {
            slot = BookcaseBlock.slotAt(pos, point, side);
        } else if (block instanceof RackBlock rack) {
            slot = rack.readingSlot(state, hit);
        } else if (block instanceof FancyWorkbenchBlock) {
            slot = FancyWorkbenchBlock.storageSlot(state, pos, hit);
        } else if ((block instanceof DeskBlock || block instanceof TableBlock) && side == Direction.UP) {
            slot = 0;
        } else if (block instanceof SwordPedestalBlock) {
            slot = 0;
        } else if (block instanceof DisplayCaseBlock) {
            slot = DisplayCaseBlockEntity.DISPLAY_SLOT;
        } else if (block instanceof TypewriterBlock) {
            slot = TYPEWRITER_ITEM;
        } else if (block instanceof CookieJarBlock || block instanceof DinnerPlateBlock) {
            slot = FIRST_ITEM;
        } else if (block instanceof LabelBlock) {
            double across = 1 - HorizontalFacingEntityBlock.northHit(state, hit).z;
            slot = across < 0.42 ? 1 : across < 0.56 ? 0 : 2;
        } else if (block instanceof FramedChestBlock) {
            slot = CHEST_LABEL;
        } else if (block instanceof ArmorStandBlock) {
            int half = state.getValue(ArmorStandBlock.HALF) == DoubleBlockHalf.UPPER ? 0 : 2;
            slot = half + (point.y - pos.getY() > 0.5 ? 0 : 1);
        } else if (block instanceof DiscRackBlock) {
            slot = DiscRackBlock.slotAt(state, pos, point);
        } else if (block instanceof FurniturePanelerBlock && side == Direction.UP) {
            slot = FurniturePanelerBlock.topSlot(state, hit);
        } else if (block instanceof PrintingPressBlock) {
            slot = PrintingPressBlock.slotAt(state, hit);
        } else if (block instanceof TypesettingTableBlock) {
            slot = TypesettingTableBlock.slotAt(state, hit);
            if (slot == NO_SLOT) {
                slot = SELECTED_TITLE;
            }
        }
        return slot == NO_SLOT ? null : new Target(pos, slot);
    }

    public static List<Component> read(Player player, Target target) {
        if (!(player.level() instanceof ServerLevel level) || player.isSpectator()
                || !ReadingEnchantment.canRead(player.getItemBySlot(EquipmentSlot.HEAD))
                || !(FurnitureBlockEntity.findAt(level, target.pos()) instanceof FurnitureInventoryBlockEntity furniture)
                || furniture.isRemoved() || !furniture.canAccess(player)) {
            return List.of();
        }
        int slot = target.slot();
        var text = new ArrayList<Component>();
        if (furniture instanceof TypesettingTableBlockEntity table) {
            int cost = slot == TypesettingTableBlockEntity.SOURCE ? table.sourceCost() : 0;
            if (cost > 0) {
                text.add(Component.translatable("message.bibliocraft.typesetting.levels", cost)
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            } else {
                var selected = PrintingLibrary.get(level).find(table.selected());
                if (selected != null && selected.canRead(player)) {
                    text.add(Component.translatable("overlay.bibliocraft.reading.selected", selected.title()));
                }
            }
        }
        if (furniture instanceof FramedChestBlockEntity chest) {
            var other = FramedChestBlock.partner(level, target.pos(), chest.getBlockState());
            if (other != null && !other.canAccess(player)) {
                return List.of();
            }
            ItemStack label = chest.getLabel();
            int count = 0;
            for (int index = 0; index < chest.getContainerSize(); index++) {
                ItemStack item = chest.getItem(index);
                if (ItemStack.isSameItemSameTags(item, label)) {
                    count += item.getCount();
                }
            }
            addItem(text, label, count);
        } else {
            ItemStack stack = item(furniture, slot);
            addItem(text, stack, stack.getCount());
        }
        return text;
    }

    private static ItemStack item(FurnitureInventoryBlockEntity furniture, int slot) {
        if (slot == FIRST_ITEM) {
            for (int index = 0; index < furniture.getContainerSize(); index++) {
                if (!furniture.getItem(index).isEmpty()) {
                    return furniture.getItem(index);
                }
            }
        } else if (slot == TYPEWRITER_ITEM) {
            ItemStack book = furniture.getItem(TypewriterBlockEntity.BOOK);
            return book.isEmpty() ? furniture.getItem(TypewriterBlockEntity.PAPER) : book;
        } else if (furniture instanceof FurniturePanelerBlockEntity paneler && slot == FurniturePanelerMenu.RESULT) {
            return paneler.getResult();
        } else if (slot >= 0 && slot < furniture.getContainerSize()) {
            if (furniture.getBlockState().getBlock() instanceof LabelBlock
                    && furniture.getItem(1).isEmpty() && furniture.getItem(2).isEmpty()) {
                slot = 0;
            }
            return furniture.getItem(slot);
        }
        return ItemStack.EMPTY;
    }

    private static void addItem(List<Component> text, ItemStack stack, int count) {
        if (stack.isEmpty()) {
            return;
        }
        Component name = stack.hasCustomHoverName()
                ? Component.literal(CatalogContents.bounded(stack.getHoverName().getString(), TEXT_LENGTH))
                : stack.getHoverName();
        text.add(count == 1 ? name : Component.translatable("overlay.bibliocraft.reading.stack", count, name));
        if (stack.getItem() instanceof RecordItem record) {
            text.add(record.getDisplayName());
        }
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            addBookDetails(text, tag, name);
        }
        EnchantmentHelper.getEnchantments(stack).forEach((enchantment, level) -> {
            if (text.size() < MAX_LINES) {
                text.add(enchantment.getFullname(level).copy().withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        });
    }

    private static void addBookDetails(List<Component> text, CompoundTag tag, Component name) {
        String title = tag.getString("bookName");
        if (title.isEmpty()) {
            title = tag.getString("title");
        }
        if (title.isEmpty()) {
            title = tag.getString("Title");
        }
        if (!title.isEmpty() && !title.equals(name.getString())) {
            text.add(Component.literal(CatalogContents.bounded(title, TEXT_LENGTH)));
        }
        String author = tag.getString("author");
        if (author.isEmpty()) {
            author = tag.getString("Author");
        }
        if (!author.isEmpty()) {
            text.add(Component.translatable("overlay.bibliocraft.reading.author",
                    CatalogContents.bounded(author, TEXT_LENGTH)));
        }
    }

    public record Target(BlockPos pos, int slot) {}
}

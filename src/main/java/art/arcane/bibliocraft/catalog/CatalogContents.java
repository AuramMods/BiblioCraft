package art.arcane.bibliocraft.catalog;

import art.arcane.bibliocraft.blockentity.FurnitureBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CatalogContents {
    private static final String INVENTORIES = "inventoryList";
    public static final int BATCH_SIZE = 32;
    public static final int NAME_LENGTH = 256;
    public static final int TITLE_LENGTH = 24;

    private CatalogContents() {}

    public static Set<GlobalPos> locations(ItemStack catalog, ResourceKey<Level> currentDimension) {
        Set<GlobalPos> result = new LinkedHashSet<>();
        var tag = catalog.getTag();
        if (tag == null) {
            return result;
        }
        ListTag list = tag.getList(INVENTORIES, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            var dimension = currentDimension;
            if (entry.contains("dimension", Tag.TAG_STRING)) {
                ResourceLocation id = ResourceLocation.tryParse(entry.getString("dimension"));
                if (id == null) {
                    continue;
                }
                dimension = ResourceKey.create(Registries.DIMENSION, id);
            }
            if (entry.contains("x", Tag.TAG_ANY_NUMERIC) && entry.contains("y", Tag.TAG_ANY_NUMERIC)
                    && entry.contains("z", Tag.TAG_ANY_NUMERIC)) {
                var pos = new BlockPos(entry.getInt("x"), entry.getInt("y"), entry.getInt("z"));
                result.add(GlobalPos.of(dimension, pos));
            }
        }
        return result;
    }

    public static boolean toggle(ItemStack catalog, GlobalPos location) {
        Set<GlobalPos> locations = locations(catalog, location.dimension());
        boolean added = locations.add(location);
        if (!added) {
            locations.remove(location);
        }
        ListTag serialized = new ListTag();
        for (GlobalPos target : locations) {
            CompoundTag tag = new CompoundTag();
            tag.putString("dimension", target.dimension().location().toString());
            tag.putInt("x", target.pos().getX());
            tag.putInt("y", target.pos().getY());
            tag.putInt("z", target.pos().getZ());
            serialized.add(tag);
        }
        if (serialized.isEmpty()) {
            catalog.removeTagKey(INVENTORIES);
        } else {
            catalog.getOrCreateTag().put(INVENTORIES, serialized);
        }
        return added;
    }

    public static @Nullable BlockEntity findContainer(Level level, BlockPos clicked) {
        var furniture = FurnitureBlockEntity.findAt(level, clicked);
        var blockEntity = furniture == null ? level.getBlockEntity(clicked) : furniture;
        return inventory(blockEntity) == null ? null : blockEntity;
    }

    public static Name nameOf(BlockEntity blockEntity) {
        Component name = blockEntity instanceof Nameable named ? named.getDisplayName()
                : blockEntity.getBlockState().getBlock().getName();
        if (name.getContents() instanceof TranslatableContents translated && translated.getArgs().length == 0
                && name.getSiblings().isEmpty() && translated.getKey().length() <= NAME_LENGTH) {
            return new Name(translated.getKey(), true);
        }
        return new Name(bounded(name.getString(), NAME_LENGTH), false);
    }

    public static boolean canAccess(BlockEntity blockEntity, Player player) {
        return (!(blockEntity instanceof FurnitureBlockEntity furniture) || furniture.canAccess(player))
                && (!(blockEntity instanceof BaseContainerBlockEntity container) || container.canOpen(player));
    }

    private static boolean canRead(BlockEntity blockEntity, Player player) {
        // An unopened structure chest needs a normal player interaction to generate its loot.
        return canAccess(blockEntity, player) && (!(blockEntity instanceof RandomizableContainerBlockEntity randomizable)
                || !randomizable.saveWithoutMetadata().contains("LootTable", Tag.TAG_STRING));
    }

    private static @Nullable IItemHandler inventory(@Nullable BlockEntity blockEntity) {
        if (blockEntity == null || blockEntity.isRemoved()) {
            return null;
        }
        // The direct container keeps each physical half of a vanilla double chest separate.
        if (blockEntity instanceof Container container) {
            return new InvWrapper(container);
        }
        return blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
    }

    private static @Nullable BlockEntity loadedContainer(ServerLevel world, GlobalPos location) {
        ServerLevel level = world.getServer().getLevel(location.dimension());
        if (level == null) {
            return null;
        }
        BlockPos pos = location.pos();
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null ? null : chunk.getBlockEntity(pos);
    }

    public static Snapshot scan(ServerLevel level, Player player, ItemStack catalog) {
        Set<GlobalPos> locations = locations(catalog, level.dimension());
        List<Source> sources = new ArrayList<>();
        int unavailable = 0;
        for (GlobalPos location : locations) {
            BlockEntity blockEntity = loadedContainer(level, location);
            IItemHandler inventory = inventory(blockEntity);
            if (inventory == null || !canRead(blockEntity, player)) {
                unavailable++;
                continue;
            }
            Map<String, Long> counts = new LinkedHashMap<>();
            int slots = inventory.getSlots();
            for (int slot = 0; slot < slots; slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    counts.merge(bounded(stack.getDescriptionId(), NAME_LENGTH), (long) stack.getCount(), Long::sum);
                }
            }
            sources.add(new Source(location, nameOf(blockEntity), counts));
        }
        return new Snapshot(sources, unavailable);
    }

    public static boolean canAssign(ServerLevel level, Player player, GlobalPos location) {
        BlockEntity blockEntity = loadedContainer(level, location);
        return inventory(blockEntity) != null && canRead(blockEntity, player);
    }

    public static String bounded(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        int end = maxLength;
        if (Character.isHighSurrogate(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(0, end);
    }

    public record Name(String text, boolean translatable) {
        public Component component() {
            return translatable ? Component.translatable(text) : Component.literal(text);
        }

        public void write(FriendlyByteBuf buffer) {
            buffer.writeUtf(text, NAME_LENGTH);
            buffer.writeBoolean(translatable);
        }

        public static Name read(FriendlyByteBuf buffer) {
            return new Name(buffer.readUtf(NAME_LENGTH), buffer.readBoolean());
        }
    }

    public record Source(GlobalPos location, Name name, Map<String, Long> counts) {
        public Source {
            counts = Map.copyOf(counts);
        }

        public void write(FriendlyByteBuf buffer) {
            buffer.writeGlobalPos(location);
            name.write(buffer);
            buffer.writeMap(counts, (out, key) -> out.writeUtf(key, NAME_LENGTH), FriendlyByteBuf::writeVarLong);
        }

        public static Source read(FriendlyByteBuf buffer) {
            return new Source(buffer.readGlobalPos(), Name.read(buffer),
                    buffer.readMap(FriendlyByteBuf.limitValue(LinkedHashMap::new, BATCH_SIZE),
                            in -> in.readUtf(NAME_LENGTH), FriendlyByteBuf::readVarLong));
        }

        public List<Source> batches() {
            List<Source> result = new ArrayList<>();
            Map<String, Long> batch = new LinkedHashMap<>();
            counts.forEach((key, count) -> {
                batch.put(key, count);
                if (batch.size() == BATCH_SIZE) {
                    result.add(new Source(location, name, batch));
                    batch.clear();
                }
            });
            if (!batch.isEmpty()) {
                result.add(new Source(location, name, batch));
            }
            return result;
        }
    }

    public record Snapshot(List<Source> sources, int unavailable) {
        public List<Entry> entries() {
            Map<String, List<Detail>> entries = new LinkedHashMap<>();
            for (Source source : sources) {
                source.counts().forEach((key, count) -> entries.computeIfAbsent(key, ignored -> new ArrayList<>())
                        .add(new Detail(source.location(), source.name(), count)));
            }
            return entries.entrySet().stream().map(entry -> {
                var details = entry.getValue().stream().sorted(Comparator.comparingLong(Detail::count).reversed()).toList();
                return new Entry(entry.getKey(), details.stream().mapToLong(Detail::count).sum(), details);
            }).toList();
        }
    }

    public record Detail(GlobalPos location, Name name, long count) {}

    public record Entry(String item, long count, List<Detail> details) {}
}

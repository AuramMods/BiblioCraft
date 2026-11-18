package art.arcane.bibliocraft.blockentity;

import art.arcane.bibliocraft.block.TypewriterBlock;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import art.arcane.bibliocraft.registry.ModSounds;
import art.arcane.bibliocraft.storygen.StoryBook;
import art.arcane.bibliocraft.storygen.StoryVoice;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class TypewriterBlockEntity extends FurnitureInventoryBlockEntity {
    public static final int PAPER = 0;
    public static final int BOOK = 1;
    public static final int LAST_LINE = 15;
    public static final int PAGE_COUNT = 8;
    public static final int AUTOMATIC_INTERVAL = 101;
    private int progress;
    private int ticks;
    private String author = "";
    private StoryVoice voice = StoryVoice.STANDARD;
    private boolean hasBook;

    public TypewriterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TYPEWRITER.get(), pos, state, 2);
    }

    public boolean hasPaper() {
        return !getItem(PAPER).isEmpty();
    }

    public int getProgress() {
        return progress;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == PAPER && stack.is(Items.PAPER);
    }

    @Override
    protected int getSlotLimit(int slot) {
        return slot == BOOK ? 1 : super.getSlotLimit(slot);
    }

    public void typeByHand(Player player) {
        if (readyToType()) {
            type(player.getGameProfile().getName(), StoryVoice.STANDARD, true);
        }
    }

    public void tick() {
        if (++ticks < AUTOMATIC_INTERVAL) {
            return;
        }
        ticks = 0;
        if (!(level instanceof ServerLevel server) || !BiblioConfig.COMMON.enableTypewriter.get() || !readyToType()) {
            return;
        }
        if (progress == LAST_LINE && !author.isEmpty()) {
            finishBook(server);
            return;
        }
        var front = worldPosition.relative(TypewriterBlock.front(getBlockState()));
        var writingArea = new AABB(front).inflate(0, 1, 0);
        var writers = server.getEntitiesOfClass(PathfinderMob.class, writingArea,
                entity -> entity.isAlive() && entity.hasCustomName() && !entity.getName().getString().isBlank());
        if (!writers.isEmpty()) {
            PathfinderMob writer = writers.get(0);
            type(writer.getName().getString(64), StoryVoice.forMob(writer.getType()), false);
        }
    }

    private boolean readyToType() {
        return getItem(BOOK).isEmpty() && getItem(PAPER).is(Items.PAPER) && getItem(PAPER).getCount() >= PAGE_COUNT;
    }

    private void type(String name, StoryVoice style, boolean manual) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        author = name;
        voice = style;
        if (progress == LAST_LINE) {
            finishBook(server);
        } else {
            progress++;
            setChanged();
            playSound((manual ? ModSounds.TYPINGSINGLE : ModSounds.TYPING).get());
        }
    }

    private void finishBook(ServerLevel server) {
        ItemStack book = StoryBook.create(server.random, author, voice);
        removeItem(PAPER, PAGE_COUNT);
        setItem(BOOK, book);
        playSound(ModSounds.ENDBELL.get());
    }

    public void playSound(SoundEvent sound) {
        if (level instanceof ServerLevel server) {
            server.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 0.7F, 1);
        }
    }

    @Override
    public void setChanged() {
        boolean output = !getItem(BOOK).isEmpty();
        if (hasBook && !output) {
            progress = 0;
            author = "";
            voice = StoryVoice.STANDARD;
        }
        hasBook = output;
        super.setChanged();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress = Mth.clamp(tag.getInt("bookWriteCount"), 0, LAST_LINE);
        author = tag.getString("Author");
        voice = StoryVoice.fromName(tag.getString("Voice"));
        hasBook = !getItem(BOOK).isEmpty();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("bookWriteCount", progress);
        tag.putString("Author", author);
        tag.putString("Voice", voice.name());
    }
}

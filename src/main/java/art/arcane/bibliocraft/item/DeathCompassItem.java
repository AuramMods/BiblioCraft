package art.arcane.bibliocraft.item;

import art.arcane.bibliocraft.enchantment.DeathCompassEnchantment;
import art.arcane.bibliocraft.registry.ModItems;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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

import java.util.List;

public final class DeathCompassItem extends Item {
    private static final String PENDING = "BiblioCraftPendingDeathCompass";
    private static final String DEATH_NAME = "Location of Death";

    public DeathCompassItem() {
        super(new Properties().stacksTo(1));
    }

    public static @Nullable WaypointCompassItem.Target target(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains("XCoord", Tag.TAG_ANY_NUMERIC) && tag.contains("ZCoord", Tag.TAG_ANY_NUMERIC)
                ? WaypointCompassItem.getTarget(stack) : null;
    }

    public static void recordDeath(Player player) {
        if (!DeathCompassEnchantment.enabled()) {
            return;
        }
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (DeathCompassEnchantment.tracksDeath(inventory.getItem(slot))) {
                ItemStack compass = new ItemStack(ModItems.DEATH_COMPASS.get());
                var location = GlobalPos.of(player.level().dimension(), player.blockPosition());
                WaypointCompassItem.setTarget(compass, WaypointCompassItem.at(location, DEATH_NAME));
                compass.getOrCreateTag().putInt("DeathY", location.pos().getY());
                var persisted = player.getPersistentData();
                var data = persisted.getCompound(Player.PERSISTED_NBT_TAG);
                data.put(PENDING, compass.save(new CompoundTag()));
                persisted.put(Player.PERSISTED_NBT_TAG, data);
                return;
            }
        }
    }

    public static void deliver(Player player) {
        if (!player.isAlive() || !DeathCompassEnchantment.enabled()) {
            return;
        }
        var data = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        ItemStack compass = ItemStack.of(data.getCompound(PENDING));
        if (!compass.is(ModItems.DEATH_COMPASS.get())) {
            return;
        }
        int free = player.getInventory().getFreeSlot();
        if (free >= 0) {
            player.getInventory().setItem(free, compass);
            player.getInventory().setChanged();
        } else if (player.drop(compass, false) == null) {
            return;
        }
        data.remove(PENDING);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var compass = player.getItemInHand(hand);
        if (!DeathCompassEnchantment.enabled()) {
            return InteractionResultHolder.pass(compass);
        }
        if (player instanceof ServerPlayer) {
            player.displayClientMessage(description(compass), false);
        }
        return InteractionResultHolder.sidedSuccess(compass, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(description(stack));
    }

    private static Component description(ItemStack stack) {
        var target = target(stack);
        if (target == null) {
            return Component.translatable("item.bibliocraft.death_compass.unbound");
        }
        var tag = stack.getTag();
        if (tag != null && tag.contains("DeathY", Tag.TAG_ANY_NUMERIC) && target.dimension() != null) {
            return Component.translatable("item.bibliocraft.death_compass.location", target.x(), tag.getInt("DeathY"),
                    target.z(), target.dimension().toString());
        }
        return target.description();
    }
}

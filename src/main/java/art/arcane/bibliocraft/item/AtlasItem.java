package art.arcane.bibliocraft.item;

import art.arcane.bibliocraft.blockentity.MapFrameBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.enchantment.DeathCompassEnchantment;
import art.arcane.bibliocraft.map.AtlasContents;
import art.arcane.bibliocraft.menu.AtlasMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;

public final class AtlasItem extends Item {
    public AtlasItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return DeathCompassEnchantment.enabled();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!BiblioConfig.COMMON.enableAtlas.get()) {
            return InteractionResultHolder.pass(stack);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            AtlasMenu.open(serverPlayer, hand, null);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static InteractionResult useFrame(Player player, InteractionHand hand, MapFrameBlockEntity frame) {
        if (!BiblioConfig.COMMON.enableAtlas.get()) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            var contents = new AtlasContents(player.getItemInHand(hand));
            var frameMap = frame.getItem(MapFrameBlockEntity.MAP);
            if (frameMap.isEmpty()) {
                if (contents.mapData(player.level(), contents.selected()) == null) {
                    feedback(player, "no_selected_map");
                } else {
                    var copy = contents.copyOut();
                    if (!copy.isEmpty()) {
                        frame.setItem(MapFrameBlockEntity.MAP, copy);
                        feedback(player, "copied_to_frame");
                    } else {
                        feedback(player, "no_blank_map");
                    }
                }
            } else if (MapItem.getSavedData(frameMap, player.level()) != null) {
                int slot = contents.matchingMap(frameMap);
                if (slot >= 0) {
                    if (slot == contents.selected()) {
                        AtlasMenu.open(serverPlayer, hand, frame);
                    } else {
                        contents.select(slot);
                        feedback(player, "map_selected");
                    }
                } else {
                    feedback(player, contents.copyIn(frameMap) ? "copied_to_atlas" : "cannot_copy_in");
                }
            }
            player.getInventory().setChanged();
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    private static void feedback(Player player, String message) {
        player.displayClientMessage(Component.translatable("message.bibliocraft.atlas." + message), true);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(entity instanceof ServerPlayer player) || !BiblioConfig.COMMON.enableAtlas.get()) {
            return;
        }
        if (DeathCompassEnchantment.hasLegacyMarker(stack) && DeathCompassEnchantment.enabled()) {
            DeathCompassEnchantment.apply(stack);
            player.getInventory().setChanged();
        }
        if (!selected && player.getOffhandItem() != stack
                || level.getGameTime() % BiblioConfig.COMMON.mapUpdateRate.get() != 0) {
            return;
        }
        AtlasMenu menu = player.containerMenu instanceof AtlasMenu current && current.owns(stack, player) ? current : null;
        var contents = menu == null ? new AtlasContents(stack) : menu.contents();
        if ((menu == null || menu.mode() != AtlasMenu.Mode.TRANSFER) && contents.updateSelection(player, true)) {
            player.getInventory().setChanged();
            if (menu != null) {
                menu.refresh();
            }
        }
        sendMap(player, contents.selectedMap(), true);
    }

    public static void sendMap(ServerPlayer player, ItemStack stack, boolean updateTerrain) {
        if (!stack.is(Items.FILLED_MAP)) {
            return;
        }
        var map = MapItem.getSavedData(stack, player.level());
        Integer id = MapItem.getMapId(stack);
        if (map == null || id == null) {
            return;
        }
        map.getHoldingPlayer(player);
        if (updateTerrain && !map.locked) {
            ((MapItem) Items.FILLED_MAP).update(player.level(), player, map);
        }
        var packet = map.getUpdatePacket(id, player);
        if (packet != null) {
            player.connection.send(packet);
        }
    }

    @Override
    public int getEntityLifespan(ItemStack stack, Level level) {
        return 36000;
    }
}

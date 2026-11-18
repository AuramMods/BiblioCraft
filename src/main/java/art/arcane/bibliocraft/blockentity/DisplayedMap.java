package art.arcane.bibliocraft.blockentity;

import art.arcane.bibliocraft.config.BiblioConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.ChunkPos;

final class DisplayedMap {
    private DisplayedMap() {}

    static void tick(ServerLevel level, BlockPos pos, ItemStack stack) {
        if (!stack.is(Items.FILLED_MAP) || level.getGameTime() % BiblioConfig.COMMON.mapUpdateRate.get() != 0) {
            return;
        }
        var data = MapItem.getSavedData(stack, level);
        Integer id = MapItem.getMapId(stack);
        if (data == null || id == null) {
            return;
        }
        for (var player : level.getChunkSource().chunkMap.getPlayers(new ChunkPos(pos), false)) {
            data.getHoldingPlayer(player);
            if (!data.locked) {
                ((MapItem) Items.FILLED_MAP).update(level, player, data);
            }
            var packet = data.getUpdatePacket(id, player);
            if (packet != null) {
                player.connection.send(packet);
            }
        }
    }
}

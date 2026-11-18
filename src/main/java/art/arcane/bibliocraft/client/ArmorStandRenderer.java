package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.block.ArmorStandBlock;
import art.arcane.bibliocraft.blockentity.ArmorStandBlockEntity;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

final class ArmorStandRenderer implements BlockEntityRenderer<ArmorStandBlockEntity> {
    private final EntityRenderDispatcher dispatcher;
    private RemotePlayer mannequin;

    ArmorStandRenderer(BlockEntityRendererProvider.Context context) {
        dispatcher = context.getEntityRenderer();
    }

    @Override
    public void render(ArmorStandBlockEntity stand, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        if (stand.isEmpty() || !(stand.getLevel() instanceof ClientLevel level)) {
            return;
        }
        if (mannequin == null || mannequin.level() != level) {
            mannequin = new RemotePlayer(level, new GameProfile(UUID.randomUUID(), "BiblioSteve")) {
                @Override
                public boolean isInvisibleTo(Player viewer) {
                    return true;
                }
            };
            mannequin.setInvisible(true);
        }
        float yaw = ArmorStandBlock.front(stand.getBlockState()).toYRot();
        var pos = stand.getBlockPos();
        mannequin.setPos(pos.getX() + 0.5, pos.getY() + 0.07, pos.getZ() + 0.5);
        mannequin.yBodyRotO = mannequin.yBodyRot = yaw;
        mannequin.yHeadRotO = mannequin.yHeadRot = yaw;
        for (int index = 0; index < stand.getContainerSize(); index++) {
            mannequin.getInventory().armor.set(ArmorStandBlockEntity.equipmentSlot(index).getIndex(), stand.getItem(index));
        }
        pose.pushPose();
        pose.translate(0.5, 0.07, 0.5);
        dispatcher.getRenderer(mannequin).render(mannequin, yaw, partialTick, pose, buffers, light);
        pose.popPose();
    }
}

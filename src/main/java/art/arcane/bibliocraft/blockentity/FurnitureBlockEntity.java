package art.arcane.bibliocraft.blockentity;

import art.arcane.bibliocraft.block.ArmorStandBlock;
import art.arcane.bibliocraft.block.FurnitureMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class FurnitureBlockEntity extends BlockEntity {
    public static final ModelProperty<FurnitureMaterial> MATERIAL = new ModelProperty<>();
    private FurnitureMaterial material = FurnitureMaterial.OAK;
    private UUID lockOwner;

    public FurnitureBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static @Nullable FurnitureBlockEntity findAt(BlockGetter level, BlockPos pos) {
        BlockPos base = ArmorStandBlock.basePos(level.getBlockState(pos), pos);
        if (level.getBlockEntity(base) instanceof FurnitureBlockEntity furniture
                && (base.equals(pos) || furniture instanceof ArmorStandBlockEntity)) {
            return furniture;
        }
        return null;
    }

    public FurnitureMaterial getMaterial() {
        return material;
    }

    public boolean isLocked() {
        return lockOwner != null;
    }

    public boolean canAccess(Player player) {
        return lockOwner == null || lockOwner.equals(player.getUUID());
    }

    public boolean toggleLock(Player player) {
        if (!canAccess(player)) {
            return false;
        }
        lockOwner = lockOwner == null ? player.getUUID() : null;
        super.setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
        return true;
    }

    public static InteractionResult denyAccess(Player player) {
        if (!player.level().isClientSide()) {
            player.displayClientMessage(Component.translatable("message.bibliocraft.lock.not_owner"), true);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide());
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(MATERIAL, material).build();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        lockOwner = tag.hasUUID("LockOwner") ? tag.getUUID("LockOwner") : null;
        FurnitureMaterial previous = material;
        material = FurnitureMaterial.fromTag(tag);
        if (!material.equals(previous)) {
            refreshModel();
        }
    }

    protected void refreshModel() {
        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        material.save(tag);
        if (lockOwner != null) {
            tag.putUUID("LockOwner", lockOwner);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}

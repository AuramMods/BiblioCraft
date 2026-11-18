package art.arcane.bibliocraft.blockentity;

import art.arcane.bibliocraft.block.FurnitureMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

public class FurnitureBlockEntity extends BlockEntity {
    public static final ModelProperty<FurnitureMaterial> MATERIAL = new ModelProperty<>();
    private FurnitureMaterial material = FurnitureMaterial.OAK;

    public FurnitureBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public FurnitureMaterial getMaterial() {
        return material;
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(MATERIAL, material).build();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        FurnitureMaterial previous = material;
        material = FurnitureMaterial.fromTag(tag);
        if (!material.equals(previous) && level != null && level.isClientSide) {
            requestModelDataUpdate();
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        material.save(tag);
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

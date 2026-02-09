package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.BiblioCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.ForgeRegistries;

public class PlaceholderEntityBlock extends StaticShapeBlock implements EntityBlock {
    private final ResourceLocation blockEntityId;

    public PlaceholderEntityBlock(Properties properties, VoxelShape shape, String blockEntityPath) {
        super(properties, shape);
        this.blockEntityId = ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, blockEntityPath);
    }

    public PlaceholderEntityBlock(Properties properties, VoxelShape shape, VoxelShape collisionShape, String blockEntityPath) {
        super(properties, shape, collisionShape);
        this.blockEntityId = ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, blockEntityPath);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        BlockEntityType<?> type = ForgeRegistries.BLOCK_ENTITY_TYPES.getValue(blockEntityId);
        if (type == null) {
            return null;
        }
        return type.create(pos, state);
    }
}

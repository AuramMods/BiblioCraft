package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.menu.PlaceholderMenu;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.network.NetworkHooks;

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

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (blockId == null) {
            return InteractionResult.PASS;
        }

        MenuType<PlaceholderMenu> menuType = ModMenus.getMenuForBlock(blockId);
        if (menuType == null) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            MenuProvider provider = new SimpleMenuProvider(
                    (containerId, inventory, menuPlayer) -> new PlaceholderMenu(menuType, containerId),
                    Component.translatable("menu." + blockId.getNamespace() + "." + blockId.getPath())
            );
            NetworkHooks.openScreen(serverPlayer, provider, pos);
        }

        return InteractionResult.CONSUME;
    }
}

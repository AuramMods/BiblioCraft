package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.blockentity.LabelBlockEntity;
import art.arcane.bibliocraft.menu.LabelMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.level.Level;

public final class LabelBlock extends SignDisplayBlock {
    public LabelBlock(Properties properties) {
        super(properties, box(0, 1.488, 4, 1, 6.48, 12), "label");
    }

    @Override
    protected MenuConstructor menuAt(Level level, BlockPos pos) {
        LabelBlockEntity label = (LabelBlockEntity) level.getBlockEntity(pos);
        return (id, inventory, player) -> label == null ? null : new LabelMenu(id, inventory, label);
    }
}

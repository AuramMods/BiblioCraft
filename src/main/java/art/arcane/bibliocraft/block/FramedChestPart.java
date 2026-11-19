package art.arcane.bibliocraft.block;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum FramedChestPart implements StringRepresentable {
    SINGLE, LEFT, RIGHT;

    public Direction partnerSide(Direction facing) {
        // The authored north chest opens east. Its left half joins toward the north.
        return this == LEFT ? facing : facing.getOpposite();
    }

    public FramedChestPart opposite() {
        return switch (this) {
            case SINGLE -> SINGLE;
            case LEFT -> RIGHT;
            case RIGHT -> LEFT;
        };
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}

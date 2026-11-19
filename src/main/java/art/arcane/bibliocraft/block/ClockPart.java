package art.arcane.bibliocraft.block;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum ClockPart implements StringRepresentable {
    WALL(null), LOWER(Direction.UP), UPPER(Direction.DOWN);

    @Nullable
    private final Direction partnerSide;

    ClockPart(@Nullable Direction partnerSide) {
        this.partnerSide = partnerSide;
    }

    public @Nullable Direction partnerSide() {
        return partnerSide;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}

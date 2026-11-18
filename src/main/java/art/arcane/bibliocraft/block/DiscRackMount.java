package art.arcane.bibliocraft.block;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.Vec3;

public enum DiscRackMount implements StringRepresentable {
    FLOOR("floor"),
    WALL("wall"),
    WALL_HORIZONTAL("wall_horizontal"),
    CEILING("ceiling");

    private final String name;

    DiscRackMount(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public boolean isWall() {
        return this == WALL || this == WALL_HORIZONTAL;
    }

    public Direction opening(Direction facing) {
        return switch (this) {
            case FLOOR -> Direction.UP;
            case CEILING -> Direction.DOWN;
            case WALL, WALL_HORIZONTAL -> facing.getClockWise();
        };
    }

    public double slotAxis(Vec3 hit) {
        return switch (this) {
            case FLOOR -> hit.x;
            case WALL -> 1 - hit.y;
            case WALL_HORIZONTAL -> hit.z;
            case CEILING -> 1 - hit.x;
        };
    }
}

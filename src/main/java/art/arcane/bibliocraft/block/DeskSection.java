package art.arcane.bibliocraft.block;

import net.minecraft.util.StringRepresentable;

public enum DeskSection implements StringRepresentable {
    SINGLE(false, false, "single"),
    LEFT(false, true, "left"),
    RIGHT(true, false, "right"),
    CENTER(true, true, "center");

    private final boolean left;
    private final boolean right;
    private final String name;

    DeskSection(boolean left, boolean right, String name) {
        this.left = left;
        this.right = right;
        this.name = name;
    }

    public boolean joinsLeft() {
        return left;
    }

    public boolean joinsRight() {
        return right;
    }

    public DeskSection withLeft(boolean connected) {
        return of(connected, right);
    }

    public DeskSection withRight(boolean connected) {
        return of(left, connected);
    }

    public DeskSection mirrored() {
        return of(right, left);
    }

    private static DeskSection of(boolean left, boolean right) {
        return left ? (right ? CENTER : RIGHT) : (right ? LEFT : SINGLE);
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}

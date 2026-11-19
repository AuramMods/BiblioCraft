package art.arcane.bibliocraft.painting;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;

public record PaintingSettings(int corner, int scale, int resolution, int aspect, int rotation,
                               int customWidth, int customHeight, boolean hidden) {
    public static final PaintingSettings DEFAULT = new PaintingSettings(0, 1, 0, 0, 0, 1, 1, false);

    public PaintingSettings {
        corner = Mth.clamp(corner, 0, 3);
        scale = Mth.clamp(scale, 1, 10);
        resolution = Mth.clamp(resolution, 0, 2);
        aspect = Mth.clamp(aspect, 0, 1);
        rotation = Math.floorMod(rotation, 4);
        customWidth = Mth.clamp(customWidth, 1, 32);
        customHeight = Mth.clamp(customHeight, 1, 32);
    }

    public PaintingSettings forArt(PaintingArt art, boolean borderless) {
        int newAspect = Math.min(aspect, art.aspectCount() - 1);
        return new PaintingSettings(corner, scale, Math.min(resolution, art.image(newAspect).textures().size() - 1),
                newAspect, rotation, customWidth, customHeight, borderless && hidden);
    }

    public PaintingSettings rotate() {
        return new PaintingSettings(corner, scale, resolution, aspect, rotation + 1, customWidth, customHeight, hidden);
    }

    public Footprint footprint(PaintingArt art, double inset) {
        var image = art.image(aspect);
        int width = (art.source() == PaintingArt.Source.CUSTOM ? customWidth : image.width()) * scale;
        int height = (art.source() == PaintingArt.Source.CUSTOM ? customHeight : image.height()) * scale;
        if ((rotation & 1) != 0) {
            int originalWidth = width;
            width = height;
            height = originalWidth;
        }
        float left = corner < 2 ? 0 : 1 - width;
        float bottom = corner == 1 || corner == 2 ? 1 - height : 0;
        return new Footprint(left + inset, bottom + inset, left + width - inset, bottom + height - inset);
    }

    public static PaintingSettings read(CompoundTag tag) {
        return new PaintingSettings(tag.getInt("masterCorner"), tag.getInt("paintingScale"),
                tag.getInt("paintingPixelRes"), tag.getInt("paintingAspectRatio"), tag.getInt("paintingRotation"),
                tag.getInt("customPaintingAspectX"), tag.getInt("customPaintingAspectY"), tag.getBoolean("hideFrame"));
    }

    public void save(CompoundTag tag) {
        tag.putInt("masterCorner", corner);
        tag.putInt("paintingScale", scale);
        tag.putInt("paintingPixelRes", resolution);
        tag.putInt("paintingAspectRatio", aspect);
        tag.putInt("paintingRotation", rotation);
        tag.putInt("customPaintingAspectX", customWidth);
        tag.putInt("customPaintingAspectY", customHeight);
        tag.putBoolean("hideFrame", hidden);
    }

    public static PaintingSettings read(FriendlyByteBuf buffer) {
        return new PaintingSettings(buffer.readUnsignedByte(), buffer.readUnsignedByte(), buffer.readUnsignedByte(),
                buffer.readUnsignedByte(), buffer.readUnsignedByte(), buffer.readUnsignedByte(), buffer.readUnsignedByte(),
                buffer.readBoolean());
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeByte(corner).writeByte(scale).writeByte(resolution).writeByte(aspect).writeByte(rotation)
                .writeByte(customWidth).writeByte(customHeight).writeBoolean(hidden);
    }

    public record Footprint(double left, double bottom, double right, double top) {}
}

package art.arcane.bibliocraft.painting;

import art.arcane.bibliocraft.BiblioCraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public record PaintingArt(Source source, String title, int width, int height) {
    public static final int MAX_TITLE_LENGTH = 192;
    public static final PaintingArt BLANK = new PaintingArt(Source.BIBLIOCRAFT, "", 1, 1);
    private static final List<Bundled> GALLERY = List.of(
            art("piano", versions("64painting01z", "128painting01z"),
                    versions(2, 1, "64painting01l", "128painting01l")),
            art("bowloffruit", versions("32painting02z", "64painting02z", "128painting02z"),
                    versions(2, 1, "32painting02l", "64painting02l", "128painting02l")),
            art("sunoffires", versions("32painting03z", "64painting03z", "128painting03z"),
                    versions(2, 1, "32painting03l", "64painting03l", "128painting03l")),
            art("seaofportals", versions("64painting04z", "128painting04z"),
                    versions(4, 3, "64painting04", "128painting04")),
            art("cornerexplody", versions("64painting05z", "128painting05z"),
                    versions(2, 1, "128painting05l", "256painting05l")),
            art("pie", versions("32pie", "64pie")),
            art("loveofbacon", versions("32collage", "64collage", "128collage")),
            art("boathouse", versions("boathouse_64", "boathouse_128"),
                    versions(4, 3, "boathouse_64l", "boathouse_128l")),
            art("jimi", versions("jimi_32", "jimi_64", "jimi_128")),
            art("raven", versions("raven_32", "raven_64", "raven_128"),
                    versions(4, 3, "raven_32l", "raven_64l", "raven_128l")));
    private static final Map<String, Bundled> BUNDLED = GALLERY.stream()
            .collect(Collectors.toUnmodifiableMap(bundled -> bundled.art().title(), Function.identity()));

    public static List<PaintingArt> bundled() {
        return GALLERY.stream().map(Bundled::art).toList();
    }

    public boolean isBlank() {
        return title.isEmpty();
    }

    public Component name() {
        return switch (source) {
            case BIBLIOCRAFT -> Component.translatable(isBlank() ? "gui.bibliocraft.painting.blank"
                    : "painting.bibliocraft." + title);
            case VANILLA -> Component.translatable("painting." + vanillaId().toLanguageKey() + ".title");
            case CUSTOM -> Component.literal(title.substring(title.lastIndexOf('/') + 1).replaceFirst("\\.png$", ""));
        };
    }

    public ResourceLocation vanillaId() {
        return ResourceLocation.tryParse(title);
    }

    public int aspectCount() {
        Bundled bundled = BUNDLED.get(title);
        return source == Source.BIBLIOCRAFT && bundled != null ? bundled.variants().size() : 1;
    }

    public Size image(int aspect) {
        Bundled bundled = BUNDLED.get(title);
        if (source == Source.BIBLIOCRAFT && bundled != null) {
            return bundled.variants().get(Mth.clamp(aspect, 0, bundled.variants().size() - 1));
        }
        return new Size(width, height, List.of(isBlank() ? "canvas" : "paintingnotfound"));
    }

    public ResourceLocation texture(int aspect, int resolution) {
        return switch (source) {
            case BIBLIOCRAFT -> {
                var textures = image(aspect).textures();
                yield texture("paintings/" + textures.get(Mth.clamp(resolution, 0, textures.size() - 1)) + ".png");
            }
            case VANILLA -> vanillaId().withPath(path -> "textures/painting/" + path + ".png");
            case CUSTOM -> texture("custompaintings/" + title);
        };
    }

    public static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "textures/" + path);
    }

    public static Optional<PaintingArt> validate(Source source, String title, int width, int height) {
        if (title.length() > MAX_TITLE_LENGTH) {
            return Optional.empty();
        }
        return switch (source) {
            case BIBLIOCRAFT -> {
                Bundled bundled = BUNDLED.get(title);
                yield Optional.ofNullable(bundled == null ? null : bundled.art());
            }
            case VANILLA -> {
                ResourceLocation id = ResourceLocation.tryParse(title.toLowerCase(Locale.ROOT));
                yield id == null ? Optional.empty() : BuiltInRegistries.PAINTING_VARIANT.getOptional(id)
                        .map(variant -> new PaintingArt(source, id.toString(), Math.max(1, variant.getWidth() / 16),
                                Math.max(1, variant.getHeight() / 16)));
            }
            case CUSTOM -> {
                ResourceLocation id = ResourceLocation.tryParse(BiblioCraft.MODID + ":" + title);
                boolean valid = id != null && title.endsWith(".png") && !title.startsWith("/")
                        && !title.contains("..") && width >= 1 && width <= 32 && height >= 1 && height <= 32;
                yield valid ? Optional.of(new PaintingArt(source, title, width, height)) : Optional.empty();
            }
        };
    }

    public static PaintingArt from(ItemStack stack) {
        return from(stack.getTag());
    }

    public static PaintingArt from(@Nullable CompoundTag tag) {
        if (tag == null) {
            return BLANK;
        }
        int index = tag.getInt("paintingType");
        if (index < 0 || index >= Source.values().length) {
            return BLANK;
        }
        return validate(Source.values()[index], tag.getString("paintingTitle"),
                tag.contains("ArtworkWidth") ? tag.getInt("ArtworkWidth") : 1,
                tag.contains("ArtworkHeight") ? tag.getInt("ArtworkHeight") : 1).orElse(BLANK);
    }

    public void save(CompoundTag tag) {
        tag.putInt("paintingType", source.ordinal());
        tag.putString("paintingTitle", title);
        tag.putInt("ArtworkWidth", width);
        tag.putInt("ArtworkHeight", height);
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeEnum(source).writeUtf(title, MAX_TITLE_LENGTH).writeVarInt(width).writeVarInt(height);
    }

    public static PaintingArt read(FriendlyByteBuf buffer) {
        var source = buffer.readEnum(Source.class);
        return validate(source, buffer.readUtf(MAX_TITLE_LENGTH), buffer.readVarInt(), buffer.readVarInt()).orElse(BLANK);
    }

    private static Bundled art(String title, Size... variants) {
        return new Bundled(new PaintingArt(Source.BIBLIOCRAFT, title, 1, 1), List.of(variants));
    }

    private static Size versions(String... textures) {
        return versions(1, 1, textures);
    }

    private static Size versions(int width, int height, String... textures) {
        return new Size(width, height, List.of(textures));
    }

    public enum Source {
        BIBLIOCRAFT, VANILLA, CUSTOM
    }

    public record Size(int width, int height, List<String> textures) {}

    private record Bundled(PaintingArt art, List<Size> variants) {}
}

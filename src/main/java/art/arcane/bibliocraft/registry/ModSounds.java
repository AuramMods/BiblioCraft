package art.arcane.bibliocraft.registry;

import art.arcane.bibliocraft.BiblioCraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, BiblioCraft.MODID);

    public static final RegistryObject<SoundEvent> CCLOSE = register("cclose");
    public static final RegistryObject<SoundEvent> COPEN = register("copen");
    public static final RegistryObject<SoundEvent> TAPEOPEN = register("tapeopen");
    public static final RegistryObject<SoundEvent> TAPECLOSE = register("tapeclose");
    public static final RegistryObject<SoundEvent> SCREW = register("screw");
    public static final RegistryObject<SoundEvent> DING = register("ding");
    public static final RegistryObject<SoundEvent> DRILL = register("drill");
    public static final RegistryObject<SoundEvent> TICK = register("tick");
    public static final RegistryObject<SoundEvent> TOCK = register("tock");
    public static final RegistryObject<SoundEvent> WIND = register("wind");
    public static final RegistryObject<SoundEvent> CHIME = register("chime");
    public static final RegistryObject<SoundEvent> WOUNDCHIME = register("woundchime");
    public static final RegistryObject<SoundEvent> ADDPAPER = register("addpaper");
    public static final RegistryObject<SoundEvent> ENDBELL = register("endbell");
    public static final RegistryObject<SoundEvent> REMOVEBOOK = register("removebook");
    public static final RegistryObject<SoundEvent> TYPING = register("typing");
    public static final RegistryObject<SoundEvent> TYPINGSINGLE = register("typingsingle");

    private ModSounds() {}

    private static RegistryObject<SoundEvent> register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }
}

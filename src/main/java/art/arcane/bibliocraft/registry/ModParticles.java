package art.arcane.bibliocraft.registry;

import art.arcane.bibliocraft.BiblioCraft;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, BiblioCraft.MODID);
    public static final RegistryObject<SimpleParticleType> CATALOG = PARTICLES.register("catalog",
            () -> new SimpleParticleType(false));

    private ModParticles() {}
}

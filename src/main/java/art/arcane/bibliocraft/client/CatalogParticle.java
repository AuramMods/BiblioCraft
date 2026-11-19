package art.arcane.bibliocraft.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

final class CatalogParticle extends TextureSheetParticle {
    private CatalogParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprite) {
        super(level, x, y, z);
        xd = dx;
        yd = dy;
        zd = dz;
        lifetime = 30;
        friction = 0.96F;
        hasPhysics = false;
        quadSize = 0.095F;
        pickSprite(sprite);
    }

    @Override
    public void tick() {
        super.tick();
        alpha = Math.max(0, 1 - (float) age / lifetime);
    }

    @Override
    public float getQuadSize(float partialTick) {
        return (float) (10 / (age + partialTick + 80.0) - 0.03);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    record Provider(SpriteSet sprite) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new CatalogParticle(level, x, y, z, dx, dy, dz, sprite);
        }
    }
}

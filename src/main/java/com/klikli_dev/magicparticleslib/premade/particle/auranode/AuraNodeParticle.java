// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.auranode;

import com.klikli_dev.magicparticleslib.registry.RenderTypeRegistry;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AuraNodeParticle extends Particle {
    private static final float FRAME_RATE = 1.0F;
    private static final float FADE_START_DISTANCE = 34.0F;
    private static final float FADE_END_DISTANCE = 54.0F;
    private static final float STRAND_BASE_SIZE = 0.57F;
    private static final float STRAND_PRIMARY_BREATH = 0.14F;
    private static final float STRAND_SECONDARY_BREATH = 0.045F;
    private static final float STRAND_BASE_SPEED = 0.0074F;
    private static final float STRAND_SPEED_VARIATION = 0.0011F;
    private static final float CORE_BASE_SIZE = 0.34F;
    private static final float CORE_PULSE_AMOUNT = 0.035F;
    private static final float CORE_DRIFT_SPEED = 0.0046F;

    private final AuraNodePreset preset;
    private final List<AuraNodePreset.StrandColor> strandColors;
    private final float size;
    private final float red;
    private final float green;
    private final float blue;
    private final float motionPhase;
    private final int frameOffset;

    public AuraNodeParticle(ClientLevel level, double x, double y, double z, AuraNodeParticleOptions options) {
        super(level, x, y, z);
        this.preset = AuraNodePresets.getOrDefault(options.presetId());
        this.size = options.scale();
        this.red = ARGB.red(options.color()) / 255.0F;
        this.green = ARGB.green(options.color()) / 255.0F;
        this.blue = ARGB.blue(options.color()) / 255.0F;
        boolean whitePayload = this.red > 0.99F && this.green > 0.99F && this.blue > 0.99F;
        if (whitePayload) {
            this.strandColors = this.preset.strandColors();
        } else {
            AuraNodePreset.StrandColor color = new AuraNodePreset.StrandColor(options.color(), true);
            this.strandColors = new ArrayList<>(this.preset.strandColors().size());
            for (int i = 0; i < this.preset.strandColors().size(); i++) {
                this.strandColors.add(color);
            }
        }
        this.motionPhase = this.random.nextFloat() * Mth.TWO_PI;
        this.frameOffset = this.random.nextInt(AuraNodePreset.FRAMES_PER_STRIP);
        this.lifetime = options.lifetime();
        this.hasPhysics = false;
        this.gravity = 0.0F;
    }

    @Override
    public ParticleRenderType getGroup() {
        return RenderTypeRegistry.AURA_NODE_GROUP;
    }

    @Override
    public int getLightCoords(float partialTick) {
        return LightCoordsUtil.FULL_BRIGHT;
    }

    @Override
    public void tick() {
        // Preserve previous position fields even though the particle is stationary.
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }

        this.age++;
    }

    // Rebuilds the world-space quads for the current frame. The billboard rotation is applied
    // later in AuraNodeRenderState where the camera orientation is available.
    public @Nullable AuraNodeRenderData renderData(Camera camera, float partialTick) {
        if (!this.isAlive()) {
            return null;
        }

        float time = this.level.getGameTime() + partialTick;
        int frame = Math.floorMod((int) (time * FRAME_RATE) + this.frameOffset, AuraNodePreset.FRAMES_PER_STRIP);

        TextureAtlasSprite strandSprite = this.sprite(AuraNodePreset.frameTexture(this.preset.strandTexture(), frame));
        TextureAtlasSprite coreSprite = this.sprite(AuraNodePreset.frameTexture(this.preset.texture(), frame));

        float baseX = (float) this.x;
        float baseY = (float) this.y;
        float baseZ = (float) this.z;
        Vec3 cameraPos = camera.position();
        float distance = (float) Math.sqrt(cameraPos.distanceToSqr(this.x, this.y, this.z));
        float fade = Mth.clamp((FADE_END_DISTANCE - distance) / (FADE_END_DISTANCE - FADE_START_DISTANCE), 0.0F, 1.0F);
        float alpha = fade * fade * (3.0F - 2.0F * fade);
        float strandAlpha = alpha * 0.78F / Mth.sqrt(Math.max(1, this.strandColors.size()));

        List<AuraNodeQuad> quads = new ArrayList<>(this.strandColors.size() + 1);
        for (int i = 0; i < this.strandColors.size(); i++) {
            float strandPhase = this.motionPhase + Mth.TWO_PI * i / Math.max(1, this.strandColors.size());
            float primaryWave = Mth.sin(time * (0.071F + i * 0.0043F) + strandPhase);
            float secondaryWave = Mth.sin(time * 0.029F - strandPhase * 1.7F);
            float edge = (STRAND_BASE_SIZE
                    + primaryWave * STRAND_PRIMARY_BREATH
                    + secondaryWave * STRAND_SECONDARY_BREATH) * this.size;
            float direction = (i & 1) == 0 ? 1.0F : -1.0F;
            float rotation = strandPhase + time * (STRAND_BASE_SPEED + STRAND_SPEED_VARIATION * i) * direction;
            AuraNodePreset.StrandColor strandColor = this.strandColors.get(i);
            float strandBlendAlpha = strandAlpha * (strandColor.additive() ? 1.0F : 1.28F);
            int color = ARGB.colorFromFloat(strandBlendAlpha,
                    ARGB.red(strandColor.color()) / 255.0F,
                    ARGB.green(strandColor.color()) / 255.0F,
                    ARGB.blue(strandColor.color()) / 255.0F);
            quads.add(this.addQuad(baseX, baseY, baseZ,
                    edge,
                    strandSprite,
                    color,
                    rotation,
                    strandColor.additive()));
        }

        float corePulse = 1.0F + Mth.sin(time * 0.047F + this.motionPhase) * CORE_PULSE_AMOUNT;
        float coreScale = CORE_BASE_SIZE * corePulse * this.size * this.preset.coreScaleMultiplier();
        float coreAngle = this.preset.coreRotates() ? this.motionPhase + time * CORE_DRIFT_SPEED : 0.0F;
        quads.add(this.addQuad(baseX, baseY, baseZ,
                coreScale,
                coreSprite,
                ARGB.colorFromFloat(alpha, 1.0F, 1.0F, 1.0F),
                coreAngle,
                this.preset.coreBlendAdditive()));

        return new AuraNodeRenderData(quads, this.getLightCoords(partialTick));
    }

    private AuraNodeQuad addQuad(float x, float y, float z, float edge, TextureAtlasSprite sprite, int color, float rotation, boolean additive) {
        return new AuraNodeQuad(x, y, z, edge,
                sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(), color, rotation, additive);
    }

    private TextureAtlasSprite sprite(Identifier location) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES).getSprite(location);
    }
}

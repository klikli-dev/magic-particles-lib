// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.auranode;

import com.klikli_dev.magicparticleslib.MagicParticlesLib;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;

import java.util.List;

/**
 * All visual parameters of one aura node appearance.
 *
 * <p>A preset references its own sprite strip(s) by {@link Identifier}; no strip
 * indices or texture orderings leak into the renderer or the wire format.</p>
 *
 * @param texture             particle atlas location of the node core strip, e.g. {@code magicparticleslib:aura_node/normal}
 * @param strandTexture       particle atlas location of the rotating strand strip (shared across node types by default)
 * @param strandColors        per-strand colors; each may be blended additively or normally
 * @param coreBlendAdditive   whether the core is rendered with additive blending (normal blending otherwise)
 * @param coreScaleMultiplier core scale factor (the hungry preset renders its core at 0.8)
 * @param coreRotates         whether the core slowly rotates (fixed for unstable nodes)
 */
public record AuraNodePreset(
        Identifier texture,
        Identifier strandTexture,
        List<StrandColor> strandColors,
        boolean coreBlendAdditive,
        float coreScaleMultiplier,
        boolean coreRotates) {

    public static final int FRAMES_PER_STRIP = 32;
    public static final Identifier DEFAULT_STRAND_TEXTURE = Identifier.fromNamespaceAndPath(MagicParticlesLib.MODID, "aura_node/strand");

    public static final Codec<AuraNodePreset> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("texture").forGetter(AuraNodePreset::texture),
            Identifier.CODEC.lenientOptionalFieldOf("strandTexture", DEFAULT_STRAND_TEXTURE).forGetter(AuraNodePreset::strandTexture),
            StrandColor.CODEC.listOf().fieldOf("strandColors").forGetter(AuraNodePreset::strandColors),
            Codec.BOOL.fieldOf("coreBlendAdditive").forGetter(AuraNodePreset::coreBlendAdditive),
            Codec.FLOAT.fieldOf("coreScaleMultiplier").forGetter(AuraNodePreset::coreScaleMultiplier),
            Codec.BOOL.fieldOf("coreRotates").forGetter(AuraNodePreset::coreRotates)
    ).apply(instance, AuraNodePreset::new));

    public AuraNodePreset {
        strandColors = List.copyOf(strandColors);
    }

    /**
     * Resolves the sprite location for one animation frame of a strip,
     * e.g. {@code magicparticleslib:aura_node/normal} + frame 3 -> {@code magicparticleslib:aura_node/normal_03}.
     */
    public static Identifier frameTexture(Identifier strip, int frame) {
        return Identifier.fromNamespaceAndPath(strip.getNamespace(), strip.getPath() + "_" + String.format("%02d", frame));
    }

    public record StrandColor(int color, boolean additive) {
        public static final Codec<StrandColor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ExtraCodecs.ARGB_COLOR_CODEC.fieldOf("color").forGetter(StrandColor::color),
                Codec.BOOL.fieldOf("additive").forGetter(StrandColor::additive)
        ).apply(instance, StrandColor::new));
    }
}

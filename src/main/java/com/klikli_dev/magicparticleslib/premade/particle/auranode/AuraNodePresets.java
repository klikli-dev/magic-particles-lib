// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.premade.particle.auranode;

import com.klikli_dev.magicparticleslib.MagicParticlesLib;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

import java.util.List;

/**
 * Registry of {@link AuraNodePreset}s.
 *
 * <p>This is the library extension point: other mods can register their own
 * presets by creating their own {@link DeferredRegister} bound to
 * {@link #KEY} and pointing at their own sprite strips. The particle
 * description datagen automatically lists every registered preset's
 * textures, so foreign presets only need to ship their PNG frames.</p>
 */
public final class AuraNodePresets {
    public static final ResourceKey<Registry<AuraNodePreset>> KEY = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath(MagicParticlesLib.MODID, "aura_node_preset"));

    public static final DeferredRegister<AuraNodePreset> PRESETS = DeferredRegister.create(KEY, MagicParticlesLib.MODID);

    private static Registry<AuraNodePreset> registry;

    public static final DeferredHolder<AuraNodePreset, AuraNodePreset> NORMAL = register("normal",
            preset("normal", true, 1.0F, true,
                    new AuraNodePreset.StrandColor(0xFFF4E38A, true),
                    new AuraNodePreset.StrandColor(0xFFEF7135, true),
                    new AuraNodePreset.StrandColor(0xFF70B94D, true),
                    new AuraNodePreset.StrandColor(0xFF5CC7E8, true)));

    public static final DeferredHolder<AuraNodePreset, AuraNodePreset> DARK = register("dark",
            preset("dark", false, 1.0F, true,
                    new AuraNodePreset.StrandColor(0xFF292533, true),
                    new AuraNodePreset.StrandColor(0xFF4B4654, false),
                    new AuraNodePreset.StrandColor(0xFF7D667F, true),
                    new AuraNodePreset.StrandColor(0xFF46502B, true)));

    public static final DeferredHolder<AuraNodePreset, AuraNodePreset> HUNGRY = register("hungry",
            preset("hungry", true, 0.8F, true,
                    new AuraNodePreset.StrandColor(0xFF8F2228, true),
                    new AuraNodePreset.StrandColor(0xFFD4A84F, true),
                    new AuraNodePreset.StrandColor(0xFFEA7040, true),
                    new AuraNodePreset.StrandColor(0xFF4A4145, false)));

    public static final DeferredHolder<AuraNodePreset, AuraNodePreset> PURE = register("pure",
            preset("pure", true, 1.0F, true,
                    new AuraNodePreset.StrandColor(0xFFCBCFE6, true),
                    new AuraNodePreset.StrandColor(0xFFF2E890, true),
                    new AuraNodePreset.StrandColor(0xFFEDE1A4, true),
                    new AuraNodePreset.StrandColor(0xFF72C6DC, true)));

    public static final DeferredHolder<AuraNodePreset, AuraNodePreset> TAINTED = register("tainted",
            preset("tainted", false, 1.0F, true,
                    new AuraNodePreset.StrandColor(0xFF742A82, true),
                    new AuraNodePreset.StrandColor(0xFF8D3BA7, true),
                    new AuraNodePreset.StrandColor(0xFF30263A, true),
                    new AuraNodePreset.StrandColor(0xFF77707D, false)));

    public static final DeferredHolder<AuraNodePreset, AuraNodePreset> UNSTABLE = register("unstable",
            preset("unstable", true, 1.0F, false,
                    new AuraNodePreset.StrandColor(0xFFA9E8EA, true),
                    new AuraNodePreset.StrandColor(0xFFC8EFF2, true),
                    new AuraNodePreset.StrandColor(0xFFE9834E, true),
                    new AuraNodePreset.StrandColor(0xFF51505D, false)));

    private AuraNodePresets() {
    }

    public static void createRegistry(NewRegistryEvent event) {
        registry = event.create(new RegistryBuilder<>(KEY));
    }

    public static Registry<AuraNodePreset> registry() {
        return registry;
    }

    public static AuraNodePreset get(Identifier id) {
        return registry == null ? null : registry.getValue(id);
    }

    public static AuraNodePreset getOrDefault(Identifier id) {
        AuraNodePreset preset = get(id);
        return preset != null ? preset : NORMAL.get();
    }

    public static AuraNodePreset normal() {
        return NORMAL.get();
    }

    private static DeferredHolder<AuraNodePreset, AuraNodePreset> register(String name, AuraNodePreset preset) {
        return PRESETS.register(name, () -> preset);
    }

    private static AuraNodePreset preset(String texture, boolean coreBlendAdditive, float coreScaleMultiplier, boolean coreRotates, AuraNodePreset.StrandColor... colors) {
        return new AuraNodePreset(
                Identifier.fromNamespaceAndPath(MagicParticlesLib.MODID, "aura_node/" + texture),
                AuraNodePreset.DEFAULT_STRAND_TEXTURE,
                List.of(colors),
                coreBlendAdditive,
                coreScaleMultiplier,
                coreRotates);
    }
}

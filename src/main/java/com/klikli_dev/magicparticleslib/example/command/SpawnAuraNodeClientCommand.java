// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.example.command;

import com.klikli_dev.magicparticleslib.premade.particle.auranode.AuraNodeParticleOptions;
import com.klikli_dev.magicparticleslib.premade.particle.auranode.AuraNodePreset;
import com.klikli_dev.magicparticleslib.premade.particle.auranode.AuraNodePresets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class SpawnAuraNodeClientCommand {
    private static final String ROOT_COMMAND = "mpl";
    private static final String SPAWN_SUBCOMMAND = "spawn";
    private static final String AURA_NODE_SUBCOMMAND = "aura_node";
    private static final int DEFAULT_COLOR = AuraNodeParticleOptions.DEFAULT_COLOR;
    private static final float DEFAULT_SCALE = AuraNodeParticleOptions.DEFAULT_SCALE;
    private static final int DEFAULT_LIFETIME = AuraNodeParticleOptions.DEFAULT_LIFETIME;
    private static final AuraNodePreset DEFAULT_PRESET = AuraNodePresets.normal();

    private SpawnAuraNodeClientCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(ROOT_COMMAND)
                .then(Commands.literal(SPAWN_SUBCOMMAND)
                        .then(auraNodeLiteral())));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> auraNodeLiteral() {
        LiteralArgumentBuilder<CommandSourceStack> literal = Commands.literal(AURA_NODE_SUBCOMMAND)
                .executes(context -> spawnAuraNode(
                        context.getSource(),
                        null,
                        DEFAULT_PRESET,
                        DEFAULT_COLOR,
                        DEFAULT_SCALE,
                        DEFAULT_LIFETIME
                ));
        RequiredArgumentBuilder<CommandSourceStack, ?> pos = Commands.argument("pos", Vec3Argument.vec3(false))
                .executes(context -> spawnAuraNode(
                        context.getSource(),
                        Vec3Argument.getCoordinates(context, "pos"),
                        DEFAULT_PRESET,
                        DEFAULT_COLOR,
                        DEFAULT_SCALE,
                        DEFAULT_LIFETIME
                ));
        RequiredArgumentBuilder<CommandSourceStack, ?> preset = Commands.argument("preset", IdentifierArgument.id())
                .suggests((context, builder) -> {
                    for (AuraNodePreset nodePreset : AuraNodePresets.registry()) {
                        builder.suggest(AuraNodePresets.registry().getKey(nodePreset).toString());
                    }
                    return builder.buildFuture();
                })
                .executes(context -> spawnAuraNode(
                        context.getSource(),
                        Vec3Argument.getCoordinates(context, "pos"),
                        presetOrFailure(context),
                        DEFAULT_COLOR,
                        DEFAULT_SCALE,
                        DEFAULT_LIFETIME
                ));
        RequiredArgumentBuilder<CommandSourceStack, ?> color = Commands.argument("color", IntegerArgumentType.integer())
                .executes(context -> spawnAuraNode(
                        context.getSource(),
                        Vec3Argument.getCoordinates(context, "pos"),
                        presetOrFailure(context),
                        IntegerArgumentType.getInteger(context, "color"),
                        DEFAULT_SCALE,
                        DEFAULT_LIFETIME
                ));
        RequiredArgumentBuilder<CommandSourceStack, ?> scale = Commands.argument("scale", FloatArgumentType.floatArg(0.05F))
                .executes(context -> spawnAuraNode(
                        context.getSource(),
                        Vec3Argument.getCoordinates(context, "pos"),
                        presetOrFailure(context),
                        IntegerArgumentType.getInteger(context, "color"),
                        FloatArgumentType.getFloat(context, "scale"),
                        DEFAULT_LIFETIME
                ));
        RequiredArgumentBuilder<CommandSourceStack, ?> lifetime = Commands.argument("lifetime", IntegerArgumentType.integer(1))
                .executes(context -> spawnAuraNode(
                        context.getSource(),
                        Vec3Argument.getCoordinates(context, "pos"),
                        presetOrFailure(context),
                        IntegerArgumentType.getInteger(context, "color"),
                        FloatArgumentType.getFloat(context, "scale"),
                        IntegerArgumentType.getInteger(context, "lifetime")
                ));

        scale.then(lifetime);
        color.then(scale);
        preset.then(color);
        pos.then(preset);
        literal.then(pos);
        return literal;
    }

    private static AuraNodePreset presetOrFailure(CommandContext<CommandSourceStack> context) {
        Identifier id = IdentifierArgument.getId(context, "preset");
        AuraNodePreset preset = AuraNodePresets.get(id);
        if (preset == null) {
            context.getSource().sendFailure(Component.literal("Unknown aura node preset: " + id));
            return null;
        }
        return preset;
    }

    private static int spawnAuraNode(CommandSourceStack source, Coordinates posCoordinates, AuraNodePreset preset, int color, float scale, int lifetime) {
        if (preset == null) {
            return 0;
        }
        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.literal("This command requires a local player."));
            return 0;
        }
        if (!(player.level() instanceof ClientLevel level)) {
            source.sendFailure(Component.literal("This command requires a client level."));
            return 0;
        }

        CommandSourceStack anchoredSource = source.withAnchor(EntityAnchorArgument.Anchor.EYES);
        Vec3 pos = posCoordinates != null
                ? posCoordinates.getPosition(anchoredSource)
                : player.getEyePosition(0.0F).add(player.getViewVector(0.0F).scale(1.0D));

        level.addParticle(
                AuraNodeParticleOptions.of(preset, color, scale, lifetime),
                pos.x,
                pos.y,
                pos.z,
                0.0D,
                0.0D,
                0.0D
        );

        source.sendSuccess(() -> Component.literal("Spawned aura node."), false);
        return 1;
    }
}
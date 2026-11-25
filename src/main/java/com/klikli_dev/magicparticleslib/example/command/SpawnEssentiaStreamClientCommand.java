// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.example.command;

import com.klikli_dev.magicparticleslib.example.CommonTargeting;
import com.klikli_dev.magicparticleslib.premade.particle.essentiastream.EssentiaStreamParticleOptions;
import com.klikli_dev.magicparticleslib.premade.particle.essentiastream.EssentiaStreams;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public final class SpawnEssentiaStreamClientCommand {
    private static final String ROOT_COMMAND = "mpl";
    private static final String SPAWN_SUBCOMMAND = "spawn";
    private static final String ESSENTIA_STREAM_SUBCOMMAND = "essentia_stream";
    private static final String EXTEND_ARGUMENT = "extend";
    private static final String RADIUS_ARGUMENT = "radius";
    private static final String MAX_LENGTH_ARGUMENT = "maxLength";
    private static final int DEFAULT_EXTEND = EssentiaStreamParticleOptions.DEFAULT_EXTEND;
    private static final float DEFAULT_RADIUS = EssentiaStreamParticleOptions.DEFAULT_RADIUS;
    private static final float DEFAULT_MAX_LENGTH = EssentiaStreamParticleOptions.DEFAULT_MAX_LENGTH;
    private static final float MIN_RADIUS = 0.01F;
    private static final float MAX_RADIUS = 1.0F;
    private static final float MAX_MAX_LENGTH = 64.0F;
    // Reach and entity margin used to find the stream destination, the same as the other example commands.
    private static final double TARGET_RANGE = 16.0D;
    private static final double ENTITY_MARGIN = 0.5D;
    // Random hues are sampled at this saturation and brightness, so streams are colourful but still read as liquid.
    private static final float RANDOM_SATURATION = 0.6F;
    private static final float RANDOM_BRIGHTNESS = 1.0F;

    private SpawnEssentiaStreamClientCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(ROOT_COMMAND)
                .then(Commands.literal(SPAWN_SUBCOMMAND)
                        .then(essentiaStreamLiteral())));
    }

    // Arguments are positional: each optional argument requires the ones before it.
    private static LiteralArgumentBuilder<CommandSourceStack> essentiaStreamLiteral() {
        LiteralArgumentBuilder<CommandSourceStack> literal = Commands.literal(ESSENTIA_STREAM_SUBCOMMAND)
                .executes(context -> spawn(context.getSource(), DEFAULT_EXTEND, DEFAULT_RADIUS, DEFAULT_MAX_LENGTH));

        RequiredArgumentBuilder<CommandSourceStack, ?> extend = Commands.argument(EXTEND_ARGUMENT, IntegerArgumentType.integer(0))
                .executes(context -> spawn(
                        context.getSource(),
                        IntegerArgumentType.getInteger(context, EXTEND_ARGUMENT),
                        DEFAULT_RADIUS,
                        DEFAULT_MAX_LENGTH
                ));

        RequiredArgumentBuilder<CommandSourceStack, ?> radius = Commands.argument(RADIUS_ARGUMENT, FloatArgumentType.floatArg(MIN_RADIUS, MAX_RADIUS))
                .executes(context -> spawn(
                        context.getSource(),
                        IntegerArgumentType.getInteger(context, EXTEND_ARGUMENT),
                        FloatArgumentType.getFloat(context, RADIUS_ARGUMENT),
                        DEFAULT_MAX_LENGTH
                ));

        RequiredArgumentBuilder<CommandSourceStack, ?> maxLength = Commands.argument(MAX_LENGTH_ARGUMENT, FloatArgumentType.floatArg(EssentiaStreamParticleOptions.MIN_MAX_LENGTH, MAX_MAX_LENGTH))
                .executes(context -> spawn(
                        context.getSource(),
                        IntegerArgumentType.getInteger(context, EXTEND_ARGUMENT),
                        FloatArgumentType.getFloat(context, RADIUS_ARGUMENT),
                        FloatArgumentType.getFloat(context, MAX_LENGTH_ARGUMENT)
                ));

        radius.then(maxLength);
        extend.then(radius);
        literal.then(extend);
        return literal;
    }

    private static int spawn(CommandSourceStack source, int extend, float radius, float maxLength) {
        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.literal("This command requires a local player."));
            return 0;
        }
        if (!(player.level() instanceof ClientLevel level)) {
            source.sendFailure(Component.literal("This command requires a client level."));
            return 0;
        }

        CommonTargeting.Targeting targeting = CommonTargeting.resolve(player, TARGET_RANGE, ENTITY_MARGIN);
        int color = Mth.hsvToRgb(level.getRandom().nextFloat(), RANDOM_SATURATION, RANDOM_BRIGHTNESS);
        EssentiaStreams.spawn(level, targeting.origin(), targeting.target(), color, radius, extend, maxLength);

        source.sendSuccess(() -> Component.literal("Spawned essentia stream."), false);
        return 1;
    }
}

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
    private static final int DEFAULT_EXTEND = 0;
    private static final float DEFAULT_RADIUS = EssentiaStreamParticleOptions.DEFAULT_RADIUS;
    private static final float MIN_RADIUS = 0.01F;
    private static final float MAX_RADIUS = 1.0F;
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

    private static LiteralArgumentBuilder<CommandSourceStack> essentiaStreamLiteral() {
        LiteralArgumentBuilder<CommandSourceStack> literal = Commands.literal(ESSENTIA_STREAM_SUBCOMMAND)
                .executes(context -> spawn(context.getSource(), DEFAULT_EXTEND, DEFAULT_RADIUS));
        RequiredArgumentBuilder<CommandSourceStack, ?> extend = Commands.argument("extend", IntegerArgumentType.integer(0))
                .executes(context -> spawn(
                        context.getSource(),
                        IntegerArgumentType.getInteger(context, "extend"),
                        DEFAULT_RADIUS
                ));
        RequiredArgumentBuilder<CommandSourceStack, ?> radius = Commands.argument("radius", FloatArgumentType.floatArg(MIN_RADIUS, MAX_RADIUS))
                .executes(context -> spawn(
                        context.getSource(),
                        IntegerArgumentType.getInteger(context, "extend"),
                        FloatArgumentType.getFloat(context, "radius")
                ));

        extend.then(radius);
        literal.then(extend);
        return literal;
    }

    private static int spawn(CommandSourceStack source, int extend, float radius) {
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
        EssentiaStreams.spawn(level, EssentiaStreamParticleOptions.of(targeting.origin(), targeting.target(), color, radius, extend));

        source.sendSuccess(() -> Component.literal("Spawned essentia stream."), false);
        return 1;
    }
}

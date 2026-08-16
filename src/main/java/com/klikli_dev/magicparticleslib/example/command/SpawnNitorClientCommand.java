// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.example.command;

import com.klikli_dev.magicparticleslib.premade.particle.nitor.NitorCoreType;
import com.klikli_dev.magicparticleslib.premade.particle.nitor.NitorParticleOptions;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class SpawnNitorClientCommand {
    private static final String ROOT_COMMAND = "mpl";
    private static final String SPAWN_SUBCOMMAND = "spawn";
    private static final String NITOR_SUBCOMMAND = "nitor";
    // Warm orange default that reads well against the procedural flame.
    private static final int DEFAULT_COLOR = 0xFFB84D;
    private static final float DEFAULT_SIZE = 1.0F;
    private static final float DEFAULT_SPEED = 1.0F;
    private static final float DEFAULT_INTENSITY = 1.0F;
    private static final NitorCoreType DEFAULT_CORE = NitorCoreType.DARK;

    private SpawnNitorClientCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(ROOT_COMMAND)
                .then(Commands.literal(SPAWN_SUBCOMMAND)
                        .then(nitorLiteral())));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> nitorLiteral() {
        LiteralArgumentBuilder<CommandSourceStack> literal = Commands.literal(NITOR_SUBCOMMAND)
                .executes(context -> spawnNitor(
                        context.getSource(),
                        null,
                        DEFAULT_COLOR,
                        DEFAULT_SIZE,
                        DEFAULT_SPEED,
                        DEFAULT_INTENSITY,
                        DEFAULT_CORE
                ));
        RequiredArgumentBuilder<CommandSourceStack, ?> pos = Commands.argument("pos", Vec3Argument.vec3(false))
                .executes(context -> spawnNitor(
                        context.getSource(),
                        Vec3Argument.getCoordinates(context, "pos"),
                        DEFAULT_COLOR,
                        DEFAULT_SIZE,
                        DEFAULT_SPEED,
                        DEFAULT_INTENSITY,
                        DEFAULT_CORE
                ));
        RequiredArgumentBuilder<CommandSourceStack, ?> color = Commands.argument("color", IntegerArgumentType.integer())
                .executes(context -> spawnNitor(
                        context.getSource(),
                        Vec3Argument.getCoordinates(context, "pos"),
                        IntegerArgumentType.getInteger(context, "color"),
                        DEFAULT_SIZE,
                        DEFAULT_SPEED,
                        DEFAULT_INTENSITY,
                        DEFAULT_CORE
                ));
        RequiredArgumentBuilder<CommandSourceStack, ?> size = Commands.argument("size", FloatArgumentType.floatArg(0.05F, 16.0F))
                .executes(context -> spawnNitor(
                        context.getSource(),
                        Vec3Argument.getCoordinates(context, "pos"),
                        IntegerArgumentType.getInteger(context, "color"),
                        FloatArgumentType.getFloat(context, "size"),
                        DEFAULT_SPEED,
                        DEFAULT_INTENSITY,
                        DEFAULT_CORE
                ));
        RequiredArgumentBuilder<CommandSourceStack, ?> speed = Commands.argument("speed", FloatArgumentType.floatArg(0.05F, 8.0F))
                .executes(context -> spawnNitor(
                        context.getSource(),
                        Vec3Argument.getCoordinates(context, "pos"),
                        IntegerArgumentType.getInteger(context, "color"),
                        FloatArgumentType.getFloat(context, "size"),
                        FloatArgumentType.getFloat(context, "speed"),
                        DEFAULT_INTENSITY,
                        DEFAULT_CORE
                ));
        RequiredArgumentBuilder<CommandSourceStack, ?> intensity = Commands.argument("intensity", FloatArgumentType.floatArg(0.0F, 4.0F))
                .executes(context -> spawnNitor(
                        context.getSource(),
                        Vec3Argument.getCoordinates(context, "pos"),
                        IntegerArgumentType.getInteger(context, "color"),
                        FloatArgumentType.getFloat(context, "size"),
                        FloatArgumentType.getFloat(context, "speed"),
                        FloatArgumentType.getFloat(context, "intensity"),
                        DEFAULT_CORE
                ));
        RequiredArgumentBuilder<CommandSourceStack, ?> core = Commands.argument("core", StringArgumentType.word())
                .executes(context -> spawnNitor(
                        context.getSource(),
                        Vec3Argument.getCoordinates(context, "pos"),
                        IntegerArgumentType.getInteger(context, "color"),
                        FloatArgumentType.getFloat(context, "size"),
                        FloatArgumentType.getFloat(context, "speed"),
                        FloatArgumentType.getFloat(context, "intensity"),
                        NitorCoreType.fromName(StringArgumentType.getString(context, "core"))
                ));

        intensity.then(core);
        speed.then(intensity);
        size.then(speed);
        color.then(size);
        pos.then(color);
        literal.then(pos);
        return literal;
    }

    private static int spawnNitor(CommandSourceStack source, Coordinates posCoordinates, int color, float size, float speed, float intensity, NitorCoreType core) {
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
                : player.getEyePosition(0.0F).add(player.getViewVector(0.0F).scale(2.0D));

        level.addParticle(
                new NitorParticleOptions(color, size, speed, intensity, NitorParticleOptions.DEFAULT_LIFETIME, core),
                pos.x,
                pos.y,
                pos.z,
                0.0D,
                0.0D,
                0.0D
        );

        source.sendSuccess(() -> Component.literal("Spawned nitor."), false);
        return 1;
    }
}
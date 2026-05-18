// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.example.command;

import com.klikli_dev.magicparticleslib.example.electricarc.ElectricArcTestHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public final class SpawnElectricArcClientCommand {
    private static final String ROOT_COMMAND = "mpl";
    private static final String SPAWN_SUBCOMMAND = "spawn";
    private static final String ELECTRIC_ARC_SUBCOMMAND = "electric_arc";
    private static final int DEFAULT_DURATION_TICKS = 100;
    private static final int DEFAULT_TICK_SPACING = 4;
    private static PendingHoldSimulation pendingSimulation;

    private SpawnElectricArcClientCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> electricArc = Commands.literal(ELECTRIC_ARC_SUBCOMMAND)
                .executes(context -> startHoldSimulation(context.getSource(), DEFAULT_DURATION_TICKS, DEFAULT_TICK_SPACING));
        RequiredArgumentBuilder<CommandSourceStack, ?> durationTicks = Commands.argument("durationTicks", IntegerArgumentType.integer(1))
                .executes(context -> startHoldSimulation(
                        context.getSource(),
                        IntegerArgumentType.getInteger(context, "durationTicks"),
                        DEFAULT_TICK_SPACING
                ));
        RequiredArgumentBuilder<CommandSourceStack, ?> tickSpacing = Commands.argument("tickSpacing", IntegerArgumentType.integer(1))
                .executes(context -> startHoldSimulation(
                        context.getSource(),
                        IntegerArgumentType.getInteger(context, "durationTicks"),
                        IntegerArgumentType.getInteger(context, "tickSpacing")
                ));

        durationTicks.then(tickSpacing);
        electricArc.then(durationTicks);

        dispatcher.register(Commands.literal(ROOT_COMMAND)
                .then(Commands.literal(SPAWN_SUBCOMMAND)
                        .then(electricArc)));
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        LocalPlayer player = Minecraft.getInstance().player;
        if (level == null || player == null) {
            pendingSimulation = null;
            return;
        }

        if (pendingSimulation == null) {
            return;
        }

        if (pendingSimulation.ticksUntilNextSpawn > 0) {
            pendingSimulation.ticksUntilNextSpawn--;
            pendingSimulation.remainingTicks--;
        } else {
            spawnFromPlayer(level, player);
            pendingSimulation.ticksUntilNextSpawn = pendingSimulation.tickSpacing;
            pendingSimulation.remainingTicks -= pendingSimulation.tickSpacing;
        }

        if (pendingSimulation.remainingTicks <= 0) {
            pendingSimulation = null;
        }
    }

    private static int startHoldSimulation(CommandSourceStack source, int durationTicks, int tickSpacing) {
        if (!(source.getEntity() instanceof LocalPlayer player)) {
            source.sendFailure(Component.literal("This command requires a local player."));
            return 0;
        }
        if (!(player.level() instanceof ClientLevel level)) {
            source.sendFailure(Component.literal("This command requires a client level."));
            return 0;
        }

        pendingSimulation = new PendingHoldSimulation(durationTicks, tickSpacing, 0);
        spawnFromPlayer(level, player);
        pendingSimulation.ticksUntilNextSpawn = tickSpacing;
        pendingSimulation.remainingTicks -= tickSpacing;
        if (pendingSimulation.remainingTicks <= 0) {
            pendingSimulation = null;
        }

        source.sendSuccess(() -> Component.literal("Simulating held electric arc test item."), false);
        return 1;
    }

    private static void spawnFromPlayer(ClientLevel level, LocalPlayer player) {
        ElectricArcTestHelper.ArcTargeting targeting = ElectricArcTestHelper.resolve(player);
        RandomSource random = level.getRandom();

        level.addParticle(
                ElectricArcTestHelper.createOptions(targeting.target(), random),
                targeting.origin().x(),
                targeting.origin().y(),
                targeting.origin().z(),
                0.0D,
                0.0D,
                0.0D
        );
        ElectricArcTestHelper.playSound(level, player, random);
    }

    private static final class PendingHoldSimulation {
        private final int tickSpacing;
        private int remainingTicks;
        private int ticksUntilNextSpawn;

        private PendingHoldSimulation(int remainingTicks, int tickSpacing, int ticksUntilNextSpawn) {
            this.remainingTicks = remainingTicks;
            this.tickSpacing = tickSpacing;
            this.ticksUntilNextSpawn = ticksUntilNextSpawn;
        }
    }
}

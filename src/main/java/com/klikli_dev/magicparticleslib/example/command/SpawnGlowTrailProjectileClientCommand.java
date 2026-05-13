// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.example.command;

import com.klikli_dev.magicparticleslib.premade.projectile.GlowTrailProjectile;
import com.klikli_dev.magicparticleslib.premade.projectile.VisualEntitySpawner;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class SpawnGlowTrailProjectileClientCommand {
    private static final String ROOT_COMMAND = "mpl";
    private static final String SPAWN_SUBCOMMAND = "spawn";
    private static final String PROJECTILE_SUBCOMMAND = "glow_trail_projectile";

    private SpawnGlowTrailProjectileClientCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(ROOT_COMMAND)
                .then(Commands.literal(SPAWN_SUBCOMMAND)
                        .then(Commands.literal(PROJECTILE_SUBCOMMAND)
                                .executes(context -> spawnProjectile(context.getSource())))));
    }

    private static int spawnProjectile(CommandSourceStack source) {
        Entity entity = source.getEntity();
        if (!(entity instanceof Player player)) {
            source.sendFailure(Component.literal("This command requires a local player."));
            return 0;
        }

        Vec3 from = player.position();
        Vec3 to = from.add(player.getLookAngle().scale(10.0D)).add(0.0D, 3.0D, 0.0D);

        GlowTrailProjectile projectile = new GlowTrailProjectile(player.level(), from, to)
                .colors(ARGB.color(255, 255, 25, 180), ARGB.color(255, 0, 255, 255))
                .size(0.15F)
                .arrivalDistance(0.3F)
                .spawnImpactParticles(true)
                .onArrival(ignored -> source.sendSuccess(() -> Component.literal("Glow trail projectile arrived."), false));

        Vec3 left = player.getLookAngle().cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        projectile.initialVelocity(left.scale(GlowTrailProjectile.DEFAULT_SPEED));
        VisualEntitySpawner.spawn(player.level(), projectile, true);
        source.sendSuccess(() -> Component.literal("Spawned glow trail projectile."), false);
        return 1;
    }
}

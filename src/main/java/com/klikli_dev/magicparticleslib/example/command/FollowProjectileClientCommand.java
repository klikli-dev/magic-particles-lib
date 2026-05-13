// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.example.command;

import com.klikli_dev.magicparticleslib.premade.projectile.FollowProjectile;
import com.klikli_dev.magicparticleslib.premade.projectile.VisualEntitySpawner;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class FollowProjectileClientCommand {
    private static final String COMMAND = "mpl_follow_projectile";

    private FollowProjectileClientCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(COMMAND)
                .executes(context -> spawnProjectile(context.getSource())));
    }

    private static int spawnProjectile(CommandSourceStack source) {
        Entity entity = source.getEntity();
        if (!(entity instanceof Player player)) {
            source.sendFailure(Component.literal("This command requires a local player."));
            return 0;
        }

        Vec3 from = player.position();
        Vec3 to = from.add(player.getLookAngle().scale(10.0D)).add(0.0D, 3.0D, 0.0D);

        FollowProjectile projectile = new FollowProjectile(
                player.level(),
                from,
                to,
                ARGB.color(255, 255, 25, 180),
                ARGB.color(255, 0, 255, 255),
                0.15F
        )
                .arrivalDistance(0.3F)
                .spawnImpactParticles(true);

        projectile.setDeltaMovement(to.subtract(from).normalize().scale(FollowProjectile.DEFAULT_SPEED));
        VisualEntitySpawner.spawn(player.level(), projectile, true);
        source.sendSuccess(() -> Component.literal("Spawned follow projectile."), false);
        return 1;
    }
}

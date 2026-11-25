// SPDX-FileCopyrightText: 2026 klikli-dev
//
// SPDX-License-Identifier: MIT

package com.klikli_dev.magicparticleslib.example.command;

import com.klikli_dev.magicparticleslib.premade.particle.essentiastream.EssentiaStreamParticleOptions;
import com.klikli_dev.magicparticleslib.premade.particle.essentiastream.StreamPort;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Server-side demo of linked mirrors: stream A goes from a source into mirror 1, and stream B leaves mirror 2 and goes
 * around a wall corner to a target. Both streams are spawned in the same tick.
 * <p>
 * It is a server command because it places blocks and item frames. The streams are then sent as particles with
 * {@code ServerLevel.sendParticles}. The waypoint around the corner is written by hand; this demo does not run a pathfinder.
 */
public final class SpawnEssentiaStreamMirrorsCommand {
    private static final String ROOT_COMMAND = "mpl";
    private static final String SPAWN_SUBCOMMAND = "spawn";
    private static final String MIRRORS_SUBCOMMAND = "essentia_stream_mirrors";
    // Layout: the demo is built from the block two east and two south of the player, and its stream plane is this many blocks above it.
    private static final int ORIGIN_OFFSET_X = 2;
    private static final int ORIGIN_OFFSET_Z = 2;
    private static final int PLANE_HEIGHT = 2;
    private static final float RADIUS = EssentiaStreamParticleOptions.DEFAULT_RADIUS;
    private static final int EXTEND = 0;
    private static final int LENGTH = EssentiaStreamParticleOptions.DEFAULT_LENGTH;
    // Random hues are sampled at this saturation and brightness, the same as the client command.
    private static final float RANDOM_SATURATION = 0.6F;
    private static final float RANDOM_BRIGHTNESS = 1.0F;
    // Streams are sent to players within 512 blocks of their source, so nearby players see the demo.
    private static final boolean OVERRIDE_PARTICLE_LIMITER = true;

    private SpawnEssentiaStreamMirrorsCommand() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(ROOT_COMMAND)
                .then(Commands.literal(SPAWN_SUBCOMMAND)
                        .then(Commands.literal(MIRRORS_SUBCOMMAND)
                                .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                                .executes(context -> spawn(context.getSource())))));
    }

    private static int spawn(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command requires a player."));
            return 0;
        }
        ServerLevel level = source.getLevel();
        BlockPos origin = player.blockPosition().offset(ORIGIN_OFFSET_X, 0, ORIGIN_OFFSET_Z);

        // Blocks in the layout. Mirror 1 faces west, mirror 2 faces south, and the target is entered from the west.
        BlockPos sourceBlock = at(origin, 0, 0);
        BlockPos mirror1 = at(origin, 4, 0);
        BlockPos mirror2 = at(origin, 0, 6);
        BlockPos target = at(origin, 5, 9);

        clearFrames(level, at(origin, -1, -1), at(origin, 6, 10));
        level.setBlockAndUpdate(sourceBlock, Blocks.GOLD_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(mirror1, Blocks.SMOOTH_QUARTZ.defaultBlockState());
        level.setBlockAndUpdate(mirror2, Blocks.SMOOTH_QUARTZ.defaultBlockState());
        level.setBlockAndUpdate(target, Blocks.EMERALD_BLOCK.defaultBlockState());
        // The wall blocks the straight line from mirror 2 to the target, so stream B has to turn the corner.
        for (int x = 1; x <= 3; x++) {
            for (int z = 7; z <= 8; z++) {
                level.setBlockAndUpdate(at(origin, x, z), Blocks.STONE_BRICKS.defaultBlockState());
            }
        }
        spawnFrame(level, mirror1, Direction.WEST);
        spawnFrame(level, mirror2, Direction.SOUTH);

        // One colour for the whole chain, so the two streams read as one transfer.
        int color = Mth.hsvToRgb(level.getRandom().nextFloat(), RANDOM_SATURATION, RANDOM_BRIGHTNESS);
        EssentiaStreamParticleOptions streamA = EssentiaStreamParticleOptions.builder(color)
                .exit(StreamPort.nozzle(sourceBlock, Direction.EAST))
                .entry(StreamPort.pane(mirror1, Direction.WEST))
                .radius(RADIUS)
                .extend(EXTEND)
                .length(LENGTH)
                .build();
        EssentiaStreamParticleOptions streamB = EssentiaStreamParticleOptions.builder(color)
                .exit(StreamPort.pane(mirror2, Direction.SOUTH))
                .via(Vec3.atCenterOf(at(origin, 0, 9)))
                .entry(StreamPort.pane(target, Direction.WEST))
                .radius(RADIUS)
                .extend(EXTEND)
                .length(LENGTH)
                .build();

        sendStream(level, streamA);
        sendStream(level, streamB);

        source.sendSuccess(() -> Component.literal(String.format(
                "Spawned the mirror demo at %d %d %d.",
                origin.getX(), origin.getY(), origin.getZ()
        )), false);
        return 1;
    }

    // The block at (x, z) east and south of the origin, on the stream plane.
    private static BlockPos at(BlockPos origin, int x, int z) {
        return origin.offset(x, PLANE_HEIGHT, z);
    }

    // Removes item frames from an earlier run, so repeating the command does not stack frames.
    private static void clearFrames(ServerLevel level, BlockPos min, BlockPos max) {
        AABB area = AABB.encapsulatingFullBlocks(min, max);
        for (ItemFrame frame : level.getEntitiesOfClass(ItemFrame.class, area)) {
            frame.discard();
        }
    }

    // The frame sits in the air block in front of the mirror and faces away from it, which is how vanilla places frames.
    private static void spawnFrame(ServerLevel level, BlockPos mirror, Direction facing) {
        ItemFrame frame = new ItemFrame(level, mirror.relative(facing), facing);
        level.addFreshEntity(frame);
    }

    private static void sendStream(ServerLevel level, EssentiaStreamParticleOptions options) {
        Vec3 from = options.from();
        level.sendParticles(options, OVERRIDE_PARTICLE_LIMITER, false, from.x, from.y, from.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }
}

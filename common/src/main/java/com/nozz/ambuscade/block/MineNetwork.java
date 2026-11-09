package com.nozz.ambuscade.block;

import com.nozz.ambuscade.Ambuscade;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class MineNetwork {
    public static final int MAX_DISTANCE = 256;

    /** Server to client: open the destination screen for the teleport mine at pos. */
    public record OpenDestination(BlockPos mine, BlockPos destination) implements CustomPacketPayload {
        public static final Type<OpenDestination> TYPE = new Type<>(Ambuscade.id("mine_destination_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenDestination> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, OpenDestination::mine, BlockPos.STREAM_CODEC, OpenDestination::destination, OpenDestination::new);

        public Type<OpenDestination> type() { return TYPE; }
    }

    /** Client to server: set where the teleport mine sends its victim. */
    public record SetDestination(BlockPos mine, BlockPos destination) implements CustomPacketPayload {
        public static final Type<SetDestination> TYPE = new Type<>(Ambuscade.id("mine_destination_set"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SetDestination> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, SetDestination::mine, BlockPos.STREAM_CODEC, SetDestination::destination, SetDestination::new);

        public Type<SetDestination> type() { return TYPE; }
    }

    public static void init() {
        if (dev.architectury.platform.Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(OpenDestination.TYPE, OpenDestination.CODEC);
        }
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, SetDestination.TYPE, SetDestination.CODEC, (msg, ctx) -> ctx.queue(() -> {
            if (!(ctx.getPlayer() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
            if (!player.blockPosition().closerThan(msg.mine(), 8) || !valid(level, msg.mine(), msg.destination())) return;
            if (level.getBlockEntity(msg.mine()) instanceof LandmineBlockEntity mine && mine.mayConfigure(player)) {
                mine.setDestination(msg.destination());
            }
        }));
    }

    /** Same dimension, inside the build height and within MAX_DISTANCE blocks of the mine horizontally. */
    public static boolean valid(ServerLevel level, BlockPos mine, BlockPos destination) {
        double dx = destination.getX() - mine.getX(), dz = destination.getZ() - mine.getZ();
        return !level.isOutsideBuildHeight(destination.getY()) && dx * dx + dz * dz <= (double) MAX_DISTANCE * MAX_DISTANCE;
    }

    static void open(ServerPlayer player, BlockPos mine, BlockPos destination) {
        NetworkManager.sendToPlayer(player, new OpenDestination(mine, destination));
    }
}

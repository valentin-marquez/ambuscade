package com.nozz.ambuscade.block;

import com.nozz.ambuscade.Ambuscade;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** A locked door's keypad: the server opens it, the client sends back a code to try or, for the owner, to set. */
public final class Keypad {
    public static final int MIN_DIGITS = 4, MAX_DIGITS = 8;

    public record Open(BlockPos door, boolean setting) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Ambuscade.id("keypad_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Open::door, ByteBufCodecs.BOOL, Open::setting, Open::new);

        public Type<Open> type() { return TYPE; }
    }

    public record Submit(BlockPos door, boolean setting, String code) implements CustomPacketPayload {
        public static final Type<Submit> TYPE = new Type<>(Ambuscade.id("keypad_submit"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Submit> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Submit::door, ByteBufCodecs.BOOL, Submit::setting, ByteBufCodecs.stringUtf8(MAX_DIGITS), Submit::code, Submit::new);

        public Type<Submit> type() { return TYPE; }
    }

    public static boolean valid(String code) {
        return code.length() >= MIN_DIGITS && code.length() <= MAX_DIGITS && code.chars().allMatch(c -> c >= '0' && c <= '9');
    }

    public static void init() {
        if (dev.architectury.platform.Platform.getEnvironment() == Env.SERVER) NetworkManager.registerS2CPayloadType(Open.TYPE, Open.CODEC);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, Submit.TYPE, Submit.CODEC, (msg, ctx) -> ctx.queue(() -> {
            if (!(ctx.getPlayer() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
            if (!player.blockPosition().closerThan(msg.door(), 8) || !valid(msg.code())) return;
            if (!(level.getBlockEntity(msg.door()) instanceof KeypadDoorBlockEntity door)) return;
            submit(level, player, door, msg);
        }));
    }

    /** Whoever fits the lock owns it and sets the first code. */
    public static void claim(ServerPlayer player, KeypadDoorBlockEntity door) {
        door.setOwner(player);
        open(player, door);
    }

    static void open(ServerPlayer player, KeypadDoorBlockEntity door) {
        boolean owner = door.isOwner(player);
        long locked = door.lockedFor(player.level().getGameTime());
        if (!door.hasCode() && !owner) {
            player.sendOverlayMessage(Component.translatable("message.ambuscade.keypad.not_set"));
        } else if (locked > 0 && !owner) {
            player.sendOverlayMessage(Component.translatable("message.ambuscade.keypad.locked", (locked + 19) / 20));
        } else {
            NetworkManager.sendToPlayer(player, new Open(door.getBlockPos(), !door.hasCode() || owner && player.isShiftKeyDown()));
        }
    }

    private static void submit(ServerLevel level, ServerPlayer player, KeypadDoorBlockEntity door, Submit msg) {
        BlockPos pos = door.getBlockPos();
        if (msg.setting()) {
            if (!door.isOwner(player)) return;
            door.setCode(msg.code());
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS, 0.8F, 1.6F);
            player.sendOverlayMessage(Component.translatable("message.ambuscade.keypad.set"));
            return;
        }
        long now = level.getGameTime();
        if (door.lockedFor(now) > 0 && !door.isOwner(player)) return;
        if (door.tryCode(msg.code(), now) && door.getBlockState().getBlock() instanceof KeypadDoorBlock block) {
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.8F, 1.4F);
            block.unlock(level, pos, player);
        } else {
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.0F, 0.5F);
            long locked = door.lockedFor(now);
            player.sendOverlayMessage(locked > 0
                    ? Component.translatable("message.ambuscade.keypad.locked", (locked + 19) / 20)
                    : Component.translatable("message.ambuscade.keypad.wrong"));
        }
    }
}

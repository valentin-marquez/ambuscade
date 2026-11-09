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

/** A keypad on a door or chest: the server opens it, the client sends back a code to try or, for the owner, to set. */
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
            if (!(level.getBlockEntity(msg.door()) instanceof KeypadHolder holder)) return;
            submit(level, player, holder, msg);
        }));
    }

    /** Whoever fits the lock owns it and sets the first code. */
    public static void claim(ServerPlayer player, KeypadHolder holder) {
        holder.keypad().setOwner(player);
        holder.keypadChanged();
        open(player, holder);
    }

    /** An ownerless keypad (e.g. from /setblock) is claimed by the first player who uses it. */
    static boolean isOwner(KeypadHolder holder, ServerPlayer player) {
        if (!holder.keypad().hasOwner()) {
            holder.keypad().setOwner(player);
            holder.keypadChanged();
        }
        return holder.keypad().isOwner(player);
    }

    static void open(ServerPlayer player, KeypadHolder holder) {
        KeypadLock lock = holder.keypad();
        boolean owner = isOwner(holder, player);
        long locked = lock.lockedFor(player.level().getGameTime());
        if (!lock.hasCode() && !owner) {
            player.sendOverlayMessage(Component.translatable("message.ambuscade.keypad.not_set"));
        } else if (locked > 0 && !owner) {
            player.sendOverlayMessage(Component.translatable("message.ambuscade.keypad.locked", (locked + 19) / 20));
        } else {
            NetworkManager.sendToPlayer(player, new Open(holder.getBlockPos(), !lock.hasCode() || owner && player.isShiftKeyDown()));
        }
    }

    private static void submit(ServerLevel level, ServerPlayer player, KeypadHolder holder, Submit msg) {
        BlockPos pos = holder.getBlockPos();
        KeypadLock lock = holder.keypad();
        if (msg.setting()) {
            if (!isOwner(holder, player)) return;
            lock.setCode(msg.code());
            holder.keypadChanged();
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS, 0.8F, 1.6F);
            player.sendOverlayMessage(Component.translatable("message.ambuscade.keypad.set"));
            return;
        }
        long now = level.getGameTime();
        if (lock.lockedFor(now) > 0 && !isOwner(holder, player)) return;
        boolean right = lock.tryCode(msg.code(), now);
        holder.keypadChanged();
        if (right) {
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.8F, 1.4F);
            holder.unlock(level, player);
        } else {
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.0F, 0.5F);
            long locked = lock.lockedFor(now);
            player.sendOverlayMessage(locked > 0
                    ? Component.translatable("message.ambuscade.keypad.locked", (locked + 19) / 20)
                    : Component.translatable("message.ambuscade.keypad.wrong"));
        }
    }
}

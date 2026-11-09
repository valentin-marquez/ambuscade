package com.nozz.ambuscade.ledger;

import com.nozz.ambuscade.Ambuscade;
import com.nozz.ambuscade.AmbuscadeRegistry;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

public final class LedgerNetwork {
    public record Trap(GlobalPos pos, String block) {
        static final StreamCodec<RegistryFriendlyByteBuf, Trap> CODEC = StreamCodec.composite(
                GlobalPos.STREAM_CODEC, Trap::pos, ByteBufCodecs.stringUtf8(64), Trap::block, Trap::new);
    }

    /** Server to client: open (or refresh) the ledger screen. */
    public record Open(UUID id, boolean target, List<String> names, int creatures, List<Trap> traps) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Ambuscade.id("ledger_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Open::id, ByteBufCodecs.BOOL, Open::target,
                ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8, Ledger.MAX_NAMES), Open::names,
                ByteBufCodecs.VAR_INT, Open::creatures,
                ByteBufCodecs.collection(ArrayList::new, Trap.CODEC, Ledger.MAX_TRAPS), Open::traps, Open::new);

        public boolean has(Ledger.Creature creature) {
            return (creatures & (1 << creature.ordinal())) != 0;
        }

        public Type<Open> type() { return TYPE; }
    }

    /** Client to server: one edit. 0 add name, 1 remove name, 2 toggle mode, 3 toggle creature (name), 4 unlink trap (pos). */
    public record Edit(UUID id, int action, String name, GlobalPos pos) implements CustomPacketPayload {
        public static final Type<Edit> TYPE = new Type<>(Ambuscade.id("ledger_edit"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Edit> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Edit::id, ByteBufCodecs.VAR_INT, Edit::action, ByteBufCodecs.stringUtf8(16), Edit::name,
                GlobalPos.STREAM_CODEC, Edit::pos, Edit::new);
        public static final GlobalPos NOWHERE = GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO);

        public Type<Edit> type() { return TYPE; }
    }

    static Ledger.Where where(GlobalPos pos) {
        return new Ledger.Where(pos.dimension().identifier().toString(), pos.pos());
    }

    static GlobalPos global(Ledger.Where where) {
        return GlobalPos.of(ResourceKey.create(Registries.DIMENSION, Identifier.parse(where.dimension())), where.pos());
    }

    public static void init() {
        // the client registers this type through its receiver; registering it twice there is an error
        if (dev.architectury.platform.Platform.getEnvironment() == Env.SERVER) NetworkManager.registerS2CPayloadType(Open.TYPE, Open.CODEC);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, Edit.TYPE, Edit.CODEC, (edit, ctx) -> ctx.queue(() -> {
            if (!(ctx.getPlayer() instanceof ServerPlayer player) || !holds(player, edit.id())) return;
            LedgerData data = LedgerData.get((ServerLevel) player.level());
            Ledger ledger = data.find(edit.id());
            if (ledger == null) return;
            switch (edit.action()) {
                case 0 -> ledger.add(edit.name());
                case 1 -> ledger.remove(edit.name());
                case 2 -> ledger.toggleMode();
                case 3 -> {
                    for (Ledger.Creature c : Ledger.Creature.values()) if (c.name().equals(edit.name())) ledger.toggle(c);
                }
                case 4 -> unlink(player, edit.id(), ledger, edit.pos());
                default -> { return; }
            }
            data.setDirty();
            open(player, edit.id(), ledger);
        }));
    }

    public static void open(ServerPlayer player, UUID id, Ledger ledger) {
        prune((ServerLevel) player.level(), id, ledger);
        int mask = 0;
        for (Ledger.Creature c : ledger.creatures()) mask |= 1 << c.ordinal();
        List<Trap> traps = ledger.traps().stream().map(t -> new Trap(global(t.where()), t.block())).toList();
        NetworkManager.sendToPlayer(player, new Open(id, ledger.targetMode(), ledger.names(), mask, traps));
    }

    // traps that blew up, were broken or relinked elsewhere drop off; unloaded chunks are left alone
    private static void prune(ServerLevel any, UUID id, Ledger ledger) {
        boolean changed = false;
        for (Ledger.LinkedTrap t : ledger.traps()) {
            GlobalPos at = global(t.where());
            ServerLevel level = any.getServer().getLevel(at.dimension());
            if (level == null || !level.isLoaded(at.pos())) continue;
            if (!(level.getBlockEntity(at.pos()) instanceof LedgerLink link) || !id.equals(link.linkedLedger())) {
                changed |= ledger.unlink(t.where());
            }
        }
        if (changed) LedgerData.get(any).setDirty();
    }

    private static void unlink(ServerPlayer player, UUID id, Ledger ledger, GlobalPos pos) {
        if (!ledger.unlink(where(pos))) return;
        ServerLevel level = player.level().getServer().getLevel(pos.dimension());
        if (level != null && level.isLoaded(pos.pos()) && level.getBlockEntity(pos.pos()) instanceof LedgerLink link && id.equals(link.linkedLedger())) {
            link.linkLedger(null);
        }
    }

    public static boolean holdsLedger(net.minecraft.world.entity.player.Player player, UUID id) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (id.equals(player.getItemInHand(hand).get(AmbuscadeRegistry.LEDGER_ID.get()))) return true;
        }
        return false;
    }

    // only someone holding a copy of this ledger may edit it
    private static boolean holds(ServerPlayer player, UUID id) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (id.equals(player.getItemInHand(hand).get(AmbuscadeRegistry.LEDGER_ID.get()))) return true;
        }
        return false;
    }
}

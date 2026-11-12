package com.nozz.ambuscade.ledger;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.LoggerFactory;

/** Every ledger in the world, saved with the overworld so all dimensions share them. */
public final class LedgerData extends SavedData {
    private static final Codec<Map<UUID, Ledger>> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Ledger.CODEC);
    private static final SavedData.Factory<LedgerData> FACTORY = new SavedData.Factory<>(LedgerData::new, LedgerData::load, null);

    private final Map<UUID, Ledger> ledgers;

    private LedgerData() {
        this(new HashMap<>());
    }

    private LedgerData(Map<UUID, Ledger> ledgers) {
        this.ledgers = new HashMap<>(ledgers);
    }

    public static LedgerData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, "ambuscade_ledgers");
    }

    // a ledger that fails to read is dropped with a warning; the rest still load
    private static LedgerData load(CompoundTag tag, HolderLookup.Provider registries) {
        if (!tag.contains("ledgers")) return new LedgerData();
        return new LedgerData(CODEC.parse(NbtOps.INSTANCE, tag.get("ledgers"))
                .resultOrPartial(e -> LoggerFactory.getLogger("ambuscade").warn("Ledgers: {}", e)).orElseGet(HashMap::new));
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("ledgers", CODEC.encodeStart(NbtOps.INSTANCE, ledgers).getOrThrow());
        return tag;
    }

    public Ledger find(UUID id) {
        return id == null ? null : ledgers.get(id);
    }

    public UUID create(String creator) {
        UUID id = UUID.randomUUID();
        ledgers.put(id, Ledger.blank(creator));
        setDirty();
        return id;
    }
}

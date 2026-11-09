package com.nozz.ambuscade.ledger;

import com.mojang.serialization.Codec;
import com.nozz.ambuscade.Ambuscade;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Every ledger in the world, saved with the overworld so all dimensions share them. */
public final class LedgerData extends SavedData {
    private static final Codec<LedgerData> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Ledger.CODEC)
            .xmap(LedgerData::new, d -> d.ledgers);
    private static final SavedDataType<LedgerData> TYPE = new SavedDataType<>(Ambuscade.id("ledgers"), LedgerData::new, CODEC, null);

    private final Map<UUID, Ledger> ledgers;

    private LedgerData() {
        this(new HashMap<>());
    }

    private LedgerData(Map<UUID, Ledger> ledgers) {
        this.ledgers = new HashMap<>(ledgers);
    }

    public static LedgerData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
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

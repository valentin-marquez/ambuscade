package com.nozz.ambuscade.ledger;

import java.util.UUID;
import net.minecraft.world.entity.player.Player;

/** A trap block entity a hunter's ledger can be linked to. */
public interface LedgerLink {
    void linkLedger(UUID ledger);

    /** The ledger this trap follows, or null. */
    UUID linkedLedger();

    /** Whether this player may (re)link the trap: its placer, or whoever holds the ledger it already follows. */
    boolean mayLink(Player player, UUID heldLedger);
}

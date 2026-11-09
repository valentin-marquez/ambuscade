package com.nozz.ambuscade.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nozz.ambuscade.ledger.Ledger.Creature;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class LedgerTest {
    private static final Set<Creature> NONE = Set.of();

    private static Ledger of(boolean target, List<String> names, List<String> creatures) {
        Ledger.Side side = new Ledger.Side(names, creatures), empty = new Ledger.Side(List.of(), List.of());
        return new Ledger(target, target ? empty : side, target ? side : empty, List.of());
    }

    @Test
    void protectModeSparesListedPlayers() {
        Ledger l = of(false, List.of("Ally"), List.of());
        assertFalse(l.affects("ally", NONE));
        assertTrue(l.affects("Stranger", NONE));
        assertTrue(l.affects(null, NONE));
    }

    @Test
    void targetModeOnlyCatchesListedPlayers() {
        Ledger l = of(true, List.of("Rival"), List.of());
        assertTrue(l.affects("RIVAL", NONE));
        assertFalse(l.affects("Stranger", NONE));
        assertFalse(l.affects(null, NONE));
    }

    @Test
    void creatorIsProtectedButCanBeRemoved() {
        Ledger l = Ledger.blank("Valentin");
        assertFalse(l.affects("valentin", NONE));
        assertFalse(l.affects(null, Set.of(Creature.PETS)));
        l.remove("Valentin");
        assertTrue(l.affects("valentin", NONE));
    }

    @Test
    void eachModeKeepsItsOwnList() {
        Ledger l = Ledger.blank("Valentin");
        l.add("Ally");
        l.toggleMode();
        assertEquals(List.of(), l.names());
        l.add("Rival");
        l.toggle(Creature.MONSTERS);
        assertTrue(l.affects("rival", NONE));
        assertFalse(l.affects("ally", NONE));
        assertTrue(l.affects(null, Set.of(Creature.MONSTERS)));
        assertFalse(l.affects(null, Set.of(Creature.FARM)));
        l.toggleMode();
        assertEquals(List.of("ally", "valentin"), l.names());
        assertFalse(l.affects("ally", NONE));
        assertTrue(l.affects("rival", NONE));
    }

    @Test
    void linkingProtectsTheHolderInAnyMode() {
        Ledger l = of(true, List.of(), List.of());
        assertTrue(l.protect("Holder"));
        l.toggleMode();
        assertFalse(l.affects("holder", NONE));
    }

    @Test
    void ignoresUnknownCategories() {
        assertEquals(Set.of(Creature.MONSTERS), of(false, List.of(), List.of("DRAGONS", "MONSTERS")).creatures());
    }

    @Test
    void rejectsInvalidNames() {
        Ledger l = of(false, List.of(), List.of());
        assertFalse(l.add("a b"));
        assertFalse(l.add("xy"));
        assertTrue(l.add("Steve_99"));
        assertFalse(l.add("steve_99"));
    }

    @Test
    void capsTheListAtTheNetworkLimit() {
        Ledger l = of(false, IntStream.range(0, 300).mapToObj(i -> "player" + i).toList(), List.of());
        assertEquals(Ledger.MAX_NAMES, l.names().size());
        assertFalse(l.add("one_more"));
    }

    @Test
    void tracksLinkedTrapsOncePerPosition() {
        Ledger l = of(false, List.of(), List.of());
        Ledger.Where a = new Ledger.Where("minecraft:overworld", new BlockPos(1, 2, 3));
        assertTrue(l.link(a, "ambuscade:bear_trap"));
        assertTrue(l.link(a, "ambuscade:bear_trap"));
        l.link(new Ledger.Where("minecraft:the_nether", new BlockPos(1, 2, 3)), "ambuscade:explosive_mine");
        assertEquals(2, l.traps().size());
        assertTrue(l.unlink(a));
        assertFalse(l.unlink(a));
        assertEquals(1, l.traps().size());
    }
}

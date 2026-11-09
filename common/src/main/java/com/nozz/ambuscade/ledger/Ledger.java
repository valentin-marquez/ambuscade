package com.nozz.ambuscade.ledger;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;

/**
 * One perimeter ledger: who its traps leave alone (protect mode) or go after (target mode), and which traps follow it.
 * Whoever holds a copy controls it; nobody is immune unless listed.
 */
public final class Ledger {
    public enum Creature { PETS, FARM, VILLAGERS, MONSTERS }

    /** dimension is the level id as text so this class stays free of game bootstrap. */
    public record Where(String dimension, BlockPos pos) {
    }

    public record LinkedTrap(Where where, String block) {
        static final Codec<LinkedTrap> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("dimension").forGetter(t -> t.where().dimension()),
                BlockPos.CODEC.fieldOf("pos").forGetter(t -> t.where().pos()),
                Codec.STRING.fieldOf("block").forGetter(LinkedTrap::block)
        ).apply(i, (dim, pos, block) -> new LinkedTrap(new Where(dim, pos), block)));
    }

    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{3,16}");
    public static final int MAX_NAMES = 256;   // the screen packet carries at most this many
    public static final int MAX_TRAPS = 512;

    public static final Codec<Ledger> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.fieldOf("target").forGetter(l -> l.targetMode),
            Side.CODEC.fieldOf("protect").forGetter(l -> l.protect),
            Side.CODEC.fieldOf("targets").forGetter(l -> l.targets),
            LinkedTrap.CODEC.listOf().optionalFieldOf("traps", List.of()).forGetter(Ledger::traps)
    ).apply(i, Ledger::new));

    /** One mode's list: the protect whitelist or the target list, each with its own players and creatures. */
    public static final class Side {
        static final Codec<Side> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.listOf().fieldOf("names").forGetter(s -> new ArrayList<>(s.names)),
                Codec.STRING.listOf().optionalFieldOf("creatures", List.of()).forGetter(s -> s.creatures.stream().map(Enum::name).toList())
        ).apply(i, Side::new));

        private final TreeSet<String> names = new TreeSet<>();
        private final EnumSet<Creature> creatures = EnumSet.noneOf(Creature.class);

        public Side(List<String> names, List<String> creatures) {
            names.stream().limit(MAX_NAMES).forEach(n -> this.names.add(n.toLowerCase(Locale.ROOT)));
            for (String c : creatures) {
                try {
                    this.creatures.add(Creature.valueOf(c));
                } catch (IllegalArgumentException ignored) {
                    // unknown category from a newer version: drop it
                }
            }
        }

        boolean lists(String playerName, Set<Creature> kinds) {
            return (playerName != null && names.contains(playerName.toLowerCase(Locale.ROOT))) || kinds.stream().anyMatch(creatures::contains);
        }
    }

    private boolean targetMode;
    private final Side protect, targets;
    private final Map<Where, String> traps = new LinkedHashMap<>();

    public Ledger(boolean targetMode, Side protect, Side targets, List<LinkedTrap> traps) {
        this.targetMode = targetMode;
        this.protect = protect;
        this.targets = targets;
        traps.stream().limit(MAX_TRAPS).forEach(t -> this.traps.put(t.where(), t.block()));
    }

    /** A fresh ledger protects its creator and pets. A thief holding it can remove the creator. */
    public static Ledger blank(String creator) {
        return new Ledger(false, new Side(validName(creator) ? List.of(creator) : List.of(), List.of(Creature.PETS.name())),
                new Side(List.of(), List.of()), List.of());
    }

    /** playerName is null for mobs; kinds are the creature categories the entity falls in. */
    public boolean affects(String playerName, Set<Creature> kinds) {
        return targetMode ? targets.lists(playerName, kinds) : !protect.lists(playerName, kinds);
    }

    private Side side() {
        return targetMode ? targets : protect;
    }

    public static boolean validName(String name) {
        return name != null && NAME.matcher(name).matches();
    }

    /** Adds to the list of the current mode. */
    public boolean add(String name) {
        Side s = side();
        return validName(name) && s.names.size() < MAX_NAMES && s.names.add(name.toLowerCase(Locale.ROOT));
    }

    public boolean remove(String name) {
        return name != null && side().names.remove(name.toLowerCase(Locale.ROOT));
    }

    /** Puts a player on the protect whitelist whatever the current mode (used when they link a trap). */
    public boolean protect(String name) {
        return validName(name) && protect.names.size() < MAX_NAMES && protect.names.add(name.toLowerCase(Locale.ROOT));
    }

    public void toggleMode() {
        targetMode = !targetMode;
    }

    public void toggle(Creature creature) {
        Side s = side();
        if (!s.creatures.remove(creature)) s.creatures.add(creature);
    }

    public boolean link(Where pos, String block) {
        if (!traps.containsKey(pos) && traps.size() >= MAX_TRAPS) return false;
        traps.put(pos, block);
        return true;
    }

    public boolean unlink(Where pos) {
        return traps.remove(pos) != null;
    }

    public boolean targetMode() {
        return targetMode;
    }

    public List<String> names() {
        return List.copyOf(side().names);
    }

    public Set<Creature> creatures() {
        EnumSet<Creature> c = side().creatures;
        return c.isEmpty() ? EnumSet.noneOf(Creature.class) : EnumSet.copyOf(c);
    }

    public List<LinkedTrap> traps() {
        List<LinkedTrap> out = new ArrayList<>();
        traps.forEach((pos, block) -> out.add(new LinkedTrap(pos, block)));
        return out;
    }
}

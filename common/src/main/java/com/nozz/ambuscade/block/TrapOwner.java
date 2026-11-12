package com.nozz.ambuscade.block;

import com.nozz.ambuscade.ledger.Ledger;
import com.nozz.ambuscade.ledger.Ledger.Creature;
import com.nozz.ambuscade.ledger.LedgerData;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.PlayerTeam;

/** Who a trap spares: its linked hunter's ledger if any, otherwise the placer and their scoreboard team. */
public final class TrapOwner {
    private UUID uuid;
    private String name = "";
    private UUID ledger;

    void link(UUID ledgerId) {
        ledger = ledgerId;
    }

    UUID ledger() {
        return ledger;
    }

    boolean linked(Level level) {
        return ledger != null && level instanceof ServerLevel server && LedgerData.get(server).find(ledger) != null;
    }

    void set(Player player) {
        uuid = player.getUUID();
        name = player.getScoreboardName();
    }

    /** A linked trap answers to whoever holds its ledger; an unlinked one to whoever placed it. */
    boolean mayLink(Level level, Player player, UUID heldLedger) {
        if (ledger != null && level instanceof ServerLevel server && LedgerData.get(server).find(ledger) != null) {
            return ledger.equals(heldLedger);
        }
        return uuid == null || uuid.equals(player.getUUID());
    }

    public boolean canCatch(Level level, LivingEntity entity) {
        if (ledger != null && level instanceof ServerLevel server) {
            Ledger l = LedgerData.get(server).find(ledger);
            if (l != null) return l.affects(entity instanceof Player p ? p.getScoreboardName() : null, kinds(entity));
        }
        if (uuid == null) return true;
        if (entity instanceof OwnableEntity pet && uuid.equals(pet.getOwnerUUID())) return false;
        if (entity.getUUID().equals(uuid)) return false;
        PlayerTeam team = level.getScoreboard().getPlayersTeam(name);
        return team == null || !entity.isAlliedTo(team);
    }

    static Set<Creature> kinds(LivingEntity entity) {
        EnumSet<Creature> kinds = EnumSet.noneOf(Creature.class);
        boolean pet = entity instanceof OwnableEntity o && o.getOwnerUUID() != null;
        if (pet) kinds.add(Creature.PETS);
        else if (entity instanceof Animal) kinds.add(Creature.FARM);
        if (entity instanceof AbstractVillager || entity instanceof IronGolem || entity instanceof SnowGolem) kinds.add(Creature.VILLAGERS);
        if (entity instanceof Enemy) kinds.add(Creature.MONSTERS);
        return kinds;
    }

    /** A snapshot for things that outlive the block, like shells in flight. */
    public TrapOwner copy() {
        TrapOwner c = new TrapOwner();
        c.uuid = uuid;
        c.name = name;
        c.ledger = ledger;
        return c;
    }

    public void save(CompoundTag tag) {
        if (ledger != null) tag.putUUID("ledger", ledger);
        if (uuid == null) return;
        tag.putUUID("owner", uuid);
        tag.putString("owner_name", name);
    }

    public void load(CompoundTag tag) {
        ledger = tag.hasUUID("ledger") ? tag.getUUID("ledger") : null;
        uuid = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        name = tag.getString("owner_name");
    }
}

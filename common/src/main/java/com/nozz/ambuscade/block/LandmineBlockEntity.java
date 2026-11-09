package com.nozz.ambuscade.block;

import com.nozz.ambuscade.AmbuscadeRegistry;
import com.nozz.ambuscade.ledger.LedgerLink;
import com.nozz.ambuscade.ledger.LedgerNetwork;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class LandmineBlockEntity extends BlockEntity implements LedgerLink {
    private static final int VENT_TICKS = 200;
    private static final double GAS_RADIUS = 3.5;
    private static final DustParticleOptions GAS_BODY = new DustParticleOptions(0x4F7A28, 4.0F);   // thick, dark core
    private static final DustParticleOptions GAS_EDGE = new DustParticleOptions(0x9BC94E, 2.5F);   // lighter wisps

    private final TrapOwner owner = new TrapOwner();
    private int venting;
    private int victim = -1;
    private BlockPos destination;

    public LandmineBlockEntity(BlockPos pos, BlockState state) {
        super(AmbuscadeRegistry.LANDMINE_ENTITY.get(), pos, state);
    }

    public void setOwner(Player player) {
        owner.set(player);
        setChanged();
    }

    @Override
    public void linkLedger(UUID ledger) {
        owner.link(ledger);
        setChanged();
    }

    @Override
    public boolean mayLink(Player player, UUID heldLedger) {
        return level != null && owner.mayLink(level, player, heldLedger);
    }

    @Override
    public UUID linkedLedger() {
        return owner.ledger();
    }

    void setVictim(LivingEntity entity) {
        victim = entity.getId();
    }

    LivingEntity victim() {
        return level != null && level.getEntity(victim) instanceof LivingEntity e && e.isAlive() ? e : null;
    }

    public BlockPos destination() {
        return destination;
    }

    public void setDestination(BlockPos pos) {
        destination = pos;
        setChanged();
    }

    /** Same rule as relinking: the placer, or whoever holds the ledger this mine follows. */
    public boolean mayConfigure(Player player) {
        UUID linked = owner.ledger();
        return level != null && owner.mayLink(level, player, linked != null && LedgerNetwork.holdsLedger(player, linked) ? linked : null);
    }

    void startVenting() {
        venting = VENT_TICKS;
        setChanged();
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, LandmineBlockEntity mine) {
        if (mine.venting <= 0 || !(level instanceof ServerLevel server)) return;
        mine.venting--;
        Vec3 c = Vec3.atCenterOf(pos);
        if (mine.venting % 2 == 0) {
            // a heavy gas: dense near the ground, thinning towards the edge of the radius
            server.sendParticles(GAS_BODY, c.x, c.y - 0.2, c.z, 18, GAS_RADIUS * 0.35, 0.35, GAS_RADIUS * 0.35, 0.0);
            server.sendParticles(GAS_EDGE, c.x, c.y + 0.2, c.z, 12, GAS_RADIUS * 0.55, 0.6, GAS_RADIUS * 0.55, 0.01);
        }
        if (mine.venting % 20 == 0) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(GAS_RADIUS))) {
                if (!mine.canCatch(e)) continue;
                e.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1));
                e.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 120, 0));
                e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
            }
        }
        if (mine.venting == 0) level.removeBlock(pos, false);
    }

    public boolean canCatch(LivingEntity entity) {
        return level == null || owner.canCatch(level, entity);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        owner.save(output);
        if (venting > 0) output.putInt("venting", venting);
        if (destination != null) output.store("destination", BlockPos.CODEC, destination);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        owner.load(input);
        venting = input.getIntOr("venting", 0);
        destination = input.read("destination", BlockPos.CODEC).orElse(null);
    }
}

package com.nozz.ambuscade.entity;

import com.nozz.ambuscade.AmbuscadeRegistry;
import com.nozz.ambuscade.block.TrapOwner;
import net.minecraft.core.particles.DustParticleOptions;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Lobbed by a mortar; marks where it will come down while in the air and blows up on whatever it hits first. */
public class MortarShell extends AbstractArrow {
    private static final float POWER = 2.0F;
    private static final DustParticleOptions MARK = new DustParticleOptions(Vec3.fromRGB24(0xE6E9F5).toVector3f(), 1.2F);
    /** What the ender pearl ticket does on newer versions: keeps a chunk simulated, lapsing 40 ticks after its last renewal. */
    private static final TicketType<ChunkPos> FLIGHT = TicketType.create("ambuscade_mortar_shell", Comparator.comparingLong(ChunkPos::toLong), 40);
    private TrapOwner owner;   // who it spares, copied at launch; null after a reload, which spares every creature
    private Vec3 marker;
    private ChunkPos loaded;

    public MortarShell(EntityType<? extends MortarShell> type, Level level) {
        super(type, level);
    }

    public MortarShell(Level level, Vec3 at, TrapOwner owner, Vec3 marker) {
        super(AmbuscadeRegistry.MORTAR_SHELL.get(), at.x, at.y, at.z, level, new ItemStack(Items.ARROW), null);
        this.owner = owner;
        this.marker = marker;
        pickup = Pickup.DISALLOWED;
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(Items.ARROW);
    }

    @Override
    public void tick() {
        // keep the shell's chunk simulated, so a long shot does not freeze where no player is near. A FLIGHT ticket
        // only simulates the chunk it is placed on, and a frozen shell cannot place the next one, so the chunk it reaches two
        // ticks ahead is claimed before it gets there. Tickets lapse 40 ticks after their last renewal: nothing stays loaded
        if (level() instanceof ServerLevel server) {
            ChunkPos ahead = new ChunkPos(BlockPos.containing(position().add(getDeltaMovement().scale(2))));
            if (!ahead.equals(loaded) || tickCount % 20 == 0) {
                server.getChunkSource().addRegionTicket(FLIGHT, chunkPosition(), 2, chunkPosition());
                server.getChunkSource().addRegionTicket(FLIGHT, ahead, 2, ahead);
                loaded = ahead;
            }
        }
        super.tick();
        if (marker != null && tickCount % 3 == 0 && level() instanceof ServerLevel server) {
            // a ring with four inward ticks on the ground, so whoever is under it can run
            for (int i = 0; i < 20; i++) {
                double a = Math.PI * 2 * i / 20;
                server.sendParticles(MARK, marker.x + Math.cos(a) * 2, marker.y + 0.1, marker.z + Math.sin(a) * 2, 1, 0, 0, 0, 0);
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI / 2 * i;
                server.sendParticles(MARK, marker.x + Math.cos(a) * 1.4, marker.y + 0.1, marker.z + Math.sin(a) * 1.4, 1, 0, 0, 0, 0);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        explode();
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        explode();
    }

    private boolean catches(ServerLevel level, LivingEntity living) {
        return owner != null && owner.canCatch(level, living);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (owner == null) return;
        CompoundTag o = new CompoundTag();
        owner.save(o);
        tag.put("owner", o);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (!tag.contains("owner")) return;
        owner = new TrapOwner();
        owner.load(tag.getCompound("owner"));
    }

    private void explode() {
        if (!(level() instanceof ServerLevel server) || isRemoved()) return;
        ExplosionDamageCalculator spare = new ExplosionDamageCalculator() {
            @Override
            public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
                return !(entity instanceof LivingEntity living) || catches(server, living);
            }

            @Override
            public float getKnockbackMultiplier(Entity entity) {
                return entity instanceof LivingEntity living && !catches(server, living) ? 0 : 1;
            }
        };
        server.explode(this, null, spare, position(), POWER, false, Level.ExplosionInteraction.TNT);
        discard();
    }
}

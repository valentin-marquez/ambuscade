package com.nozz.ambuscade.entity;

import com.nozz.ambuscade.AmbuscadeRegistry;
import com.nozz.ambuscade.block.TrapOwner;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Lobbed by a mortar; marks where it will come down while in the air and blows up on whatever it hits first. */
public class MortarShell extends AbstractArrow {
    private static final float POWER = 2.0F;
    private static final DustParticleOptions MARK = new DustParticleOptions(0xE6E9F5, 1.2F);
    private TrapOwner owner;   // who it spares, copied at launch; null after a reload, which spares every creature
    private Vec3 marker;

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
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (owner != null) owner.save(output.child("owner"));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.child("owner").ifPresent(o -> {
            owner = new TrapOwner();
            owner.load(o);
        });
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

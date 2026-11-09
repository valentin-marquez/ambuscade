package com.nozz.ambuscade.entity;

import com.nozz.ambuscade.AmbuscadeRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/** What a sentry fires for each arrow it loads: tipped arrows keep their potion, spectral ones make the target glow.
 *  Never picked up, gone once it hits a block, and each bolt of a burst lands. */
public class SteelBolt extends Arrow {
    public SteelBolt(EntityType<? extends SteelBolt> type, Level level) {
        super(type, level);
    }

    public SteelBolt(Level level, double x, double y, double z, ItemStack ammo) {
        super(AmbuscadeRegistry.STEEL_BOLT.get(), level);
        setPos(x, y, z);
        setPickupItemStack(ammo.is(Items.SPECTRAL_ARROW) ? new ItemStack(Items.ARROW) : ammo);
        if (ammo.is(Items.SPECTRAL_ARROW)) addEffect(new MobEffectInstance(MobEffects.GLOWING, 200));
        pickup = Pickup.DISALLOWED;
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(Items.ARROW);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        // a burst lands within the victim's hurt cooldown; without this only the first bolt would count
        if (hit.getEntity() instanceof LivingEntity victim) victim.invulnerableTime = 0;
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (!level().isClientSide()) discard();
    }
}

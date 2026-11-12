package com.nozz.ambuscade.block;

import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import com.nozz.ambuscade.AmbuscadeRegistry;
import com.nozz.ambuscade.ledger.LedgerLink;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BearTrapBlockEntity extends BlockEntity implements GeoBlockEntity, LedgerLink {
    public static final String CONTROLLER = "main";
    private static final RawAnimation OPEN = RawAnimation.begin().thenPlayAndHold("animation.bear_trap.open");
    private static final RawAnimation CLOSED = RawAnimation.begin().thenPlayAndHold("animation.bear_trap.closed");
    private static final RawAnimation SNAP = RawAnimation.begin().thenPlayAndHold("animation.bear_trap.snap");
    private static final RawAnimation ARM = RawAnimation.begin().thenPlayAndHold("animation.bear_trap.arm");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final TrapOwner owner = new TrapOwner();
    private int victim = -1;
    private int holdTicks;

    public BearTrapBlockEntity(BlockPos pos, BlockState state) {
        super(AmbuscadeRegistry.BEAR_TRAP_ENTITY.get(), pos, state);
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

    public boolean canCatch(LivingEntity entity) {
        return level == null || owner.canCatch(level, entity);
    }

    public void hold(LivingEntity entity, int ticks) {
        victim = entity.getId();
        holdTicks = ticks;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BearTrapBlockEntity trap) {
        if (trap.holdTicks <= 0) return;
        trap.holdTicks--;
        Entity entity = level.getEntity(trap.victim);
        if (entity == null || !entity.isAlive()) {
            trap.holdTicks = 0;
            return;
        }
        double x = pos.getX() + 0.5, z = pos.getZ() + 0.5;
        if (entity.distanceToSqr(x, entity.getY(), z) > 0.04) entity.teleportTo(x, entity.getY(), z);
        entity.setDeltaMovement(0, Math.min(entity.getDeltaMovement().y, 0), 0);
        entity.hurtMarked = true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<BearTrapBlockEntity>(this, CONTROLLER,
                test -> test.setAndContinue(getBlockState().getValue(BearTrapBlock.ARMED) ? OPEN : CLOSED))
                .triggerableAnim("snap", SNAP)
                .triggerableAnim("arm", ARM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        owner.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner.load(tag);
    }
}

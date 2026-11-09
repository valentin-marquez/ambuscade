package com.nozz.ambuscade.block;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.nozz.ambuscade.Ambuscade;
import com.nozz.ambuscade.AmbuscadeRegistry;
import com.nozz.ambuscade.entity.SteelBolt;
import com.nozz.ambuscade.ledger.LedgerLink;
import java.util.Comparator;
import java.util.stream.IntStream;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Fires one bolt per arrow loaded into it; hoppers can feed it but not drain it, and a redstone signal holds fire. */
public class SentryBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, GeoBlockEntity, LedgerLink, Aimed {
    public static final String CONTROLLER = "main";
    private static final RawAnimation ALERT = RawAnimation.begin().thenPlay("animation.sentry.alert");
    private static final RawAnimation FIRE = RawAnimation.begin().thenPlay("animation.sentry.fire");
    private static final int WARMUP = 10, SHOT_GAP = 3, SCAN_EVERY = 5;
    private static final float TURN = 18, TILT = 12;   // degrees per tick, the same on both sides
    private static final float ON_TARGET = 6;
    private static final double MUZZLE_Y = 15.5 / 16;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final TrapOwner owner = new TrapOwner();
    private NonNullList<ItemStack> items;
    // server
    private LivingEntity target;
    private int scan, warmup, cooldown, burstLeft;
    private float aimYaw;   // where the drawn turret points, followed here so it only fires once facing the target
    // client
    private int clientTarget = -1, age;
    private float yaw, prevYaw, pitch, prevPitch;

    public SentryBlockEntity(BlockPos pos, BlockState state) {
        super(AmbuscadeRegistry.SENTRY_ENTITY.get(), pos, state);
        items = NonNullList.withSize(state.getValue(SentryBlock.TIER).slots, ItemStack.EMPTY);
    }

    /** A tier change keeps this block entity; the magazine only grows, so items keep their slots. */
    @Override
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
        int slots = state.getValue(SentryBlock.TIER).slots;
        if (items == null || items.size() >= slots) return;
        NonNullList<ItemStack> bigger = NonNullList.withSize(slots, ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) bigger.set(i, items.get(i));
        items = bigger;
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

    private Vec3 muzzle() {
        return Vec3.atBottomCenterOf(worldPosition).add(0, MUZZLE_Y, 0);
    }

    private static Vec3 aimPoint(Entity e) {
        return e.position().add(0, e.getBbHeight() * 0.6, 0);
    }

    // ---- server ----------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, SentryBlockEntity s) {
        ServerLevel server = (ServerLevel) level;
        SentryTier tier = state.getValue(SentryBlock.TIER);
        if (level.hasNeighborSignal(pos) || !s.hasAmmo()) {
            s.setTarget(null);
            return;
        }
        if (s.warmup > 0) s.warmup--;
        if (s.cooldown > 0) s.cooldown--;
        if (--s.scan <= 0) {
            s.scan = SCAN_EVERY;
            s.setTarget(s.target != null && s.valid(server, s.target, tier) ? s.target : s.find(server, tier));
        }
        if (s.target == null) return;
        float want = s.yawTo(s.target);
        float off = Mth.wrapDegrees(want - s.aimYaw);
        s.aimYaw += Mth.clamp(off, -TURN, TURN);
        if (s.warmup > 0 || s.cooldown > 0 || Math.abs(off) > ON_TARGET) return;
        if (!s.valid(server, s.target, tier)) {
            s.setTarget(null);
            return;
        }
        s.fire(server, tier);
        if (s.burstLeft <= 0) s.burstLeft = tier.burst;
        s.cooldown = --s.burstLeft > 0 ? SHOT_GAP : tier.cooldown;
    }

    private boolean valid(ServerLevel level, LivingEntity e, SentryTier tier) {
        if (!e.isAlive() || e.isSpectator() || e instanceof ArmorStand) return false;
        if (e instanceof Player p && p.getAbilities().instabuild) return false;
        if (e.isInvisible() && !e.isCurrentlyGlowing()) return false;
        double reach = e.isDiscrete() ? tier.range * 0.5 : tier.range;   // crouching halves how far it notices you
        Vec3 from = muzzle();
        if (e.distanceToSqr(from) > reach * reach) return false;
        // without a ledger it guards against monsters and strangers, not livestock
        if (!owner.linked(level) && !(e instanceof Enemy || e instanceof Player)) return false;
        if (!owner.canCatch(level, e)) return false;
        return level.clip(new ClipContext(from, aimPoint(e), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e)).getType() == HitResult.Type.MISS;
    }

    private LivingEntity find(ServerLevel level, SentryTier tier) {
        Vec3 from = muzzle();
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(worldPosition).inflate(tier.range), e -> valid(level, e, tier))
                .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(from))).orElse(null);
    }

    private void setTarget(LivingEntity next) {
        if (next == target) return;
        boolean waking = target == null && next != null;
        target = next;
        burstLeft = 0;
        if (waking) {
            warmup = WARMUP;   // the client swings round from its patrol during this
            aimYaw = yawTo(next);
            triggerAnim(CONTROLLER, "alert");
            level.playSound(null, worldPosition, SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.BLOCKS, 1.0F, 0.6F);
        }
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    private void fire(ServerLevel level, SentryTier tier) {
        Vec3 from = muzzle();
        Vec3 d = aimPoint(target).subtract(from);
        double flat = Math.sqrt(d.x * d.x + d.z * d.z);
        double drop = 0.025 * flat * flat / (tier.speed * tier.speed);   // gravity 0.05/tick over flat/speed ticks
        ItemStack ammo;
        if (endless()) {
            ammo = new ItemStack(Items.ARROW);
        } else {
            int slot = ammoSlot();
            if (slot < 0) return;
            ammo = items.get(slot).split(1);
            setChanged();
        }
        SteelBolt bolt = new SteelBolt(level, from.x, from.y, from.z, ammo);
        bolt.setBaseDamage(tier.damage);
        bolt.shoot(d.x, d.y + drop, d.z, tier.speed, 1.0F);
        level.addFreshEntity(bolt);
        level.playSound(null, worldPosition, SoundEvents.CROSSBOW_SHOOT, SoundSource.BLOCKS, 0.8F, 1.4F + level.getRandom().nextFloat() * 0.2F);
        triggerAnim(CONTROLLER, "fire");
    }

    private boolean endless() {
        return getBlockState().getValue(SentryBlock.ENDLESS);
    }

    private boolean hasAmmo() {
        return endless() || ammoSlot() >= 0;
    }

    private int ammoSlot() {
        for (int i = 0; i < items.size(); i++) if (items.get(i).is(ItemTags.ARROWS)) return i;
        return -1;
    }

    // ---- container ------------------------------------------------------------------

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.ambuscade.sentry." + getBlockState().getValue(SentryBlock.TIER).getSerializedName());
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> list) {
        items = list;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return switch (items.size()) {
            case 5 -> new HopperMenu(id, inventory, this);
            case 9 -> new DispenserMenu(id, inventory, this);
            case 18 -> new ChestMenu(MenuType.GENERIC_9x2, id, inventory, this, 2);
            default -> ChestMenu.threeRows(id, inventory, this);
        };
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return !endless() && stack.is(ItemTags.ARROWS);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return IntStream.range(0, items.size()).toArray();
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("target", target == null ? -1 : target.getId());
        return tag;
    }

    private float yawTo(Entity e) {
        Vec3 d = aimPoint(e).subtract(muzzle());
        return (float) Math.toDegrees(Math.atan2(-d.x, -d.z));
    }

    // ---- client: draws the aim ------------------------------------------------------

    public static void clientTick(Level level, BlockPos pos, BlockState state, SentryBlockEntity s) {
        float home = facingYaw(state.getValue(SentryBlock.FACING));
        if (s.age++ == 0) s.yaw = s.prevYaw = home;
        s.prevYaw = s.yaw;
        s.prevPitch = s.pitch;
        Entity t = s.clientTarget >= 0 ? level.getEntity(s.clientTarget) : null;
        float wantYaw, wantPitch;
        if (t != null) {
            Vec3 d = aimPoint(t).subtract(s.muzzle());
            wantYaw = s.yawTo(t);
            wantPitch = (float) Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        } else {   // patrol: a slow sweep either side of where it was placed facing, pausing at the ends
            double ph = 2 * Math.PI * (s.age % 160) / 160.0, sin = Math.sin(ph);
            wantYaw = home + (float) (0.8 * (38 * sin * sin * sin + 12 * sin));
            wantPitch = 0;
        }
        s.yaw += Mth.clamp(Mth.wrapDegrees(wantYaw - s.yaw), -TURN, TURN);
        s.pitch = Mth.clamp(s.pitch + Mth.clamp(wantPitch - s.pitch, -TILT, TILT), -45, 60);
    }

    private static float facingYaw(Direction f) {
        return (float) Math.toDegrees(Math.atan2(-f.getStepX(), -f.getStepZ()));
    }

    @Override
    public Identifier model() {
        return Ambuscade.id("block/sentry_" + getBlockState().getValue(SentryBlock.TIER).getSerializedName());
    }

    @Override
    public Identifier texture() {
        return Ambuscade.id("textures/block/sentry_" + getBlockState().getValue(SentryBlock.TIER).getSerializedName() + ".png");
    }

    @Override
    public Identifier animation() {
        return Ambuscade.id("block/sentry");
    }

    @Override
    public float renderYaw(float partial) {
        return Mth.lerp(partial, prevYaw, yaw);
    }

    @Override
    public float renderPitch(float partial) {
        return Mth.lerp(partial, prevPitch, pitch);
    }

    // ---- geckolib / save ------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<SentryBlockEntity>(CONTROLLER, test -> PlayState.STOP)
                .triggerableAnim("alert", ALERT)
                .triggerableAnim("fire", FIRE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        owner.save(output);
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        owner.load(input);
        items = NonNullList.withSize(getBlockState().getValue(SentryBlock.TIER).slots, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        clientTarget = input.getIntOr("target", -1);   // only present in the update packet
    }
}

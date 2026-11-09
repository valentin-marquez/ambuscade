package com.nozz.ambuscade.block;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import com.nozz.ambuscade.Ambuscade;
import com.nozz.ambuscade.AmbuscadeRegistry;
import com.nozz.ambuscade.entity.MortarShell;
import com.nozz.ambuscade.ledger.LedgerLink;
import com.nozz.ambuscade.ledger.LedgerNetwork;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** Fires one shell per redstone pulse at the spot a fire order gave it. Needs open sky, lands 6 to 32 blocks out,
 *  scatters a little and reloads between shots. Whoever its owner or ledger protects is spared by the blast. */
public class MortarBlockEntity extends BlockEntity implements GeoBlockEntity, LedgerLink, Aimed {
    public static final String CONTROLLER = "main";
    private static final RawAnimation FIRE = RawAnimation.begin().thenPlay("animation.mortar.fire");
    private static final double SCATTER = 1.5, MUZZLE_Y = 1.6;
    private static final int RELOAD = 40;
    private static final float TURN = 9, TILT = 4, REST_ELEVATION = 60, MODEL_ELEVATION = 45;

    public enum Aim { OK, NO_TARGET, OUT_OF_RANGE, NO_SKY }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final TrapOwner owner = new TrapOwner();
    private BlockPos target;   // also sent to clients so the tube points at it
    private long readyAt;
    // client
    private int age;
    private float yaw, prevYaw, elevation = REST_ELEVATION, prevElevation = REST_ELEVATION;

    public MortarBlockEntity(BlockPos pos, BlockState state) {
        super(AmbuscadeRegistry.MORTAR_ENTITY.get(), pos, state);
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

    public boolean mayAim(Player player) {
        return mayLink(player, null) || owner.ledger() != null && LedgerNetwork.holdsLedger(player, owner.ledger());
    }

    public void aimAt(BlockPos spot) {
        target = spot;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    public BlockPos target() {
        return target;
    }

    private Vec3 muzzle() {
        return Vec3.atBottomCenterOf(worldPosition).add(0, MUZZLE_Y, 0);
    }

    private static Vec3 spotOf(BlockPos pos) {
        return Vec3.atBottomCenterOf(pos.above());   // the shell lands on top of the marked block
    }

    public Aim check() {
        if (target == null) return Aim.NO_TARGET;
        if (level == null || !level.canSeeSky(worldPosition.above())) return Aim.NO_SKY;
        Vec3 d = spotOf(target).subtract(muzzle());
        double flat = Math.sqrt(d.x * d.x + d.z * d.z);
        if (flat < Ballistics.MIN_RANGE || flat > Ballistics.MAX_RANGE || Float.isNaN(Ballistics.speed(flat, d.y))) return Aim.OUT_OF_RANGE;
        return Aim.OK;
    }

    /** Called on a rising redstone edge. */
    public void fire(ServerLevel level) {
        if (level.getGameTime() < readyAt) return;
        if (check() != Aim.OK) {
            level.playSound(null, worldPosition, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0F, 0.8F);
            return;
        }
        readyAt = level.getGameTime() + RELOAD;
        double a = level.getRandom().nextDouble() * Math.PI * 2, r = Math.sqrt(level.getRandom().nextDouble()) * SCATTER;
        Vec3 spot = spotOf(target).add(Math.cos(a) * r, 0, Math.sin(a) * r);
        Vec3 from = muzzle(), d = spot.subtract(from);
        double flat = Math.sqrt(d.x * d.x + d.z * d.z), e = Math.toRadians(Ballistics.elevation(flat));
        float speed = Ballistics.speed(flat, d.y);
        if (Float.isNaN(speed)) return;
        Vec3 dir = new Vec3(d.x / flat * Math.cos(e), Math.sin(e), d.z / flat * Math.cos(e));
        MortarShell shell = new MortarShell(level, from.add(dir.scale(0.8)), owner.copy(), spot);
        shell.shoot(dir.x, dir.y, dir.z, speed, 0);
        level.addFreshEntity(shell);
        level.playSound(null, worldPosition, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.BLOCKS, 2.0F, 0.5F);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, from.x + dir.x, from.y + dir.y, from.z + dir.z, 8, 0.15, 0.15, 0.15, 0.02);
        triggerAnim(CONTROLLER, "fire");
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();   // only the aim: owner and ledger stay on the server
        if (target != null) tag.putLong("target", target.asLong());
        return tag;
    }

    // ---- client ---------------------------------------------------------------------

    public static void clientTick(Level level, BlockPos pos, BlockState state, MortarBlockEntity m) {
        Direction facing = state.getValue(MortarBlock.FACING);
        float wantYaw = (float) Math.toDegrees(Math.atan2(-facing.getStepX(), -facing.getStepZ())), wantElevation = REST_ELEVATION;
        if (m.target != null) {
            Vec3 d = spotOf(m.target).subtract(m.muzzle());
            wantYaw = (float) Math.toDegrees(Math.atan2(-d.x, -d.z));
            wantElevation = Ballistics.elevation(Math.sqrt(d.x * d.x + d.z * d.z));
        }
        if (m.age++ == 0) m.yaw = m.prevYaw = wantYaw;
        m.prevYaw = m.yaw;
        m.prevElevation = m.elevation;
        m.yaw += Mth.clamp(Mth.wrapDegrees(wantYaw - m.yaw), -TURN, TURN);
        m.elevation += Mth.clamp(wantElevation - m.elevation, -TILT, TILT);
    }

    @Override
    public float renderYaw(float partial) {
        return Mth.lerp(partial, prevYaw, yaw);
    }

    @Override
    public float renderPitch(float partial) {
        return Mth.lerp(partial, prevElevation, elevation) - MODEL_ELEVATION;   // the model's tube already leans 45 degrees
    }

    @Override
    public Identifier model() {
        return Ambuscade.id("block/mortar");
    }

    @Override
    public Identifier texture() {
        return Ambuscade.id("textures/block/mortar.png");
    }

    @Override
    public Identifier animation() {
        return Ambuscade.id("block/mortar");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<MortarBlockEntity>(CONTROLLER, test -> PlayState.STOP).triggerableAnim("fire", FIRE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        owner.save(output);
        if (target != null) output.putLong("target", target.asLong());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        owner.load(input);
        target = input.getLong("target").map(BlockPos::of).orElse(null);
    }
}

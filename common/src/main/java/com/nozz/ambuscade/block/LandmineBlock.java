package com.nozz.ambuscade.block;

import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.InteractionHand;
import com.nozz.ambuscade.AmbuscadeRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LandmineBlock extends BaseEntityBlock {
    public static final MapCodec<LandmineBlock> CODEC = simpleCodec(LandmineBlock::new);
    public static final BooleanProperty PRESSED = BooleanProperty.create("pressed");
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 6, 13);
    private static final int FUSE_TICKS = 10;
    private static final float POWER = 3.0F;   // creeper strength; TNT is 4
    private static final int DEBRIS = 6;

    public LandmineBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PRESSED, false).setValue(FACING, Direction.NORTH).setValue(Camouflage.CAMOUFLAGED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PRESSED, FACING, Camouflage.CAMOUFLAGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LandmineBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Camouflage.covered(state) ? Camouflage.COVER : SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return Camouflage.covered(state) ? RenderShape.INVISIBLE : RenderShape.MODEL;   // the cover is drawn by its renderer
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return Camouflage.tryUncover(state, level, pos, player) ? InteractionResult.SUCCESS : super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(AmbuscadeRegistry.CAMOUFLAGE_NET.get())) return InteractionResult.PASS;   // let the net cover it
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction dir, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        return dir == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof LandmineBlockEntity mine) {
            mine.setOwner(player);
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean intersects) {
        if (!(level instanceof ServerLevel server) || state.getValue(PRESSED) || !(entity instanceof LivingEntity victim)) return;
        if (!entity.blockPosition().equals(pos)) return;
        if (!(level.getBlockEntity(pos) instanceof LandmineBlockEntity mine) || !mine.canCatch(victim) || !armed(mine)) return;

        mine.setVictim(victim);
        server.setBlock(pos, state.setValue(PRESSED, true), Block.UPDATE_ALL);
        server.playSound(null, pos, SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON, SoundSource.BLOCKS, 1.0F, 0.6F);
        server.scheduleTick(pos, this, FUSE_TICKS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(PRESSED)) detonate(level, pos, random);
    }

    /** Whether the mine is ready to go off. Variants that need setup override it. */
    protected boolean armed(LandmineBlockEntity mine) {
        return true;
    }

    /** What happens when the fuse runs out. Variants override it. */
    protected void detonate(ServerLevel level, BlockPos pos, RandomSource random) {
        // read who the mine spares before the block entity disappears with the block
        LandmineBlockEntity mine = level.getBlockEntity(pos) instanceof LandmineBlockEntity m ? m : null;
        ExplosionDamageCalculator spare = new ExplosionDamageCalculator() {
            @Override
            public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
                return !(entity instanceof LivingEntity living) || mine == null || mine.canCatch(living);
            }

            @Override
            public float getKnockbackMultiplier(Entity entity) {
                return entity instanceof LivingEntity living && mine != null && !mine.canCatch(living) ? 0 : 1;
            }
        };
        level.removeBlock(pos, false);
        // debris lifts blocks itself, so it obeys the same rule that stops TNT from breaking terrain
        if (level.getGameRules().get(GameRules.TNT_EXPLODES)) throwDebris(level, pos, random);
        Vec3 at = new Vec3(pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5);
        level.explode(null, null, spare, at, POWER, false, Level.ExplosionInteraction.TNT);
    }

    /** Lifts a few crater blocks into the air before the blast so they rain down around it. */
    private static void throwDebris(ServerLevel level, BlockPos center, RandomSource random) {
        int thrown = 0;
        for (int attempt = 0; attempt < DEBRIS * 4 && thrown < DEBRIS; attempt++) {
            BlockPos p = center.offset(random.nextInt(5) - 2, -random.nextInt(2) - 1, random.nextInt(5) - 2);
            BlockState s = level.getBlockState(p);
            float hardness = s.getDestroySpeed(level, p);
            if (s.isAir() || s.hasBlockEntity() || hardness < 0 || hardness > 5 || !s.isCollisionShapeFullBlock(level, p)) continue;
            FallingBlockEntity debris = FallingBlockEntity.fall(level, p, s);
            double dx = p.getX() - center.getX() + random.nextGaussian() * 0.3, dz = p.getZ() - center.getZ() + random.nextGaussian() * 0.3;
            debris.setDeltaMovement(dx * 0.18, 0.55 + random.nextDouble() * 0.35, dz * 0.18);
            debris.hurtMarked = true;
            thrown++;
        }
    }
}

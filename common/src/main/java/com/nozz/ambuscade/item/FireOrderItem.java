package com.nozz.ambuscade.item;

import com.nozz.ambuscade.AmbuscadeRegistry;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Marks a firing spot from afar (right-click toward a block, up to 64 blocks); right-click a mortar to hand it over.
 *  Sneak-right-click clears it. */
public class FireOrderItem extends Item {
    private static final double REACH = 128;   // past the mortar's longest shot, wherever the marker stands

    public FireOrderItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide()) stack.remove(AmbuscadeRegistry.FIRE_TARGET.get());
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        if (!(player.pick(REACH, 0, false) instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide()) {
            BlockPos pos = hit.getBlockPos();
            stack.set(AmbuscadeRegistry.FIRE_TARGET.get(), GlobalPos.of(level.dimension(), pos));
            player.displayClientMessage(Component.translatable("message.ambuscade.fire_order.marked", pos.getX(), pos.getY(), pos.getZ()), true);
            level.playSound(null, player.blockPosition(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        GlobalPos target = stack.get(AmbuscadeRegistry.FIRE_TARGET.get());
        lines.add(target == null ? Component.translatable("tooltip.ambuscade.fire_order.blank").withStyle(ChatFormatting.GRAY)
                : Component.translatable("tooltip.ambuscade.fire_order.target", target.pos().getX(), target.pos().getY(), target.pos().getZ()).withStyle(ChatFormatting.GRAY));
    }
}

package com.nozz.ambuscade.item;

import com.nozz.ambuscade.AmbuscadeRegistry;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
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
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide()) stack.remove(AmbuscadeRegistry.FIRE_TARGET.get());
            return InteractionResult.SUCCESS;
        }
        if (!(player.pick(REACH, 0, false) instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            BlockPos pos = hit.getBlockPos();
            stack.set(AmbuscadeRegistry.FIRE_TARGET.get(), GlobalPos.of(level.dimension(), pos));
            player.sendOverlayMessage(Component.translatable("message.ambuscade.fire_order.marked", pos.getX(), pos.getY(), pos.getZ()));
            level.playSound(null, player.blockPosition(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        GlobalPos target = stack.get(AmbuscadeRegistry.FIRE_TARGET.get());
        lines.accept(target == null ? Component.translatable("tooltip.ambuscade.fire_order.blank").withStyle(ChatFormatting.GRAY)
                : Component.translatable("tooltip.ambuscade.fire_order.target", target.pos().getX(), target.pos().getY(), target.pos().getZ()).withStyle(ChatFormatting.GRAY));
    }
}

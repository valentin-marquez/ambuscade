package com.nozz.ambuscade.item;

import com.nozz.ambuscade.block.SentryBlock;
import com.nozz.ambuscade.block.SentryTier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;

/** The tier rides in the block_state component, so a broken sentry keeps its upgrades. */
public class SentryItem extends BlockItem {
    public SentryItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static SentryTier tier(ItemStack stack) {
        SentryTier tier = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).get(SentryBlock.TIER);
        return tier == null ? SentryTier.IRON : tier;
    }

    public static ItemStack of(Block block, SentryTier tier) {
        ItemStack stack = new ItemStack(block);
        stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(SentryBlock.TIER, tier));
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        if (Boolean.TRUE.equals(stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).get(SentryBlock.ENDLESS))) {
            lines.add(Component.translatable("tooltip.ambuscade.sentry.endless").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("block.ambuscade.sentry." + tier(stack).getSerializedName());
    }
}

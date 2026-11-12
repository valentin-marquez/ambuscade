package com.nozz.ambuscade.item;

import com.nozz.ambuscade.block.Camouflage;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Right-click a mine or bear trap to hide it under a layer that looks like the ground. */
public class CamouflageNetItem extends Item {
    public CamouflageNetItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        var level = ctx.getLevel();
        var pos = ctx.getClickedPos();
        if (!level.getBlockState(pos).hasProperty(Camouflage.CAMOUFLAGED)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!Camouflage.cover(level, pos, level.getBlockState(pos))) return InteractionResult.FAIL;
        ctx.getItemInHand().consume(1, ctx.getPlayer());
        return InteractionResult.CONSUME;
    }
}

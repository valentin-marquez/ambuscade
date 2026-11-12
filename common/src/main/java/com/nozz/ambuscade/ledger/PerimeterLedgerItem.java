package com.nozz.ambuscade.ledger;

import com.nozz.ambuscade.AmbuscadeRegistry;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class PerimeterLedgerItem extends Item {
    public PerimeterLedgerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
            UUID id = idOf(stack, server, player);
            LedgerNetwork.open(sp, id, LedgerData.get(server).find(id));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel().getBlockEntity(ctx.getClickedPos()) instanceof LedgerLink link)) return InteractionResult.PASS;
        Player player = ctx.getPlayer();
        if (ctx.getLevel() instanceof ServerLevel server && player != null) {
            UUID held = ctx.getItemInHand().get(AmbuscadeRegistry.LEDGER_ID.get());
            if (!link.mayLink(player, held)) {
                player.displayClientMessage(Component.translatable("message.ambuscade.ledger.not_yours"), true);
                return InteractionResult.FAIL;
            }
            UUID id = idOf(ctx.getItemInHand(), server, player);
            LedgerData data = LedgerData.get(server);
            GlobalPos pos = GlobalPos.of(server.dimension(), ctx.getClickedPos());
            Ledger previous = id.equals(link.linkedLedger()) ? null : data.find(link.linkedLedger());
            if (previous != null) previous.unlink(LedgerNetwork.where(pos));
            String block = BuiltInRegistries.BLOCK.getKey(server.getBlockState(ctx.getClickedPos()).getBlock()).toString();
            if (!data.find(id).link(LedgerNetwork.where(pos), block)) {
                player.displayClientMessage(Component.translatable("message.ambuscade.ledger.full"), true);
                return InteractionResult.FAIL;
            }
            link.linkLedger(id);
            data.find(id).protect(player.getScoreboardName());   // whoever arms a trap is spared by it
            data.setDirty();
            player.displayClientMessage(Component.translatable("message.ambuscade.ledger.linked"), true);
        }
        return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide());
    }

    // a blank ledger becomes a new list owned by whoever first uses it
    private static UUID idOf(ItemStack stack, ServerLevel level, Player player) {
        UUID id = stack.get(AmbuscadeRegistry.LEDGER_ID.get());
        if (id == null || LedgerData.get(level).find(id) == null) {
            id = LedgerData.get(level).create(player.getScoreboardName());
            stack.set(AmbuscadeRegistry.LEDGER_ID.get(), id);
        }
        return id;
    }
}

package com.nozz.ambuscade.item;

import com.nozz.ambuscade.AmbuscadeRegistry;
import com.nozz.ambuscade.block.SentryUpgrade;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** One item for every upgrade; what it installs lives in a component. */
public class UpgradeModuleItem extends Item {
    public UpgradeModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.ambuscade.upgrade_module." + stack.getOrDefault(AmbuscadeRegistry.UPGRADE.get(), SentryUpgrade.GOLD).getSerializedName());
    }
}

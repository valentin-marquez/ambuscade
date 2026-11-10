package com.nozz.ambuscade;

import com.nozz.ambuscade.block.MineNetwork;
import com.nozz.ambuscade.ledger.LedgerNetwork;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

public final class Ambuscade {
    public static final String MOD_ID = "ambuscade";

    public static final ResourceKey<DamageType> BARBED_WIRE_DAMAGE = damageType("barbed_wire");
    public static final ResourceKey<DamageType> BEAR_TRAP_DAMAGE = damageType("bear_trap");

    public static void init() {
        AmbuscadeConfig.load(dev.architectury.platform.Platform.getConfigFolder());
        AmbuscadeRegistry.init();
        LedgerNetwork.init();
        MineNetwork.init();
        com.nozz.ambuscade.block.Keypad.init();
        com.nozz.ambuscade.item.KeypadLockItem.init();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static DamageSource damage(ServerLevel level, ResourceKey<DamageType> type) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type));
    }

    private static ResourceKey<DamageType> damageType(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, id(name));
    }
}

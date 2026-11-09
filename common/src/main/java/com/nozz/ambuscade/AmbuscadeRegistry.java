package com.nozz.ambuscade;

import com.nozz.ambuscade.block.BarbedWireBlock;
import com.nozz.ambuscade.ledger.PerimeterLedgerItem;
import com.nozz.ambuscade.block.BearTrapBlock;
import com.nozz.ambuscade.block.BearTrapBlockEntity;
import com.nozz.ambuscade.block.GasMineBlock;
import com.nozz.ambuscade.block.TeleportMineBlock;
import com.nozz.ambuscade.block.LandmineBlock;
import com.nozz.ambuscade.block.LandmineBlockEntity;
import com.nozz.ambuscade.block.MortarBlock;
import com.nozz.ambuscade.block.MortarBlockEntity;
import com.nozz.ambuscade.block.KeypadDoorBlock;
import com.nozz.ambuscade.block.KeypadDoorBlockEntity;
import com.nozz.ambuscade.block.SentryBlock;
import com.nozz.ambuscade.block.SentryBlockEntity;
import com.nozz.ambuscade.block.SentryTier;
import com.nozz.ambuscade.block.SentryUpgrade;
import com.nozz.ambuscade.entity.MortarShell;
import com.nozz.ambuscade.entity.SteelBolt;
import com.nozz.ambuscade.item.CamouflageNetItem;
import com.nozz.ambuscade.item.FireOrderItem;
import com.nozz.ambuscade.item.KeypadLockItem;
import com.nozz.ambuscade.item.SentryItem;
import com.nozz.ambuscade.item.UpgradeModuleItem;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class AmbuscadeRegistry {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Ambuscade.MOD_ID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Ambuscade.MOD_ID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Ambuscade.MOD_ID, Registries.BLOCK_ENTITY_TYPE);
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Ambuscade.MOD_ID, Registries.DATA_COMPONENT_TYPE);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Ambuscade.MOD_ID, Registries.ENTITY_TYPE);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Ambuscade.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<BarbedWireBlock> BARBED_WIRE = block("barbed_wire", BarbedWireBlock::new,
            BlockBehaviour.Properties.of().noCollision().noOcclusion().strength(2.0F, 3.0F).sound(SoundType.CHAIN));
    public static final RegistrySupplier<BearTrapBlock> BEAR_TRAP = block("bear_trap", BearTrapBlock::new,
            BlockBehaviour.Properties.of().noCollision().noOcclusion().strength(3.0F, 6.0F).sound(SoundType.CHAIN));
    public static final RegistrySupplier<LandmineBlock> EXPLOSIVE_MINE = block("explosive_mine", LandmineBlock::new,
            BlockBehaviour.Properties.of().noCollision().noOcclusion().strength(1.5F, 0.5F).sound(SoundType.CHAIN));

    public static final RegistrySupplier<DataComponentType<UUID>> LEDGER_ID = COMPONENTS.register("ledger_id",
            () -> DataComponentType.<UUID>builder().persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC).build());
    public static final RegistrySupplier<PerimeterLedgerItem> PERIMETER_LEDGER = ITEMS.register("perimeter_ledger",
            () -> new PerimeterLedgerItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Ambuscade.id("perimeter_ledger"))).stacksTo(16)));   // copies are the same key: they may stack

    public static final RegistrySupplier<GasMineBlock> GAS_MINE = block("gas_mine", GasMineBlock::new,
            BlockBehaviour.Properties.of().noCollision().noOcclusion().strength(1.5F, 0.5F).sound(SoundType.CHAIN));

    public static final RegistrySupplier<TeleportMineBlock> TELEPORT_MINE = block("teleport_mine", TeleportMineBlock::new,
            BlockBehaviour.Properties.of().noCollision().noOcclusion().strength(1.5F, 0.5F).sound(SoundType.CHAIN));

    public static final RegistrySupplier<DataComponentType<SentryUpgrade>> UPGRADE = COMPONENTS.register("upgrade",
            () -> DataComponentType.<SentryUpgrade>builder().persistent(SentryUpgrade.CODEC).networkSynchronized(SentryUpgrade.STREAM_CODEC).build());
    public static final RegistrySupplier<SentryBlock> SENTRY = BLOCKS.register("sentry", () -> new SentryBlock(BlockBehaviour.Properties.of()
            .noOcclusion().strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)
            .setId(ResourceKey.create(Registries.BLOCK, Ambuscade.id("sentry")))));
    public static final RegistrySupplier<SentryItem> SENTRY_ITEM = ITEMS.register("sentry", () -> new SentryItem(SENTRY.get(),
            new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Ambuscade.id("sentry"))).useBlockDescriptionPrefix()));
    public static final RegistrySupplier<UpgradeModuleItem> UPGRADE_MODULE = ITEMS.register("upgrade_module", () -> new UpgradeModuleItem(
            new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Ambuscade.id("upgrade_module"))).component(UPGRADE.get(), SentryUpgrade.GOLD)));
    public static final RegistrySupplier<EntityType<SteelBolt>> STEEL_BOLT = ENTITIES.register("steel_bolt", () -> EntityType.Builder
            .<SteelBolt>of(SteelBolt::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20).noLootTable()
            .build(ResourceKey.create(Registries.ENTITY_TYPE, Ambuscade.id("steel_bolt"))));
    public static final RegistrySupplier<MortarBlock> MORTAR = block("mortar", MortarBlock::new,
            BlockBehaviour.Properties.of().noOcclusion().strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.METAL));
    public static final RegistrySupplier<DataComponentType<GlobalPos>> FIRE_TARGET = COMPONENTS.register("fire_target",
            () -> DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC).build());
    public static final RegistrySupplier<FireOrderItem> FIRE_ORDER = ITEMS.register("fire_order",
            () -> new FireOrderItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Ambuscade.id("fire_order"))).stacksTo(1)));
    public static final RegistrySupplier<CamouflageNetItem> CAMOUFLAGE_NET = ITEMS.register("camouflage_net",
            () -> new CamouflageNetItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Ambuscade.id("camouflage_net")))));
    public static final RegistrySupplier<BlockEntityType<MortarBlockEntity>> MORTAR_ENTITY = BLOCK_ENTITIES.register("mortar",
            () -> Platform.blockEntityType(MortarBlockEntity::new, MORTAR.get()));
    public static final RegistrySupplier<EntityType<MortarShell>> MORTAR_SHELL = ENTITIES.register("mortar_shell", () -> EntityType.Builder
            .<MortarShell>of(MortarShell::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(5).noLootTable()
            .build(ResourceKey.create(Registries.ENTITY_TYPE, Ambuscade.id("mortar_shell"))));
    public static final RegistrySupplier<BlockEntityType<SentryBlockEntity>> SENTRY_ENTITY = BLOCK_ENTITIES.register("sentry",
            () -> Platform.blockEntityType(SentryBlockEntity::new, SENTRY.get()));

    /** Items too heavy for one hand: carried in both arms on the chest (sentries, mortars). */
    public static final TagKey<Item> CARRIED = TagKey.create(Registries.ITEM, Ambuscade.id("carried"));

    /** Every vanilla door, keyed to the locked twin a keypad lock turns it into. */
    public static final Map<Block, RegistrySupplier<KeypadDoorBlock>> LOCKED_DOORS = new LinkedHashMap<>();
    static {
        for (Block door : List.of(Blocks.OAK_DOOR, Blocks.SPRUCE_DOOR, Blocks.BIRCH_DOOR, Blocks.JUNGLE_DOOR, Blocks.ACACIA_DOOR, Blocks.CHERRY_DOOR,
                Blocks.DARK_OAK_DOOR, Blocks.PALE_OAK_DOOR, Blocks.MANGROVE_DOOR, Blocks.BAMBOO_DOOR, Blocks.CRIMSON_DOOR, Blocks.WARPED_DOOR,
                Blocks.IRON_DOOR, Blocks.COPPER_DOOR, Blocks.EXPOSED_COPPER_DOOR, Blocks.WEATHERED_COPPER_DOOR, Blocks.OXIDIZED_COPPER_DOOR,
                Blocks.WAXED_COPPER_DOOR, Blocks.WAXED_EXPOSED_COPPER_DOOR, Blocks.WAXED_WEATHERED_COPPER_DOOR, Blocks.WAXED_OXIDIZED_COPPER_DOOR)) {
            String name = "locked_" + BuiltInRegistries.BLOCK.getKey(door).getPath();
            LOCKED_DOORS.put(door, BLOCKS.register(name, () -> new KeypadDoorBlock(((DoorBlock) door).type(), BlockBehaviour.Properties.ofFullCopy(door)
                    .setId(ResourceKey.create(Registries.BLOCK, Ambuscade.id(name))).overrideLootTable(Optional.empty()).explosionResistance(1200.0F)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK))));
        }
    }
    public static final RegistrySupplier<KeypadLockItem> KEYPAD_LOCK = ITEMS.register("keypad_lock",
            () -> new KeypadLockItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Ambuscade.id("keypad_lock")))));
    public static final RegistrySupplier<BlockEntityType<KeypadDoorBlockEntity>> KEYPAD_DOOR_ENTITY = BLOCK_ENTITIES.register("keypad_door",
            () -> Platform.blockEntityType(KeypadDoorBlockEntity::new, LOCKED_DOORS.values().stream().map(RegistrySupplier::get).toArray(Block[]::new)));

    public static final RegistrySupplier<BlockEntityType<BearTrapBlockEntity>> BEAR_TRAP_ENTITY = BLOCK_ENTITIES.register("bear_trap",
            () -> Platform.blockEntityType(BearTrapBlockEntity::new, BEAR_TRAP.get()));
    public static final RegistrySupplier<BlockEntityType<LandmineBlockEntity>> LANDMINE_ENTITY = BLOCK_ENTITIES.register("landmine",
            () -> Platform.blockEntityType(LandmineBlockEntity::new, EXPLOSIVE_MINE.get(), GAS_MINE.get(), TELEPORT_MINE.get()));

    static {
        TABS.register("main", () -> CreativeTabRegistry.create(tab -> tab
                .title(Component.translatable("itemGroup.ambuscade"))
                .icon(() -> new ItemStack(BEAR_TRAP.get()))
                .displayItems((params, out) -> {
                    out.accept(BARBED_WIRE.get());
                    out.accept(BEAR_TRAP.get());
                    out.accept(EXPLOSIVE_MINE.get());
                    out.accept(GAS_MINE.get());
                    out.accept(TELEPORT_MINE.get());
                    for (SentryTier tier : SentryTier.values()) out.accept(SentryItem.of(SENTRY.get(), tier));
                    for (SentryUpgrade upgrade : SentryUpgrade.values()) {
                        ItemStack module = new ItemStack(UPGRADE_MODULE.get());
                        module.set(UPGRADE.get(), upgrade);
                        out.accept(module);
                    }
                    out.accept(MORTAR.get());
                    out.accept(FIRE_ORDER.get());
                    out.accept(CAMOUFLAGE_NET.get());
                    out.accept(KEYPAD_LOCK.get());
                    out.accept(PERIMETER_LEDGER.get());
                })));
    }

    static void init() {
        COMPONENTS.register();
        BLOCKS.register();
        ITEMS.register();
        BLOCK_ENTITIES.register();
        ENTITIES.register();
        TABS.register();
    }

    private static <B extends Block> RegistrySupplier<B> block(String name, Function<BlockBehaviour.Properties, B> factory, BlockBehaviour.Properties props) {
        RegistrySupplier<B> block = BLOCKS.register(name, () -> factory.apply(props.setId(ResourceKey.create(Registries.BLOCK, Ambuscade.id(name)))));
        ITEMS.register(name, () -> new BlockItem(block.get(),
                new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Ambuscade.id(name))).useBlockDescriptionPrefix()));
        return block;
    }
}

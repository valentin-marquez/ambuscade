package com.nozz.ambuscade.client;

import com.nozz.ambuscade.block.MineNetwork;
import com.nozz.ambuscade.ledger.LedgerNetwork;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import com.nozz.ambuscade.Ambuscade;
import com.nozz.ambuscade.AmbuscadeRegistry;
import com.nozz.ambuscade.block.BearTrapBlockEntity;
import com.nozz.ambuscade.block.SentryUpgrade;
import com.nozz.ambuscade.item.SentryItem;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import dev.architectury.registry.item.ItemPropertiesRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class AmbuscadeClient {
    public static void init() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, MineNetwork.OpenDestination.TYPE, MineNetwork.OpenDestination.CODEC,
                (data, ctx) -> ctx.queue(() -> Minecraft.getInstance().setScreen(new MineDestinationScreen(data))));
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, com.nozz.ambuscade.block.Keypad.Open.TYPE, com.nozz.ambuscade.block.Keypad.Open.CODEC,
                (data, ctx) -> ctx.queue(() -> Minecraft.getInstance().setScreen(new KeypadScreen(data))));
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, LedgerNetwork.Open.TYPE, LedgerNetwork.Open.CODEC, (data, ctx) -> ctx.queue(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof LedgerScreen open && open.id().equals(data.id())) open.update(data);
            else mc.setScreen(new LedgerScreen(data));
        }));
    }

    /** Starts the local dev harness when it was built in (see common/build.gradle); release jars do not have it. */
    public static void startHarness() {
        try {
            Class.forName("com.nozz.ambuscade.client.harness.AmbuscadeHarness").getMethod("startIfEnabled").invoke(null);
        } catch (ClassNotFoundException absent) {
            // release build
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Needs the blocks and items registered: Fabric calls it from its client entry point, NeoForge from client setup. */
    public static void setup() {
        // 1.21.1 does not pick the cutout layer from the textures: anything with see-through pixels says so here
        // (sentries, mortars and the bear trap draw through GeckoLib, but their items use these block models)
        List<Block> cutout = new ArrayList<>(List.of(AmbuscadeRegistry.BEAR_TRAP.get(), AmbuscadeRegistry.EXPLOSIVE_MINE.get(),
                AmbuscadeRegistry.GAS_MINE.get(), AmbuscadeRegistry.TELEPORT_MINE.get(), AmbuscadeRegistry.SENTRY.get(), AmbuscadeRegistry.MORTAR.get()));
        AmbuscadeRegistry.BARBED_WIRE_WOODS.forEach(wire -> cutout.add(wire.get()));
        AmbuscadeRegistry.LOCKED_DOORS.values().forEach(door -> cutout.add(door.get()));
        RenderTypeRegistry.register(RenderType.cutout(), cutout.toArray(Block[]::new));

        // item models pick their variant through these (see models/item): a quarter per step, as the values are clamped to 0..1
        ItemPropertiesRegistry.register(AmbuscadeRegistry.SENTRY_ITEM.get(), Ambuscade.id("tier"),
                (stack, level, entity, seed) -> SentryItem.tier(stack).ordinal() * 0.25F);
        ItemPropertiesRegistry.register(AmbuscadeRegistry.UPGRADE_MODULE.get(), Ambuscade.id("upgrade"),
                (stack, level, entity, seed) -> stack.getOrDefault(AmbuscadeRegistry.UPGRADE.get(), SentryUpgrade.GOLD).ordinal() * 0.25F);
        ItemPropertiesRegistry.register(AmbuscadeRegistry.FIRE_ORDER.get(), Ambuscade.id("marked"),
                (stack, level, entity, seed) -> stack.has(AmbuscadeRegistry.FIRE_TARGET.get()) ? 1.0F : 0.0F);
    }

    public static GeoBlockRenderer<BearTrapBlockEntity> bearTrapRenderer(BlockEntityRendererProvider.Context ctx) {
        return new BearTrapRenderer(ctx);
    }
}

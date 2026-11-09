package com.nozz.ambuscade.client;

import com.geckolib.renderer.GeoBlockRenderer;
import com.nozz.ambuscade.block.MineNetwork;
import com.nozz.ambuscade.ledger.LedgerNetwork;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import com.nozz.ambuscade.AmbuscadeRegistry;
import com.nozz.ambuscade.block.BearTrapBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

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

    public static GeoBlockRenderer<BearTrapBlockEntity, BlockEntityRenderState> bearTrapRenderer(BlockEntityRendererProvider.Context ctx) {
        return new BearTrapRenderer(ctx);
    }
}

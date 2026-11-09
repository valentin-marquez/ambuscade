package com.nozz.ambuscade.neoforge;

import com.nozz.ambuscade.Ambuscade;
import com.nozz.ambuscade.AmbuscadeRegistry;
import com.nozz.ambuscade.client.AmbuscadeClient;
import com.nozz.ambuscade.client.AimedRenderer;
import com.nozz.ambuscade.client.LandmineRenderer;
import com.nozz.ambuscade.client.MortarShellRenderer;
import com.nozz.ambuscade.client.SteelBoltRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(value = Ambuscade.MOD_ID, dist = Dist.CLIENT)
public final class AmbuscadeNeoForgeClient {
    public AmbuscadeNeoForgeClient(IEventBus bus) {
        bus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> {
            event.registerBlockEntityRenderer(AmbuscadeRegistry.BEAR_TRAP_ENTITY.get(), AmbuscadeClient::bearTrapRenderer);
            event.registerBlockEntityRenderer(AmbuscadeRegistry.SENTRY_ENTITY.get(), AimedRenderer::new);
            event.registerBlockEntityRenderer(AmbuscadeRegistry.MORTAR_ENTITY.get(), AimedRenderer::new);
            event.registerBlockEntityRenderer(AmbuscadeRegistry.LANDMINE_ENTITY.get(), LandmineRenderer::new);
            event.registerEntityRenderer(AmbuscadeRegistry.MORTAR_SHELL.get(), MortarShellRenderer::new);
            event.registerEntityRenderer(AmbuscadeRegistry.STEEL_BOLT.get(), SteelBoltRenderer::new);
        });
        AmbuscadeClient.init();
        com.nozz.ambuscade.client.AmbuscadeClient.startHarness();
    }
}

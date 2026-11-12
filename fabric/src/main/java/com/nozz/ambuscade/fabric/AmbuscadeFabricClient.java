package com.nozz.ambuscade.fabric;

import com.nozz.ambuscade.AmbuscadeRegistry;
import com.nozz.ambuscade.client.AmbuscadeClient;
import com.nozz.ambuscade.client.AimedRenderer;
import com.nozz.ambuscade.client.LandmineRenderer;
import com.nozz.ambuscade.client.MortarShellRenderer;
import com.nozz.ambuscade.client.SteelBoltRenderer;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import net.fabricmc.api.ClientModInitializer;

public final class AmbuscadeFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRendererRegistry.register(AmbuscadeRegistry.BEAR_TRAP_ENTITY.get(), AmbuscadeClient::bearTrapRenderer);
        BlockEntityRendererRegistry.register(AmbuscadeRegistry.SENTRY_ENTITY.get(), AimedRenderer::new);
        BlockEntityRendererRegistry.register(AmbuscadeRegistry.MORTAR_ENTITY.get(), AimedRenderer::new);
        BlockEntityRendererRegistry.register(AmbuscadeRegistry.LANDMINE_ENTITY.get(), LandmineRenderer::new);
        BlockEntityRendererRegistry.register(AmbuscadeRegistry.LOCKED_CHEST_ENTITY.get(), com.nozz.ambuscade.client.LockedChestRenderer::new);
        EntityRendererRegistry.register(AmbuscadeRegistry.MORTAR_SHELL, MortarShellRenderer::new);
        EntityRendererRegistry.register(AmbuscadeRegistry.STEEL_BOLT, SteelBoltRenderer::new);
        AmbuscadeClient.init();
        AmbuscadeClient.setup();
        com.nozz.ambuscade.client.AmbuscadeClient.startHarness();
    }
}

package com.nozz.ambuscade.client;

import com.nozz.ambuscade.Ambuscade;
import com.nozz.ambuscade.entity.SteelBolt;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class SteelBoltRenderer extends ArrowRenderer<SteelBolt> {
    private static final ResourceLocation TEXTURE = Ambuscade.id("textures/entity/steel_bolt.png");

    public SteelBoltRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(SteelBolt bolt) {
        return TEXTURE;
    }
}

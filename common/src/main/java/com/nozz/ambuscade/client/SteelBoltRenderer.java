package com.nozz.ambuscade.client;

import com.nozz.ambuscade.Ambuscade;
import com.nozz.ambuscade.entity.SteelBolt;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.Identifier;

public class SteelBoltRenderer extends ArrowRenderer<SteelBolt, ArrowRenderState> {
    private static final Identifier TEXTURE = Ambuscade.id("textures/entity/steel_bolt.png");

    public SteelBoltRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    protected Identifier getTextureLocation(ArrowRenderState state) {
        return TEXTURE;
    }

    @Override
    public ArrowRenderState createRenderState() {
        return new ArrowRenderState();
    }
}

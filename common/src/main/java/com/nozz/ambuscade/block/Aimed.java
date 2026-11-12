package com.nozz.ambuscade.block;

import net.minecraft.resources.ResourceLocation;

/** A block entity drawn by GeckoLib whose yaw and pitch bones are turned from code to aim. */
public interface Aimed {
    float renderYaw(float partial);

    float renderPitch(float partial);

    ResourceLocation model();

    ResourceLocation texture();

    ResourceLocation animation();
}

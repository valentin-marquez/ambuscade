package com.nozz.ambuscade.block;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/** What an upgrade module installs: the next tier, or endless ammo (any tier, no magazine). */
public enum SentryUpgrade implements StringRepresentable {
    GOLD(SentryTier.GOLD), DIAMOND(SentryTier.DIAMOND), NETHERITE(SentryTier.NETHERITE), ENDLESS(null);

    public static final Codec<SentryUpgrade> CODEC = StringRepresentable.fromEnum(SentryUpgrade::values);
    public static final StreamCodec<ByteBuf, SentryUpgrade> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Enum::ordinal);

    public final SentryTier tier;

    SentryUpgrade(SentryTier tier) {
        this.tier = tier;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}

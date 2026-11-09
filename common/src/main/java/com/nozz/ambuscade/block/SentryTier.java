package com.nozz.ambuscade.block;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/** Sentry stats per tier: reach in blocks, bolts per burst, ticks between bursts, bolt speed, damage and ammo slots. */
public enum SentryTier implements StringRepresentable {
    IRON(8, 2, 30, 2.4F, 1.0, 5),
    GOLD(12, 3, 26, 2.6F, 1.0, 9),
    DIAMOND(16, 4, 22, 2.8F, 1.1, 18),
    NETHERITE(24, 5, 18, 3.0F, 1.25, 27);

    public static final Codec<SentryTier> CODEC = StringRepresentable.fromEnum(SentryTier::values);
    public static final StreamCodec<ByteBuf, SentryTier> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Enum::ordinal);

    public final int range, burst, cooldown, slots;
    public final float speed;
    public final double damage;

    SentryTier(int range, int burst, int cooldown, float speed, double damage, int slots) {
        this.slots = slots;
        this.range = range;
        this.burst = burst;
        this.cooldown = cooldown;
        this.speed = speed;
        this.damage = damage;
    }

    public SentryTier next() {
        return ordinal() + 1 < values().length ? values()[ordinal() + 1] : null;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}

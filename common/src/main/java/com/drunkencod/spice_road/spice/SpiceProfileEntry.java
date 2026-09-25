package com.drunkencod.spice_road.spice;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

/**
 * One {@code data/<namespace>/spice_profile/*.json} entry: the {@link Item}
 * it applies to, and the {@link SpiceProfile} that item should report as its
 * default (no per-stack {@code spice_road:spice_profile} data component
 * override present) - see {@link SpiceProfileReloadListener}.
 */
public record SpiceProfileEntry(Item item, SpiceProfile profile) {

    public static final Codec<SpiceProfileEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(SpiceProfileEntry::item),
            SpiceProfile.CODEC.fieldOf("profile").forGetter(SpiceProfileEntry::profile))
            .apply(instance, SpiceProfileEntry::new));
}

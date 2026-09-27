package com.drunkencod.spice_road.spice;

import java.util.List;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * One {@code data/<namespace>/spice_profile/*.json} entry: the items it
 * targets, and the Default Profile they get - see
 * {@link SpiceProfileReloadListener}.
 * <p>
 * {@code items} is a single item ID, a single {@code #tag}, or a list mixing
 * both. Unknown item IDs and empty tags are skipped rather than failing the
 * entry, so datapacks can target items of mods that may not be installed.
 *
 * @param targets Each target, either an item tag (left) or an item ID (right).
 * @param profile The Default Profile of every targeted item.
 */
public record SpiceProfileEntry(List<Either<TagKey<Item>, ResourceLocation>> targets, SpiceProfile profile) {

    private static final Codec<Either<TagKey<Item>, ResourceLocation>> TARGET_CODEC = Codec
            .either(TagKey.hashedCodec(Registries.ITEM), ResourceLocation.CODEC);

    /** Codec of a single {@code spice_profile/*.json} file. */
    public static final Codec<SpiceProfileEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.withAlternative(TARGET_CODEC.listOf(), TARGET_CODEC.xmap(List::of, List::getFirst))
                    .fieldOf("items").forGetter(SpiceProfileEntry::targets),
            SpiceProfile.CODEC.fieldOf("profile").forGetter(SpiceProfileEntry::profile))
            .apply(instance, SpiceProfileEntry::new));
}

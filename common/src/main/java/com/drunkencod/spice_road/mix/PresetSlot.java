package com.drunkencod.spice_road.mix;

import java.util.Comparator;
import java.util.Optional;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * One proportion of a {@link MixPreset}: either a single Spice Item, or an item
 * tag of interchangeable spices, together with how many of it one batch holds.
 * A tag is resolved lazily, whenever it is needed, as tags aren't bound yet
 * while the presets load.
 *
 * @param source A Spice Item ({@code "item"}) or an item tag ({@code "tag"}).
 * @param count  How many spices one batch holds of it; for a tag, in total over
 *               all of its members.
 */
public record PresetSlot(Either<Item, TagKey<Item>> source, int count) {

    private static final StreamCodec<RegistryFriendlyByteBuf, Item> ITEM_STREAM_CODEC = ByteBufCodecs
            .registry(Registries.ITEM);

    /** Persistent (JSON) codec, as {@code {"item": ..., "count": ...}} or {@code {"tag": ..., "count": ...}}. */
    public static final Codec<PresetSlot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.mapEither(
                    BuiltInRegistries.ITEM.byNameCodec().fieldOf("item"),
                    TagKey.codec(Registries.ITEM).fieldOf("tag")).forGetter(PresetSlot::source),
            Codec.intRange(1, 1000).fieldOf("count").forGetter(PresetSlot::count))
            .apply(instance, PresetSlot::new));

    /** Network codec. */
    public static final StreamCodec<RegistryFriendlyByteBuf, PresetSlot> STREAM_CODEC = StreamCodec.of(
            (buf, slot) -> {
                slot.source().ifLeft(item -> {
                    buf.writeBoolean(false);
                    ITEM_STREAM_CODEC.encode(buf, item);
                }).ifRight(tag -> {
                    buf.writeBoolean(true);
                    ResourceLocation.STREAM_CODEC.encode(buf, tag.location());
                });
                ByteBufCodecs.VAR_INT.encode(buf, slot.count());
            },
            buf -> {
                Either<Item, TagKey<Item>> source = buf.readBoolean()
                        ? Either.right(TagKey.create(Registries.ITEM, ResourceLocation.STREAM_CODEC.decode(buf)))
                        : Either.left(ITEM_STREAM_CODEC.decode(buf));
                return new PresetSlot(source, ByteBufCodecs.VAR_INT.decode(buf));
            });

    /** Single items first, then tags, each by ID, so equal presets always list their slots alike. */
    static final Comparator<PresetSlot> ORDER = Comparator
            .comparing((PresetSlot slot) -> slot.source().right().isPresent())
            .thenComparing(slot -> slot.source().map(
                    item -> BuiltInRegistries.ITEM.getKey(item).toString(),
                    tag -> tag.location().toString()));

    /**
     * @param item A Spice Item.
     * @return Whether {@code item} is this slot's item, or a member of its tag.
     */
    public boolean accepts(Item item) {
        return source.map(
                own -> own == item,
                tag -> BuiltInRegistries.ITEM.wrapAsHolder(item).is(tag));
    }

    /**
     * @return The slot's item, or the first member of its tag; empty if the tag
     *         is unknown or has no members.
     */
    public Optional<Item> representative() {
        return source.<Optional<Item>>map(
                Optional::of,
                tag -> BuiltInRegistries.ITEM.getTag(tag).flatMap(members -> members.stream().findFirst())
                        .map(Holder::value));
    }
}

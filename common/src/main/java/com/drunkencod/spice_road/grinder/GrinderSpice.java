package com.drunkencod.spice_road.grinder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.drunkencod.spice_road.mix.SpiceMix;
import com.drunkencod.spice_road.mix.SpiceMixes;
import com.drunkencod.spice_road.registry.ModDataComponents;
import com.drunkencod.spice_road.registry.ModItems;
import com.drunkencod.spice_road.spice.SpiceProfileRegistry;

/**
 * One kind of thing the Spice Grinder can add to a food: a loose Spice Item, or
 * a Spice Mix with particular contents. Mixes with different contents are
 * different kinds, even though they are the same item.
 *
 * @param item The Spice Item, or the Spice Mix item for a mix.
 * @param mix  What the mix holds, empty for a loose spice.
 */
public record GrinderSpice(Item item, Optional<SpiceMix> mix) {

    /** Network codec. */
    public static final StreamCodec<RegistryFriendlyByteBuf, GrinderSpice> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.ITEM), GrinderSpice::item,
            ByteBufCodecs.optional(SpiceMix.STREAM_CODEC), GrinderSpice::mix,
            GrinderSpice::new);

    /**
     * @param item A Spice Item.
     * @return It as a loose spice.
     */
    public static GrinderSpice loose(Item item) {
        return new GrinderSpice(item, Optional.empty());
    }

    /**
     * @param stack A stack.
     * @return What the Grinder would add from it: a mix if it is a filled Spice
     *         Mix, a loose spice if it is a Spice Item, or nothing otherwise.
     */
    public static Optional<GrinderSpice> of(ItemStack stack) {
        if (stack.isEmpty())
            return Optional.empty();
        SpiceMix mix = stack.get(ModDataComponents.SPICE_MIX.get());
        if (mix != null)
            return Optional.of(new GrinderSpice(stack.getItem(), Optional.of(mix)));
        if (SpiceProfileRegistry.getDefault(stack.getItem()).isPresent())
            return Optional.of(loose(stack.getItem()));
        return Optional.empty();
    }

    /**
     * @param counts How many of each kind.
     * @return How many of each Spice Item those hold together.
     */
    public static Map<Item, Integer> expand(Map<GrinderSpice, Integer> counts) {
        Map<Item, Integer> spices = new LinkedHashMap<>();
        counts.forEach((spice, count) -> spice.perUnit()
                .forEach((item, amount) -> spices.merge(item, amount * count, Integer::sum)));
        return spices;
    }

    /** @return Whether it is valid to add: a Spice Item, or a Spice Mix item holding spices. */
    public boolean isValid() {
        return mix.map(contents -> item == ModItems.SPICE_MIX.get() && !contents.spices().isEmpty())
                .orElseGet(() -> SpiceProfileRegistry.getDefault(item).isPresent());
    }

    /** @return How many of each Spice Item one of it holds. */
    public Map<Item, Integer> perUnit() {
        return mix.map(SpiceMix::spices).orElseGet(() -> Map.of(item, 1));
    }

    /**
     * @param stack A stack.
     * @return Whether it is one of this kind.
     */
    public boolean matches(ItemStack stack) {
        return stack.is(item) && Objects.equals(stack.get(ModDataComponents.SPICE_MIX.get()), mix.orElse(null));
    }

    /** @return A stack of one, for showing it. */
    public ItemStack displayStack() {
        return mix.map(SpiceMixes::stackOf).orElseGet(item::getDefaultInstance);
    }
}

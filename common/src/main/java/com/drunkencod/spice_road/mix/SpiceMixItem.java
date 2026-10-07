package com.drunkencod.spice_road.mix;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A jar filled with spices - see {@link SpiceMix}. Named after its Mix Preset
 * if it has one. Its tooltip, which lists what it holds, is contributed by the
 * global spice tooltips. Its crafting remainder is the empty jar.
 */
public class SpiceMixItem extends Item {

    /** @param properties Item properties, with the empty jar as crafting remainder. */
    public SpiceMixItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return SpiceMixes.contentsOf(stack).flatMap(SpiceMix::preset)
                .<Component>map(id -> Component.translatable(SpiceMixes.presetTranslationKey(id)))
                .orElseGet(() -> super.getName(stack));
    }
}

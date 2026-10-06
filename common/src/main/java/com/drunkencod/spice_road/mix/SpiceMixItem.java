package com.drunkencod.spice_road.mix;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import com.drunkencod.spice_road.Constants;

/**
 * A jar filled with spices - see {@link SpiceMix}. Named after its Mix Preset
 * if it has one, and lists what it holds in its tooltip. Its crafting remainder
 * is the empty jar.
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

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        SpiceMixes.contentsOf(stack).ifPresent(mix -> {
            tooltip.add(Component.translatable(Constants.MOD_ID + ".tooltip.spice_mix.contents")
                    .withStyle(ChatFormatting.GRAY));
            mix.spices().forEach((item, count) -> tooltip.add(Component.literal(" ")
                    .append(Component.translatable(Constants.MOD_ID + ".tooltip.flavor_contributor",
                            item.getDescription(), count))
                    .withStyle(ChatFormatting.DARK_GRAY)));
        });
    }
}

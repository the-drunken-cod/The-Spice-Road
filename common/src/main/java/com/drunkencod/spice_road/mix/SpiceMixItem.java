package com.drunkencod.spice_road.mix;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.drunkencod.spice_road.advancement.ModCriteriaTriggers;

/**
 * A jar filled with spices - see {@link SpiceMix}. Named after its Mix Preset
 * if it has one. Its tooltip, which lists what it holds, is contributed by the
 * global spice tooltips. Its crafting remainder is the empty jar. Taking one
 * from the crafting grid fires {@link ModCriteriaTriggers#SPICE_MIX_CRAFTED}.
 */
public class SpiceMixItem extends Item {

    /** @param properties Item properties, with the empty jar as crafting remainder. */
    public SpiceMixItem(Properties properties) {
        super(properties);
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);
        if (player instanceof ServerPlayer serverPlayer)
            ModCriteriaTriggers.SPICE_MIX_CRAFTED.get().trigger(serverPlayer, stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        return SpiceMixes.contentsOf(stack).flatMap(SpiceMix::preset)
                .<Component>map(id -> Component.translatable(SpiceMixes.presetTranslationKey(id)))
                .orElseGet(() -> super.getName(stack));
    }
}

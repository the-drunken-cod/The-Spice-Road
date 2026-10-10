package com.drunkencod.spice_road.item;

import java.util.List;

import com.drunkencod.spice_road.Constants;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class JarItem extends Item {
    protected static final String USAGE_TOOLTIP_KEY = Constants.MOD_ID + ".tooltip.jar_usage";

    public JarItem(Item.Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(USAGE_TOOLTIP_KEY).withStyle(ChatFormatting.GRAY));
    }
}

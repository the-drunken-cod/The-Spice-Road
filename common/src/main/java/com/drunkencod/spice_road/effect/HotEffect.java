package com.drunkencod.spice_road.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import com.drunkencod.spice_road.spice.FlavorAxis;

/**
 * Seasoning Effect of the fiery pole of {@link FlavorAxis#HEAT_COOLING}: the
 * eater sweats, so hunger drains faster.
 */
public class HotEffect extends MobEffect {

    private static final float EXHAUSTION_PER_TICK = 0.003F;

    /** Creates the effect, colored like the fiery pole. */
    public HotEffect() {
        super(MobEffectCategory.HARMFUL, FlavorAxis.HEAT_COOLING.getPositiveColor());
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof Player player)
            player.causeFoodExhaustion(EXHAUSTION_PER_TICK * (amplifier + 1));
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}

package com.drunkencod.spice_road.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import com.drunkencod.spice_road.spice.FlavorAxis;

/**
 * Seasoning Effect of the cooling pole of {@link FlavorAxis#HEAT_COOLING}: the
 * eater freezes like in powder snow (overlay and slight slowdown), but never
 * far enough to take freezing damage.
 */
public class ChilledEffect extends MobEffect {

    /** Freeze ticks gained per tick, three to outpace the two vanilla thaws per tick plus one. */
    private static final int FREEZE_PER_TICK = 3;

    /** Creates the effect, colored like the cooling pole. */
    public ChilledEffect() {
        super(MobEffectCategory.HARMFUL, FlavorAxis.HEAT_COOLING.getNegativeColor());
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        int limit = entity.getTicksRequiredToFreeze() - 1;
        if (entity.canFreeze() && entity.getTicksFrozen() < limit)
            entity.setTicksFrozen(Math.min(limit, entity.getTicksFrozen() + FREEZE_PER_TICK + amplifier));
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}

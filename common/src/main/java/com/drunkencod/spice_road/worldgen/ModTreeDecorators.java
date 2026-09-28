package com.drunkencod.spice_road.worldgen;

import java.util.function.Supplier;

import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

import com.drunkencod.spice_road.platform.Services;

/**
 * Registers the {@link TreeDecoratorType}s used by Spice worldgen, via
 * {@link Services#REGISTRY} - {@code BuiltInRegistries.TREE_DECORATOR_TYPE}
 * freezes during vanilla bootstrap, before mods construct, for the same reason
 * {@link ModFeatures} can't use a plain {@code Registry.register} either.
 * <p>
 * The decorators themselves are configured in the {@code minecraft:tree}
 * configured features under
 * {@code common/src/main/resources/data/spice_road/worldgen/}.
 */
public final class ModTreeDecorators {

    /**
     * {@code spice_road:attached_to_logs} - see {@link AttachedToLogsDecorator}.
     */
    public static final Supplier<TreeDecoratorType<AttachedToLogsDecorator>> ATTACHED_TO_LOGS = Services.REGISTRY
            .registerTreeDecoratorType("attached_to_logs",
                    () -> new TreeDecoratorType<AttachedToLogsDecorator>(AttachedToLogsDecorator.CODEC));

    private ModTreeDecorators() {
    }

    /**
     * No-op other than forcing this class (and therefore
     * {@link #ATTACHED_TO_LOGS}'s static initializer) to load.
     */
    public static void register() {
    }
}

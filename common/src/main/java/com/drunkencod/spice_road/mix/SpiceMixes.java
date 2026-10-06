package com.drunkencod.spice_road.mix;

import java.util.Map;
import java.util.Optional;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

import com.drunkencod.spice_road.registry.ModDataComponents;
import com.drunkencod.spice_road.registry.ModItems;
import com.drunkencod.spice_road.spice.SpiceProfileRegistry;

/**
 * Building and reading Spice Mix stacks.
 */
public final class SpiceMixes {

    private SpiceMixes() {
    }

    /**
     * @param spices The counted spices going into the jar.
     * @return A Spice Mix holding {@code spices}, as the first Mix Preset they
     *         match, or as a custom mix if they match none.
     */
    public static ItemStack create(Map<Item, Integer> spices) {
        Optional<ResourceLocation> preset = MixPresetRegistry.match(SpiceMix.canonical(spices));
        return stackOf(new SpiceMix(preset, spices));
    }

    /**
     * @param id     A Mix Preset's ID.
     * @param preset The preset.
     * @return A Spice Mix of one batch of the preset's proportions, with the
     *         first member of each tag; empty if a tag has no members.
     */
    public static Optional<ItemStack> ofPreset(ResourceLocation id, MixPreset preset) {
        return preset.representativeSpices().map(spices -> stackOf(new SpiceMix(Optional.of(id), spices)));
    }

    /**
     * @param mix What the jar holds.
     * @return A Spice Mix stack of one, with the custom model data of its
     *         preset, if it has one.
     */
    public static ItemStack stackOf(SpiceMix mix) {
        ItemStack stack = new ItemStack(ModItems.SPICE_MIX.get());
        stack.set(ModDataComponents.SPICE_MIX.get(), mix);
        mix.preset().flatMap(MixPresetRegistry::get).flatMap(MixPreset::customModelData)
                .ifPresent(data -> stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(data)));
        return stack;
    }

    /**
     * @param stack A stack.
     * @return What it holds, if it is a filled Spice Mix.
     */
    public static Optional<SpiceMix> contentsOf(ItemStack stack) {
        return Optional.ofNullable(stack.get(ModDataComponents.SPICE_MIX.get()));
    }

    /**
     * @param stack A stack.
     * @return Whether it may go into a Spice Mix: a Spice Item that isn't a jar
     *         or a mix itself.
     */
    public static boolean isMixable(ItemStack stack) {
        return !stack.isEmpty() && !stack.is(ModItems.JAR.get()) && !stack.is(ModItems.SPICE_MIX.get())
                && !stack.has(ModDataComponents.SPICE_MIX.get())
                && SpiceProfileRegistry.getDefault(stack.getItem()).isPresent();
    }

    /**
     * @param id A Mix Preset's ID.
     * @return The lang key of its name, {@code mix_preset.<namespace>.<path>}.
     */
    public static String presetTranslationKey(ResourceLocation id) {
        return "mix_preset." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }
}

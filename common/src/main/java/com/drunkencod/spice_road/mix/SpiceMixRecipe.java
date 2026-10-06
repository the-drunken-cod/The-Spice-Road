package com.drunkencod.spice_road.mix;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.ModItems;
import com.drunkencod.spice_road.registry.ModRecipeSerializers;

/**
 * Shapeless crafting of a Spice Mix, like a firework star: exactly one empty
 * jar plus up to the configured capacity of loose Spice Items, in any
 * arrangement. Spices that match a Mix Preset's proportions make that preset's
 * mix, anything else a custom one; checking presets here first is what makes a
 * preset always win.
 */
public class SpiceMixRecipe extends CustomRecipe {

    /** @param category The recipe book category. */
    public SpiceMixRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return spicesIn(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Map<Item, Integer> spices = spicesIn(input);
        return spices == null ? ItemStack.EMPTY : SpiceMixes.create(spices);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.SPICE_MIX.get();
    }

    /**
     * @return How many of each spice the grid holds, one per slot, or
     *         {@code null} unless it holds exactly one jar, at least one spice,
     *         no more spices than the capacity, and nothing else.
     */
    private static @Nullable Map<Item, Integer> spicesIn(CraftingInput input) {
        int jars = 0;
        int total = 0;
        Map<Item, Integer> spices = new LinkedHashMap<>();
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty())
                continue;
            if (stack.is(ModItems.JAR.get())) {
                jars++;
            } else if (SpiceMixes.isMixable(stack)) {
                spices.merge(stack.getItem(), 1, Integer::sum);
                total++;
            } else {
                return null;
            }
        }
        if (jars != 1 || total == 0 || total > Services.CONFIG.getSpiceMixCapacity())
            return null;
        return spices;
    }
}

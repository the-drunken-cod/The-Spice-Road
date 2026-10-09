package com.drunkencod.spice_road.rack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;

class WoodRackRecipeTest {

    /** Slabs around a stick, shaped like the Drying Rack's recipe, with planks standing in for the racks. */
    private static final String RECIPE = """
            {
              "category": "building",
              "key": { "S": { "item": "minecraft:stick" }, "W": { "item": "minecraft:cherry_slab" } },
              "pattern": [" S ", "SWS", "SWS"],
              "result": { "id": "minecraft:oak_planks", "count": 2 },
              "variants": {
                "minecraft:cherry_slab": "minecraft:cherry_planks",
                "minecraft:bamboo_slab": "spice_road:not_registered"
              }
            }
            """;

    private static final HolderLookup.Provider REGISTRIES = HolderLookup.Provider.create(Stream.empty());

    private static WoodRackRecipe recipe;

    @BeforeAll
    static void load() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        JsonObject json = JsonParser.parseString(RECIPE).getAsJsonObject();
        recipe = new WoodRackRecipe.Serializer().codec().codec().parse(JsonOps.INSTANCE, json).getOrThrow();
    }

    /** @return A 3x3 grid with {@code bottomCenter} in the center of its bottom row and a stick in every other slot. */
    private static CraftingInput gridWith(Item bottomCenter) {
        ItemStack[] slots = new ItemStack[9];
        for (int i = 0; i < slots.length; i++)
            slots[i] = new ItemStack(i == 7 ? bottomCenter : Items.STICK);
        return CraftingInput.of(3, 3, List.of(slots));
    }

    private static ItemStack assemble(Item bottomCenter) {
        return recipe.assemble(gridWith(bottomCenter), REGISTRIES);
    }

    @Test
    void slabWithVariantMakesThatVariantAtTheResultCount() {
        ItemStack result = assemble(Items.CHERRY_SLAB);
        assertTrue(result.is(Items.CHERRY_PLANKS));
        assertEquals(2, result.getCount());
    }

    @Test
    void slabWithoutVariantMakesTheRecipesOwnResult() {
        assertTrue(assemble(Items.STONE_SLAB).is(Items.OAK_PLANKS));
    }

    @Test
    void variantOfAnItemThatDoesntExistMakesTheRecipesOwnResult() {
        assertTrue(assemble(Items.BAMBOO_SLAB).is(Items.OAK_PLANKS));
    }

    @Test
    void onlyTheBottomCenterSlotDecidesTheWood() {
        ItemStack[] slots = new ItemStack[9];
        for (int i = 0; i < slots.length; i++)
            slots[i] = new ItemStack(i == 7 ? Items.OAK_SLAB : Items.CHERRY_SLAB);
        ItemStack result = recipe.assemble(CraftingInput.of(3, 3, List.of(slots)),
                REGISTRIES);
        assertTrue(result.is(Items.OAK_PLANKS));
    }

    @Test
    void viewerVariantsListEachExistingVariantWithItsSlabAtTheBottomCenter() {
        List<WoodRackRecipe.Variant> variants = recipe.getVariants();
        assertEquals(1, variants.size());
        WoodRackRecipe.Variant variant = variants.get(0);
        assertEquals(Items.CHERRY_SLAB, variant.slab());
        assertTrue(variant.result().is(Items.CHERRY_PLANKS));
        assertEquals(2, variant.result().getCount());
        assertEquals(9, variant.ingredients().size());
        assertTrue(variant.ingredients().get(7).test(new ItemStack(Items.CHERRY_SLAB)));
        assertTrue(variant.ingredients().get(4).test(new ItemStack(Items.CHERRY_SLAB)));
        assertTrue(variant.ingredients().get(1).test(new ItemStack(Items.STICK)));
        assertTrue(variant.ingredients().get(0).isEmpty());
    }

    /** @return A 3x3 grid matching the recipe's pattern, with {@code center} and {@code other} as its two slab slots. */
    private static CraftingInput patternWith(Item center, Item other) {
        Item[] items = { Items.AIR, Items.STICK, Items.AIR, Items.STICK, other, Items.STICK, Items.STICK, center,
                Items.STICK };
        return CraftingInput.of(3, 3, Stream.of(items).map(ItemStack::new).toList());
    }

    @Test
    void slabsOfTheSameWoodMatch() {
        assertTrue(recipe.matches(patternWith(Items.CHERRY_SLAB, Items.CHERRY_SLAB), null));
    }

    @Test
    void slabsOfDifferentWoodsDontMatch() {
        assertFalse(recipe.matches(patternWith(Items.CHERRY_SLAB, Items.OAK_SLAB), null));
    }
}

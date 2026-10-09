package com.drunkencod.spice_road.rack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;

import com.drunkencod.spice_road.registry.ModRecipeSerializers;

/**
 * A shaped recipe whose result depends on the wood of the slab in the center of
 * its bottom row, so one recipe makes the rack of every wood. Every slot
 * sharing
 * that slot's ingredient must hold the same item, so the wood can't be mixed.
 * The
 * slab's item is looked up in the {@code variants} (slab item ID to result item
 * ID); any slab not listed, e.g. one of a modded wood, makes the recipe's own
 * {@code result}. Otherwise it is written like
 * {@code minecraft:crafting_shaped}.
 */
public class WoodRackRecipe extends ShapedRecipe {

    private final String group;
    private final CraftingBookCategory category;
    private final ShapedRecipePattern shape;
    private final ItemStack fallback;
    private final boolean showNotification;
    private final Map<ResourceLocation, ResourceLocation> variants;

    /**
     * @param group            The recipe book group.
     * @param category         The recipe book category.
     * @param shape            The pattern and its ingredients.
     * @param fallback         The result for a slab that has no variant.
     * @param showNotification Whether unlocking the recipe shows a toast.
     * @param variants         The result item of each slab item.
     */
    public WoodRackRecipe(String group, CraftingBookCategory category, ShapedRecipePattern shape, ItemStack fallback,
            boolean showNotification, Map<ResourceLocation, ResourceLocation> variants) {
        super(group, category, shape, fallback, showNotification);
        this.group = group;
        this.category = category;
        this.shape = shape;
        this.fallback = fallback;
        this.showNotification = showNotification;
        this.variants = Map.copyOf(variants);
    }

    /**
     * @return The slots of the recipe holding the same ingredient as the bottom
     *         center one, which together decide the wood; empty if that slot is no
     *         ingredient.
     */
    private List<Integer> getWoodSlots() {
        List<Integer> slots = new ArrayList<>();
        NonNullList<Ingredient> ingredients = getIngredients();
        int center = getWidth() * (getHeight() - 1) + getWidth() / 2;
        if (center >= ingredients.size() || ingredients.get(center).isEmpty())
            return slots;
        for (int i = 0; i < ingredients.size(); i++)
            if (ingredients.get(i).equals(ingredients.get(center)))
                slots.add(i);
        return slots;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (!super.matches(input, level))
            return false;
        List<Integer> woodSlots = getWoodSlots();
        for (int slot : woodSlots)
            if (!input.getItem(slot % getWidth(), slot / getWidth()).is(
                    input.getItem(getWidth() / 2, getHeight() - 1).getItem()))
                return false;
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack slab = input.getItem(input.width() / 2, input.height() - 1);
        ResourceLocation variant = variants.get(BuiltInRegistries.ITEM.getKey(slab.getItem()));
        if (variant != null) {
            ItemStack result = BuiltInRegistries.ITEM.getOptional(variant)
                    .map(item -> new ItemStack(item, fallback.getCount())).orElse(ItemStack.EMPTY);
            if (!result.isEmpty())
                return result;
        }
        return super.assemble(input, registries);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.WOOD_RACK.get();
    }

    /**
     * One wood's way of crafting this recipe, for recipe viewers, which only know a
     * recipe by its single result.
     *
     * @param slab        The slab the wood is made from.
     * @param result      What the recipe makes from that slab.
     * @param ingredients The recipe's ingredients, row by row, with {@code slab}
     *                    in every slot that decides the wood.
     * @param width       How many columns the {@code ingredients} have.
     * @param height      How many rows the {@code ingredients} have.
     */
    public record Variant(Item slab, ItemStack result, NonNullList<Ingredient> ingredients, int width, int height) {
    }

    /**
     * @return The {@link Variant} of every slab that makes a different rack than
     *         the recipe's own {@code result}, which viewers already show; empty
     *         if the bottom center of the recipe is no ingredient. Variants of
     *         items
     *         that don't exist are left out.
     */
    public List<Variant> getVariants() {
        List<Variant> found = new ArrayList<>();
        int width = getWidth();
        int height = getHeight();
        List<Integer> woodSlots = getWoodSlots();
        NonNullList<Ingredient> base = getIngredients();
        if (woodSlots.isEmpty())
            return found;
        variants.forEach((slabId, resultId) -> {
            Item slab = BuiltInRegistries.ITEM.getOptional(slabId).orElse(null);
            Item result = BuiltInRegistries.ITEM.getOptional(resultId).orElse(null);
            if (slab == null || result == null || result == fallback.getItem())
                return;
            NonNullList<Ingredient> ingredients = NonNullList.of(Ingredient.EMPTY, base.toArray(Ingredient[]::new));
            for (int slot : woodSlots)
                ingredients.set(slot, Ingredient.of(slab));
            found.add(new Variant(slab, new ItemStack(result, fallback.getCount()), ingredients, width, height));
        });
        return found;
    }

    /**
     * Reads and sends the recipe: the fields of a shaped recipe plus the
     * {@code variants}.
     */
    public static class Serializer implements RecipeSerializer<WoodRackRecipe> {

        private static final MapCodec<WoodRackRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(recipe -> recipe.group),
                CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC)
                        .forGetter(recipe -> recipe.category),
                ShapedRecipePattern.MAP_CODEC.forGetter(recipe -> recipe.shape),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.fallback),
                Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(recipe -> recipe.showNotification),
                Codec.unboundedMap(ResourceLocation.CODEC, ResourceLocation.CODEC)
                        .optionalFieldOf("variants", Map.of()).forGetter(recipe -> recipe.variants))
                .apply(instance, WoodRackRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, WoodRackRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, recipe -> recipe.group,
                CraftingBookCategory.STREAM_CODEC, recipe -> recipe.category,
                ShapedRecipePattern.STREAM_CODEC, recipe -> recipe.shape,
                ItemStack.STREAM_CODEC, recipe -> recipe.fallback,
                ByteBufCodecs.BOOL, recipe -> recipe.showNotification,
                ByteBufCodecs.<ByteBuf, ResourceLocation, ResourceLocation, Map<ResourceLocation, ResourceLocation>>map(
                        HashMap::new, ResourceLocation.STREAM_CODEC, ResourceLocation.STREAM_CODEC),
                recipe -> recipe.variants,
                WoodRackRecipe::new);

        @Override
        public MapCodec<WoodRackRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, WoodRackRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}

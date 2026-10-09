package com.drunkencod.spice_road.rack;

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

import com.drunkencod.spice_road.registry.ModRecipeSerializers;

/**
 * A shaped recipe whose result depends on the wood of the slab in the center of
 * its bottom row, so one recipe makes the rack of every wood. The slab's item is
 * looked up in the {@code variants} (slab item ID to result item ID); any slab
 * not listed, e.g. one of a modded wood, makes the recipe's own {@code result}.
 * Otherwise it is written like {@code minecraft:crafting_shaped}.
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

    /** Reads and sends the recipe: the fields of a shaped recipe plus the {@code variants}. */
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

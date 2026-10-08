package com.drunkencod.spice_road.drying;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import com.drunkencod.spice_road.registry.ModRecipeSerializers;
import com.drunkencod.spice_road.registry.ModRecipeTypes;

/**
 * Drying one item on a Drying Rack into a result, plus an optional
 * {@linkplain Output secondary result} that goes into the rack's secondary
 * slot. Each result has its own chance, rolled independently, so a cycle may
 * yield both, either or neither. Nothing is derived from the input's crafting
 * remainder: a recipe that wants an empty bucket back lists it.
 * <p>
 * Registered as {@code spice_road:drying}.
 */
public class DryingRecipe implements Recipe<SingleRecipeInput> {

    /** Drying time, in ticks, of a recipe that doesn't set one: one minute. */
    public static final int DEFAULT_DRYING_TIME = 1200;

    private final Ingredient ingredient;
    private final Output result;
    private final Optional<Output> secondaryResult;
    private final int dryingTime;

    /**
     * @param ingredient      What can be dried; one item of it is used up.
     * @param result          The primary result.
     * @param secondaryResult The secondary result, if any.
     * @param dryingTime      Ticks one cycle takes at a speed multiplier of 1.
     */
    public DryingRecipe(Ingredient ingredient, Output result, Optional<Output> secondaryResult, int dryingTime) {
        this.ingredient = ingredient;
        this.result = result;
        this.secondaryResult = secondaryResult;
        this.dryingTime = dryingTime;
    }

    /**
     * One result of a {@link DryingRecipe}, as written in its JSON: the fields
     * of an item stack ({@code id}, {@code count}, {@code components}) plus a
     * {@code chance} defaulting to 1.
     *
     * @param stack  The stack produced, never empty.
     * @param chance The chance, from 0 to 1, that a finished cycle produces it.
     */
    public record Output(ItemStack stack, double chance) {

        public static final MapCodec<Output> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ItemStack.ITEM_NON_AIR_CODEC.fieldOf("id").forGetter(output -> output.stack.getItemHolder()),
                ExtraCodecs.intRange(1, 99).optionalFieldOf("count", 1).forGetter(output -> output.stack.getCount()),
                DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY)
                        .forGetter(output -> output.stack.getComponentsPatch()),
                Codec.doubleRange(0, 1).optionalFieldOf("chance", 1.0).forGetter(Output::chance))
                .apply(instance, (item, count, components, chance) -> new Output(
                        new ItemStack(item, count, components), chance)));

        public static final StreamCodec<RegistryFriendlyByteBuf, Output> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, Output::stack,
                ByteBufCodecs.DOUBLE, Output::chance,
                Output::new);
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return result.stack().copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result.stack().copy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, ingredient);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.DRYING.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.DRYING.get();
    }

    /** @return What can be dried. */
    public Ingredient getIngredient() {
        return ingredient;
    }

    /** @return The primary result. */
    public Output getResult() {
        return result;
    }

    /** @return The secondary result, if any. */
    public Optional<Output> getSecondaryResult() {
        return secondaryResult;
    }

    /** @return Ticks one cycle takes at a speed multiplier of 1. */
    public int getDryingTime() {
        return dryingTime;
    }

    /** The serializer of {@link DryingRecipe}. */
    public static class Serializer implements RecipeSerializer<DryingRecipe> {

        public static final MapCodec<DryingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(DryingRecipe::getIngredient),
                Output.CODEC.codec().fieldOf("result").forGetter(DryingRecipe::getResult),
                Output.CODEC.codec().optionalFieldOf("secondary_result").forGetter(DryingRecipe::getSecondaryResult),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("drying_time", DEFAULT_DRYING_TIME)
                        .forGetter(DryingRecipe::getDryingTime))
                .apply(instance, DryingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, DryingRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, DryingRecipe::getIngredient,
                Output.STREAM_CODEC, DryingRecipe::getResult,
                ByteBufCodecs.optional(Output.STREAM_CODEC), DryingRecipe::getSecondaryResult,
                ByteBufCodecs.VAR_INT, DryingRecipe::getDryingTime,
                DryingRecipe::new);

        @Override
        public MapCodec<DryingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DryingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}

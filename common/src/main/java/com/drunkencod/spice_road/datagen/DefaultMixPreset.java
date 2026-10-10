package com.drunkencod.spice_road.datagen;

import java.util.List;
import java.util.Optional;

import com.mojang.datafixers.util.Either;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.mix.MixPreset;
import com.drunkencod.spice_road.mix.PresetSlot;
import com.drunkencod.spice_road.registry.ModItems;
import com.drunkencod.spice_road.spice.ProcessedSpice;
import com.drunkencod.spice_road.spice.Spice;

/**
 * The Mix Presets the mod ships, written to
 * {@code data/spice_road/mix_preset/} and given their own item models by
 * datagen. Datapacks may add, change or remove presets; this only seeds the
 * defaults. A slot is either a single {@link Spice} or a hand-written item tag
 * of this mod's namespace, whose members are interchangeable. A Spice with
 * Processed Spices fills its slot with its raw or any processed item, unless
 * the slot names a single {@link ProcessedSpice}, which then only takes that.
 */
public enum DefaultMixPreset {
    BENGALI("bengali", 1,
            spice(Spice.FENUGREEK), spice(Spice.NIGELLA), spice(Spice.CUMIN), spice(ProcessedSpice.DRIED_MUSTARD),
            spice(Spice.FENNEL)),
    BIRYANI("biryani", 2,
            spice(Spice.CORIANDER, 2), spice(Spice.CARDAMOM), spice(Spice.FENNEL), spice(Spice.CARAWAY),
            spice(ProcessedSpice.DRIED_CLOVE)),
    CAJUN("cajun", 3,
            spice(Spice.HABANERO), spice(Spice.GARLIC), tag("spice/peppercorn"), spice(Spice.THYME)),
    CURRY("curry", 4,
            spice(Spice.CURRY), spice(Spice.CUMIN), spice(Spice.CORIANDER), spice(Spice.TURMERIC),
            spice(Spice.FENNEL), spice(Spice.FENUGREEK)),
    HOLIDAY("holiday", 5,
            spice(ProcessedSpice.DRIED_TONKA), spice(ProcessedSpice.DRIED_STAR_ANISE), spice(Spice.VANILLA),
            spice(Spice.LICORICE), tag("spice/cinnamon")),
    MEDITERRANEAN("mediterranean", 6,
            spice(Spice.THYME), spice(Spice.OREGANO), spice(Spice.ROSEMARY), spice(Spice.PARSLEY),
            spice(Spice.BASIL)),
    MEXICAN("mexican", 7,
            spice(Spice.OREGANO), spice(Spice.HABANERO), spice(Spice.CUMIN), spice(Spice.CORIANDER)),
    PERSIAN("persian", 8,
            spice(Spice.SAFFRON), spice(Spice.TURMERIC), tag("spice/cinnamon"), tag("spice/peppercorn"),
            spice(Spice.CARDAMOM)),
    PUMPKIN_SPICE("pumpkin_spice", 9,
            tag("spice/cinnamon"), spice(ProcessedSpice.DRIED_NUTMEG), spice(ProcessedSpice.DRIED_CLOVE),
            spice(Spice.ALLSPICE), spice(Spice.GINGER)),
    RAS_EL_HANOUT("ras_el_hanout", 10,
            spice(Spice.CARDAMOM), spice(Spice.CUMIN), spice(ProcessedSpice.DRIED_CLOVE), tag("spice/cinnamon"),
            spice(ProcessedSpice.DRIED_NUTMEG), spice(Spice.ALLSPICE), tag("spice/peppercorn"), spice(Spice.MASTIC)),
    SALAD("salad", 11, spice(Spice.TURMERIC), spice(Spice.PARSLEY), spice(Spice.DILL), spice(Spice.GARLIC)),
    ZAATAR("zaatar", 12,
            spice(Spice.THYME), spice(Spice.SUMAC), spice(Spice.OREGANO), spice(Spice.SESAME),
            spice(ProcessedSpice.DRIED_CLOVE));

    /**
     * One proportion of a default preset. Exactly one of {@code spice},
     * {@code processed} and {@code tag} is set.
     *
     * @param spice     The Spice (taking its processed forms too), or {@code null}.
     * @param processed One specific Processed Spice, excluding the raw item and
     *                  its siblings, or {@code null}.
     * @param tag       The path of the item tag in this mod's namespace, or
     *                  {@code null}.
     * @param count     How many of it one batch holds.
     */
    public record Slot(Spice spice, ProcessedSpice processed, String tag, int count) {

        /**
         * @return The slot as the preset's own slot: the Processed Spice's item, or
         *         the Spice's variant tag if it has Processed Spices (else its raw
         *         item), or the hand-written tag.
         */
        PresetSlot toPresetSlot() {
            if (processed != null)
                return new PresetSlot(Either.left(processed.getItem()), count);
            if (spice != null) {
                TagKey<Item> variants = ProcessedSpice.variantTag(spice);
                return new PresetSlot(variants != null ? Either.right(variants)
                        : Either.left(ModItems.byId(spice.getId())), count);
            }
            TagKey<Item> key = TagKey.create(Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, tag));
            return new PresetSlot(Either.right(key), count);
        }
    }

    private final String id;
    private final int customModelData;
    private final List<Slot> slots;

    DefaultMixPreset(String id, int customModelData, Slot... slots) {
        this.id = id;
        this.customModelData = customModelData;
        this.slots = List.of(slots);
    }

    private static Slot spice(Spice spice) {
        return spice(spice, 1);
    }

    private static Slot spice(Spice spice, int count) {
        return new Slot(spice, null, null, count);
    }

    private static Slot spice(ProcessedSpice processed) {
        return spice(processed, 1);
    }

    private static Slot spice(ProcessedSpice processed, int count) {
        return new Slot(null, processed, null, count);
    }

    private static Slot tag(String path) {
        return new Slot(null, null, path, 1);
    }

    /** @return The preset's ID path, also the name of its texture and model. */
    public String getId() {
        return id;
    }

    /**
     * @return The {@code custom_model_data} its mixes are given; unique among
     *         presets, as it picks the model override.
     */
    public int getCustomModelData() {
        return customModelData;
    }

    /**
     * @return The preset as the game loads it; only valid once the items are
     *         registered.
     */
    public MixPreset toPreset() {
        return new MixPreset(slots.stream().map(Slot::toPresetSlot).toList(), Optional.of(customModelData));
    }
}

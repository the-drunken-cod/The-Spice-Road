package com.drunkencod.spice_road.datagen;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.advancement.FoodEatenTrigger;
import com.drunkencod.spice_road.advancement.ItemDriedTrigger;
import com.drunkencod.spice_road.advancement.SpiceMixCraftedTrigger;
import com.drunkencod.spice_road.advancement.SpiceRackFilledTrigger;
import com.drunkencod.spice_road.rack.SpiceRackWood;
import com.drunkencod.spice_road.registry.ModBlocks;
import com.drunkencod.spice_road.registry.ModItems;
import com.drunkencod.spice_road.spice.ProcessedSpice;
import com.drunkencod.spice_road.spice.Spice;

/**
 * Datagens the {@code spice_road} advancement tab: a hidden, auto-granted
 * root (own tab, so future advancements don't crowd a vanilla one) plus
 * Everything Bagel, Hang Out to Dry, Pie Purist (a child of Hang Out to Dry),
 * Nice Rack and Mix-and-Match. Plain vanilla {@code AdvancementProvider}, so
 * this runs unchanged on both loaders.
 */
public class SpiceRoadAdvancements implements AdvancementSubProvider {

    private static final ResourceLocation ROOT_ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "root");

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver) {
        AdvancementHolder root = Advancement.Builder.advancement()
                .display(Spice.getRawById(Spice.HABANERO.getId()),
                        Component.translatable("advancements.spice_road.root.title"),
                        Component.translatable("advancements.spice_road.root.description"),
                        ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID,
                                "textures/block/cinnamon/stripped_cinnamon_log.png"),
                        AdvancementType.TASK, false, false, true)
                .addCriterion("tick", PlayerTrigger.TriggerInstance.tick())
                .build(ROOT_ID);
        saver.accept(root);

        AdvancementHolder everythingBagel = Advancement.Builder.advancement()
                .parent(root)
                .display(Items.BREAD,
                        Component.translatable(
                                "advancements.spice_road.everything_bagel.title"),
                        Component.translatable(
                                "advancements.spice_road.everything_bagel.description"),
                        null, AdvancementType.CHALLENGE, true, true, false)
                .addCriterion("everything_bagel", FoodEatenTrigger.TriggerInstance.foodEaten(
                        Optional.of(ItemPredicate.Builder.item().of(Items.BREAD)),
                        List.of(),
                        Optional.of(16D)))
                .build(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "everything_bagel"));
        saver.accept(everythingBagel);

        AdvancementHolder hangOutToDry = Advancement.Builder.advancement()
                .parent(root)
                .display(ProcessedSpice.DRIED_NUTMEG.getItem(),
                        Component.translatable("advancements.spice_road.hang_out_to_dry.title"),
                        Component.translatable("advancements.spice_road.hang_out_to_dry.description"),
                        null, AdvancementType.TASK, true, true, false)
                .addCriterion("item_dried", ItemDriedTrigger.TriggerInstance.itemDried(Optional.empty(),
                        Optional.empty()))
                .build(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "hang_out_to_dry"));
        saver.accept(hangOutToDry);

        AdvancementHolder niceRack = Advancement.Builder.advancement()
                .parent(root)
                .display(ModBlocks.SPICE_RACK_ITEMS.get(SpiceRackWood.OAK).get(),
                        Component.translatable("advancements.spice_road.nice_rack.title"),
                        Component.translatable("advancements.spice_road.nice_rack.description"),
                        null, AdvancementType.TASK, true, true, false)
                .addCriterion("spice_rack_filled", SpiceRackFilledTrigger.TriggerInstance.spiceRackFilled())
                .build(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "nice_rack"));
        saver.accept(niceRack);

        AdvancementHolder mixAndMatch = Advancement.Builder.advancement()
                .parent(root)
                .display(ModItems.SPICE_MIX.get(),
                        Component.translatable("advancements.spice_road.mix_and_match.title"),
                        Component.translatable("advancements.spice_road.mix_and_match.description"),
                        null, AdvancementType.TASK, true, true, false)
                .addCriterion("spice_mix_crafted",
                        SpiceMixCraftedTrigger.TriggerInstance.spiceMixCrafted(Optional.empty()))
                .build(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "mix_and_match"));
        saver.accept(mixAndMatch);

        AdvancementHolder pumpkinPurist = Advancement.Builder.advancement()
                .parent(hangOutToDry)
                .display(Items.PUMPKIN_PIE, Component.translatable(
                        "advancements.spice_road.pumpkin_purist.title"),
                        Component.translatable(
                                "advancements.spice_road.pumpkin_purist.description"),
                        null, AdvancementType.CHALLENGE, true, true, false)
                .addCriterion("pumpkin_purist", FoodEatenTrigger.TriggerInstance.foodEaten(
                        Optional.of(ItemPredicate.Builder.item().of(Items.PUMPKIN_PIE)),
                        List.of(
                                ItemPredicate.Builder.item()
                                        .of(Spice.getRawById(Spice.CINNAMON.getId())),
                                ItemPredicate.Builder.item()
                                        .of(Spice.getRawById(Spice.CLOVE.getId())),
                                ItemPredicate.Builder.item()
                                        .of(Spice.getRawById(Spice.NUTMEG.getId()))
                        // ItemPredicate.Builder.item()
                        // .of(Spice.getRawById(Spice.CINNAMON.getId()),
                        // ProcessedSpice.DRIED_CINNAMON.getItem()),
                        // ItemPredicate.Builder.item()
                        // .of(Spice.getRawById(Spice.NUTMEG.getId()),
                        // ProcessedSpice.DRIED_NUTMEG.getItem())
                        // ItemPredicate.Builder.item()
                        // .of(Spice.getRawById(Spice.GINGER.getId()),
                        // ProcessedSpice.DRIED_GINGER.getItem()),
                        ),
                        Optional.empty()))
                .build(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "pumpkin_purist"));
        saver.accept(pumpkinPurist);
    }
}

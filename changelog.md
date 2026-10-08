# `0.1.0`
## `0.1.0-alpha.4`
- **Features:**
    - Added some more original textures to replace their placeholders. Spice Mixes are now fully textured.
    - Added JEI and EMI categories (each can be hidden via client config):
        - Spice Profile: the flavor of every spice and Spice Mix preset, including datapack-added ones.
        - Spice Origin: each spice's tier, climate, whether it's hardy or region-bound, yield, plant, harvest action and tool, and seasons.
        - Drying: every Drying Rack recipe, with result chances and the drying time with and without heat.
    - Added the Spice Rack in every vanilla wood variant (crafted from 6 slabs around an iron ingot), which stores 8 stacks of spices and spice mixes and shows them off in a fancy way.
        - Has three models for when it is placed on the floor, on a wall or on a ceiling, and doesn't need a supporting block.
        - Right-click with a spice or spice mix to put it in, with an empty hand to open the GUI.
        - Hoppers and pipes can fill and empty it from any side, and a comparator reads how full it is.
        - Items it accepts can be extended with the `#spice_road:spice_rack_storable` item tag.
        - The Spice Grinder takes spices from nearby racks (`#spice_road:spice_racks` is part of `#spice_road:spice_storage`).
        - Configurable item display and item render distance on the client.
    - The Spice Grinder can now be placed on the floor by sneaking and right-clicking the top of a block, in one of 4 rotations.
        - Placing keeps the run in progress, and right-clicking it opens the same GUI. Sneak with empty hands to pick it up again.
        - Only one player can use it at a time, and it drops itself with its run when broken or when its support is removed.
- **Changes:**
    - Spice Grinder doesn't consume spices anymore when player is in creative mode.
    - Spice Grinder GUI movement buttons have two warning colors now; yellow and red, with configurable steps-left-thresholds.
- **Fixes:**
    - Dried spices are now correctly used in the Spice Mix recipes.
    - `#spice/peppercorns` tag now correctly uses Dried Long Pepper.
    - JEI and EMI now tell Spice Mixes with different contents apart.

<br>

## `0.1.0-alpha.3`
⚠️ **Contains breaking changes.** Existing worlds will be affected!  
- **Features:**
    - Added Spice Mix, which combines a jar (new item) with up to 8 spices in any configuration.
        - Added datapack-provided spice mix presets with a custom name and texture and JEI/EMI recipe.
            - Preset ingredients may be item tags (e.g. `spice_road:spice/cinnamon`), of which any combination of members counts.
        - Added 12 default spice mix presets (look up ingredients with JEI/EMI): bengali, biryani, cajun, curry, holiday, mediterranean, mexican, persian, pumpkin_spice, ras_el_hanout, salad, zaatar.
    - ⚠️ **BREAKING:** Added 6 new spices (spice regions in existing worlds will change):
        - Bushes: basil (temperate), oregano (arid), parsley (temperate).
        - Crops: garlic (temperate), sesame (tropical).
        - Trees: tonka (tropical).
    - Added the Drying Rack, which dries up to 2 items at once (one per side) into processed spices and more.
        - Dries faster while heated, by a heat source block next to it (`#spice_road:heat_sources`) or in a hot biome (`#spice_road:drying_always_heated`, the nether by default).
        - Right-click the left or right half to hang an item up or take one off (sneak with an empty hand for the input). Hoppers and pipes feed the left and right sides from the left and right, share the load from the top, front and back, and extract from below.
        - A redstone signal pauses it and a comparator reads its progress.
        - Added the `items_dried` statistic and the Hang Out to Dry advancement.
        - Has an input, an output and a secondary output per side. Drying recipes (`spice_road:drying`) can yield a second item, each with its own chance.
        - Default recipes include turning raw spices into processed spices, and bleaching dyes to make them brighter.
        - Configurable speed multipliers, sounds and particles.
    - Added 5 new processed spices, each with a drying recipe (longer for rarer spices): dried_clove, dried_long_pepper, dried_mustard, dried_nutmeg, dried_tonka.
        - Moved spice profile from the raw/fresh spices to their new dried/processed spice counterparts.
    - Added some more original textures to replace their placeholders.
    - Added optional GUI dark mode (can be enabled via client config).
- **Changes:**
    - Edited Spice Grinder GUI:
        - Adjusted layout to be more spacious, cohesive and hopefully more intuitive.
        - Added scroll bar to spice ingredient list.
        - Added notches to the spice profile value bars that can help estimate how many steps are left.
        - Added colored outlines around the movement buttons.
        - Added and improved sound effects when adding and removing spices and moving on the board.
        - Improved board cell sprites.
        - Better area for info and warning messages.
    - Reworked some more textures.
    - Rebalanced some spice profile values.

<br>

## `0.1.0-alpha.2`
⚠️ **Contains breaking changes.** Existing worlds will be affected!  
- **Features:**
    - Added Spice Grinder item to season any food items, up to 64 at a time.
        - Roguelite Mechanic involving a 9x9 board of cells that can apply positive effects (boons) or negative effects (banes). Cell positions are pseudo-random, seeded by the input food item, meaning every food has its own board.
        - Movement on the board costs spice profile points, depending on the movement direction.
        - Cells in the outer rings have better effects but cost more to unlock.
        - Unlocked cells are stored on the server, giving a nice sense of progression and repeatability.
    - When seasoned foods are eaten, additionally to their Spice-Grinder-acquired effects, their saturation will receive a boost depending on how many different kinds of spices were used (configurable).
        - Added a config option to add an overcap to the player saturation, so that the saturation boosts aren't wasted. Since this is a mixin and may potentially cause issues with other mods, it can be turned off.
        - If [AppleSkin](https://modrinth.com/mod/appleskin) is installed, its tooltips include the saturation boost of seasoned foods. The hunger bar above the toolbar unfortunately still shows the base value, as adding compatibility for that isn't as straightforward.
    - Added mod icon for in-game mod list.
    - Added spiceless regions with a configurable chance percentage (15% by default).
    - Added water surface decorations to spice ponds. Currently only includes lily pads. May be extended by datapacks to add other lily-pad-like blocks.
    - Rare spice maps now also spawn in shipwreck map chests, desert pyramids, pillager outposts, and woodland mansions. They are slightly more common than epic tier maps.
    - Added textures for the spice plants `Nigella` and `Safflower`.
    - The potion effects Luck and Bad Luck now affect spice drops: each level (capped at 3, configurable) scales the spice yield and the seed drop chance of every player harvest by 15% (also configurable). Guaranteed seed drops are unaffected.
    - Added recipes for the pots from [Botany Pots](https://modrinth.com/mod/botany-pots) and [Botany Trees](https://modrinth.com/mod/botany-trees) as well as the Garden Cloche from [Immersive Engineering](https://modrinth.com/mod/immersiveengineering), so they can be used to grow hardy spices (common and uncommon tiers). They can each be turned off in the new common config file. Applies on `/reload` or restart.
- **Changes:**
    - ⚠️ **BREAKING:** the spice plant and spice tree harvest yield multipliers now also apply to broken plants, vines and leaves, instead of only to right-click harvests. Existing loot tables of other packs that overwrite spice break drops need to use the new `spice_road:harvest_yield` loot function.
    - Fractional harvest yields of spice plants and vines are now rounded up or down at random, like on spice trees.
    - ⚠️ **BREAKING:** renamed `curry_leaf` to just `curry`.
    - Aquatic rhizomes now have a 2-block tall model, so they poke outside their water block.
    - Spice crops and rhizomes now drop seeds based on the tier and a configurable chance.
    - Reordered creative mode tab items.
    - Adjusted a whole bunch of textures.
    - Adjusted many default config values, especially regarding worldgen.
    - Tweaked amounts and sizes of spice ponds and satellite ponds generating, especially in arid biomes.
    - Rebalanced loot generation.
    - (Debug mode) improved F3 spice heart directions.
- **Fixes:**
    - Bonemealing vines outside their "natural randomTick" coords is now possible.
    - Vine-type spice trees growing on the same spot as other trees no longer generate floating vines.
    - Spice crop hitboxes now have a 14x14 footprint.
    - Spice bush hitboxes now start higher.
    - Spice bush with out-of-range age property (like when cycling it with the debug stick) no longer crashes the game.
    - Random errors & warnings: missing refmap, implicit config long->int cast, duplicate `spice_road.png` file.

<br>

## `0.1.0-alpha.1`
Features:
- Items/Blocks:
	- 36 spice plants of types tree, bush, vine, flower_patch, crop, rhizome.
    	- Spices are sourced from tree bark, fruiting leaves, bushes and vines, mature flower crops or spice crops, or rhizomes (of which some need to be grown in still or flowing water (configurable)).
    	- Harvesting can range from plainly picking them by clicking or breaking them, to requiring a harvesting tool, to entirely preventing automated harvesting for some of the highest tier spices (configurable).
    - Fancy and configurable tooltip that shows spice rarity and profile values.
- Worldgen:
	- Generate spice regions with a configurable size and rarity distribution, which pick which kind of spices will be native to that area. Biome climate will then further decide which kinds of spice plants can be randomly picked from.
	- Highly configurable worldgen, with sensible defaults for singleplayer and multiplayer.
	- Worldgen features for generating spices (spice patches, spice ponds, spice trees, spice satellite patches & groves).
	- Rare loot chests (shipwrecks, desert pyramids, pillager outposts, woodland mansion) have a chance to contain epic tier spice maps (configurable).
	- `/locate spice` and `/locate spice_climate` to find naturally generating spice groves.
- Crafting:
	- Crafting any food item with one or more spice items as the ingredients will sum up the spice values and apply them to the resulting food item.
	- Spices can currently only be applied via crafting, the Spice Grinder is not yet implemented.
- Trade:
	- Cartographers sell spice maps with the tier roughly equivalent to the Villager's tier (configurable).
- Advancements:
	- Everything Baguette: Buff yourself by eating a loaf of bread with every flavor axis close to maxed out in either direction.
	- Pie Purist: Feast on a pumpkin pie seasoned with cinnamon, nutmeg, ginger and cloves.
- Data components:
    - `spice_profile`: Holds an object which contains the 8 accumulated bipolar spice axis double values.  
      For example, on the `heat_cooling` axis, positive values contribute to heat, while negative ones contribute to cooling.
        - Spice profiles can be added via datapacks, to add items from other mods as valid spices with their own unique values.
        	- For the spice ingredients, the spice profile is not stored as a data component, but resolved using a registry at runtime, so that the datapack-added spices can stay compatible.
	- `flavor_contributors`: List of items that contributed to the values in `spice_profile`. Used for advancement triggers and max spices-of-type config.
- Compatibility:
	- Supports most other mod's recipe types that extend the default ones.
	- Supports Farmer's Delight pots and cutting boards, so spice_profile components get summed and carry over.
	- Supports Cooking For Blockheads' oven and toaster, so spice_profile components carry over.
	- Supports Sable global coordinate resolution, so spice regions can properly gate growth on sublevels.
	- Supports Serene Seasons season tags, to make all crops work with the season system.
- Debug:
	- Adds F3 overlay lines when the mod was compiled in debug mode that show the current spice climate, type, grove location and distance, and final worldgen seed.
	- Sensible log messages to debug runtime issues with the mod.

Known issues:
- Textures are currently mostly a mix of placeholders and unfinished programmer art.
- Water-dependent rhizomes should poke out of the water with their flowering parts.
- Seed/cutting drops are inconsistent.
- Amount of applications of the same kind of spice is currently uncapped.


<br>

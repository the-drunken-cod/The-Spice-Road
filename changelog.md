# `0.1.0`
## `0.1.0-alpha.2`
- Features:
    - Added mod icon for in-game mod list.
    - Added spiceless regions with a configurable chance percentage.
    - Added water surface decorations to spice ponds. Currently only includes lily pads.
    - Rare spice maps now also spawn in shipwreck map chests, desert pyramids, pillager outposts, and woodland mansions. They are slightly more common than epic tier maps.
    - TODO: Added textures for the spice plants `Nigella`.
- Changes:
    - ⚠️ **BREAKING:** renamed `curry_leaf` to just `curry`.
    - Aquatic rhizomes now have a 2-block tall model, so they poke outside their water block.
    - Spice crops and rhizomes now drop seeds based on the tier and a configurable chance.
    - Reordered creative mode tab items.
    - Adjusted a whole bunch of textures.
    - Adjusted many default config values, especially regarding worldgen.
    - Tweaked amounts and sizes of spice ponds and satellite ponds generating.
    - Tweaked loot generation.
    - (Debug mode) improved F3 spice heart directions.
- Fixes:
    - Bonemealing vines outside their "natural randomTick" coords is now possible.
    - Vine-type spice trees growing on the same spot as other trees no longer generate floating vines.
    - Spice crop hitboxes now have a 14x14 footprint.
    - Spice bush hitboxes now start higher.
    - Spice bush with out-of-range age property used to crash the game (like when cycling it with the debug stick).
    - Random errors; missing refmap and implicit config long->int cast.

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

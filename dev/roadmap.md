### Milestones
Warning: may contain outdated information, and is only to be used as a guideline. Milestone order is not fixed.
1. [x] **Spice Plants**
    - [x] Uses custom perlin map, at a much larger scale than biomes (more like the "large biomes" preset).
        - Add F3 debug info for identifying spice regions (only in a debug env).
        - Plant generation respects biome parameters like temperature.
        - Plant generation uses standard feature JSONs for base definition, and/or datagenning from an enum class' members.
    - [x] Tiers: common, uncommon, rare, epic.
    - [x] Spice Profiles contain an assortment of double values of certain "flavor components" (like spicy, sweet, umami, but also fresh, strong, persistent, etc.).  
    The range of the values is -1 to +1, which allows spices to negatively stack. In order to do this in an intuitive way, spice profiles need to correspond to how the spices would interact in real life.
        - [ ] TODO: find an easy way of creating this system, i.e. a geometrical diagram or tree that will be traversed by the mechanic's algorithm.
    - [ ] Make certain spices non-automatable.
        - Vanilla and Saffron need to be hand-picked by an actual living player (no Create deployers).
<!-- 2. **Spice Amplifiers**
    - [ ] Materials that aren't usually eaten on their own, but also don't count as spices. Instead, they apply a multiplier onto the current flavor profile. If amplifiers are used more than n times (configurable, default 2), they harshly sway all values back towards zero.
    - [ ] Data-driven in the same way spice profiles are.
    - [ ] Items:
        - Solar Salt (from drying): boosts all with a tiny multiplier.
        - Kombu / MSG: boosts all with a small multiplier.
        - Oil / Fat: boosts heat, clean, woody, earthy, floral, savory with a medium multiplier.
            - 
        - Acid: boosts sour, green & pungent with a medium multiplier.
            - Vinegar
            - Citrus
        - Sugar / Honey: boosts sweet, lowers heat with a small multiplier. -->
2. [ ] **Drying Rack**
    - Note: mostly already implemented in Mycomancy, needs to be ported, except for the model, since it's limited to 2 inputs and outputs, and since Mycomancy only runs on NeoForge.
    - Dries up to 2 items at the same time, hard limited by the model and renderer.
    - Custom recipe serializer (see Mycomancy).
        - 1 input slot, 1 output slot, 1 extra slot per side, so 6 slots in total. This is an intentional deviation from Mycomancy's 4 slots.
    - Add Dried Spice item variants, with a set of different spice profile values.
        - Some spices will have more beneficial values when dried, others will have less.
        - Not all spices are dried.
    - Drying Rack will be faster when placed next to a hot block (identified by a tag, again, refer to Mycomancy).
3. **Spice Buffs**
    - Each spice gets a spice profile for its "fresh" and "dried" item variants.
    - There is a mechanic that makes different kinds of spices interact differently and yield different buffs of different levels.
        - The mechanic yields diminishing returns, a good amount of spices for max benefits should be around 8-10 (configurable).
    - Buffs have 3 levels of potency (with configurable multipliers).
4. **Spice Rack**
    - Way to store and display spices.
    - Inventory is exposed to the mod loader so automation is supported.
    - Similar in shape to vanilla shelves in 1.21.whatever.
5. **Spice Grinder**
    - Note: texture and sounds already implemented in Mycomancy, need to be ported.
    - Used to apply spices to food items.
        - Spice profile is stored via a data component. Items with the component render a custom tooltip when holding shift, displaying the final spice profile, and buffs.
    - When used, opens a GUI that shows all available spices and their amounts in a scrollable container on the left (grouped by "raw", "dried" and "crushed"), a food slot in the top right, and an info panel (displaying the current spice profile) in the bottom right. Below this, the regular inventory is rendered, as usual.
        - Food slot accepts any food item that is not on an exclude-taglist (`spice_road:unseasonable`). ItemStack can be up to 64 in size.
        - Pulls all items from the player inventory, as well as all nearby (configurable radius) spice racks. When the player position changes, update the list (debounced by 10 ticks). Spice racks are a blockentity added by The Spice Road, but also exist in the mod cooking for blockheads, which should be included in this functionality and made compatible.
        - Hovering over a spice shows its rarity and spice profile levels tooltip.
        - Clicking a spice adds it to the food item stack's spice profile. When more food items are in the food slot than there are of the selected spice, nothing happens and a different click sound plays that indicates the action failed.
6. **Villagers**
    - Note: trades were also already a part of Mycomancy, so they can be used as a baseline (no datagenning tho).
    - Add Spice Trader villager type.
        - Sells common spices and some less common ones, few rare ones and none of the epic ones.
        - Buys all tiers of spices.
        - Datagen the trades from the enums.
    - Make cartographer sell Spice Maps (Journeyman common, Expert uncommon, Master rare); epic Spice Maps are chest loot only (see `dev/todos.md`).
7. **Fermentation Jar**
    - Note: model already implemented in Mycomancy, needs to be ported.
    - Small 1x1 blockentity that can be used to produce up to 2 (configurable) fermented items at a time.
    - Custom recipe serializer, so that datapack JSONs and kubejs can be supported properly.
    - No UI, just world interactions.
    - Hopper support.
    - Has a 0-10 hygiene blockstate value (mirroring a 0-1 value in NBT), which decreases with each finished recipe. Disinfectant can increase the value. All values can be configured.
        - Model reacts to blockstate value and becomes noticeably grimy.
8. **Fermentation Minigame Events**
    - Add different events that can happen to the fermentation machines:
        - Infestation: stray bacteria or mold cultures destroy an increasing percentage of the outputs. Starts to happen when fermenter is below 50% hygiene. Has a numeric blockstate prop.
        - Clog: the airlock is clogged, causing over-pressure sounds. If not corrected in a configurable time frame, it violently overflows, lowering hygiene by a lot (of course also configurable), and reducing output items. Has a bool blockstate prop.
        - Overheat: the jar is placed near a hot block, or in a hot biome during the day (deserts, badlands, nether) in direct sunlight (and transparent blocks). Check for this every 200 ticks (configurable) and update a bool blockstate prop accordingly.
    - Events can be configured in two different modes: "reduction", which makes failed events an active detriment, or "bonus", which makes processes start out with a configured bonus amount, which can decrease back to the base amount via failed events.
9. **Spice Mixes**
    - Combine spices into condensed items that impart more spice values at once.
    - Predefined list of ~8 spice mixes with custom textures and a custom name ("Mediterranean Seasoning", etc.).
    - Allow players to mix any spices together into custom spice mixes.
10. **Documentation**
    - Implement Patchouli as an in-game guide.
    - Create user wiki.
    - Create developer documentation.

<br>

### Development Guidelines
- Use enums to declare common items like the different kinds of spices, and the different spice buffs.  
  Everything else should then source the data from those enum classes.
- Abstract through the services architecture, so that the bulk of the code can live in `common/`.
- Most things should be highly configurable. For example, all perlin values for the "spice regions", how effective different tiers of spices are, how much the spice plants yield, whether the climate mechanic should be active, values for scaling the villager trades, etc. etc.


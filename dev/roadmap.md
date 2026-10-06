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
    - [x] Make certain spices non-automatable.
        - Vanilla and Saffron need to be hand-picked by an actual living player (no Create deployers).
2. [ ] **Spice Mixes**
    - Allows recipes to be reproduced easier, and trade to include preset and custom spice mixes.
    - Use recipe type similar to firework stars. Use new jar item as base, allow adding 8 of any spice.
    - [ ] Combine up to 10 spices into condensed flavor_carrier items that impart more spice values at once.
    - [ ] Predefined list of ~8 spice mixes with custom models (custom textures) and a custom name ("Mediterranean Seasoning", etc.). Listed separately in the creative tab, and recipe takes precedence over the firework-star-like recipe.
3. [ ] **Spice Rack**
    - Way to store and display spices.
    - Inventory is exposed to the mod loader so automation is supported.
    - Similar in shape to vanilla shelves in 1.21.whatever.
4. [x] **Spice Grinder**
    - Note: texture and sounds already implemented in Mycomancy, need to be ported.
    - Used to apply spices to food items using a custom roguelite minigame.
        - Spice profile is stored via a data component. Items with the component render a custom tooltip when holding shift, displaying the final spice profile, effect buffs and spice contributors.
    - When used, opens a GUI that shows all available spices and their amounts in a scrollable container on the left (grouped by "raw", "processed" and "crushed"), a food slot in the top right, and an info panel (displaying the current spice profile) in the bottom right. Below this, the regular inventory is rendered, as usual.
        - Food slot accepts any food item that is not on an exclude-taglist (`spice_road:unseasonable`). ItemStack can be up to 64 in size.
        - Pulls all items from the player inventory, as well as all nearby (configurable radius) spice racks. When the player position changes, update the list (debounced by 10 ticks). Spice racks are a blockentity added by The Spice Road, but also exist in the mod cooking for blockheads, which should be included in this functionality and made compatible.
        - Hovering over a spice shows its rarity and spice profile levels tooltip.
        - Clicking a spice adds it to the food item stack's spice profile. When more food items are in the food slot than there are of the selected spice, nothing happens and a different click sound plays that indicates the action failed.
        - Applying more and more spices gives higher value profiles whose points can be spent in the minigame.
        - Pressing an "Apply" button below the info panel modifies the GUI to show the minigame screen, which can then be played to turn the spice profile values into actual effects. See [todos.md](./todos.md) for more info.
5. [x] **Spice Buffs**
    - Each spice gets a spice profile for its raw item and each of its processed items.
    - There is a mechanic that makes different kinds of spices interact differently and yield different buffs of different levels.
        - The mechanic yields diminishing returns, a good amount of spices for max benefits should be around 8-10 (configurable).
    - Buffs have 3 levels of potency (with configurable multipliers).
6. [ ] **Drying Rack**
    - Note: mostly already implemented in Mycomancy, needs to be ported, except for the model, since it's limited to 2 inputs and outputs, and since Mycomancy only runs on NeoForge.
    - Dries up to 2 items at the same time, hard limited by the model and renderer.
    - Custom recipe serializer (see Mycomancy).
        - 1 input slot, 1 output slot, 1 extra slot per side, so 6 slots in total. This is an intentional deviation from Mycomancy's 4 slots.
    - Add Processed Spice items made by drying (e.g. dried nutmeg), each its own item with its own spice profile values.
        - Some spices will have more beneficial values when dried, others will have less.
        - Not all spices can be dried. Other processing methods (soaking, smoking, cutting, etc.) may follow in the future .
    - Drying Rack will be faster when placed next to a hot block (identified by a tag, again, refer to Mycomancy).
7. [ ] **Spice Effects / Boons & Banes**
    - [ ] Add 16 boons (mostly custom, maybe some vanilla).
    - [ ] Add 16 banes (mostly custom, maybe some vanilla).
    - [ ] Add optional extra boons and banes from common mods like Farmer's Delight.
8. [ ] **Villagers**
    - Note: trades were also already a part of Mycomancy, so they can be used as a baseline (no datagenning tho).
    - [ ] Add Spice Trader villager type.
        - Sells common spices and some less common ones, few rare ones and none of the epic ones.
        - Buys all tiers of spices.
        - Datagen the trades from the enums.
        - Custom house .nbt structures that jigsaw onto multiple of the vanilla village road segments.
    - [x] Make cartographer sell Spice Maps (Journeyman common, Expert uncommon, Master rare); epic Spice Maps are chest loot only (see `dev/todos.md`).
9. [ ] **Documentation**
    - [ ] Create user wiki.
    - [ ] Create developer documentation.
    - [ ] Implement Patchouli as an in-game guide.
10. [x] **Stats**
    - Record how many seasoned food items were eaten.
    - Record how many items were eaten of any of the 16 axes (8 x 2, separated by pos. and neg.).
    - Record how many unique spice types were discovered for 100% purposes.
11. **Advancements**
    - Set of advancements to help teach game mechanics and for some 100% goals.
12. [ ] **Effects pt. 2**
    - Hot (eating something spicy):
        - Adds an FOV vignette overlay (like when freezing is applied via powder snow).
        - (maybe?) Impair vision past n blocks by adding a haze effect similar to a tiny render distance.
        - (maybe?) Potion for pvp?


<br>

### Development Guidelines
- Use enums to declare common items like the different kinds of spices, and the different spice buffs.  
  Everything else should then source the data from those enum classes.
- Abstract through the services architecture, so that the bulk of the code can live in `common/`.
- Most things should be highly configurable. For example, all perlin values for the "spice regions", how effective different tiers of spices are, how much the spice plants yield, whether the climate mechanic should be active, values for scaling the villager trades, etc. etc.


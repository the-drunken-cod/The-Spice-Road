# The Spice Road - Mix Presets
## Table of Contents
- [Introduction](#introduction)
- [Creating a Mix Preset](#creating-a-mix-preset)
- [Name and Look](#name-and-look)

<br>

## Introduction
A Spice Mix is a jar filled with spices, crafted similarly to a firework star: one empty jar plus up to 8 (configurable) spices, in any arrangement. A mix used as a recipe ingredient brings all its spices into the result at once, as if they were added loose, and gives the empty jar back.  
A Mix Preset is a named combination of spices in fixed proportions. When the spices in the grid match a preset's proportions, at any multiple that fits into the jar, the result is that preset's mix (with its own name and look) instead of a plain "Spice Mix". Presets always win over a plain mix.

<br>

## Creating a Mix Preset
Presets are loaded from `data/<namespace>/mix_preset/<name>.json`. The preset's ID is `<namespace>:<name>`. Example (`data/spice_road/mix_preset/pumpkin_spice.json`):
```json
{
  "spices": [
    { "item": "spice_road:cinnamon", "count": 1 },
    { "item": "spice_road:clove", "count": 1 },
    { "item": "spice_road:nutmeg", "count": 1 },
    { "item": "spice_road:allspice", "count": 1 }
  ],
  "custom_model_data": 1
}
```
- `spices` (required): The proportions, each an item (or an item tag, see below) and a whole-number count. Proportions are reduced to their smallest whole numbers, so `2:2:2` behaves exactly like `1:1:1`, and a grid of 2 cinnamon, 2 clove and 2 nutmeg also makes Pumpkin Spice. Any item with a Spice Profile may be used.
- `custom_model_data` (optional): The `minecraft:custom_model_data` value the preset's mixes are given, so a resource pack can give them their own model and texture (see below).

If two presets have the same proportions, the one whose ID sorts first wins. A preset naming an item that doesn't exist (e.g. from a mod that isn't installed) is skipped with an error in the log.

### Tags
Instead of `item`, an entry may name an item tag with `tag`, so that several spices are interchangeable in that spot:
```json
{ "tag": "spice_road:spice/cinnamon", "count": 1 }
```
- The count of a tag is shared by all of its members: with `"count": 2`, two of one member or one each of two members both fill it. Counts still scale with the multiple, so `"count": 1` and a grid of 3 spices from the tag is the 3x batch.
- Spices that only fit one entry go there; if a spice fits several entries (overlapping tags), whichever assignment makes everything fit is used.
- The mix a player crafts holds the spices they actually put in, but is named and looks like the preset no matter which members were used.
- Tags are looked up when crafting, so datapacks may extend them freely, and a tag that is empty or missing makes the preset unmatchable.
- In the creative tab, a preset shows with the first member of each tag, in the order the tag lists them.

### Showing a preset in recipe viewers
The jar-and-spices crafting is a special recipe, which recipe viewers like JEI and EMI can't show. So each shipped preset also has a plain `minecraft:crafting_shapeless` recipe of a jar and one batch of its spices, whose result carries the same components the special recipe would produce (see `data/spice_road/recipe/spice_mix/pumpkin_spice.json`). It's only needed for the preset to be listed; crafting works without it. When adding a preset of your own, copy that recipe and keep its `result` components in line with the preset JSON (the `preset` ID, the `spices` and the `custom_model_data`), otherwise the grid may yield either version.  
For a preset with tags, use the first member of each tag in the recipe, so it only matches the combination whose result it describes. Don't use tag ingredients there, as the recipe would then yield the wrong spices for every other member.

<br>

## Name and Look
- The name is read from the lang key `mix_preset.<namespace>.<name>`, e.g. `"mix_preset.spice_road.pumpkin_spice": "Pumpkin Spice"`.
- The look is chosen through a `custom_model_data` override on the `spice_road:spice_mix` item model. To add one, override `assets/spice_road/models/item/spice_mix.json` in a resource pack and add an entry to its `overrides`:
```json
{ "predicate": { "custom_model_data": 2 }, "model": "mypack:item/my_mix" }
```

<br><br><br><br>

<div style="text-align: center;" align="center">

---

[⬅️ Back: Spice Profile](./spice_profile.md) &bull; [🏠 Docs - Home](../README.md)

</div>

# The Spice Road - Spice Trader
## Table of Contents
- [Introduction](#introduction)
- [Trade Catalog](#trade-catalog)
- [Prices](#prices)
- [Village Houses](#village-houses)
- [Stocked Spice Racks](#stocked-spice-racks)

<br>

## Introduction
The Spice Trader is a villager profession (`spice_road:spice_trader`) whose workstation is any Spice Rack, of any wood and placed any way. It buys and sells loose spices (every raw spice and every Processed Spice) and Spice Mix presets for emeralds. Like every villager, it has one buy offer and one sell offer per villager level, drawn from the pools below.  
What it trades is data; how much it asks for is worked out from the server config.

<br>

## Trade Catalog
Offers are loaded from `data/<namespace>/spice_trade/<name>.json`. The name doesn't matter. Files with the same `level` and `direction` are merged, so a datapack adds choices by adding a file. Example (`data/spice_road/spice_trade/sell_5.json`):
```json
{
  "level": 5,
  "direction": "sell",
  "entries": [
    { "weight": 2, "source": { "type": "spice_road:spices", "tiers": ["common"] }, "uses": 8, "xp": 20 },
    { "weight": 1, "source": { "type": "spice_road:presets", "ids": ["spice_road:salad"] }, "uses": 8, "xp": 20 }
  ]
}
```
- `level` (required): The villager level, 1 (Novice) to 5 (Master).
- `direction` (required): `buy` if the trader pays emeralds for the goods, `sell` if it sells them.
- `entries` (required): The weighted choices. When an offer is rolled, one entry is picked by `weight`, then a random good from its source. An entry whose source holds no good at that moment (e.g. no preset is rare enough) is passed over.
    - `weight` (optional, default 1).
    - `uses` (optional, default 12): How many times the offer can be made before it locks until the villager restocks.
    - `xp` (optional, default 5): The villager XP each trade gives.
    - `source` (required): One of:
        - `spice_road:spices`: Loose spices. `tiers` (required) lists the Tiers (`common`, `uncommon`, `rare`, `epic`) that qualify; `processed` (optional, default `true`) includes Processed Spices, which count as the Tier of the spice they are made from.
        - `spice_road:presets`: Spice Mix presets, each as its standard batch (the first member of every tag in its proportions). `ids` (optional) limits the presets to those IDs, and `max_tier` (optional, default `epic`) leaves out presets with a rarer spice in them. A preset counts as the Tier of its rarest spice.

Only this mod's own spices are ever traded, never other mods' Spice Items, and never custom mixes. When the trader **buys** a preset, it only accepts that standard batch: a mix of the same preset made from other tag members is refused.

<br>

## Prices
Entries never state a price. Counts and prices are worked out when an offer is rolled, from the `spiceTrader` section of the server config:
- `buyRate.<tier>`: How many loose spices of a Tier the trader pays one emerald for.
- `sellRate.<tier>`: How many loose spices of a Tier one emerald buys from the trader. Keep these below the matching buy rates, or the trader pays out more than it charges.
- `priceMultiplier`: Scales everything the trader values, in both directions.

A spice worth less than one emerald is traded in bulk (e.g. 16 common spices for 1 emerald); a more valuable one is 1 for several emeralds, up to 64. A preset's value is the sum of its spices' values, and is always whole emeralds for one mix, at least 1.  
Only offers rolled from now on are affected by a change; existing villagers keep theirs.

<br>

## Village Houses
Spice Trader houses are added to the vanilla village house pools (plains, desert, savanna, snowy and taiga; zombie villages are left alone) when a server starts. Vanilla offers no way to add to a pool, so houses are listed in `data/<namespace>/village_house/<name>.json`. Example (`data/spice_road/village_house/plains.json`):
```json
{
  "pool": "minecraft:village/plains/houses",
  "weight": 3,
  "element": {
    "element_type": "minecraft:legacy_single_pool_element",
    "location": "spice_road:village/houses/plains_spice_trader",
    "processors": "minecraft:mossify_10_percent",
    "projection": "rigid"
  }
}
```
- `pool` (required): The template pool to add to.
- `weight` (optional, default 1): How many times the element is entered into the pool, like the weight of a vanilla entry.
- `element` (required): A template pool element, in the same format as in a vanilla template pool.

Houses are only added to the pools in memory, so they don't show up in the pool files themselves, and villages that already generated keep what they have. The config option `spiceTrader.villageHouses` turns the whole thing off (it applies when the world is loaded). A file naming a pool that doesn't exist, or that fails to parse, is skipped with an error in the log.

The house structures are `data/<namespace>/structure/<path>.nbt` as usual. To attach to the vanilla road pieces, copy the jigsaw blocks of a vanilla house of the same village style.

<br>

## Stocked Spice Racks
A Spice Rack can carry a loot table, like a chest, with an optional loot seed (a seed of `0` means a fresh random roll each time). It rolls the table on its first server tick after it has loaded into a world, not when first opened, so the spices are on it for anyone to see, and automation can use them from the start. Once rolled it's an ordinary rack.
- Set it in a structure by saving the rack with `LootTable` (and optionally `LootTableSeed`) in its block entity data, and nothing in its items.
- A table set on a rack that is already in a loaded world, e.g. `/setblock ~ ~ ~ spice_road:oak_spice_rack[face=floor,facing=north]{LootTable:"spice_road:racks/village/plains"}`, stays pending until the chunk next loads, or until someone opens, breaks or empties the rack. When saving a structure, save it before either happens.
- The racks of the shipped village houses use `spice_road:racks/village/<plains|desert|savanna|snowy|taiga>`: a few common and uncommon spices from the Climates of that village style, with a small chance of a Processed Spice.

<br><br><br><br>

<div style="text-align: center;" align="center">

---

[⬅️ Back: Spice Mix Presets](./mix_preset.md) &bull; [🏠 Docs - Home](../README.md)

</div>

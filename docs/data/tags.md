# The Spice Road - Tags
The following datapack taglists are created and managed by The Spice Road and may be used to make other mods' items compatible.

<br>

## Item Tags
| Tag | Description |
| :-- | :-- |
| `spice_road:spices` | Should contain all spice items that have a spice profile. |
| `spice_road:spices/raw` | Contains all raw spices. |
| `spice_road:spices/processed` | Contains all processed spices, like dried ones. |
| `spice_road:retains_flavor` | Any item with this tag is a "flavor carrier", meaning that it can have an accumulated spice profile without being a food item itself (for example includes storage blocks like dried kelp). |
| `spice_road:unseasonable` | Any item with this tag cannot be seasoned. Includes things like suspicious stew by default, as it uses a funky custom crafting recipe type. |
| `spice_road:cutting_tools` | Any item that can be used on spice crops or leaves that require a cutting tool. Contains shears, swords and modded knives by default. |
| `spice_road:picking_tools` | Any item that can be used to pick spices that require a tool, like vanilla and saffron. Contains `#spice_road:cutting_tools` by default. |
| `spice_road:spice_plant_seeds` | Contains all spice crop seed items. |
| `spice_road:spice_tree_leaves` | Contains all spice tree leaves block items. |
| `spice_road:spice_tree_logs` | Contains all spice tree log block items. |
| `spice_road:spice_tree_saplings` | Contains all spice tree sapling block items. |

<br>

## Block Tags
| Tag | Description |
| :-- | :-- |
| `spice_road:
| `spice_road:
| `spice_road:spice_growable` | Natural ground wild spice plants survive on besides farmland. Only `#minecraft:dirt` by default. |
| `spice_road:aquatic_spice_growable` | Ground aquatic spices like wasabi grow on under water. Contains dirt, sand, gravel, terracotta and clay by default. |
| `spice_road:pond_shore_replaceable` | Ground a spice pond's shore may paint over, unless the pond sets its own `replaceable` tag. |
| `spice_road:spice_crops` | Contains all spice crop blocks, both the farmland-grown ones and their worldgen-only wild counterparts. |
| `spice_road:spice_tree_leaves` | Contains all spice tree leaves blocks. |
| `spice_road:spice_tree_logs` | Contains all spice tree log blocks. |
| `spice_road:spice_tree_saplings` | Contains all spice tree sapling blocks. |
| `spice_road:spice_vines` | Contains all spice vine blocks. Added to `minecraft:climbable`, so they can be climbed like vanilla vines. |

<br>

## Vanilla Tags
| Tag | Description |
| :-- | :-- |
| `minecraft:flowers` | Contains `#spice_road:spice_crops`, `#spice_road:spice_vines` and fruiting spice tree leaves, so bees can pollinate them. Bees only visit ripe spice plants unless the `cultivation.beesPollinateUnripe` config option is enabled. Remove entries to stop bees from visiting them at all. Spice crops are deliberately not in `minecraft:bee_growables`, since bee-grown crops would skip the spice region and growth-speed rules. |

<br>

## Biome Tags
| Tag | Description |
| :-- | :-- |
| `spice_road:no_region_heart` | Biomes that don't grow spices (like oceans) should be added to this list, so that the algorithm that finds spice region hearts (for `/locate` and the maps) doesn't yield them as a result. |
| `spice_road:oasis/desert`, `spice_road:oasis/badlands`, `spice_road:oasis/coast`, `spice_road:oasis/stony` | Biomes whose heart groves, when they sit on unsuitable ground, carve the matching oasis variant. Checked in that order; biomes in none of them get the generic oasis. |
| `spice_road:


<br><br><br><br>

<div style="text-align: center;" align="center">

---

[⬅️ Back: TODO]() &bull; [🏠 Docs - Home](../README.md) &bull; [➡️ Next: Spice Profile](./spice_profile.md)

</div>

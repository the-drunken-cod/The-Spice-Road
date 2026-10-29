# Asset BOM

Assets that are placeholders or still missing, to be made by hand. Placeholders come from `pnpm create_placeholder_texture`.

## Flavor Folio

| Path (under `common/src/main/resources/assets/spice_road/`) | Size | Status | Used for |
| :-- | :-- | :-- | :-- |
| `textures/item/flavor_folio.png` | 16x16 | Placeholder ("FF" on brown) | The book item, via `models/item/flavor_folio.json` |
| `textures/gui/flavor_folio/book.png` | 512x256 | Optional, not created | Custom book GUI. Until it exists the book uses Patchouli's brown one. Set `"book_texture": "spice_road:textures/gui/flavor_folio/book.png"` in `data/spice_road/patchouli_books/flavor_folio/book.json` once it does |
| 

## Spice Trader

Everything else (profession, workstation, trades, village house pool entries, rack loot tables, lang strings) is in code or datagenned. Only these files are missing.

| Path | Size | Status | Used for |
| :-- | :-- | :-- | :-- |
| `common/src/main/resources/assets/spice_road/textures/entity/villager/profession/spice_trader.png` | 64x64 | Required, not created | The villager's outfit, drawn over the biome-type skin (vanilla profession layout, same UV map as e.g. `librarian.png`). Without it the villager is the missing-texture checkerboard |
| `common/src/main/resources/assets/spice_road/textures/entity/zombie_villager/profession/spice_trader.png` | 64x64 | Required, not created | The same outfit on a zombified villager (vanilla zombie villager layout) |
| `...profession/spice_trader.png.mcmeta` (next to each of the two above) | n/a | Optional | `{"villager": {"hat": "none"}}`, or `"partial"` / `"full"`, for whether the head covering hides the villager's hair/hat layer. Vanilla's default without the file is `none` |
| `common/src/main/resources/data/spice_road/structure/village/houses/plains_spice_trader.nbt` | n/a | Exists, but in the wrong folder (`data/spice_road/worldgen/structures/...`); move it | Plains house |
| `.../structure/village/houses/desert_spice_trader.nbt` | n/a | Required, not created | Desert house |
| `.../structure/village/houses/savanna_spice_trader.nbt` | n/a | Required, not created | Savanna house |
| `.../structure/village/houses/snowy_spice_trader.nbt` | n/a | Required, not created | Snowy house |
| `.../structure/village/houses/taiga_spice_trader.nbt` | n/a | Required, not created | Taiga house |
| A work sound | n/a | Optional | The villager uses vanilla's mason work sound. A custom one needs a sound event registered and passed to `ModVillagers` |

Requirements for each house `.nbt`:
- The jigsaw blocks must be copied from a vanilla house of the same village style (`village/<style>/houses/*`, e.g. `plains_small_house_1`), with the same name, target pool, joint type and orientation, so the house attaches to the vanilla road pieces. Save the structure with a structure block with the same size convention vanilla houses use.
- At least one Spice Rack (any wood, on the floor, a wall or the ceiling) is the workstation. Give it a `LootTable` of `spice_road:racks/village/<style>` and, optionally, a `LootTableSeed`, and **no items**. The loot table is rolled when the structure generates.
    - Put it down with `/setblock ~ ~ ~ spice_road:oak_spice_rack[face=floor,facing=north]{LootTable:"spice_road:racks/village/plains"}`, and save the structure from the structure block right away. Opening, breaking or emptying the rack, or reloading its chunk, rolls the table and the saved structure would contain spices instead of the table.
- Beds like in vanilla houses, or the villager has nowhere to sleep. Villagers also only take the job if the rack is reachable.

The profession overlay and the biome *type* skins (plains, desert, ...) are separate in vanilla: the biome skin is vanilla's, and only the overlay above is ours.

## Flavor Folio articles

Every entry containing a red `TODO` needs its text written. Spice entries are in `en_us/entries/spices/<tier>/`, the rest in `en_us/entries/{basics,exploration,mechanics,grinder_minigame}/`. See `dev/flavor_folio_pages.md`.

# Asset BOM

Assets that are placeholders or still missing, to be made by hand. Placeholders come from `pnpm create_placeholder_texture`.

## Flavor Folio

| Path (under `common/src/main/resources/assets/spice_road/`) | Size | Status | Used for |
| :-- | :-- | :-- | :-- |
| `textures/item/flavor_folio.png` | 16x16 | Placeholder | The book item. |
| `textures/gui/flavor_folio/book.png` | 512x256 | Placeholder | Custom book GUI. |
| 

## Spice Trader

Everything else (profession, workstation, trades, village house pool entries, rack loot tables, lang strings) is in code or datagenned. Only these files are missing.

| Path | Size | Status | Used for |
| :-- | :-- | :-- | :-- |
| A work sound | n/a | Optional | The villager uses vanilla's mason work sound. A custom one needs a sound event registered and passed to `ModVillagers` |

## Flavor Folio articles

Every entry containing a red `TODO` needs its text written. Spice entries are in `en_us/entries/spices/<tier>/`, the rest in `en_us/entries/{basics,exploration,mechanics,grinder_minigame}/`. See `dev/flavor_folio_pages.md`.

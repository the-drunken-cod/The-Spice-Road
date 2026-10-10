# Asset BOM

Assets that are placeholders or still missing, to be made by hand. Placeholders come from `pnpm create_placeholder_texture`.

## Flavor Folio

| Path (under `common/src/main/resources/assets/spice_road/`) | Size | Status | Used for |
|---|---|---|---|
| `textures/item/flavor_folio.png` | 16x16 | Placeholder ("FF" on brown) | The book item, via `models/item/flavor_folio.json` |
| `textures/gui/flavor_folio/grinder_board.png` | 256x256 | Placeholder ("SCREENSHOT") | The example `patchouli:image` pages in `mechanics/spice_grinder` and all five `grinder_minigame` entries. Replace per entry with real screenshots, and give each its own file name |
| `textures/gui/flavor_folio/book.png` | 512x256 | Optional, not created | Custom book GUI. Until it exists the book uses Patchouli's brown one. Set `"book_texture": "spice_road:textures/gui/flavor_folio/book.png"` in `data/spice_road/patchouli_books/flavor_folio/book.json` once it does |

## Flavor Folio articles

Every entry containing a red `TODO` needs its text written. Spice entries are in `en_us/entries/spices/<tier>/`, the rest in `en_us/entries/{basics,exploration,mechanics,grinder_minigame}/`. See `dev/flavor_folio_pages.md`.

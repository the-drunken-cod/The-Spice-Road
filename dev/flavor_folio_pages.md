# Flavor Folio Cheatsheet

The Flavor Folio is a Patchouli book (`spice_road:flavor_folio`). This is everything needed to write its articles.

## Where things live

```
common/src/main/resources/
├── data/spice_road/patchouli_books/flavor_folio/book.json   # name, landing text, macros, model, creative tab
└── assets/spice_road/patchouli_books/flavor_folio/
    └── en_us/                                                # one folder per locale, falls back to en_us
        ├── categories/<id>.json                              # id = spice_road:<file name>
        ├── entries/<path>.json                               # any folder depth, linked as $(l:<path>)
        └── templates/<name>.json                             # optional, usable as a page type spice_road:<name>
```

- Categories: `basics`, `spices` (parent) with `spices_common`, `spices_uncommon`, `spices_rare`, `spices_epic`, `exploration`, `mechanics` (parent) with `grinder_minigame`. A sub-category has `"parent": "spice_road:<parent id>"`.
- Entries: `entries/spices/<tier>/<spice>.json` are stamped by `pnpm flavor_folio_entries` (never overwrites; `--check` lists missing, orphaned and mis-filed ones). Everything else is hand-made.
- To translate, copy `en_us/` to e.g. `de_de/` and translate in place. Missing locales and entries fall back to `en_us`. The book name and landing text are lang keys (`spice_road.guide.name`, `spice_road.guide.landing`).
- Extending the book (modpacks, addons): a resource pack with the same folder structure. Patchouli does not allow a datapack for this.
- Testing: the book needs Patchouli in `neoforge/run/mods` (or the Fabric run's `mods` folder). Craft it from a book and any spice, or `/give @s patchouli:guide_book[patchouli:book="spice_road:flavor_folio"]`. After editing files, rebuild the resources and press F3+T in game, which reloads the book.

## Entry and category files

```json
{
    "name": "Cinnamon",
    "icon": "spice_road:cinnamon",
    "category": "spice_road:spices_epic",
    "sortnum": 0,
    "pages": [ ... ]
}
```

Optional entry fields: `advancement` (locks the entry until earned), `flag`, `read_by_default`, `priority`, `secret`. Every page also takes `anchor`, `flag` and `advancement`. Categories take `name`, `description`, `icon`, `sortnum`, `parent`.

## Our page types

All take the optional fields `title` (replaces the default title; `""` hides it) and `text` (shown below the content, same formatting as every text). They draw the same content as the JEI/EMI categories, built when the page is shown, so config values and datapack changes are always current.

| Type | Fields | Shows |
|---|---|---|
| `spice_road:spice_info` | `spice` (e.g. `"cinnamon"`) | Planting item, raw item, tier, climate, growth, yield, plant, harvest, seasons, plus "Found" / "Not found yet" for the reader |
| `spice_road:spice_profile` | `item` (e.g. `"spice_road:cinnamon"`) | The Effective Profile as bars per Flavor Axis. Any item with a Default Profile works, including datapack-added ones |
| `spice_road:drying_recipe` | `recipe`, optional `recipe2` (e.g. `"spice_road:drying/dried_clove"`) | Input, results with chances, and drying time with and without heat. Two fit one page |
| `spice_road:mix_preset` | `preset` (e.g. `"spice_road:pumpkin_spice"`) | The preset's spices in proportion, its jar and its Effective Profile |
| `spice_road:block_tag` | `tag` (e.g. `"spice_road:spice_storage"`) | The items of all blocks in a block tag as a grid, up to 6 rows |

A page whose id doesn't resolve shows a red "Unknown ..." line instead of failing.

```json
{ "type": "spice_road:spice_info", "spice": "cinnamon", "text": "Optional note below the facts." }
{ "type": "spice_road:drying_recipe", "title": "Dried Spices", "recipe": "spice_road:drying/dried_clove", "recipe2": "spice_road:drying/dried_nutmeg" }
```

## Built-in Patchouli page types

```json
{ "type": "patchouli:text", "title": "Optional title", "text": "..." }
{ "type": "patchouli:spotlight", "item": "tag:spice_road:spices", "title": "...", "text": "...", "link_recipe": true }
{ "type": "patchouli:crafting", "recipe": "spice_road:spice_rack", "recipe2": "...", "title": "...", "text": "..." }
{ "type": "patchouli:smelting", "recipe": "minecraft:...", "text": "..." }   // also blasting, smoking, campfire, smithing, stonecutting
{ "type": "patchouli:image", "images": ["spice_road:textures/gui/flavor_folio/grinder_board.png"], "title": "...", "border": true, "text": "..." }
{ "type": "patchouli:relations", "title": "See Also", "entries": ["mechanics/spice_rack", "mechanics/spice_grinder"] }
{ "type": "patchouli:link", "url": "https://...", "link_text": "...", "text": "..." }
{ "type": "patchouli:entity", "entity": "minecraft:villager", "name": "...", "text": "..." }
{ "type": "patchouli:multiblock", "name": "...", "multiblock_id": "...", "text": "..." }
{ "type": "patchouli:quest", "trigger": "spice_road:...", "title": "...", "text": "..." }
{ "type": "patchouli:empty", "draw_filler": true }
```

- Item fields accept an item id, a list separated by commas, or `tag:namespace:path` for an item tag. Block tags can't be used there, which is what `spice_road:block_tag` is for.
- `patchouli:crafting` takes the id of a crafting recipe, e.g. the Drying Rack's `spice_road:crafting/drying_rack` and the Spice Rack's `spice_road:spice_rack`.
- `patchouli:image` expects 256x256 images. Screenshots go in `assets/spice_road/textures/gui/flavor_folio/`.

## Text formatting

| Code | Effect |
|---|---|
| `$(br)`, `$(br2)` | Line break, paragraph break |
| `$(o)`, `$(bold)`, `$(strike)`, `$(underline)` | Italic, bold, strikethrough, underline; end with `$()` |
| `$(l:mechanics/spice_grinder)text$(/l)` | Link to an entry (path below `entries/`, or a full `namespace:path`) |
| `$(l:https://example.com)text$(/l)` | Link to a website |
| `$(t:Tooltip text)text$(/t)` | Hover tooltip |
| `$(k:jump)` | The reader's keybind |
| `$(item)`, `$(thing)` | Patchouli's highlight colors |
| `$(#rrggbb)text$()` | Any color |
| `$(list)` | Bullet |

Macros from `book.json`, usable anywhere in text. Close them with `$()`:
`$(tier_common)`, `$(tier_uncommon)`, `$(tier_rare)`, `$(tier_epic)` (tier colors readable on parchment), `$(spice)` (spice names), `$(cfg)` (config option names), `$(todo)` (the red TODO marker).

## Templates

A file `templates/<name>.json` becomes the page type `spice_road:<name>`. Its `components` (text, items, images, tooltips, ...) can use `#variables` that the page fills in. See Patchouli's documentation for the component list. Use one wherever 3 or more pages share a layout.

# Phase 1: Spice Plants - Implementation Plan

Scope: Phase 1 of `dev/roadmap.md` only (Spice Plants). Food-item components and tooltips for the Spice Grinder are Phase 5 and explicitly out of scope here. Domain terms below are defined in `CONTEXT.md`; the region/climate design rationale is in `docs/adr/0001-spice-regions-independent-of-biomes.md`.

## Build order

Not all 6 Source Types ship at once. Build in this order to prove the enum → plant → profile pipeline cheaply before tackling novel mechanics:

1. `flower_patch` + `crop` (share growth-stage/model-swap logic, both regrow on farmland)
2. `bush` + `vine` (moderate new work, close to vanilla sweet-berry/cave-vine)
3. `tree` + `rhizome` (most novel: sapling growth stages + optional leaf-harvest stage; farmland-spreading)

Spice roster for the initial pass should be pulled from `dev/spice_sources_example.csv` / `dev/flavors_example.csv`, restricted to `flower_patch`/`crop` entries first.

## Creative Work

- Final spice roster selection (which real CSV rows ship first, matching the build order above)
- Spice Profile values per spice/state (raw + dried) across the 8 Flavor Axes
- Models and textures - hand-authored, **not datagenned** (models will stay flexible/hand-tunable)
- Per-source-type blockstate/growth-stage checklist (see below) - this is derived for you, but building the actual assets is your task

### Derived blockstate/model checklist (what needs a model, per Source Type)

- **flower_patch**: N growth-stage blockstates (configurable stage count) → final stage is "mature with flower"; separate flower item (spice-use or decorative) + seed item model
- **crop**: same stage structure as flower_patch, but a distinct (non-flower) model line; decorative flower stage optional per-spice
- **bush**: growth-stage blockstates like sweet berry bush (empty → growing → mature-with-berries)
- **vine**: growth-stage blockstates like cave vines/glow berries (age property → berry-bearing head state)
- **tree**: vanilla-style sapling growth stages; spices with harvestable leaves additionally need a "leaves with harvestable part" blockstate variant
- **rhizome**: unique model, single mature state (no fruiting-body variant needed unless a specific spice calls for it)

## Boilerplate

### 1. Enum classes
- `SourceType`: `TREE, BUSH, VINE, FLOWER_PATCH, CROP, RHIZOME`
- `HarvestAction`: `STRIP, PICK, SHEAR, BREAK` - orthogonal to `SourceType` (a `SourceType` can combine with more than one `HarvestAction` across different spices)
- `Climate`: `TROPICAL, TEMPERATE, ARID, COLD`
- `Tier`: `COMMON, UNCOMMON, RARE, EPIC` - derived from Harvest Difficulty (1→COMMON, {2,3}→UNCOMMON, 4→RARE, 5→EPIC)
- `FlavorAxis`: the 8 bipolar axes from `dev/flavors_example.csv` (`heat`, `sweet_bitter`, `sour_mellow`, `earthy_floral`, `woody_green`, `pungent`, `resinous`, `savory_delicate`), each -1..+1
- `Spice`: one member per spice, holding `SourceType`, `HarvestAction`, `Climate`, `Tier` (or the raw difficulty it derives from), `requiresCuttingTool: boolean`, and two `SpiceProfile`s (raw + dried; `SpiceProfile` = fixed array/map over `FlavorAxis`)
  - Keep per-member data lean - avoid mirroring every CSV column (e.g. flavor-text `harvested_part` strings aren't needed as a runtime field) unless something downstream (loot table datagen) actually consumes it

### 2. Base NeoForge/Fabric platform services
- Extend `IPlatformHelper` with a dedicated-vs-integrated-server check (`server.isDedicatedServer()` equivalent) so config defaults can differ by server type
- Standard multiloader service pattern already established in `common/src/main/java/com/drunkencod/spice_road/platform/`

### 3. Item identification utility
- Tag/registry-based helpers for: "is this item a valid cutting tool for this harvest," "is this item a raw/dried Spice item at all," etc. Used by harvest/loot logic. No food-component involvement.

### 4. Tooltip utility
- Generic helper class making it easy to register "render this content on shift-hold (or always)" tooltip contributions - not spice-specific
- Initial consumer: raw/dried Spice items display their 8 Flavor Axis values as integers -100..100 (i.e. round(value * 100))

### 5. Perlin/Voronoi region (cell) system
Requirements to satisfy (design agreed in `docs/adr/0001-...`):
- Large-scale, biome-independent noise/cell layer (evaluate Voronoi/cellular noise over smooth Perlin - regions need clean, contiguous, large-scale boundaries, not a smoothly-thresholded gradient)
- Each cell has a static, **world-seed-dependent** id/seed
- Climate per point is derived from the real biome's temperature/humidity, overridable via datapack biome tags (e.g. `spice_road:biome/climate/temperate`)
- Climate Bucket = all Spices sharing a Climate value
- Within a cell, the concrete Spice at a point = deterministic pick from the local Climate Bucket, driven by that cell's static seed (so a single region can yield different spices at different points as climate changes across it, but reads as "one roll" per region)
- Cell size: randomized but fairly uniform across all spices, configurable scale
- Spice Tier controls spacing/frequency of that spice's occurrences (rarer = sparser), independent of cell size
- Datapack tag lists can force-add/force-remove specific spices from specific biomes, overriding Climate Bucket membership
- Config: clustering strength must be tunable, including a different default for singleplayer (integrated server) vs. dedicated server, using the platform-service check from item 2

### 6. F3 debug renderer (debug env only)
- Display the assigned Spice at the player's position and the Climate Bucket it was drawn from
- Also surface whatever other intermediate values the region algorithm naturally produces (cell id/seed, raw noise value, etc.) - expose what's useful for tuning rather than a fixed hardcoded list

### 7. Feature template + datagen
- One hand-authored template feature/interaction shape per (`SourceType` × `HarvestAction`) combination actually used by the initial roster - not per spice
- Datagen instantiates configured_feature/placed_feature/biome-tag JSON per `Spice` enum member from its template, driven by the enum's fields
- Loot tables: datagenned from `Spice` enum members, kept minimal - drop the raw item (± seeds for flower_patch/crop), not an exhaustive parameter set
- Harvest yield: **flat, single configurable count for Phase 1** (no tier-based scaling yet - defer until playtesting informs balance)

## Explicitly deferred (not this pass)

- Tree/bush/vine/rhizome mechanics (build order above)
- Harvest-yield tier scaling
- Dried-state values for spices where "dried" isn't beneficial (case-by-case, your call during profile authoring)
- Spice Buff stacking/interaction algorithm (Phase 3)
- Food-item component + Spice Grinder tooltip (Phase 5)

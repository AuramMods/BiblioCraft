# BiblioCraft 1.20.1 Port Plan

## Working Rules
- Breadth-first first, depth later.
- Always read `PORTING_MEMORY.md` before coding.
- Use `PORTING_MANIFEST.md` as the source index for old 1.12.2 locations.
- Keep this file updated whenever phase scope changes.

## Current Snapshot (2026-02-08)
- Old codebase scan completed across `old-1.12.2`.
- All primary registries mapped: blocks, items, block-items, tile entities, enchantments, entity, networking, GUI containers, sounds, recipes, and add-on modules.
- No fluid registry found in old source.

## Phase 0 - Recon and Inventory (Complete)
- [x] Locate all block and item registration entry points.
- [x] Enumerate all registered blocks/items and their registry names.
- [x] Enumerate block entity registrations and mappings.
- [x] Enumerate enchantments, entity types, and networking packets.
- [x] Enumerate GUI/container topology.
- [x] Enumerate recipe/sound/resource inventory at high level.
- [x] Build initial manifest and memory files.

## Phase 1 - 1.20.1 Registry Skeleton
- [ ] Create `DeferredRegister` sets for:
- [ ] `Block`
- [ ] `Item`
- [ ] `BlockEntityType`
- [ ] `MenuType`
- [ ] `EntityType`
- [ ] `Enchantment`
- [ ] `SoundEvent`
- [ ] `CreativeModeTab`
- [ ] `RecipeSerializer`/`RecipeType` (only where needed)
- [ ] Create one central registry bootstrap package (no feature logic yet).
- [ ] Add placeholder registrations for every legacy block/item/entity/enchantment by ID (can be stub behavior).
- [ ] Ensure game starts with full registry surface present (even if mostly placeholder blocks/items).

## Phase 2 - Block + Item Breadth Port
- [ ] Port all block classes as compile-safe stubs with registry IDs preserved.
- [ ] Port all item classes as compile-safe stubs with registry IDs preserved.
- [ ] Preserve variant strategy placeholders:
- [ ] Wood variants (7-state concept including framed)
- [ ] Color variants (16 colors)
- [ ] Lamp/Lantern metal variants (gold/iron)
- [ ] Typewriter/Sword pedestal color variants
- [ ] Recreate creative tab exposure in new tab system.

## Phase 3 - Block Entities + Menus Breadth
- [ ] Register all legacy block entities in 1.20.1.
- [ ] Add minimal block entity classes per legacy type.
- [ ] Register all menu types used by legacy GUI/container flow.
- [ ] Create placeholder menu classes so open paths compile.

## Phase 4 - Interaction + Network Breadth
- [ ] Create packet channel and message registration equivalent to legacy packet list.
- [ ] Stub packet handlers to no-op/log first.
- [ ] Recreate core event bus hooks (death drop, toss, spawn, overlays, highlights) as placeholders.
- [ ] Port entity seat registration and minimal behavior.

## Phase 5 - Rendering + Data Breadth
- [ ] Stand up model/data pipeline for 1.20.1:
- [ ] Blockstate + model JSON strategy for every block/item ID
- [ ] Temporary/generated assets to make all IDs visible in game
- [ ] Convert or replace OBJ-driven dynamic model system incrementally
- [ ] Migrate sounds (`sounds.json` + `SoundEvent` registry).
- [ ] Move legacy recipes into data pack format where needed.

## Phase 6 - Feature Depth Passes (Iterative)
- [ ] Inventory logic parity per block entity.
- [ ] Lock/key ownership rules.
- [ ] Painting/canvas systems.
- [ ] Printing/typesetting systems.
- [ ] Atlas/compass systems.
- [ ] Rendering parity and performance.
- [ ] Config migration strategy (legacy config booleans -> 1.20 config).

## Phase 7 - Add-on and Compatibility Modules
- [ ] Decide scope for BiblioWoods add-ons in 1.20.1.
- [ ] Re-implement framed texture variant support or replace with modern data-driven alternative.
- [ ] Add integration tests for optional-mod behavior.

## Definition of Done (Port Breadth Milestone)
- [ ] Every legacy registry ID has a 1.20.1 registered counterpart.
- [ ] Game loads with all content IDs present and obtainable.
- [ ] No missing registry/object crash from legacy feature surface.
- [ ] All major systems have at least stub behavior with clear TODO anchors for depth passes.

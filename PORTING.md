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
- 1.20.1 placeholder smoke-test pass added for all 37 legacy blocks:
- Blocks + block-items registered in-game via `DeferredRegister`.
- Creative tab now lists all registered smoke-test block items.
- Blockstate/model/item JSON files generated for all registered blocks.
- `./gradlew compileJava` passes with the new registry/assets.
- 1.20.1 placeholder smoke-test pass extended to all 31 legacy standalone items:
- Standalone items registered in-game via `DeferredRegister`.
- Creative tab now lists block-items and standalone items.
- Item model JSON files generated for all standalone items.
- `./gradlew processResources` passes with full smoke-test asset set.
- Visual baseline pass completed for smoke testing:
- Copied legacy textures and OBJ/MTL assets from `old-1.12.2` into current resources.
- Replaced all 37 block model JSON files with Forge OBJ loader definitions (`flip_v=true`).
- Replaced 31 standalone item model JSON files with legacy texture-backed models (no paper placeholders).
- Normalized legacy MTL references for 1.20 naming/path differences.
- `./gradlew processResources compileJava` passes with the visual baseline assets.
- Log-driven model validation baseline (without launching client in this pass):
- Existing `run/logs/latest.log` and `run/logs/debug.log` currently report `0` missing textures and `0` failed model loads.
- Initial custom-item 3D baseline restored for key legacy items:
- `atlas_book`, `compass`, `death_compass`, `maptool`, and `painting_canvas` now use Forge OBJ item models.
- Added `deathcompass.mtl` and `mtl_override` so death compass uses its own texture map.
- Early block visual-correction pass applied:
- Non-occluding placeholder block behavior enabled (removes full-cube face-culling assumptions).
- Added explicit block particle texture mapping to all 37 OBJ block model JSON files.
- Translated major negatively-authored OBJ meshes by `+1 x`/`+1 z` to reduce whole-model offset in static render path.
- Added follow-up `+1 z` translation for `sign.obj` and `label.obj` (still north-shifted after first pass).
- Added broad 1.12-informed OBJ `visibility` maps to many block models so static 1.20 baseline no longer renders all variant groups at once.
- Added iron material overrides for lamp/lantern model parity (`lamp_iron.mtl`, `lantern_iron.mtl`).
- Added pass-1 non-full block shapes in 1.20 registry:
- New `StaticShapeBlock` class with fixed `getShape`/`getCollisionShape`.
- Replaced full-cube placeholder registration for most display blocks with explicit voxel shapes derived from old 1.12 bounding boxes and current OBJ extents.
- `./gradlew compileJava` passes after shape wiring.
- `runClient` is still manual only (user-run visual checks), no automatic launch.
- Block/custom-item in-hand + inventory 3D pass:
- All 37 block item model JSONs now use direct Forge OBJ loader definitions (copied from block OBJ model config), not `parent` links to block models.
- Added explicit `display` transforms and `automatic_culling=false` to all OBJ-backed item models for more consistent first/third-person and GUI rendering.
- Converted `seatback1..seatback5` from flat generated sprites to OBJ-based item models using `seat.obj` part visibility maps.
- `./gradlew processResources compileJava` passes after item-model rewiring.
- Registry breadth expansion pass completed for major missing surfaces:
- Added placeholder `DeferredRegister` + entries for block entities, menu types, entity types, enchantments, and sound events.
- Added new placeholder runtime classes: `PlaceholderBlockEntity`, `PlaceholderMenu`, `SeatEntity`, `PlaceholderEnchantment`.
- Wired new registers into mod bootstrap (`BiblioCraft` constructor).
- `./gradlew compileJava` passes with the new registry surfaces.
- Block-to-block-entity hookup pass completed:
- Added `PlaceholderEntityBlock` (`EntityBlock` implementation) to resolve BE type by registry id at runtime.
- Switched all 37 block registrations in `ModBlocks` to BE-backed placeholders with explicit BE id mapping.
- Shared legacy mappings preserved: `bookcase` (`bookcase` + `bookcase_creative`) and `bibliolight` (all lamp/lantern variants).
- `desk`, `fancy_workbench`, and `printing_press` now stay full-voxel placeholders but are BE-backed.
- `./gradlew --no-daemon compileJava` passes after wiring.
- Horizontal-facing placement/state breadth pass completed:
- Added `HorizontalFacingEntityBlock` with `facing` blockstate, placement rotation, and per-facing rotated voxel/collision shapes.
- Updated `ModBlocks` placeholder helpers to use `HorizontalFacingEntityBlock` for all registered blocks.
- Rewrote all 37 blockstate JSON files to explicit `facing=north/east/south/west` variants with Y-rotation transforms.
- `./gradlew --no-daemon processResources compileJava` passes after state/rotation wiring.
- Block-item OBJ parity pass (item visibility groups) completed:
- Updated OBJ-backed item models that have dedicated `*_item` mesh groups to use legacy-equivalent item parts instead of block parts.
- Updated files: `models/item/case.json`, `models/item/framed_chest.json`, `models/item/painting_press.json`, `models/item/printing_press.json`.
- This aligns item render baselines with old `ModelCase`/`ModelFramedChest`/`ModelPaintingPress`/`ModelPrintingPress` default part sets.
- `./gradlew --no-daemon processResources compileJava` passes after visibility updates.
- Network + recipe skeleton breadth pass completed:
- Added `ModNetwork` (`SimpleChannel`) and placeholder registration of legacy packet surface:
- 23 serverbound packet placeholders.
- 10 clientbound packet placeholders.
- Added recipe infrastructure placeholders:
- `ModRecipes` with `DeferredRegister` for `RecipeSerializer` and `RecipeType`.
- `EnchantedAtlasRecipe` + `enchantedatlas` serializer/type registration as a placeholder for legacy atlas-enchant recipe flow.
- Registered network/recipe surfaces from mod bootstrap.
- `./gradlew --no-daemon compileJava` passes after wiring.
- Mounted-light placement/state parity pass completed:
- Added `MountedFacingEntityBlock` with `facing + face` (`floor/wall/ceiling`) placement/state logic.
- Switched `lamp_gold`, `lamp_iron`, `lantern_gold`, and `lantern_iron` registrations to mounted placeholders.
- Added mount-specific blockstate variants + model JSONs for wall/ceiling lamp and lantern geometry.
- `./gradlew --no-daemon processResources compileJava` passes after mounted-light wiring.
- Mount-state breadth expansion pass completed for additional legacy blocks:
- Added `FloorWallFacingEntityBlock` for old floor/wall placement logic (no ceiling state) and used it for `case`.
- Updated `MountedFacingEntityBlock` to rotate floor/ceiling shapes by `facing` (not only wall shapes).
- Switched `fancy_sign` to mounted wood placeholder and split model visibility by mount face:
- wall: sign/front only
- floor: sign/front + `feetBottom`
- ceiling: sign/front + `feetTop`
- Switched `map_frame` to mounted wood placeholder with floor/wall/ceiling shape parity baseline.
- Rewrote `case`, `fancy_sign`, and `map_frame` blockstate JSON files to `face + facing` variant grids.
- `./gradlew --no-daemon processResources compileJava` passes after mount-state expansion.
- Event-hook + seat behavior breadth pass completed:
- Added Forge FORGE-bus placeholder event subscribers for legacy hook surface:
- Common: death drop, item toss, spawn/join, seat right-click interaction.
- Client: GUI overlay and block highlight render hooks.
- Added minimal seat entity behavior:
- `SeatEntity` now persists target seat block position, pins itself to seat center, discards when seat block is missing, and self-cleans after passenger leaves.
- Implemented seat mounting baseline:
- Right-clicking `seat` block server-side spawns/reuses `SeatEntity` and mounts player.
- `./gradlew --no-daemon compileJava` passes after event/seat pass.
- Menu-open interaction breadth pass completed:
- `PlaceholderMenu` now uses real registered `MenuType` instead of `null`.
- Added block-id -> menu mapping in `ModMenus` for interactive legacy blocks.
- Added server-side placeholder menu open flow in `PlaceholderEntityBlock#use` via `NetworkHooks.openScreen`.
- This establishes compile-safe/open-safe interaction paths for placeholder menus while deeper container logic remains TODO.
- `./gradlew --no-daemon compileJava` passes after menu-open wiring.

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
- [x] `Block`
- [x] `Item`
- [x] `BlockEntityType`
- [x] `MenuType`
- [x] `EntityType`
- [x] `Enchantment`
- [x] `SoundEvent`
- [x] `CreativeModeTab`
- [x] `RecipeSerializer`/`RecipeType` (only where needed)
- [x] Create one central registry bootstrap package (no feature logic yet).
- [x] Add placeholder registrations for every legacy block/item/entity/enchantment by ID (can be stub behavior).
- [ ] Ensure game starts with full registry surface present (even if mostly placeholder blocks/items).

## Phase 2 - Block + Item Breadth Port
- [x] Port all block classes as compile-safe stubs with registry IDs preserved.
- [x] Port all item classes as compile-safe stubs with registry IDs preserved.
- [x] Wire placeholder blocks to spawn placeholder block entities for all legacy BE-backed block IDs.
- [x] Replace full-cube placeholder shapes with first-pass non-full voxel shapes for smoke testing.
- [x] Add horizontal-facing placeholder state + model rotation variants for broad orientation parity.
- [ ] Preserve variant strategy placeholders:
- [ ] Wood variants (7-state concept including framed)
- [ ] Color variants (16 colors)
- [x] Lamp/Lantern metal variants (gold/iron)
- [x] Mount-state placeholder coverage for legacy mounted blocks (lamp/lantern/case/fancy_sign/map_frame)
- [ ] Typewriter/Sword pedestal color variants
- [ ] Recreate creative tab exposure in new tab system.

## Phase 3 - Block Entities + Menus Breadth
- [x] Register all legacy block entities in 1.20.1.
- [x] Add minimal block entity classes per legacy type.
- [x] Register all menu types used by legacy GUI/container flow.
- [x] Create placeholder menu classes so open paths compile.
- [x] Wire placeholder block interaction paths to open registered placeholder menus.

## Phase 4 - Interaction + Network Breadth
- [x] Create packet channel and message registration equivalent to legacy packet list.
- [x] Stub packet handlers to no-op/log first.
- [x] Recreate core event bus hooks (death drop, toss, spawn, overlays, highlights) as placeholders.
- [x] Port entity seat registration and minimal behavior.

## Phase 5 - Rendering + Data Breadth
- [ ] Stand up model/data pipeline for 1.20.1:
- [x] Blockstate + model JSON strategy for every block/item ID
- [x] Temporary/generated assets to make all IDs visible in game
- [x] Replace placeholder oak/paper visuals with legacy-asset smoke baseline
- [x] Restore first-pass OBJ item models for legacy custom-rendered tools (`atlas_book`, `compass`, `death_compass`, `maptool`, `painting_canvas`)
- [ ] Convert or replace OBJ-driven dynamic model system incrementally
- [ ] Recreate legacy dynamic transform/state behavior (old `ExtendedBlockState` + custom baked model logic) for off-axis/shifted models
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

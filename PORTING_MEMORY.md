# PORTING_MEMORY

**FIRST STEP EVERY SESSION: read this file first, then `PORTING.md`, then `PORTING_MANIFEST.md` before touching code.**

## Session Snapshot (2026-02-08)
- Scope scanned: `old-1.12.2` (425 Java files).
- Core registry flow located and mapped.
- Initial port planning and manifest docs created.
- Implemented 1.20.1 smoke-test block surface:
- Registered all 37 legacy blocks as placeholder blocks.
- Auto-registered matching block-items.
- Added creative tab listing all registered block-items.
- Generated blockstates/models/lang entries for all 37 blocks.
- Verified compile with `./gradlew compileJava`.
- Extended smoke-test surface to standalone items:
- Registered all 31 legacy standalone items as placeholder items.
- Generated item model JSONs for all standalone items.
- Updated creative tab listing to include block-items + standalone items.
- Verified resources with `./gradlew processResources`.
- Replaced placeholder visuals with legacy-asset smoke baseline:
- Copied legacy `textures/` and OBJ/MTL assets into current resources.
- Rewrote all 37 block model JSONs to Forge OBJ loader (`loader: forge:obj`) with `flip_v: true`.
- Replaced all 31 standalone item model JSONs to real legacy texture-backed models (no paper placeholders).
- Normalized legacy MTL texture references for 1.20 naming and case-sensitive paths.
- Verified no missing `bibliocraft:*` texture references from all copied MTL files.
- Verified resources/compile with `./gradlew processResources compileJava`.
- Restored first-pass 3D item models for legacy custom-rendered items:
- `atlas_book`, `compass`, `death_compass`, `maptool`, `painting_canvas` now point to OBJ models.
- Added `models/item/deathcompass.mtl` + `mtl_override` for death-compass texture mapping.
- Re-validated build after model changes with `./gradlew processResources compileJava`.

## Critical Facts To Remember
- Legacy main mod entry: `old-1.12.2/src/main/java/jds/bibliocraft/BiblioCraft.java`.
- Main 1.12 registration split:
- Blocks + block-items: `BlockLoader`
- Standalone items + custom recipes: `ItemLoader`
- Block entities: `CommonProxy.initTileEntities()`
- Enchantments + event subscriber hooks: `BiblioCraft.RegisterTheThings`
- Network packets: `BiblioNetworking.setup()`
- GUI handler: `GuiLoader`
- Entity registration: `EntitySeat` via `EntityRegistry.registerModEntity(...)`
- Old OBJ models were always loaded with `flip-v=true` in 1.12 custom model classes.
- For 1.20.1 static model JSONs, use:
- `{"loader":"forge:obj","model":"bibliocraft:models/block/<file>.obj","flip_v":true}`
- User workflow constraint:
- Do not run `runClient` automatically; ask user when a visual smoke test is needed.

## Registry Surface Size (Legacy)
- Registered blocks: 37
- Registered standalone items: 31
- Registered block-item classes: 26 + 9 direct `ItemBlock` wrappers
- Registered block entities: 33 explicit + shared `BiblioLightTileEntity` (total 34 classes in use)
- Enchantments: 2
- Mod entity types: 1 (`EntitySeat`)
- Packets: 23 serverbound + 10 clientbound
- Recipes JSON files: 344
- Sounds JSON keys: 17
- Fluids: none found

## Registry Surface Size (Current 1.20.1 Placeholder Implementation)
- Placeholder blocks registered: 37
- Placeholder block-items registered: 37
- Placeholder standalone items registered: 31
- Total registered placeholder items currently visible in creative tab: 68
- Baseline visual asset pass completed:
- 37 block models now OBJ-backed (legacy meshes + textures)
- 68 item models now mapped (37 block-item parents + 31 standalone texture-backed models)

## Important Architecture Notes
- Many blocks inherit custom base classes (`BiblioBlock`, `BiblioWoodBlock`, `BiblioColorBlock`, `BiblioLightBlock`) and depend on:
- Block metadata (wood/color variants)
- Tile entity orientation/lock state
- NBT-driven custom texture key `renderTexture`
- Extended blockstate + OBJ model pipeline in `jds.bibliocraft.models`
- Legacy rendering stack heavily uses:
- `OBJLoader`
- custom state mappers
- TESR bindings (`ClientRegistry.bindTileEntitySpecialRenderer`)
- These patterns do not port directly to 1.20.1 and need replacement strategy.

## Gotchas / Risk Items
- Legacy registry names are mixed case (`Bookcase`, `LampGold`, etc.); preserve IDs intentionally or define migration aliases strategy.
- No sound `RegistryEvent.Register<SoundEvent>` flow found; legacy often instantiates `new SoundEvent(...)` directly.
- `RegisterCustomFramedBlocks` recipe registration is mostly commented out; used primarily for framed-content item generation/display in add-ons.
- BiblioWoods modules (`bop`, `forestry`, `natura`, `botania`) are separate `@Mod` classes in old source, but mostly integration logic, not new core block classes.
- Legacy leftovers exist and are not registered (`ItemCompendium`, utility block-item classes, abstract `BlockPainting` base).
- Many legacy models were dynamic (wood/color/parts/needle/lighting) via custom baked model code; current pass is static smoke baseline only.
- Several item models were `builtin/generated` in 1.12 and relied on custom item override renderers; current pass uses static texture-backed item models.
- Legacy block render alignment relied on 1.12 `ExtendedBlockState` + `OBJModel.OBJState` transforms (angle/shift/vertical variants); static 1.20 OBJ JSONs can still appear offset until those transforms are ported.

## Immediate Next Steps (When Continuing)
- Expand placeholder registry from blocks to remaining surfaces (standalone items, block entities, menus, sounds, enchantments, entity).
- Ask user to run a visual smoke test and verify all 37 block models render/place correctly (especially known offset-risk blocks).
- Ask user to verify upgraded OBJ items (`atlas_book`, `compass`, `death_compass`, `maptool`, `painting_canvas`) render as 3D and use expected textures.
- Fix any model/texture issues surfaced in client log (especially lighting and dynamic-state models).
- Keep mapping parity against `PORTING_MANIFEST.md` as ground truth.

## Workspace Reminder
- Active 1.20.1 registry code now in:
- `src/main/java/art/arcane/bibliocraft/BiblioCraft.java`
- `src/main/java/art/arcane/bibliocraft/registry/ModBlocks.java`
- `src/main/java/art/arcane/bibliocraft/registry/ModItems.java`
- Active smoke-test assets now in:
- `src/main/resources/assets/bibliocraft/blockstates`
- `src/main/resources/assets/bibliocraft/models/block`
- `src/main/resources/assets/bibliocraft/models/item`
- `src/main/resources/assets/bibliocraft/lang/en_us.json`
- Use old source under `old-1.12.2` for reference only; do not edit old files unless intentionally annotating migration notes.

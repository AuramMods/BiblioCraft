# PORTING_MEMORY

**FIRST STEP EVERY SESSION: read this file first, then `PORTING.md`, then `PORTING_MANIFEST.md` before touching code.**

## Session Snapshot (2026-02-08)
- Scope scanned: `old-1.12.2` (425 Java files).
- Core registry flow located and mapped.
- Initial port planning and manifest docs created.

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

## Immediate Next Steps (When Continuing)
- Build Phase 1 deferred-register skeleton in 1.20.1 project.
- Register all legacy IDs breadth-first as placeholders.
- Keep mapping parity against `PORTING_MANIFEST.md` as ground truth.

## Workspace Reminder
- New 1.20.1 source currently minimal: `src/main/java/art/arcane/bibliocraft/BiblioCraft.java`.
- Use old source under `old-1.12.2` for reference only; do not edit old files unless intentionally annotating migration notes.

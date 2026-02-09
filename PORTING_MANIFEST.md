# BiblioCraft 1.12.2 Porting Manifest

This file is the breadth-first source index for porting from Forge 1.12.2 to Forge 1.20.1.

## How To Use This Manifest
- Start at registry entry points.
- Use block/item/block-entity tables to locate legacy IDs and source classes.
- Use package indexes when you need implementation details.

## High-Level Inventory
- Old source root: `old-1.12.2/src/main/java`
- Total Java files: 425
- Core package Java files (`jds/bibliocraft`): 421
- Add-on package Java files (`jds/bibliowoods`): 4

## Current 1.20.1 Asset Mapping Snapshot (Smoke Baseline)
- Current resources root: `src/main/resources/assets/bibliocraft`
- Legacy resource source: `old-1.12.2/src/main/resources/assets/bibliocraft`
- Imported for baseline:
- `textures/**`
- `models/block/*.obj` + `models/block/*.mtl`
- `models/item/*.obj` + `models/item/*.mtl`
- `sounds.json`
- `sounds/*.ogg`
- Sound baseline migration (2026-02-09):
- `src/main/resources/assets/bibliocraft/sounds.json` copied from legacy.
- `src/main/resources/assets/bibliocraft/sounds/*.ogg` copied from legacy (39 files).
- `src/main/java/art/arcane/bibliocraft/registry/ModSounds.java` currently registers 17 keys matching `sounds.json` one-to-one.
- Block model loader strategy:
- All 37 block model JSON files now use Forge OBJ loader with `flip_v: true`.
- Path pattern:
- `src/main/resources/assets/bibliocraft/models/block/<block_id>.json`
- -> `bibliocraft:models/block/<legacy_obj_name>.obj`
- Blockstate strategy (current baseline):
- Most blockstate JSON files use `facing` variants (`north/east/south/west`) with Y rotations.
- Mount-aware blocks use `face + facing` variants (`face=floor|wall|ceiling` or `face=floor|wall`) with Y rotations.
- Path pattern:
- `src/main/resources/assets/bibliocraft/blockstates/<block_id>.json`
- -> model `bibliocraft:block/<block_id>` with optional `y` transform.
- Key model filename exceptions (1.20 id -> legacy OBJ):
- `marker_pole` -> `markerpole`
- `furniture_paneler` -> `paneler`
- `framed_chest` -> `framedchest`
- `fancy_sign` -> `sign`
- `potion_shelf` -> `potionshelf`
- `tool_rack` -> `toolrack`
- `map_frame` -> `mapframe`
- `painting_frame_*` -> `paintingframe`
- `painting_press` -> `paintpress`
- `sword_pedestal` -> `swordpedestal`
- `armor_stand` -> `armorstand`
- `typesetting_table` -> `typesettingtable`
- `printing_press` -> `printpress`
- `cookie_jar` -> `cookiejar`
- `dinner_plate` -> `dinnerplate`
- `disc_rack` -> `discrack`
- Standalone item model baseline:
- All 31 standalone item JSON files are now explicit `minecraft:item/generated` with legacy texture paths (no paper placeholders).
- First-pass custom item OBJ restorations (from legacy baked-model path):
- `atlas_book` -> `models/item/atlas.obj`
- `compass` -> `models/item/compass.obj`
- `death_compass` -> `models/item/compass.obj` + `mtl_override=models/item/deathcompass.mtl`
- `maptool` -> `models/item/maptool.obj`
- `painting_canvas` -> `models/item/canvas.obj`
- Item 3D parity update:
- All 37 block item model JSONs now use direct `forge:obj` configs instead of `parent: bibliocraft:block/...`.
- OBJ-backed item models now include explicit `display` transforms and `automatic_culling=false`.
- `seatback1..seatback5` are now OBJ item models based on `models/block/seat.obj` visibility groups:
- `seatback1`: `backSupport`, `backCloth`
- `seatback2`: `backWood2`, `backCloth2`
- `seatback3`: `backWood2`, `backCloth2`, `backWood2Top`
- `seatback4`: `backCloth003`, `backsupport2`
- `seatback5`: `backCloth2`, `backWood2`, `fancyBackWood`
- Item visibility-group parity updates (legacy-equivalent item parts):
- `models/item/case.json` now uses `case_lid_*_item` groups.
- `models/item/framed_chest.json` now uses `small_lid_item` + `latch_item`.
- `models/item/painting_press.json` now enables `painting` + `lid_item`.
- `models/item/printing_press.json` now enables `arm_item` + `bed_item` + default books (`book1`, `book2`, `bookBlue`).
- Known parity gap:
- Legacy 1.12 rendering used `ExtendedBlockState` + `OBJModel.OBJState` dynamic transforms and part selection.
- Current 1.20 smoke baseline is static OBJ JSON, so some models may still appear offset or incomplete until dynamic behavior is ported.
- Current visual-fix overlays on top of smoke baseline:
- All 37 block model JSON files now include a particle texture entry derived from legacy MTL maps.
- Current block placeholder code marks models as non-occluding (prevents full-cube face culling behavior).
- Applied temporary mesh translations (`+1 x`, `+1 z`) to these OBJ files to counter whole-model offset in static render path:
- `cookiejar.obj`, `dinnerplate.obj`, `discrack.obj`, `lamp.obj`, `lantern.obj`, `markerpole.obj`, `paneler.obj`, `potionshelf.obj`, `seat.obj`, `swordpedestal.obj`, `table.obj`, `typewriter.obj`.
- Applied temporary mesh translations (`+1 z`) to:
- `sign.obj`, `label.obj`.
- Added static `visibility` maps (Forge model JSON `visibility`) to suppress overlapping orientation/inventory/frame variant groups for current no-BE/no-dynamic-render baseline.
- First-pass visibility tuned files:
- `lamp_gold`, `lamp_iron`, `case`, `clock`, `table`, `seat`, `typewriter`, `furniture_paneler`, `fancy_sign`, `framed_chest`, `painting_press`, `printing_press`, `desk`, `typesetting_table`, `fancy_workbench`, `cookie_jar`, `bookcase`, `bookcase_creative`, `map_frame`, `painting_frame_borderless`, `painting_frame_simple`, `painting_frame_flat`, `painting_frame_middle`, `painting_frame_fancy`.
- Iron variant material overrides now explicit:
- `lamp_iron` -> `models/block/lamp_iron.mtl`
- `lantern_iron` -> `models/block/lantern_iron.mtl`
- Mounted-state parity updates now in baseline:
- `lamp_*` + `lantern_*` now use `face + facing` blockstates and dedicated wall/ceiling model JSON variants.
- `case` now uses `face=floor|wall` + `facing` blockstate variants (legacy floor/wall placement parity baseline).
- `fancy_sign` now uses `face + facing` with mount-specific visibility:
- wall: `sign` + `front`
- floor: `sign` + `front` + `feetBottom`
- ceiling: `sign` + `front` + `feetTop`
- `map_frame` now uses `face + facing` blockstate variants and mount-specific shapes.

## Current 1.20 Voxel Shape Snapshot (Pass 1)
- Source of truth: `src/main/java/art/arcane/bibliocraft/registry/ModBlocks.java` (`SHAPE_*` constants + registrations).
- Shape block classes:
- `src/main/java/art/arcane/bibliocraft/block/StaticShapeBlock.java`
- `src/main/java/art/arcane/bibliocraft/block/HorizontalFacingEntityBlock.java` (adds facing-based shape rotation)
- `src/main/java/art/arcane/bibliocraft/block/MountedFacingEntityBlock.java` (adds `face + facing` mount-state shape rotation)
- `src/main/java/art/arcane/bibliocraft/block/FloorWallFacingEntityBlock.java` (adds floor/wall mount-state shape rotation)
- Intent: remove full-cube placeholder behavior so model hitboxes are closer to legacy.

| Block ID | Current Shape (normalized 0-1) | Basis |
|---|---|---|
| `bookcase`, `bookcase_creative`, `shelf`, `tool_rack` | `x:0.0-0.5 y:0.0-1.0 z:0.0-1.0` | old half-block behavior + OBJ extent |
| `potion_shelf` | `x:0.5-1.0 y:0.0-1.0 z:0.0-1.0` | OBJ extent (`potionshelf.obj`) |
| `marker_pole` | `x:0.25-0.75 y:0.0-0.9 z:0.25-0.75` | OBJ extent (`markerpole.obj`) |
| `clipboard` | `x:0.0-0.08 y:0.08-0.92 z:0.15-0.85` | old wall board bbox |
| `lantern_gold`, `lantern_iron` | `x/z:0.3-0.7 y:0.0-0.7` | old `BlockLantern*` bbox |
| `lamp_gold`, `lamp_iron` | `x/z:0.18-0.82 y:0.0-1.0` | old floor lamp bbox |
| `furniture_paneler` | `x/z:0.0-1.0 y:0.0-0.63` | old `BlockFurniturePaneler` bbox |
| `framed_chest` | `x/z:0.054-0.946 y:0.0-0.866` | old single chest bbox |
| `fancy_sign` | mounted shape baseline: thin sign plane (`x:0.0-0.06 y:0.2-0.8 z:0.0-1.0`) rotated by `facing` for floor/wall/ceiling placements | old sign bbox + legacy mount states |
| `label` | `x:0.0-0.06 y:0.12-0.38 z:0.28-0.72` | old wall label bbox |
| `table` | union of top slab `y:0.88-1.0` + center leg | old table top logic + current visible variant |
| `seat` | `x/z:0.16-0.84 y:0.0-0.74` | old `BlockSeat` bbox |
| `clock` | `x/z:0.3-0.7 y:0.0-1.0` | conservative non-full placeholder |
| `case` | floor/wall mount baseline: floor (`x:0.06-0.94 y:0.0-0.5 z:0.0-1.0`), wall (`x:0.0-0.5 y:0.0-1.0 z:0.06-0.94`), both rotated by `facing` | old floor/wall case bbox |
| `map_frame` | mounted baseline: wall (`x:0.0-0.05 y:0.0-1.0 z:0.0-1.0`), floor (`y:0.0-0.05` full XZ), ceiling (`y:0.95-1.0` full XZ) | old map-frame floor/wall/ceiling bbox |
| `painting_frame_*` | `x:0.0-0.08 y:0.0-1.0 z:0.0-1.0` | old painting-frame wall bbox |
| `painting_press` | `x/z:0.0-1.0 y:0.0-0.98` | OBJ/legacy near-full profile |
| `typewriter` | `x:0.1384-0.7053 y:0.0-0.4835 z:0.2073-0.7927` | OBJ extent (`typewriter.obj`) |
| `sword_pedestal` | `x:0.2758-0.7242 y:0.0-0.234 z:0.0712-0.9288` | OBJ extent + old bbox |
| `armor_stand` | `x/z:0.3-0.7 y:0.0-1.0` | old orientation bbox collapsed to static |
| `bell` | `x/z:0.4-0.6 y:0.0-0.2` | old `BlockBell` bbox |
| `typesetting_table` | `x/z:0.0-1.0 y:0.0-0.92` | old `BlockTypesettingTable` bbox |
| `cookie_jar` | `x/z:0.18-0.82 y:0.0-0.75` | old `BlockCookieJar` bbox |
| `dinner_plate` | `x/z:0.15-0.85 y:0.0-0.1` | old `BlockDinnerPlate` bbox |
| `disc_rack` | `x:0.0-1.0 y:0.0-0.35 z:0.25-0.75` | old floor disc-rack bbox |
| `fancy_workbench`, `desk`, `printing_press` | full shape placeholder (BE-backed) | pending deeper shape/state pass |

## Current 1.20 Registry Skeleton Snapshot
- Mod bootstrap wiring in: `src/main/java/art/arcane/bibliocraft/BiblioCraft.java`
- Active registry classes:
- `src/main/java/art/arcane/bibliocraft/registry/ModBlocks.java`
- `src/main/java/art/arcane/bibliocraft/registry/ModItems.java`
- `src/main/java/art/arcane/bibliocraft/registry/ModBlockEntities.java`
- `src/main/java/art/arcane/bibliocraft/registry/ModMenus.java`
- `src/main/java/art/arcane/bibliocraft/registry/ModEntities.java`
- `src/main/java/art/arcane/bibliocraft/registry/ModEnchantments.java`
- `src/main/java/art/arcane/bibliocraft/registry/ModSounds.java`
- Placeholder runtime classes:
- `src/main/java/art/arcane/bibliocraft/block/HorizontalFacingEntityBlock.java`
- `src/main/java/art/arcane/bibliocraft/block/MountedFacingEntityBlock.java`
- `src/main/java/art/arcane/bibliocraft/block/FloorWallFacingEntityBlock.java`
- `src/main/java/art/arcane/bibliocraft/block/PlaceholderEntityBlock.java`
- `src/main/java/art/arcane/bibliocraft/blockentity/PlaceholderBlockEntity.java`
- `src/main/java/art/arcane/bibliocraft/menu/PlaceholderMenu.java`
- `src/main/java/art/arcane/bibliocraft/item/MenuOpeningItem.java`
- `src/main/java/art/arcane/bibliocraft/entity/SeatEntity.java`
- `src/main/java/art/arcane/bibliocraft/enchantment/PlaceholderEnchantment.java`
- `src/main/java/art/arcane/bibliocraft/event/CommonGameplayEvents.java`
- `src/main/java/art/arcane/bibliocraft/event/ClientRenderEvents.java`

### Current 1.20 Block -> BlockEntity ID Mapping (Placeholder Runtime)
- Source: `src/main/java/art/arcane/bibliocraft/registry/ModBlocks.java` + `src/main/java/art/arcane/bibliocraft/registry/ModBlockEntities.java`.
- Mapping strategy:
- Most blocks map 1:1 (`<block_id>` -> `<block_entity_id>`).
- Shared mappings:
- `bookcase`, `bookcase_creative` -> `bookcase`
- `lamp_gold`, `lamp_iron`, `lantern_gold`, `lantern_iron` -> `bibliolight`
- Full-shape BE-backed placeholders (pending custom shape/state port):
- `desk` -> `desk`
- `fancy_workbench` -> `fancy_workbench`
- `printing_press` -> `printing_press`

### Placeholder Registry Counts (Current 1.20.1)
- Blocks: 37
- Block items: 37
- Standalone items: 31
- Block entity types: 34
- Menu types: 26
- Entity types: 1 (`seat_entity`)
- Enchantments: 2 (`deathcompassench`, `readingench`)
- Sound events: 17 (from legacy `sounds.json` keys)
- Sound assets: 1 `sounds.json` + 39 `.ogg` files migrated into active resources
- Packet registrations (placeholder): 33 (23 serverbound + 10 clientbound)
- Recipe placeholders: 1 serializer + 1 type (`enchantedatlas`)

### Current 1.20 Network + Recipe Skeleton Snapshot
- Network channel placeholder:
- `src/main/java/art/arcane/bibliocraft/network/ModNetwork.java`
- Legacy packet parity represented via no-payload placeholders:
- Serverbound placeholders: `atlas_wpt`, `type`, `type_flag`, `type_delete`, `type_update`, `mcb_edit`, `mcb_page`, `paneler`, `recipe_craft`, `stock_title`, `stock_compass`, `clipboard`, `update_inv`, `atlas`, `measure`, `map_pin`, `rbook`, `rbook_load`, `sign`, `clock`, `paint_press`, `painting`, `painting_clear`.
- Clientbound placeholders: `drill_text`, `atlas_client`, `paneler_client`, `recipe_text`, `stock_log`, `open_book`, `desk_open_gui`, `atlas_swp_client`, `atlas_tgui`, `sound_player`.
- Recipe placeholder:
- `src/main/java/art/arcane/bibliocraft/registry/ModRecipes.java`
- `src/main/java/art/arcane/bibliocraft/recipe/EnchantedAtlasRecipe.java`
- Registered key: `bibliocraft:enchantedatlas` (serializer + recipe type placeholder).

### Current 1.20 Event Hook + Seat Snapshot
- Common Forge event subscriber:
- `src/main/java/art/arcane/bibliocraft/event/CommonGameplayEvents.java`
- Placeholder legacy hook coverage:
- death flow (`LivingDeathEvent`) captures pending player death position.
- spawn/join flow (`EntityJoinLevelEvent`) consumes pending death marker (placeholder waypoint handoff hook).
- item toss flow (`ItemTossEvent`) closes container for legacy packet-heavy item set.
- seat interaction flow (`PlayerInteractEvent.RightClickBlock`) mounts player onto seat entity for `bibliocraft:seat`.
- Client Forge event subscriber:
- `src/main/java/art/arcane/bibliocraft/event/ClientRenderEvents.java`
- Placeholder legacy client hook coverage:
- overlay hook (`RenderGuiOverlayEvent.Post`).
- highlight hook (`RenderHighlightEvent.Block`).
- Seat entity runtime behavior:
- `src/main/java/art/arcane/bibliocraft/entity/SeatEntity.java`
- persists target seat block position in NBT.
- snaps to seat center every tick.
- self-removes when target block is not `bibliocraft:seat`.
- self-removes after short empty timeout when no passenger remains.

### Current 1.20 Menu-Open Placeholder Snapshot
- Placeholder menu class:
- `src/main/java/art/arcane/bibliocraft/menu/PlaceholderMenu.java`
- now requires a real registered `MenuType` (no null placeholder type).
- Placeholder menu-opening item class:
- `src/main/java/art/arcane/bibliocraft/item/MenuOpeningItem.java`
- opens mapped placeholder menus from main-hand right-click.
- Menu registration:
- `src/main/java/art/arcane/bibliocraft/registry/ModMenus.java`
- uses holder-backed registration so client menu factory can construct typed placeholder menus.
- Placeholder menu open path:
- `src/main/java/art/arcane/bibliocraft/block/PlaceholderEntityBlock.java#use`
- server-side opens mapped menus via `NetworkHooks.openScreen`.
- mapped blocks return `SUCCESS` client-side and `CONSUME` server-side; unmapped blocks return `PASS`.
- Current mapped block IDs:
- `bookcase`, `bookcase_creative`, `shelf`, `armor_stand`, `potion_shelf`, `tool_rack`, `case`, `label`, `desk`, `table`, `cookie_jar`, `dinner_plate`, `disc_rack`, `fancy_sign`, `fancy_workbench`, `painting_press`, `painting_frame_borderless`, `painting_frame_flat`, `painting_frame_simple`, `painting_frame_middle`, `painting_frame_fancy`, `furniture_paneler`, `framed_chest`, `clipboard`, `clock`, `typesetting_table`, `printing_press`, `map_frame`.
- Item-held placeholder menu mapping:
- in `src/main/java/art/arcane/bibliocraft/registry/ModItems.java`:
- `atlas_book` -> `ModMenus.ATLAS`
- `slotted_book` -> `ModMenus.SLOTTED_BOOK`
- `tester_item` -> `ModMenus.TESTER`

## Registry Entry Points (Legacy)
- Main mod class and event subscriber: `old-1.12.2/src/main/java/jds/bibliocraft/BiblioCraft.java`
- Block registration: `old-1.12.2/src/main/java/jds/bibliocraft/BlockLoader.java` (`initBlocks`)
- Block item registration: `old-1.12.2/src/main/java/jds/bibliocraft/BlockLoader.java` (`initBlockItems`)
- Standalone item registration: `old-1.12.2/src/main/java/jds/bibliocraft/ItemLoader.java` (`initItems`)
- Recipe registration hook: `old-1.12.2/src/main/java/jds/bibliocraft/ItemLoader.java` (`addRecipies`)
- Enchantment registration hook: `old-1.12.2/src/main/java/jds/bibliocraft/BiblioCraft.java` (`registerEnchantments`)
- Block entity registration: `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` (`initTileEntities`)
- Entity registration (`EntitySeat`): `old-1.12.2/src/main/java/jds/bibliocraft/BiblioCraft.java` (`preInit`)
- GUI handler registration: `old-1.12.2/src/main/java/jds/bibliocraft/BiblioCraft.java` (`load`)
- GUI/container routing: `old-1.12.2/src/main/java/jds/bibliocraft/GuiLoader.java`
- Simple network channel + packet registration: `old-1.12.2/src/main/java/jds/bibliocraft/network/BiblioNetworking.java`

## Registered Blocks (37)
| Class | Name Constant | Config Gate | Source |
|---|---|---|---|
| `BlockBookcase` | `Bookcase` | `enableBookcase` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockBookcase.java` |
| `BlockBookcaseCreative` | `BookcaseCreative` | `enableBookcase` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockBookcaseCreative.java` |
| `BlockShelf` | `Shelf` | `enableGenericshelf` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockShelf.java` |
| `BlockMarkerPole` | `MarkerPole` | `enableTapemeasure` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockMarkerPole.java` |
| `BlockClipboard` | `Clipboard` | `enableClipboard` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockClipboard.java` |
| `BlockLanternGold` | `LanternGold` | `enableLantern` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockLanternGold.java` |
| `BlockLanternIron` | `LanternIron` | `enableLantern` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockLanternIron.java` |
| `BlockLampGold` | `LampGold` | `enableLamp` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockLampGold.java` |
| `BlockLampIron` | `LampIron` | `enableLamp` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockLampIron.java` |
| `BlockFurniturePaneler` | `FurniturePaneler` | `enableFurniturePaneler` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockFurniturePaneler.java` |
| `BlockFramedChest` | `FramedChest` | `enableFramedChest` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockFramedChest.java` |
| `BlockFancySign` | `FancySign` | `enableFancySign` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockFancySign.java` |
| `BlockFancyWorkbench` | `FancyWorkbench` | `enableFancyWorkbench` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockFancyWorkbench.java` |
| `BlockPotionShelf` | `PotionShelf` | `enablePotionshelf` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockPotionShelf.java` |
| `BlockToolRack` | `ToolRack` | `enableToolrack` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockToolRack.java` |
| `BlockLabel` | `Label` | `enableWoodLabel` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockLabel.java` |
| `BlockDesk` | `Desk` | `enableWritingdesk` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockDesk.java` |
| `BlockTable` | `Table` | `enableTable` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockTable.java` |
| `BlockSeat` | `Seat` | `enableSeat` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockSeat.java` |
| `BlockClock` | `Clock` | `enableClock` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockClock.java` |
| `BlockCase` | `Case` | `enableWeaponcase` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockCase.java` |
| `BlockMapFrame` | `MapFrame` | `enableMapFrame` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockMapFrame.java` |
| `BlockPaintingFrameFlat` | `PaintingFrameFlat` | `enablePainting` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockPaintingFrameFlat.java` |
| `BlockPaintingFrameSimple` | `PaintingFrameSimple` | `enablePainting` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockPaintingFrameSimple.java` |
| `BlockPaintingFrameMiddle` | `PaintingFrameMiddle` | `enablePainting` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockPaintingFrameMiddle.java` |
| `BlockPaintingFrameFancy` | `PaintingFrameFancy` | `enablePainting` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockPaintingFrameFancy.java` |
| `BlockPaintingFrameBorderless` | `PaintingFrameBorderless` | `enablePainting` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockPaintingFrameBorderless.java` |
| `BlockPaintingPress` | `PaintingPress` | `enablePainting` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockPaintingPress.java` |
| `BlockTypeWriter` | `Typewriter` | `enableTypewriter` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockTypeWriter.java` |
| `BlockSwordPedestal` | `SwordPedestal` | `enableSwordPedestal` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockSwordPedestal.java` |
| `BlockArmorStand` | `ArmorStand` | `enableArmorstand` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockArmorStand.java` |
| `BlockBell` | `Bell` | `enableDeskBell` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockBell.java` |
| `BlockTypesettingTable` | `TypesettingTable` | `enablePrintpressTypeMachine` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockTypesettingTable.java` |
| `BlockPrintingPress` | `PrintingPress` | `enablePrintpressTypeMachine` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockPrintingPress.java` |
| `BlockCookieJar` | `CookieJar` | `enableCookieJar` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockCookieJar.java` |
| `BlockDinnerPlate` | `DinnerPlate` | `enableDinnerPlate` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockDinnerPlate.java` |
| `BlockDiscRack` | `DiscRack` | `enableDiscRack` | `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockDiscRack.java` |

## Block Item Registrations

### BlockItem Classes Registered Directly (26 classes / 28 registry instances)
- `BlockItemBookcase` -> `Bookcase`
- `BlockItemBookcaseCreative` -> `BookcaseCreative`
- `BlockItemShelf` -> `Shelf`
- `BlockItemLantern.instanceGold` -> `LanternGold`
- `BlockItemLantern.instanceIron` -> `LanternIron`
- `BlockItemLamp.instanceGold` -> `LampGold`
- `BlockItemLamp.instanceIron` -> `LampIron`
- `BlockItemFurniturePaneler` -> `FurniturePaneler`
- `BlockItemFramedChest` -> `FramedChest`
- `BlockItemFancySign` -> `FancySign`
- `BlockItemFancyWorkbench` -> `FancyWorkbench`
- `BlockItemPotionShelf` -> `PotionShelf`
- `BlockItemToolRack` -> `ToolRack`
- `BlockItemLabel` -> `Label`
- `BlockItemDesk` -> `Desk`
- `BlockItemTable` -> `Table`
- `BlockItemSeat` -> `Seat`
- `BlockItemClock` -> `Clock`
- `BlockItemCase` -> `Case`
- `BlockItemMapFrame` -> `MapFrame`
- `BlockItemPaintingFrameFlat` -> `PaintingFrameFlat`
- `BlockItemPaintingFrameSimple` -> `PaintingFrameSimple`
- `BlockItemPaintingFrameMiddle` -> `PaintingFrameMiddle`
- `BlockItemPaintingFrameFancy` -> `PaintingFrameFancy`
- `BlockItemPaintingFrameBorderless` -> `PaintingFrameBorderless`
- `BlockItemTypewriter` -> `Typewriter`
- `BlockItemSwordPedestal` -> `SwordPedestal`
- `BlockItemArmorStand` -> `ArmorStand`

Source package: `old-1.12.2/src/main/java/jds/bibliocraft/blocks/blockitems`

### Direct `ItemBlock` Wrappers (9)
- `BlockMarkerPole` -> `MarkerPole`
- `BlockClipboard` -> `Clipboard`
- `BlockPaintingPress` -> `PaintingPress`
- `BlockBell` -> `Bell`
- `BlockTypesettingTable` -> `TypesettingTable`
- `BlockPrintingPress` -> `PrintingPress`
- `BlockCookieJar` -> `CookieJar`
- `BlockDinnerPlate` -> `DinnerPlate`
- `BlockDiscRack` -> `DiscRack`

Registration source: `old-1.12.2/src/main/java/jds/bibliocraft/BlockLoader.java`

## Registered Standalone Items (31)
| Class | Name Constant | Config Gate | Source |
|---|---|---|---|
| `ItemWaypointCompass` | `compass` | `enableWaypointCompass` | `old-1.12.2/src/main/java/jds/bibliocraft/items/ItemWaypointCompass.java` |
| `ItemMapTool` | `maptool` | `enableMapFrame` | `old-1.12.2/src/main/java/jds/bibliocraft/items/ItemMapTool.java` |
| `ItemSeatBack` | `seatback1` | `enableSeat` | `old-1.12.2/src/main/java/jds/bibliocraft/items/ItemSeatBack.java` |
| `ItemSeatBack2` | `seatback2` | `enableSeat` | `old-1.12.2/src/main/java/jds/bibliocraft/items/ItemSeatBack2.java` |
| `ItemSeatBack3` | `seatback3` | `enableSeat` | `old-1.12.2/src/main/java/jds/bibliocraft/items/ItemSeatBack3.java` |
| `ItemSeatBack4` | `seatback4` | `enableSeat` | `old-1.12.2/src/main/java/jds/bibliocraft/items/ItemSeatBack4.java` |
| `ItemSeatBack5` | `seatback5` | `enableSeat` | `old-1.12.2/src/main/java/jds/bibliocraft/items/ItemSeatBack5.java` |
| `ItemStockroomCatalog` | `StockroomCatalog` | `enableStockroomCatalog` | `old-1.12.2/src/main/java/jds/bibliocraft/items/ItemStockroomCatalog.java` |
| `ItemPlumbLine` | `PlumbLine` | `enablePlumbLine` | `old-1.12.2/src/main/java/jds/bibliocraft/items/ItemPlumbLine.java` |
| `ItemFramingSaw` | `FramingSaw` | `enableFurniturePaneler` | `old-1.12.2/src/main/java/jds/bibliocraft/items/ItemFramingSaw.java` |
| `ItemFramingBoard` | `FramingBoard` | `enableFurniturePaneler` | `old-1.12.2/src/main/java/jds/bibliocraft/items/ItemFramingBoard.java` |
| `ItemFramingSheet` | `FramingSheet` | `enableFurniturePaneler` | `old-1.12.2/src/main/java/jds/bibliocraft/items/ItemFramingSheet.java` |
| `ItemNameTester` | `TesterItem` | `enableTesterItem` | `old-1.12.2/src/main/java/jds/bibliocraft/items/ItemNameTester.java` |
| `ItemAtlas` | `AtlasBook` | `enableAtlas` | `old-1.12.2/src/main/java/jds/bibliocraft/items/ItemAtlas.java` |
| `ItemAtlasPlate` | `AtlasPlate` | `enableAtlas` | `old-1.12.2/src/main/java/jds/bibliocraft/items/ItemAtlasPlate.java` |
| `ItemDeathCompass` | `DeathCompass` | `enableAtlas && enableDeathCompass` | `old-1.12.2/src/main/java/jds/bibliocraft/items/ItemDeathCompass.java` |
| `ItemPaintingCanvas` | `PaintingCanvas` | `enablePainting` | `old-1.12.2/src/main/java/jds/bibliocraft/items/ItemPaintingCanvas.java` |
| `ItemBigBook` | `BigBook` | `enableBigBook` | `old-1.12.2/src/main/java/jds/bibliocraft/items/ItemBigBook.java` |
| `ItemRecipeBook` | `RecipeBook` | `enableFancyWorkbench` | `old-1.12.2/src/main/java/jds/bibliocraft/items/ItemRecipeBook.java` |
| `ItemSlottedBook` | `SlottedBook` | `enableSlottedBook` | `old-1.12.2/src/main/java/jds/bibliocraft/items/ItemSlottedBook.java` |
| `ItemHandDrill` | `HandDrill` | `enableHandDrill` | `old-1.12.2/src/main/java/jds/bibliocraft/items/ItemHandDrill.java` |
| `ItemTapeMeasure` | `tapeMeasure` | `enableTapemeasure` | `old-1.12.2/src/main/java/jds/bibliocraft/items/ItemTapeMeasure.java` |
| `ItemTape` | `tape` | `enableTapemeasure` | `old-1.12.2/src/main/java/jds/bibliocraft/items/ItemTape.java` |
| `ItemChase` | `BiblioChase` | `enablePrintpressTypeMachine` | `old-1.12.2/src/main/java/jds/bibliocraft/items/ItemChase.java` |
| `ItemPlate` | `PrintPlate` | `enablePrintpressTypeMachine` | `old-1.12.2/src/main/java/jds/bibliocraft/items/ItemPlate.java` |
| `ItemEnchantedPlate` | `EnchantedPlate` | `enablePrintpressTypeMachine` | `old-1.12.2/src/main/java/jds/bibliocraft/items/ItemEnchantedPlate.java` |
| `ItemRedstoneBook` | `BiblioRedBook` | `enableRedstonebook` | `old-1.12.2/src/main/java/jds/bibliocraft/items/ItemRedstoneBook.java` |
| `ItemReadingGlasses` | `BiblioGlasses` | `enableReadingglasses` | `old-1.12.2/src/main/java/jds/bibliocraft/items/ItemReadingGlasses.java` |
| `ItemDrill` | `BiblioDrill` | `enableDrill` | `old-1.12.2/src/main/java/jds/bibliocraft/items/ItemDrill.java` |
| `ItemLock` | `BiblioCreativeLock` | `enableLock` | `old-1.12.2/src/main/java/jds/bibliocraft/items/ItemLock.java` |
| `ItemClipboard` | `BiblioClipboard` | `enableClipboard` | `old-1.12.2/src/main/java/jds/bibliocraft/items/ItemClipboard.java` |

## Enchantments
| Class | Registry Name | Config Gate | Source |
|---|---|---|---|
| `EnchantmentDeathCompass` | `bibliocraft.deathcompassench` | `enableAtlas && enableDeathCompass` | `old-1.12.2/src/main/java/jds/bibliocraft/enchantments/EnchantmentDeathCompass.java` |
| `EnchantmentReading` | `bibliocraft.readingench` | `enableReadingglasses` | `old-1.12.2/src/main/java/jds/bibliocraft/enchantments/EnchantmentReading.java` |

## Entity Registrations
| Type | Legacy ID | Source |
|---|---|---|
| `EntitySeat` | `bibliocraft:BiblioSeat` (`SeatEntity`, numeric ID 0 in callsite) | `old-1.12.2/src/main/java/jds/bibliocraft/BiblioCraft.java` |

## Block Entity Registrations (34 active registrations)
| Block Entity Class | Registry Key Source | Config Gate | Source |
|---|---|---|---|
| `TileEntityBookcase` | `BlockBookcase.name` | `enableBookcase` | `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` |
| `TileEntityShelf` | `BlockShelf.name` | `enableGenericshelf` | `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` |
| `TileEntityMarkerPole` | `BlockMarkerPole.name` | `enableTapemeasure` | `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` |
| `TileEntityClipboard` | `BlockClipboard.name` | `enableClipboard` | `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` |
| `BiblioLightTileEntity` | `BiblioLightBlock.name` (`bibliolight`) | `enableLamp || enableLantern` | `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` |
| `TileEntityFurniturePaneler` | `BlockFurniturePaneler.name` | `enableFurniturePaneler` | `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` |
| `TileEntityPotionShelf` | `BlockPotionShelf.name` | `enablePotionshelf` | `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` |
| `TileEntityToolRack` | `BlockToolRack.name` | `enableToolrack` | `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` |
| `TileEntityLabel` | `BlockLabel.name` | `enableWoodLabel` | `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` |
| `TileEntityDesk` | `BlockDesk.name` | `enableWritingdesk` | `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` |
| `TileEntityTable` | `BlockTable.name` | `enableTable` | `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` |
| `TileEntitySeat` | `BlockSeat.name` | `enableSeat` | `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` |
| `TileEntityFancySign` | `BlockFancySign.name` | `enableFancySign` | `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` |
| `TileEntityFancyWorkbench` | `BlockFancyWorkbench.name` | `enableFancyWorkbench` | `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` |
| `TileEntityFramedChest` | `BlockFramedChest.name` | `enableFramedChest` | `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` |
| `TileEntityMapFrame` | `BlockMapFrame.name` | `enableMapFrame` | `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` |
| `TileEntityCase` | `BlockCase.name` | `enableWeaponcase` | `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` |
| `TileEntityClock` | `BlockClock.name` | `enableClock` | `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` |
| `TileEntityPaintingBorderless` | `BlockPaintingFrameBorderless.name` | `enablePainting` | `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` |
| `TileEntityPaintingFancy` | `BlockPaintingFrameFancy.name` | `enablePainting` | `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` |
| `TileEntityPaintingFlat` | `BlockPaintingFrameFlat.name` | `enablePainting` | `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` |
| `TileEntityPaintingMiddle` | `BlockPaintingFrameMiddle.name` | `enablePainting` | `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` |
| `TileEntityPaintingSimple` | `BlockPaintingFrameSimple.name` | `enablePainting` | `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` |
| `TileEntityPaintPress` | `BlockPaintingPress.name` | `enablePainting` | `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` |
| `TileEntityArmorStand` | `BlockArmorStand.name` | `enableArmorstand` | `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` |
| `TileEntityTypeMachine` | `BlockTypesettingTable.name` | `enablePrintpressTypeMachine` | `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` |
| `TileEntityPrintPress` | `BlockPrintingPress.name` | `enablePrintpressTypeMachine` | `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` |
| `TileEntityCookieJar` | `BlockCookieJar.name` | `enableCookieJar` | `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` |
| `TileEntityDinnerPlate` | `BlockDinnerPlate.name` | `enableDinnerPlate` | `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` |
| `TileEntityDiscRack` | `BlockDiscRack.name` | `enableDiscRack` | `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` |
| `TileEntitySwordPedestal` | `BlockSwordPedestal.name` | `enableSwordPedestal` | `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` |
| `TileEntityBell` | `BlockBell.name` | `enableDeskBell` | `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` |
| `TileEntityTypewriter` | `BlockTypeWriter.name` | `enableTypewriter` | `old-1.12.2/src/main/java/jds/bibliocraft/CommonProxy.java` |

## GUI / Container Topology
### Explicit GUI IDs
- `100` -> Atlas (`ItemAtlas`) container/gui
- `101` -> Slotted Book (`ItemSlottedBook`) container/gui
- `102` -> Name Tester GUI

### Tile-based GUI routing (`GuiLoader`)
- Bookcase, Generic Shelf, Armor Stand, Potion Shelf, Tool Rack, Weapon Case, Label, Writing Desk, Table, Cookie Jar, Dinner Plate, Disc Rack, Fancy Sign, Fancy Workbench, Paint Press, Painting, Furniture Paneler, Framed Chest.

Source: `old-1.12.2/src/main/java/jds/bibliocraft/GuiLoader.java`

## Networking
- Channel: `SimpleNetworkWrapper` with channel name `bibliocraft`
- Source: `old-1.12.2/src/main/java/jds/bibliocraft/network/BiblioNetworking.java`

### Serverbound Packets (23)
- `BiblioAtlasWPT`
- `BiblioType`
- `BiblioTypeFlag`
- `BiblioTypeDelete`
- `BiblioTypeUpdate`
- `BiblioMCBEdit`
- `BiblioMCBPage`
- `BiblioPaneler`
- `BiblioRecipeCraft`
- `BiblioStockTitle`
- `BiblioStockCompass`
- `BiblioClipboard`
- `BiblioUpdateInv`
- `BiblioAtlas`
- `BiblioMeasure`
- `BiblioMapPin`
- `BiblioRBook`
- `BiblioRBookLoad`
- `BiblioSign`
- `BiblioClock`
- `BiblioPaintPress`
- `BiblioPainting`
- `BiblioPaintingC`

### Clientbound Packets (10)
- `BiblioDrillText`
- `BiblioAtlasClient`
- `BiblioPanelerClient`
- `BiblioRecipeText`
- `BiblioStockLog`
- `BiblioOpenBook`
- `BiblioDeskOpenGui`
- `BiblioAtlasSWPClient`
- `BiblioAtlasTGUI`
- `BiblioSoundPlayer`

### Legacy FMLEventChannel Fields
There are 30+ legacy `ch_Biblio*` channel fields still declared in `BiblioCraft.java`; current packet registration uses `SimpleNetworkWrapper` (`BiblioNetworking`) instead.

## Sound Registry Surface
- Sound definitions file: `old-1.12.2/src/main/resources/assets/bibliocraft/sounds.json`
- Sound files: 38 `.ogg` files in `old-1.12.2/src/main/resources/assets/bibliocraft/sounds`
- Key sound event names in `sounds.json`:
- `cclose`, `copen`, `tapeopen`, `tapeclose`, `screw`, `ding`, `drill`, `tick`, `tock`, `wind`, `chime`, `woundchime`, `addpaper`, `endbell`, `removebook`, `typing`, `typingsingle`

Note: explicit `RegistryEvent.Register<SoundEvent>` path was not found in legacy code; many sounds are instantiated directly in `CommonProxy`.

## Recipes and Data Assets
- JSON files total in old resources: 375
- Recipe JSON files: 344 (`old-1.12.2/src/main/resources/assets/bibliocraft/recipes`)
- Recipe pattern highlights:
- 182 recipe files use wood-prefix variants (`oak`, `spruce`, `birch`, `jungle`, `acacia`, `darkoak`, `framed`)
- 96 recipe files are dyed variants (`*_dyed_*`)
- 132 recipe files start with color prefixes (`white`, `orange`, etc.)
- Extra code-based recipes in `ItemLoader.addRecipies`:
- `bibliocraft:enchantedatlas`
- `bibliocraft:enchantedatlasalt`

## Resource Layout Snapshot
- Root: `old-1.12.2/src/main/resources/assets/bibliocraft`
- `models/item`: 40 files
- `models/block`: 57 files
- `recipes`: 344 files
- `sounds`: 38 files
- `lang`: 30 files
- `textures/models`: 104 files
- `textures/items`: 20 files
- `textures/gui`: 34 files
- `textures/paintings`: 46 files
- `textures/armor`: 3 files

## Add-on Mods in Old Tree (`jds/bibliowoods`)
### `BiblioWoodsBoP`
- File: `old-1.12.2/src/main/java/jds/bibliowoods/bop/BiblioWoodsBoP.java`
- Mod ID: `bibliowoodsbop`
- Version: `2.0`
- Integrates 16 BoP wood textures through `RegisterCustomFramedBlocks`

### `BiblioWoodsForestry`
- File: `old-1.12.2/src/main/java/jds/bibliowoods/forestry/BiblioWoodsForestry.java`
- Mod ID: `bibliowoodsforestry`
- Version: `2.0`
- Integrates 29 Forestry wood textures through `RegisterCustomFramedBlocks`

### `BiblioWoodsNatura`
- File: `old-1.12.2/src/main/java/jds/bibliowoods/natura/BiblioWoodsNatura.java`
- Mod ID: `bibliowoodsnatura`
- Version: `2.0`
- Integrates 13 Natura wood textures through `RegisterCustomFramedBlocks`

### `BiblioWoodsBotania`
- File: `old-1.12.2/src/main/java/jds/bibliowoods/botania/BiblioWoodsBotania.java`
- Mod ID: `bibliowoodsbotania`
- Version: `2.0`
- Contains probing/test logic; does not register framed block content like other add-ons.

## Fluids
- No fluid registration path found.
- No `FluidRegistry` usage found.
- No fluid block/item classes found in scanned legacy sources.

## Legacy / Unused / Non-registered Classes
- `ItemCompendium` (empty class, not registered): `old-1.12.2/src/main/java/jds/bibliocraft/items/ItemCompendium.java`
- `BlockPainting` (abstract base, not directly registered): `old-1.12.2/src/main/java/jds/bibliocraft/blocks/BlockPainting.java`
- `BiblioWoodBlockItem` utility base (not directly registered): `old-1.12.2/src/main/java/jds/bibliocraft/blocks/blockitems/BiblioWoodBlockItem.java`
- `BlockItemPainting` utility subclass (not directly registered): `old-1.12.2/src/main/java/jds/bibliocraft/blocks/blockitems/BlockItemPainting.java`
- `SoundLoader` contains fully commented legacy logic: `old-1.12.2/src/main/java/jds/bibliocraft/SoundLoader.java`

## Package Indexes (Where Files Are)

### Blocks Package
- Path: `old-1.12.2/src/main/java/jds/bibliocraft/blocks`
- Files:
- `BiblioBlock`, `BiblioColorBlock`, `BiblioLightBlock`, `BiblioSimpleBlock`, `BiblioWoodBlock`
- `BlockArmorStand`, `BlockBell`, `BlockBookcase`, `BlockBookcaseCreative`, `BlockCase`, `BlockClipboard`, `BlockClock`, `BlockCookieJar`, `BlockDesk`, `BlockDinnerPlate`, `BlockDiscRack`, `BlockFancySign`, `BlockFancyWorkbench`, `BlockFramedChest`, `BlockFurniturePaneler`, `BlockLabel`, `BlockLampGold`, `BlockLampIron`, `BlockLanternGold`, `BlockLanternIron`, `BlockMapFrame`, `BlockMarkerPole`, `BlockPainting`, `BlockPaintingFrameBorderless`, `BlockPaintingFrameFancy`, `BlockPaintingFrameFlat`, `BlockPaintingFrameMiddle`, `BlockPaintingFrameSimple`, `BlockPaintingPress`, `BlockPotionShelf`, `BlockPrintingPress`, `BlockSeat`, `BlockShelf`, `BlockSwordPedestal`, `BlockTable`, `BlockToolRack`, `BlockTypeWriter`, `BlockTypesettingTable`

### BlockItems Package
- Path: `old-1.12.2/src/main/java/jds/bibliocraft/blocks/blockitems`
- Files:
- `BiblioWoodBlockItem`, `BlockItemArmorStand`, `BlockItemBookcase`, `BlockItemBookcaseCreative`, `BlockItemCase`, `BlockItemClock`, `BlockItemDesk`, `BlockItemFancySign`, `BlockItemFancyWorkbench`, `BlockItemFramedChest`, `BlockItemFurniturePaneler`, `BlockItemLabel`, `BlockItemLamp`, `BlockItemLantern`, `BlockItemMapFrame`, `BlockItemPainting`, `BlockItemPaintingFrameBorderless`, `BlockItemPaintingFrameFancy`, `BlockItemPaintingFrameFlat`, `BlockItemPaintingFrameMiddle`, `BlockItemPaintingFrameSimple`, `BlockItemPotionShelf`, `BlockItemSeat`, `BlockItemShelf`, `BlockItemSwordPedestal`, `BlockItemTable`, `BlockItemToolRack`, `BlockItemTypewriter`

### Items Package
- Path: `old-1.12.2/src/main/java/jds/bibliocraft/items`
- Files:
- `ItemAtlas`, `ItemAtlasPlate`, `ItemBigBook`, `ItemChase`, `ItemClipboard`, `ItemCompendium`, `ItemDeathCompass`, `ItemDrill`, `ItemEnchantedPlate`, `ItemFramingBoard`, `ItemFramingSaw`, `ItemFramingSheet`, `ItemHandDrill`, `ItemLock`, `ItemMapTool`, `ItemNameTester`, `ItemPaintingCanvas`, `ItemPlate`, `ItemPlumbLine`, `ItemReadingGlasses`, `ItemRecipeBook`, `ItemRedstoneBook`, `ItemSeatBack`, `ItemSeatBack2`, `ItemSeatBack3`, `ItemSeatBack4`, `ItemSeatBack5`, `ItemSlottedBook`, `ItemStockroomCatalog`, `ItemTape`, `ItemTapeMeasure`, `ItemWaypointCompass`

### TileEntities Package
- Path: `old-1.12.2/src/main/java/jds/bibliocraft/tileentities`
- Files:
- `BiblioLightTileEntity`, `BiblioTileEntity`, `TileEntityArmorStand`, `TileEntityBell`, `TileEntityBookcase`, `TileEntityCase`, `TileEntityClipboard`, `TileEntityClock`, `TileEntityCookieJar`, `TileEntityDesk`, `TileEntityDinnerPlate`, `TileEntityDiscRack`, `TileEntityFancySign`, `TileEntityFancyWorkbench`, `TileEntityFramedChest`, `TileEntityFurniturePaneler`, `TileEntityLabel`, `TileEntityMapFrame`, `TileEntityMarkerPole`, `TileEntityPaintPress`, `TileEntityPainting`, `TileEntityPaintingBorderless`, `TileEntityPaintingFancy`, `TileEntityPaintingFlat`, `TileEntityPaintingMiddle`, `TileEntityPaintingSimple`, `TileEntityPotionShelf`, `TileEntityPrintPress`, `TileEntitySeat`, `TileEntityShelf`, `TileEntitySwordPedestal`, `TileEntityTable`, `TileEntityToolRack`, `TileEntityTypeMachine`, `TileEntityTypewriter`

### Containers Package
- Path: `old-1.12.2/src/main/java/jds/bibliocraft/containers`
- Files:
- `ContainerArmor`, `ContainerAtlas`, `ContainerBookcase`, `ContainerClipboard`, `ContainerCookieJar`, `ContainerDinnerPlate`, `ContainerDiscRack`, `ContainerFancySign`, `ContainerFancyWorkbench`, `ContainerFramedChest`, `ContainerFurniturePaneler`, `ContainerGenericShelf`, `ContainerLabel`, `ContainerNameTester`, `ContainerPaintPress`, `ContainerPainting`, `ContainerPotionShelf`, `ContainerPrintPress`, `ContainerSlottedBook`, `ContainerTable`, `ContainerTypeMachine`, `ContainerTypewriter`, `ContainerWeaponCase`, `ContainerWeaponRack`, `ContainerWritingDesk`

### GUI Package
- Path: `old-1.12.2/src/main/java/jds/bibliocraft/gui`
- Files: 44 GUI classes including all major block/item screens (`GuiBookcase`, `GuiArmorStand`, `GuiFancyWorkbench`, `GuiPainting`, `GuiTypesetting`, `GuiStockCatalog`, etc.)

### Network Package
- Path: `old-1.12.2/src/main/java/jds/bibliocraft/network`
- Files:
- `BiblioNetworking`
- Packet subpackages:
- `old-1.12.2/src/main/java/jds/bibliocraft/network/packet/server` (23 files)
- `old-1.12.2/src/main/java/jds/bibliocraft/network/packet/client` (10 files)

### Events Package
- Path: `old-1.12.2/src/main/java/jds/bibliocraft/events`
- Files:
- `BakeEventHandler`, `EventBlockMarkerHighlight`, `EventDeathDrop`, `EventItemToss`, `EventSpawn`, `GuiBiblioOverlay`, `RenderAtlasFace`, `RenderClipboardText`, `TextureStichHandler`

### Rendering Package
- Path: `old-1.12.2/src/main/java/jds/bibliocraft/rendering`
- Files: 22 TESR renderer classes

### Models Package
- Path: `old-1.12.2/src/main/java/jds/bibliocraft/models`
- Files: 44 model wrapper classes (`BiblioModelWood`, `BiblioModelColor`, `BiblioModelSimple`, plus per-block/item model classes)

### Helpers Package
- Path: `old-1.12.2/src/main/java/jds/bibliocraft/helpers`
- Files:
- `BiblioEnums`, `BiblioRenderHelper`, `BiblioSortingHelper`, `BiblioWoodHelperTab`, `CustomBlockItemDataPack`, `EnumColor`, `EnumCustomDataType`, `EnumMetalType`, `EnumPaintingFrame`, `EnumRelativeLocation`, `EnumShiftPosition`, `EnumVertPosition`, `FileUtil`, `InventoryListItem`, `InventorySet`, `ModelCache`, `ModelCachePackage`, `PaintingUtil`, `RecipeBiblioAtlas`, `RecipeBiblioFramedWood`, `RecipeShapelessFramedWood`, `RegisterCustomFramedBlocks`, `SeatHelper`, `SortedListItem`, `WoodRegistryEntry`

### Slots Package
- Path: `old-1.12.2/src/main/java/jds/bibliocraft/slots`
- Files: 37 custom slot classes (`SlotAtlas`, `SlotBook`, `SlotWeaponCase`, `SlotPrintPlate`, etc.)

### Story Generation Package
- Path: `old-1.12.2/src/main/java/jds/bibliocraft/storygen`
- Files:
- `BookGenUtil`, `BookGenWordLists`, `BookStructure1`, `WordsStructure1Chicken`, `WordsStructure1Cow`, `WordsStructure1Creeper`, `WordsStructure1Enderman`, `WordsStructure1Ocelot`, `WordsStructure1Pig`, `WordsStructure1Sheep`, `WordsStructure1Standard`, `WordsStructure1Wolf`, `WordsStructure1Zombie`

### State + Statemapper Packages
- Paths:
- `old-1.12.2/src/main/java/jds/bibliocraft/states`
- `old-1.12.2/src/main/java/jds/bibliocraft/statemappers`
- State files: `TextureState`, `TextureProperty`, `MetalTypeState`, `MetalTypeProperty`, `PanelState`, `PanelProperty`
- Statemapper files: `BiblioBlockStateMapper`, `ClipboardStateMapper`, `LightingStateMapper`, `MarkerPoleStateMapper`

### Enchantments Package
- Path: `old-1.12.2/src/main/java/jds/bibliocraft/enchantments`
- Files: `EnchantmentDeathCompass`, `EnchantmentReading`

### Entity Package
- Path: `old-1.12.2/src/main/java/jds/bibliocraft/entity`
- Files: `AbtractSteve`, `EntityCandleFX`, `EntityCatalogFX`, `EntitySeat`, `EntitySeatRenderer`, `ModelDummy`

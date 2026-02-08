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
- Block model loader strategy:
- All 37 block model JSON files now use Forge OBJ loader with `flip_v: true`.
- Path pattern:
- `src/main/resources/assets/bibliocraft/models/block/<block_id>.json`
- -> `bibliocraft:models/block/<legacy_obj_name>.obj`
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

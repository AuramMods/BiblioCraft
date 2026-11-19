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

## Bookcase Port

- `bookcase` and `bookcase_creative` keep both block and item IDs and share the `bibliocraft:bookcase` block entity and menu. The block entity stores inventory in `Items` and reuses `FurnitureMaterial` from the shelf.
- The shared OBJ contains `book1` through `book16`. The MTL records the zero-based slot index for each group with `forge_TintIndex`. The world model filters quads using the books sprite and reads the sixteen occupied slots from Forge model data.
- Empty placeholder bookcases already in 1.20.1 saves load as oak unless they specify a material. Existing creative bookcases show their real contents. Their item model keeps the full legacy display, and normal placement generates authored stories.
- Bookcase crafting uses the original three-row plank and slab pattern for each wood. A framed bookcase uses framing sheets and boards and can be paneled in the furniture paneler.

## Potion Shelf and Tool Rack Port

- `potion_shelf` and `tool_rack` retain their block, item, block entity, and menu IDs. Both store their actual stacks under `Items` and share the paneler material pipeline.
- The potion shelf's OBJ sits in the east half and the tool rack's sits in the west half in the base orientation. Click targets and fixed item render poses follow the authored rotations. Both potion shelf faces access the same four-column, three-row inventory.
- The menus use the legacy slot coordinates and textures. Third-party packs can extend the built-in rules and configured item-name matching through the `bibliocraft:potion_shelf_contents` and `bibliocraft:tool_rack_contents` item tags.
- Both recipes preserve their legacy patterns and produce material-tagged furniture from matching wood or framing parts. Loot copies the material and ejects contents as separate stacks.

## Display Case Port

- The block, item, block entity, and menu retain `bibliocraft:case`. The `face` and `facing` block states keep their existing meanings. Cases from earlier 1.20.1 saves default to `open=false`.
- Open and closed case models select the corresponding wood, latch, and glass groups in the same legacy OBJ. The floor model and its rendered exhibit use the same authored coordinate transform. Glass uses the cutout layer on both mounts.
- Slot 0 stores the exhibit and slot 1 stores the carpet. The lining selects the matching wool sprite independently of the wood or custom panel. Replacing a carpet returns the old carpet as an item. Breakage drops both stacks separately and preserves the case's furniture material.
- The legacy menu exposes the exhibit slot and closes when the case lid closes. A stored redstone block powers all sides whether the lid is open or closed.

## Cookie Jar Port

- The block, item, block entity, and menu retain `bibliocraft:cookie_jar`. Eight slots accept any item, and vanilla NBT persists actual contents.
- The glass uses cutout rendering. The eight legacy cookie OBJ meshes are linked to their original order, and the number of occupied slots determines how many appear. Bookcases share the same visibility filter and continue to show the exact occupied slots.
- Opening the menu emits the legacy 20-tick redstone pulse on all sides. The block state and scheduled tick preserve the pulse across reload. The block entity stores the deadline so a replacement jar receives its own full pulse. The menu remains usable after the pulse.

## Dinner Plate and Disc Rack Port

- `dinner_plate` and `disc_rack` retain their block, item, block entity, and menu IDs. Both use vanilla inventory NBT. The dinner plate stores three stacks of food with their tags intact. The disc rack stores nine stacks and accepts vanilla records, dinner plates, configured keywords, and the `bibliocraft:disc_rack_contents` item tag.
- The dinner plate shows one large central pile or up to three smaller piles, with heights based on stack size. A hungry player's fourth bite takes one item from each occupied food slot while hunger remains and uses vanilla finishing behavior to apply effects and return bowls.
- The disc rack's `mount` state selects floor, upright wall, horizontal wall, or ceiling geometry. A rack saved by the earlier port has its original facing and defaults to floor geometry. The OBJ mounts, collision bounds, insertion and removal, and item poses agree on the nine grooves.
- The hand drill and powered drill retain their separate IDs and sounds. Sneak-use changes the facing on both blocks. Normal use on a wall rack switches between upright and horizontal layouts.

## Furniture Lock Port

- The legacy Lock and Key retains the `bibliocraft:biblio_creative_lock` item ID. It appears in creative by default. The original optional recipe uses two gold nuggets and three gold ingots in the `NI`, `NI`, ` I` pattern. Its recipe was recovered from `ItemLoader.addRecipies` in the official v1.11.7 JAR because the bundled 1.12.2 source kept the option without the recipe. [Original recipe option](https://www.bibliocraftmod.com/wiki/lock-and-key/), [BiblioCraft v1.11.7](https://www.curseforge.com/minecraft/mc-mods/bibliocraft/files/2423369).
- Ported furniture stores the owner UUID as `LockOwner` in the block entity. Existing 1.20.1 furniture without this field remains unlocked. Legacy 1.12.2 stored a display-name string. Ordinary pick-block and loot preserve furniture materials.
- Both the direct interaction and server menu validation check the current owner. Server drill actions honor the same rule. Locked furniture retains the legacy survival mining protection and creative break behavior.
- Vanilla hoppers and Forge item handlers resume their normal transfers after unlocking. Cached handlers check the current lock on each transfer. A disabled Lock and Key setting prevents creating new locks and lets existing owners remove their locks.

## Sword Pedestal Port

- The block, item, and inventory retain `bibliocraft:sword_pedestal`. Existing 1.20.1 worlds keep their `facing` and `color`. A pedestal without `Items` loads with an empty display slot.
- The original `swordpedestal.obj` has a stone body and a separate wool trim material. One MTL resolves the trim from the child model's `trim` sprite. Placed blocks and inventory models inherit the same OBJ. The block entity renderer uses the legacy center, rotation, and scale for the displayed blade.
- Sixteen shaped recipes carry the wool color through `BlockStateTag.color`. The dye recipe carries the new trim and preserves the item's existing tags. In-world dyeing, pick-block, and ordinary loot preserve the trim. Breaking the pedestal returns the stored sword separately.
- A displayed sword powers all sides, and updates reach neighbors through solid blocks. The same slot accepts swords from players, vanilla hoppers, and Forge item handlers. The shared furniture lock protects user interactions and automation.

## Desk Bell Port

- The block, item, and block entity retain `bibliocraft:bell`. The vanilla sound event `bibliocraft:ding` uses the four original bell recordings. A manual press or a redstone increase broadcasts a position-based sound in the blocks category.
- The server samples the analog strength every three ticks. `LastPower` persists the prior sample across chunk reloads. Earlier placeholder saves start from zero. Lock ownership affects manual use and survival mining while leaving redstone behavior available.
- The original OBJ and texture render in both placements and inventory. The outline reaches the rim of the authored mesh, and the inventory transform centers the miniature bell.

## Mounted Light Port

- `lamp_gold`, `lamp_iron`, `lantern_gold`, and `lantern_iron` keep their block and item IDs and share `bibliocraft:bibliolight`. Existing `face`, `facing`, and `color` states keep the same names. The block ID selects the metal.
- One MTL per OBJ resolves the inherited `body` and `shade` textures. Mounts select the original lamp arms and plates or lantern hangers and plates. The model keeps the original faces, UVs, and textures with the earlier port's block-local coordinate translation. Cutout rendering preserves the original glass openings.
- The sprite indices follow `ModelLamp` and `ModelLantern` in 1.12.2. For example, legacy `lamplight4` is red and `lamplight14` is pink. Modern dye IDs select the item override only. The named color persists as `BlockStateTag.color`.
- The four recolor recipe IDs retain their inputs and now return the matching colored light. The hand drill and powered drill cycle floor, wall, and ceiling. Sneak-use rotates the facing. The shared Lock and Key protects both actions and in-world dyeing. Lanterns use the modern small-flame particle at the legacy wick position.

## Typewriter Port

- `bibliocraft:typewriter` retains its block, item, and block entity IDs and its `facing` and `color` states. The block renders the original body OBJ, and its renderer moves the original carriage and paper together. The sixteen progress textures select the legacy seven paper meshes as the sheet advances. Shell textures follow the old sprite table, independently of modern dye IDs.
- An existing north-facing writer keeps its geometry and takes input at its west-facing keyboard. Newly placed writers point their keyboard toward the player. The top of the visible keyboard also takes manual input. The original blank page is visible on the inventory model.
- Saved `Items` slots 0 and 1 store up to 64 paper and one finished book. The legacy `bookWriteCount` field and modern `Author` and `Voice` fields preserve progress. Placeholder block entities without these fields start empty. A writing session uses eight sheets and creates a valid eight-page book. Named mobs can type from the adjacent cell in front and use their original species vocabulary.
- All sixteen crafting recipes use matching terracotta and ink sac. The white recipe also accepts the white dye from the earlier 1.20.1 port. The same `typewriter_recolor` recipe ID changes `BlockStateTag.color`, which also carries through ordinary pick-block and loot. The finished book and excess paper are separate drops.
- The original paper, typing, completion, and removal sounds use positional sound events. Shared locks apply to clicks, drills, and automation. The typewriter offers finished books to the desk directly below on its automatic tick.

## Writing Desk Port

- The block, item, block entity, and inventory menu retain `bibliocraft:desk`. `Items` holds the open book or map in slot 0, the stacked books in slots 1–8, and the carpet in slot 9. `Page` remembers the open book's current page. Earlier placeholder saves default to a single oak desk with empty slots.
- The original OBJ groups select the single, left, center, and right desk sections. All 24 group names, 484 vertices, 930 texture coordinates, 152 normals, and 672 faces match the 1.12.2 OBJ. Distinct material assignments let Forge filter the nine book meshes. Model data shows the proper number of books, the open book, and the carpet color. The leg, shelf, and writing surface bounds follow each section. The vanilla map renderer displays saved map pixels on the writing surface and receives periodic map updates through the configured map update rate.
- Both drills use two sneak-clicks to join or separate aligned, adjacent desks. Selection belongs to the player and drill stack. Breaking a desk clears adjoining links. The desk's existing saved facing still selects the same authored geometry.
- The original nine-slot desk inventory screen opens through the ordinary menu. A separate book menu edits or signs a writable book in place, or reads a written book and remembers the page. The server ties edits to the live menu and the same book in the same desk. The client uses the book texture and confines typing to the page editor.
- The typewriter fills the desk's left stack, then the right. A full or locked desk leaves the output in the writer. The original shaped pattern crafts all twelve furniture materials. Pick-block and loot preserve the saved material, and the desk returns its books and carpet separately.

## Armor Stand Port

- The block, item, block entity, and menu keep `bibliocraft:armor_stand`. The lower half owns `Items` in legacy head, chest, legs, and feet order. The synchronized upper half resolves direct use, locks, rotations, pick-block, and removal to the same lower inventory.
- The original `armorstand.obj` stays byte-identical. The lower block renders both `bottomStand` and `topStand` with the saved wood or framed texture, as in the legacy mapper. The upper block supplies collision and hit targets. The block entity renders the four worn stacks with vanilla humanoid armor and head layers at the legacy pose.
- A matching piece goes directly into the clicked quarter. Sneak-use swaps that quarter with the player's equipment. Power at either half enables the full swap. The original menu shows the stand and worn equipment side by side, with vanilla slot filters for armor, elytra, carved pumpkins, and mob heads.
- New placement takes two clear cells. A pre-existing single-block 1.20.1 placeholder keeps its facing and becomes a lower half. An upper half is added when the cell above is clear. Other blocks in old worlds are preserved.
- Both drills rotate the two halves together. The stand uses the legacy wooden block sounds and mines at axe speed. Survival removal of either half drops one stand with its saved material and returns its armor separately. The original stick and slab recipe is available for eleven woods and the framed variant.

## Table Port

- The block, item, block entity, and menu retain `bibliocraft:table` and its existing facing. Four directional blockstate properties track all neighboring tables at the same height, including different materials. Old tables recompute connections when their chunks load. Adding or removing a neighbor refreshes the adjoining edge.
- The model retains all 30 original OBJ groups, 1,389 texture coordinates, and 487 faces. All 612 vertices keep the earlier port's one-block X and Z translation into modern block coordinates. Material indexes select the center pedestal, outer corner legs, square or beveled quarters, and cloth along exposed sides. The selection shape follows the surviving supports.
- The lower floor carpet and tabletop cloth each apply an independent wool texture from one carpet item. A regular top click adds or returns a display stack or adds a cloth. A side click with carpet adds the floor carpet. Sneak-clicking the top opens the original one-slot screen.
- Both drills remove the cloth from the top and remove the floor carpet on sneak-use. Side clicks with either drill or a tool in `bibliocraft:table_rotation_tools` rotate the display in 90-degree steps around the clicked axes. The tag includes the tape measure and accepts optional Forge wrench, screwdriver, and crowbar tags.
- The display, cloth, and carpet occupy `Items` slots 0–2. The display angles retain the legacy `slotxangle` and `slotyangle` names. A vanilla filled map keeps its ID and displays its live terrain on the tabletop. Material, lock ownership, and contents use the shared furniture storage. Breaking returns a table with its material and drops the display and fabrics separately.

## Seating Port

- The block, item, and block entity retain `bibliocraft:seat`. `bibliocraft:seat_entity` remains the invisible riding anchor and saves its position as `SeatPos`. Existing seat states default to separated and unoccupied. The server reconciles occupancy with the real rider on load.
- All 31 original mesh groups, 866 UV coordinates, and 382 faces match 1.12.2. The 534 vertices retain the earlier port's one-block X and Z translation. The baked model picks legs and braces, bench extensions, the five kinds of back, and the original connected back cloth. The saved facing drives both rotations and the seat shape.
- The `Items` slots retain the legacy order of cushion, back, and floor carpet. Right-click from above or below to apply a cushion and from the sides to place floor carpet. Install a separate seat back from any side. Both fabrics retain their colors. The installed back selects the wood for the back, legs, and braces. A framed base renders the custom panel saved with a framed back.
- Two sneak-clicks on adjacent seat tops with either drill join or separate the sides. A sneak-click on the side rotates the seat without moving its joins. An ordinary drill click returns the cushion first from above, then the back. The sides also let the player remove the floor carpet. Breaking returns the original attachments and the seat material separately.
- An ordinary click seats one player. A nearby named mob can board an empty seat every five seconds and can be released with a sneak-click. Occupied seats provide full power with the chair redstone option enabled, including power through a solid neighbor. Safely dismounting or removing the seat clears the output.
- The old slab recipes craft the seat and all five seat backs in twelve materials. Upgrading a style-two back uses the material of the added slabs as in 1.12.2. The existing furniture paneler accepts any framed back.

## Label and Fancy Sign Port

- The block, item, block entity, and menu retain the Forge 1.20.1 IDs `bibliocraft:label` and `bibliocraft:fancy_sign`. The legacy sign item used `bibliocraft:fancysign`. Existing port facings and sign mounts are retained. Existing signs without a depth property load at the wall edge with `shift=0`. Existing placeholders default to oak and an empty inventory.
- The label retains 32 vertices, 46 UV coordinates, and 22 faces. The sign retains 160 vertices, 315 UV coordinates, and 135 faces across its front, wooden border, and two mount groups. Both OBJ files rotate 180 degrees around the original Y axis to align their faces with the saved block orientation. The hitboxes, world text, and icons use the same orientation. The two additional board positions are 0.43 and 0.93 blocks inward from the wall edge. Item model transforms center both meshes and present the original faces in inventory and fixed displays.
- A label holds full stacks in three slots. The center icon expands if the side slots are empty. The sign holds one item in each of two icon slots. Both screens open on sneak-use and respect furniture locks. Either drill cycles the sign depth with ordinary use or rotates the sign with sneak-use.
- The sign stores up to fifteen lines with six text sizes, legacy formatting codes, and two icon layouts with nine sizes, three block orientations, and independent positions. It trims text to the board at render time. The server accepts edits only through a current in-range sign menu and merges fields changed by the player. Sign editing introduced wire protocol version 2.
- The modern block entities use `Items` and `FurnitureMaterial`. Fancy signs store text and icon layout in `SignLayout`. Survival harvest of a sign packs those values in `BlockEntityTag` so the sign can be placed again with its contents. A label drops its items separately. The old `Inventory`, `textscale`, `text0` through `text14`, and `s1*` and `s2*` tags remain part of the separate 1.12.2 migration problem.
- Six identical slabs or framing boards craft a label. A label, a vanilla sign, and paper make a fancy sign in the label's wood or custom framed material. Both work with the existing furniture paneler. Normal pick-block returns the material variant.

## Clock Port

- The block, item, block entity, and editor retain `bibliocraft:clock`. Existing port states keep their saved facing and default to an oak wall clock at the wall-edge position. The `part` and `shift` properties remember the case and depth. Two sneak-clicks with either drill join or separate aligned clocks directly above one another. A regular drill click cycles through all three positions on both halves.
- The original OBJ remains byte-identical to the bundled source, retaining all ten groups, 606 vertices, 667 UVs, and 814 faces. The wall case translates along the authored X axis. The grandfather case retains the uncut two-block geometry and renders from the upper block entity across both cells. Both cases use the original face, hour and minute hands, and appropriate pendulum. The inventory uses the original static hands and pendulum group.
- The upper clock controls a grandfather clock's redstone output and editor. Both halves emit direct and indirect power. Breaking either half makes the other a wall clock again. The upper clock's material supplies the joined case and each component keeps its own material and settings.
- The original dial schedules chimes and redstone separately in half-hour intervals. The two rings align with the in-game times printed on the clock. Presets select every 30 minutes, hour, two hours, or six hours, or clear a ring. Redstone supports 40-tick pulses and sustained power for the whole selected interval. The tick, tock, wound chime, and wind use their original sound events.
- Modern schedules are two 48-bit masks under `ClockSettings`. Activation times are saved separately to keep an in-flight pulse or avoid replaying the current chime after reload. For migration, legacy arrays start at 18:00 in their editor and modern index zero is midnight. Clock editing introduced wire protocol version 3. Lock and range checks apply to schedule edits and the server merges separately edited slots.
- The twelve recipes retain the original slab, clock, stick, and gold pattern. Normal pick-block and harvest preserve the individual clock's schedule and frame, and placing either item restores them. The joined case always uses two real blocks and survival removal of one returns one clock.

## Framed Chest Port

- The block, item, block entity, and menu retain `bibliocraft:framed_chest`. The legacy block and item used the identifier `bibliocraft:framedchest`. Existing port chest states keep their saved facing and default to the single model, an empty inventory, and oak.
- All nine groups, 504 vertices, 1,330 UVs, and 916 faces are byte-identical to the bundled OBJ. The Forge blockstate chooses the body, and the renderer moves the original lid and latch about their authored hinge. The large chest uses one latch at the seam. Each half's own material applies to its body and lid. The item continues to use the authored static lid and latch.
- Two sneak-uses with either drill join or separate horizontally adjacent chests facing the same direction. An ordinary drill use rotates the clicked chest and separates it from any partner. Placement and selection follow the same authored axis. Harvest returns one chest item with its material and drops only that half's contents.
- Each chest stores 27 slots under `Items`. A joined menu shows the left inventory first and the right inventory second. Both halves count active viewers and share lid animation until the final viewer leaves, with one vanilla chest sound at the seam. Locking either half revokes shared menu access. The individual sided inventories still track their own locks.
- The front label is a representative item from the largest total of stacks with matching item and tags. It is derived after inventory mutations and world loading. Chunk updates send the rendered item, material, lock data, and transient lid state. Menu synchronization handles the private slot contents. The `part` blockstate replaces legacy `isDouble` and `isLeft`. The derived icon replaces `labelStack`. The chest menu uses wire protocol version 4.
- The legacy frame recipe requires matching planks and a wood label. The framed variant uses framing sheets and inherits a custom panel from an already paneled label. Pick-block, the furniture paneler, and loot all preserve the frame material.

## Stockroom Catalog and Waypoint Compass Port

- The items retain `bibliocraft:stockroom_catalog` and `bibliocraft:compass`. Both use held-item menus opened and checked by the server. The stockroom catalog and compass keep their existing shaped recipes and their one-item stack limit from the legacy items.
- A catalog stores each tracked inventory by dimension and block position in `inventoryList`. The legacy positional tags without a dimension use the current dimension. Modern snapshots read the live names and contents from accessible block containers and unsided Forge item handlers. Both halves of a double chest are counted individually. Pending structure loot, unloaded chunks, and furniture locked by another player show as unavailable. The catalog title uses the item's vanilla custom name.
- Legacy mod-specific count readers used private save tags from JABBA barrels, MineFactory Reloaded and Quantime deep storage units, Thermal Expansion caches, and compacting drawers. The current catalog uses the counts exposed by the modern inventory interfaces. Verification of third-party storage totals belongs to the integration pass.
- The original catalog and detail textures support name or quantity sorting in either direction, 22 rows per summary page, and five inventories per item detail page. The server sends item summaries in bounded batches and validates compass assignments against the open menu and tracked inventory. The original catalog particle texture marks nearby registered storage for the player holding the book.
- The standalone waypoint compass saves `XCoord`, `ZCoord`, and `WaypointName` and adds `WaypointDimension`. Old coordinate tags remain readable. An unconfigured compass points to the world origin. A named target can be edited in the original compass screen or assigned from a catalog location. Assigned targets point within their recorded dimension.
- The compass OBJ matches the legacy file byte for byte with SHA-256 `a1e03b6f3bdcb69dd9e72a602eab20f4e89d1eeee29b33e8e5e82a5873123dbb`. A Forge geometry loader bakes the face and one of the 36 authored needle positions per frame. An item override selects the index using the holder's heading and saved target. The catalog and waypoint packets use wire protocol version 5. Atlas and death compass workflows remain separate.

## Fancy Workbench and Recipe Book Port

- The block, item, block entity, and menu keep `bibliocraft:fancy_workbench`. The item and new book menu use `bibliocraft:recipe_book`. The legacy identifiers are `bibliocraft:fancyworkbench` and `bibliocraft:recipebook`. Existing workbench states keep their facing and default to an empty oak bench.
- The OBJ retains all eleven authored groups. Each of its eight book meshes references a separate MTL entry with a tint index. Forge model data selects the visible books from the eight storage slots. World rotations and direct shelf clicks share the front defined by the mesh. The item displays the original empty bench, with a separate crafting top and side material preserved across all twelve wood and frame variants.
- `Items` stores one active recipe or plain book and eight shared book storage slots. Only the shelf's visibility mask and furniture state go to bystanders. The menu adds the inventories of accessible bookcases on each side and uses vanilla `ResultSlot` with a private 3×3 grid for each viewer. Closing the menu returns the remaining grid items through the normal crafting-container flow.
- Recipe books preserve the legacy `grid`, `result`, `text`, `edited`, and `signed` tags. `RecipeId` locates the server recipe. Older books with no ID resolve their exact input grid through the current recipe manager. Recorded samples preserve item tags and store one item per slot. The bench fills complete batches from matching carried items. Portable crafting validates the recipe and ingredients, consumes them once, and returns the current Forge crafting remainders. The crafting toggle applies to the held book.
- The old screen textures back the bench, bookcase panels and recipe book, including the notes, confirmation, and saved pattern. Signed books retain their recipe and notes. A book on the writing desk can be read, edited or signed in place. The recipe book menus and annotation packets use wire protocol version 6.

## Big Book Port

- The item keeps `bibliocraft:big_book` and its existing paper-around-writable-book recipe. The legacy item was `bibliocraft:bigbook`. The item sprite, parchment, navigation arrows, color palette, and chapter tabs use their original textures.
- Books retain `pagesTotal`, `pagesCurrent`, `pages.pageN`, `pages.pageScaleN`, `chapters.chapBools`, `chapters.chapPages`, `chapters.chaplineN`, `author`, and `signed`. A signed book also uses the vanilla JSON custom name and a `title` tag. Older raw signed names remain readable. The default line size comes from `default_big_book_text_scale`. Books support 256 pages, 44 lines per page, eight line sizes, and eight chapter links.
- The book menu anchors to the item in the specific hand or writing desk slot. The server sends one page at a time. Draft changes are merged against the pages and chapter labels the editor opened, then committed together. Signing uses the server account name. Readers can retain a bookmark after signing. The book menu and its packets use wire protocol version 7.
- Furniture packets omit Big Book pages and chapters while saved inventories and item drops retain them. The item uses a metadata-only inventory packet when its full tag approaches Minecraft's NBT quota. Opening that item still reads all its pages from the server. A client-side creative clone of an oversized item only has the metadata carried by its slot.

## Typesetting and Printing Port

- The blocks, items, block entities, and menus keep `bibliocraft:typesetting_table` and `bibliocraft:printing_press`. The chase and plates keep `bibliocraft:biblio_chase`, `bibliocraft:print_plate`, and `bibliocraft:enchanted_plate`. Their legacy IDs were `typesettingtable`, `printingpress`, `bibliochase`, `printplate`, and `enchantedplate`. Existing placed blocks keep their facing. The earlier empty block entities load with empty slots and no active print.
- The world library lives in the overworld's `data/bibliocraft_printing_library.dat`. Its entries store the original item stack, an opaque ID, title, author, and visibility. The library keeps the signed text and formatting of written books, Big Books, signed recipe books, and stockroom catalogs. The menu sends eight visible titles per page, with only the metadata the viewer may see. Typesetting packets use wire protocol 8. The historical 1.12.2 file-based library is part of world migration.
- The table keeps the selected entry and the last sampled signal. Crouch-use at the source slot saves a book, recycles an ordinary plate to a chase, or spends experience and an enchanted book to make an enchanted plate. The press keeps its inventory, committed plate, and print time on disk. Its inputs accept automation from above and the sides, and its output is available from below. Lock and Key access protects those transfers and player use. Standard plates refer to the world library and stay reusable. Discarding the entry invalidates its plates. Enchanted plates wear on successful copies. The plate recipes that created unconfigured placeholder items have been removed.
- The original OBJ bodies, stack layers, plates, and book variants use material indices and client model data for visibility. The press swaps the original nine ink sprites and animates the original arm and bed around the authored pivots. Both drills rotate the blocks. The table uses the original library artwork. The press provides both direct slots and a side-access inventory and progress screen.
- Atlas plates use the same workstation slots and press animation. The reading-glasses overlay is tracked with the shared glasses port.

## Painting Press and Frames Port

- The press, canvas, and five styles keep `bibliocraft:painting_press`, `bibliocraft:painting_canvas`, and `bibliocraft:painting_frame_*`. The existing frame menu remains `bibliocraft:painting`. The old IDs were `paintingpress`, `paintingcanvas`, and `paintingframeborderless`, `paintingframeflat`, `paintingframesimple`, `paintingframemiddle`, and `paintingframefancy`. Existing Forge 1.20.1 block facings are preserved. Earlier empty block entities load with empty canvas slots and default layouts.
- The press keeps the original base, optional canvas, and moving lid geometry with its original pivot. The press records the selected title and saves the artwork and position of an in-progress transfer. The single canvas can be inserted by hand or through the original gallery layout, where Transfer does the same job as the press handle. Blank canvases stack to 64 and painted canvases stack to one. Press and frame edits are validated against the active menu with wire protocol 9.
- The ten bundled titles retain their original aspect and resolution choices. The vanilla tab uses 1.20.1 painting variants and textures. Resource packs put custom PNGs in `assets/bibliocraft/textures/custompaintings/`. Each client resolves the same saved path from its loaded packs. Players missing an artwork see the original missing-painting image. The canvas retains `paintingType` and `paintingTitle`, plus `ArtworkWidth` and `ArtworkHeight` for the default layout. The frame editor accepts custom aspect dimensions of 1–32 blocks per side before applying the legacy 1–10× scale.
- The original frame OBJ has discrete edge and corner groups. The mutual in-plane links in `FrameLinks` select the original exterior profiles and inside corners. Borderless placement links adjacent co-planar frames. Either drill can connect or disconnect a pair with two sneak-uses, or rotate the artwork with a regular use. Frame collision follows the hidden setting while the outline stays selectable. The canvas and the frame drop separately. The frame's material survives pick-block, drops, paneling, and upgrading from a borderless frame.

## Map Frame and Map Tool Port

- The frame block, item, and block entity keep `bibliocraft:map_frame`, and the tool keeps `bibliocraft:maptool`. The legacy block declared `MapFrame`. The earlier frames retain their saved face and facing and default to an empty oak frame at zero map rotation. The same registry name now has a waypoint editor menu.
- The original OBJ supplies the frame body, four sides, four corners, and wool pin. Per-mount transforms align the original vertices with the selection shape and the map on floors, walls, and ceilings. The horizontal models specify a corner origin because the Forge OBJ baker adds the center offset. Coplanar neighbors suppress the shared edges. There are eleven wood variants and a framed family that preserves the panel through placement, pick-block, paneling, and drops.
- The frame's single slot keeps the filled map's vanilla `map` ID. Its `mapRotation` tag counts quarter turns. The filled map now owns up to 32 `BiblioMapPins` with stable IDs, unrotated map coordinates, names, and colors. Rotation moves their display positions while compass targets retain the map's world coordinates and dimension. Both drills and harvesting return the map with its pins intact.
- The map tool uses the active frame, held item, and filled map in the validated server menu. Wire protocol 10 sends and validates the editor changes. Atlas transfers can share the same filled-map pin data.

## Atlas and Atlas Waypoint Port

- The atlas book keeps `bibliocraft:atlas_book` and its `bibliocraft:atlas` menu. The 1.12.2 item declared `AtlasBook`. Earlier unfilled Forge 1.20.1 atlases start with empty slots, no selected sheet or compass, and manual selection at scale one. `Inventory` stores the six supply slots and 42 individual sheets. The tags `mapSlot`, `autoCenter`, `autoCreate`, `zoomLevel`, `lastGUImode`, and `savedCompass` retain the legacy field names. The vanilla `map` ID on each filled sheet supplies the dimension, scale, and center. The additional legacy `maps` cache is superseded by vanilla saved data and the `BiblioMapPins` on each filled sheet.
- Control-click or middle-click selects a sheet in the inventory. Map arrows show neighboring same-scale sheets. Auto mode searches by dimension and scale and creates aligned sheets on the Forge 1.20.1 vanilla map grid. The original book, compass and 36 needle groups are baked individually. The first-person cover reuses the original OBJ and texture, with live map pixels and colored pin meshes. Its sprite lives at `textures/item/atlas_cover.png` for the modern block atlas.
- Crouch-use with an atlas consumes one empty map when making a copy. The backed frame and the atlas preserve independent pin lists on their copies. Using the atlas on the currently selected matching map opens the original transfer panel. The active menu validates the held atlas, frame access, the exact live framed map stack, and the map ID. Waypoint edits and compass targeting validate their slot and map ID under wire protocol 11.

## Atlas Printing and Death Compass Port

- Atlas plates and death compasses keep `bibliocraft:atlas_plate` and `bibliocraft:death_compass`. Their legacy item names were `AtlasPlate` and `DeathCompass`. A crouch-use on a filled atlas in the typesetting table stamps one plate from one chase. The level charge uses the original 32 XP per map calculation. A two-map atlas costs five levels. Its filled sheets, selected map, and pins are saved under `AtlasTag`. Compasses, blank maps, and other supplies stay in the source. Indexes pointing to removed supplies are cleared from the copy.
- The original atlas plate sprite has its legacy glint. A press job uses a stamped plate, ink, and an empty atlas. A populated destination stays protected from overwrite. The existing `JobPlate` and `TicksLeft` fields persist progress, and `JobAtlas` holds the empty atlas's data for input comparison. Finishing consumes the plate once and returns the populated atlas. Stamping and tracked respawn restore the original acquisition paths for the two items. Old unstamped plates and unbound compasses keep their item IDs.
- `bibliocraft:deathcompassench` keeps its Forge 1.20.1 ID. It is a level-one atlas enchantment available from the enchanting table and both legacy upgrade layouts. The `bibliocraft:enchantedatlas` recipe is discoverable through the normal crafting type and preserves the atlas's data. Atlases with the earlier `bibliocraft:death_compass_enabled` marker convert it to the enchantment in a player's inventory.
- A tracked death records one pending reward in Forge's persisted player data. The player receives it on respawn or when next logging in alive. The compass keeps `XCoord`, `ZCoord`, `WaypointName`, and `WaypointDimension` and records the height as `DeathY`. The original distinct death-compass texture uses the shared OBJ with one of its 36 original needle poses. The needle tracks the death coordinates in the recorded dimension.

## Reading Glasses and Furniture Readouts

- The existing item ID `bibliocraft:biblio_glasses` replaces legacy `BiblioGlasses` metadata with an optional `GlassesVariant` tag. Plain glasses use the default value 0, tinted glasses use 1, and the monocle uses 2. Untagged Forge 1.20.1 stacks become wearable plain glasses. The original hand and inventory sprites and the three original armor textures follow the tag. Tinting preserves the original stack's custom name and enchantments.
- `bibliocraft:readingench` keeps its earlier port ID. The original one-level enchantment appears on books at an enchantment table and applies to head armor at an anvil. All three glasses grant the same reading ability when worn. The `reading_glasses` config controls the readout and enchantment availability.
- The readout uses the legacy position below the crosshair. Shelf slots use the shared interaction coordinates. Both armor-stand halves resolve to the lower block entity, and the typesetting table shares the actual formulas for atlas and enchanted plate costs. A framed chest shows its built-in label item and matching count. The cookie jar shows its first stored stack.
- Wire protocol 12 adds a rate-limited readout request and reply. The server selects the aimed slot from the player's current reach and checks furniture ownership and printing library visibility. The reply contains display text for the matching target. Each client and server uses the same JAR.

## Tape Measure, Marker Pole, and Plumb Line

- The early Forge 1.20.1 IDs remain `bibliocraft:tape_measure`, `bibliocraft:tape`, `bibliocraft:marker_pole`, and `bibliocraft:plumb_line`. The tape and plumb line keep the original item sprites and crafting layouts. Air use switches the tape's mode. The tape keeps its coordinates and dimension in `TapeStart` and the absolute mode in `TapeAbsolute`. The client derives the legacy meter readout from the held tape and the player's current block position.
- The marker pole uses one translated legacy OBJ with its original UVs. Its floor, ceiling, and wall states rotate the same model and the outline follows the mount. Earlier poles default to permanent floor mounts. The existing crafting recipe and block drop remain available for permanent poles.
- Tape-generated poles are temporary, have no collision or drops, and carry the owner's UUID and a measurement UUID. Completing a tape removes its own pole. Active marker IDs are saved in `bibliocraft_markers.dat` so a completed pole can retire after an unloaded origin starts ticking. Removing the support or leaving the tape out of its online owner's inventory also removes the temporary pole.
- The tape uses the normal Forge item interaction and posts the ordinary place event when the clicked face can accept a marker. The distance still works on occupied faces. The tape still rotates a table display when used against its side. Wire protocol 13 removes the empty measurement packet.
- The plumb line searches the adjacent column in the direction the player faces. It reports the depth and surface height, stops at the dimension floor, and reports an empty column when it finds no surface.

## Clipboard Port

- The handheld item keeps `bibliocraft:biblio_clipboard`. The wall block, wall block entity, compatibility block item, and editor menu retain `bibliocraft:clipboard`. The handheld item's legacy name was `BiblioClipboard`. The handheld recipe accepts any vanilla wooden pressure plate. The existing one-item conversion to a wall block item stores the complete checklist in `BlockEntityTag.Clipboard`.
- The item keeps the legacy one-based `currentPage`, `totalPages`, and `pageN` keys, with the original title, task text, and task state limits. The wall block entity saves the full item under `Clipboard`. An empty wall block from the earlier port keeps its block item until it acquires checklist data. Pickup, loot, and pick-block return the whole edited item.
- The editor uses identical pages in the hand and the desk. Wire protocol 14 sends a bounded page at a time through the active menu, validates the original held or desk stack, and merges independently edited text and marks. The wall uses ordinary block interactions for marks and page navigation. Both the desk and the wall honor furniture locks.
- One OBJ retains the original board vertices, faces, normals, and UVs. Forge material indices select the eighteen original check and cross groups. The front text and page number follow the board rotation. The handheld keeps its original sprite, and the desk displays the legacy open-book mesh for a clipboard in its writing slot.

## Slotted Book and Redstone Book Port

- `bibliocraft:slotted_book` and `bibliocraft:biblio_red_book` keep their item IDs, author strings, glint, and legacy recipes. Slotted books remain unstackable. The redstone book title editor uses `bibliocraft:redstone_book` for its menu.
- The slotted book saves one full item stack in the legacy `Inventory` list at `Slot:0`. `lines.line1` through `lines.line5` hold its description, and the title uses the standard stack display name. Inventory moves persist as they happen, and Save applies the title and description. The menu prevents nested slotted books and protects the held book from inventory moves.
- The redstone book opens a description of its interaction with the bookcase and lets the player save a custom title. Both editors check that the exact book still belongs to the live server menu. A redstone book occupies a normal bookcase slot and supplies its zero-based slot strength to every side except the front, whether renamed or carrying the default title.

## Shared Drill Actions and Item Tester

- Both drills cycle shelves, bookcases, creative bookcases, tool racks, and potion shelves through the original zero, quarter-block, and half-block shifts. `shift=0` preserves geometry in older Forge 1.20.1 saves. The shift moves the baked mesh, selection and collision shapes, and stored item poses together. The potion shelf moves toward its opposite edge because its existing base OBJ occupies the east half of the block.
- Sneak-use rotates those blocks and map frames. Regular use changes a display case between its floor and wall mounts while preserving the open or closed lid, lining, material, and contents. Plain drill use on a filled map frame still returns the map. Rotating a powered bookcase updates the circuits at both the old and new openings.
- Chests, dispensers, droppers, furnaces, ender chests, and comparators rotate on ordinary or sneak-use. Doors, trapdoors, repeaters, levers, anvils, and retracted pistons rotate on sneak-use. Both door halves update together. A double chest separates into two intact inventories if one half turns. An extended piston keeps its orientation until retracted. Right-clicking in the air clears the held drill's selection, and completed joins provide a localized action-bar readout.
- `bibliocraft:tester_item` opens the original parchment and one inspection slot under `bibliocraft:tester`. It shows the live display name, translation key, item ID, and damage. It shares the slotted book's server-owned `Inventory` storage and exact held-stack validation. Opening a stacked tester from an earlier port gives the player back the extra testers and leaves any sample with one copy.

## Display Lighting, Config, Recipes, and Loot

- The legacy `BiblioWoodBlock` lighting path now uses the brightest standalone luminous block in wooden furniture. The block entity computes `display_light` from its actual inventory on load and after inventory changes. Forge propagates that block state's light to neighbors and clients. Older 1.20.1 saves default to zero and compute the current value from their stored items. Disabling `rendering.emit_light` and restarting the world clears saved furniture light.
- The existing `config/bibliocraft-common.toml` keys govern creative listings, block item placement, and feature-specific actions. The server's `blocks_enabled` switches filter bundled recipes based on the result and any required BiblioCraft inputs. A recipe with a vanilla alternative remains available when the alternate BiblioCraft ingredient's feature is disabled. Forge applies these `bibliocraft:features_enabled` conditions when loading recipes, including on datapack reload. Existing world blocks, item stacks, and lock ownership keep their data and IDs.
- `recipes.enable_lock_recipe` and `blocks_enabled.lock_and_key` must both be enabled for the gold recipe. Disabled redstone books remain in bookcases and lose their signal. A new world load refreshes the circuit from the current setting.
- `rendering.disable_renderers` skips client block entity render passes and leaves the baked furniture mesh visible for troubleshooting. The per-block fast-render keys remain for TOML compatibility, matching the bundled source's unused options. The legacy update-check key remains in the TOML. Update checking awaits a release feed for this port.
- All 37 block loot tables retain their current item identities. Wooden variants copy their source panel, dyeable blocks copy their color, and clocks carry their settings. The dedicated code paths return a packed fancy sign or a filled clipboard once, and the armor stand lower half owns its drop. Temporary measuring poles have empty loot. The ordinary inventory path drops contents separately from furniture.

## Current 1.20.1 Asset Mapping Snapshot (Smoke Baseline)
- Current resources root: `src/main/resources/assets/bibliocraft`
- Legacy resource source: `old-1.12.2/src/main/resources/assets/bibliocraft`
- Imported for baseline:
- `textures/**`
- `models/block/*.obj` + `models/block/*.mtl`
- `models/item/*.obj` + `models/item/*.mtl`
- `sounds.json`
- `sounds/*.ogg`
- `data/bibliocraft/recipes/*.json` (representative migrated subset)
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
- Color-aware placeholders use `facing + color` variants (`color=white..black`) with Y rotations.
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
- Typewriter/sword color-state parity updates now in baseline:
- `typewriter` and `sword_pedestal` now use `facing + color` blockstates (`color=white..black`).
- Added sixteen color-specific block models per block:
- `models/block/typewriter_<color>.json` inherits the shared typewriter OBJ and selects the legacy shell sprite. The carriage and paper models use separate groups from the same OBJ.
- `models/block/sword_pedestal_<color>.json` inherits the shared pedestal OBJ and resolves `trim` to `minecraft:block/<color>_wool`.
- Mounted-light color-state parity updates now in baseline:
- `lamp_gold`, `lamp_iron`, `lantern_gold`, and `lantern_iron` now use `face + facing + color` blockstates.
- Mounted light variants inherit their mount geometry and textures from the base model. `lamp.mtl` and `lantern.mtl` share the body and shade slots across both metals and all colors.

## Current 1.20 Voxel Shape Snapshot (Pass 1)
- Source of truth: `src/main/java/art/arcane/bibliocraft/registry/ModBlocks.java` (`SHAPE_*` constants + registrations).
- Shape block classes:
- `src/main/java/art/arcane/bibliocraft/block/StaticShapeBlock.java`
- `src/main/java/art/arcane/bibliocraft/block/HorizontalFacingEntityBlock.java` (adds facing-based shape rotation)
- `src/main/java/art/arcane/bibliocraft/block/ColorFacingEntityBlock.java` (adds `facing + color` state for colorized placeholders)
- `src/main/java/art/arcane/bibliocraft/block/ColorMountedFacingEntityBlock.java` (adds `face + facing + color` state for mounted colorized placeholders)
- `src/main/java/art/arcane/bibliocraft/block/MountedFacingEntityBlock.java` (adds `face + facing` mount-state shape rotation)
- `src/main/java/art/arcane/bibliocraft/block/FloorWallFacingEntityBlock.java` (adds floor/wall mount-state shape rotation)
- Intent: remove full-cube placeholder behavior so model hitboxes are closer to legacy.

| Block ID | Current Shape (normalized 0-1) | Basis |
|---|---|---|
| `bookcase`, `bookcase_creative`, `shelf`, `tool_rack` | `x:0.0-0.5 y:0.0-1.0 z:0.0-1.0` | old half-block behavior + OBJ extent |
| `potion_shelf` | `x:0.5-1.0 y:0.0-1.0 z:0.0-1.0` | OBJ extent (`potionshelf.obj`) |
| `marker_pole` | `x:0.25-0.75 y:0.0-0.9 z:0.25-0.75` | OBJ extent (`markerpole.obj`) |
| `clipboard` | `x:0.0-0.08 y:0.08-0.92 z:0.15-0.85` | old wall board bbox |
| `lantern_gold`, `lantern_iron` | floor: `x:0.2133-0.7867 y:0.0-0.7007 z:0.2517-0.7483`; wall: `x:0.2133-1.0 y:0.0-0.9789 z:0.2517-0.7483`; ceiling: `x:0.15-0.85 y:0.0-1.0 z:0.1971-0.8033` | OBJ visibility extents (`lantern.obj`) with mount-state variants |
| `lamp_gold`, `lamp_iron` | floor: `x:0.0485-0.85 y:0.0-1.0 z:0.1-0.9`; wall: `x:0.0485-1.0 y:0.0-0.5403 z:0.1-0.9`; ceiling: `x:0.0485-0.9515 y:0.0-1.0 z:0.1-0.9` | OBJ visibility extents (`lamp.obj`) with mount-state variants |
| `furniture_paneler` | `x/z:0.0-1.0 y:0.0-0.63` | old `BlockFurniturePaneler` bbox |
| `framed_chest` | `x:0.05-0.965 y:0-0.868 z:0.052389-0.952389` for the north single state | Joined shapes extend to the seam |
| `fancy_sign` | mounted shape baseline: thin sign plane (`x:0.0-0.06 y:0.2-0.8 z:0.0-1.0`) rotated by `facing` for floor/wall/ceiling placements | old sign bbox + legacy mount states |
| `label` | `x:0.0-0.0625 y:0.093-0.405 z:0.25-0.75` in the north state | Selection follows the originally authored frame |
| `table` | Full tabletop slab with either the original center post or exposed corner supports, plus the optional floor carpet | Neighbor connections pick the same legs for the shape and the rendered mesh |
| `seat` | Seat, surviving legs and braces, linked extensions, and optional floor carpet | Collision and rendered bench pieces share the same directional connections |
| `clock` | `x:0.0-0.3372 y:0.0-1.0 z:0.2549-0.7451`, with the chosen depth offset | Rotates with the visible case |
| `case` | floor/wall mount baseline: floor (`x:0.06-0.94 y:0.0-0.5 z:0.0-1.0`), wall (`x:0.0-0.5 y:0.0-1.0 z:0.06-0.94`), both rotated by `facing` | old floor/wall case bbox |
| `map_frame` | mounted baseline: wall (`x:0.0-0.05 y:0.0-1.0 z:0.0-1.0`), floor (`y:0.0-0.05` full XZ), ceiling (`y:0.95-1.0` full XZ) | old map-frame floor/wall/ceiling bbox |
| `painting_frame_*` | `x:0.0-0.08 y:0.0-1.0 z:0.0-1.0` | old painting-frame wall bbox |
| `painting_press` | `x/z:0.0-1.0 y:0.0-0.98` | OBJ/legacy near-full profile |
| `typewriter` | Body collision `x:0.1384–0.6452 y:0.0–0.1966 z:0.2466–0.7534` in the base orientation | Selection also includes the carriage and loaded paper across their full travel |
| `sword_pedestal` | `x:0.2758-0.7242 y:0.0-0.234 z:0.0712-0.9288` | OBJ extent + old bbox |
| `armor_stand` | Rotated 0.4-block-wide upright in both halves and a thin lower base | Old upright hit regions split by equipment height |
| `bell` | `x/z:0.36518–0.63482 y:0.0–0.2` | OBJ rim and legacy height |
| `typesetting_table` | `x/z:0.0-1.0 y:0.0-0.92` | old `BlockTypesettingTable` bbox |
| `cookie_jar` | `x/z:0.18-0.82 y:0.0-0.75` | old `BlockCookieJar` bbox |
| `dinner_plate` | `x:0.1536-0.8464 y:0.0-0.0767 z:0.11-0.91` | OBJ extents (`dinnerplate.obj`) |
| `disc_rack` | `x:0.0-1.0 y:0.0-0.35 z:0.25-0.75` | old floor disc-rack bbox |
| `desk` | Rotated top, legs, shelf, and back panel | Separate shapes for the four legacy joined desk sections |
| `fancy_workbench`, `printing_press` | full shape placeholder (BE-backed) | pending deeper shape/state pass |

## Current 1.20 Registry Skeleton Snapshot
- The shelf preserves the `bibliocraft:shelf` block, item, block-entity, and menu IDs. Its feature implementation is in `ShelfBlock`, `ShelfBlockEntity`, `ShelfMenu`, `ShelfScreen`, and `ShelfRenderer`.
- The shelf blockstate selects the full OBJ or hides the `shelf_top` group. The blockstate rotates the original east-opening mesh around the block center. Slot hit testing and rendering use the same rotation.
- `FurnitureMaterial`, `FurnitureBlockEntity`, and `FurnitureMaterialModel` carry shelf materials through item NBT, world NBT, block-entity updates, and baked quads. The shelf keeps its existing registry IDs across all materials. The oak recipe keeps `bibliocraft:shelf`. The other shaped recipes use `bibliocraft:shelf_<material>`.
- `FurniturePanelerBlock`, `FurniturePanelerBlockEntity`, `FurniturePanelerMenu`, `FurniturePanelerScreen`, and `FurniturePanelerRenderer` restore paneling using the existing `bibliocraft:furniture_paneler` IDs. The `panels` blockstate selects visible OBJ groups. The saved panel input supplies their sprite through model data. The crafted preview is derived from the two inputs.
- The closed case wall model is `models/block/case.json`. The floor version inherits its OBJ, visibility, and cutout layer through `models/block/case_floor.json`. Its centered rotation maps raw OBJ coordinates `(x, y, z)` to `(1 - z, x, 1 - y)` before the blockstate yaw. Existing floor/wall state names are preserved.
- Mod bootstrap wiring in: `src/main/java/art/arcane/bibliocraft/BiblioCraft.java`
- Active registry classes:
- `src/main/java/art/arcane/bibliocraft/registry/ModBlocks.java`
- `src/main/java/art/arcane/bibliocraft/registry/ModItems.java`
- `src/main/java/art/arcane/bibliocraft/registry/ModBlockEntities.java`
- `src/main/java/art/arcane/bibliocraft/registry/ModMenus.java`
- `src/main/java/art/arcane/bibliocraft/registry/ModEntities.java`
- `src/main/java/art/arcane/bibliocraft/registry/ModEnchantments.java`
- `src/main/java/art/arcane/bibliocraft/registry/ModSounds.java`
- Config scaffold:
- `src/main/java/art/arcane/bibliocraft/config/BiblioConfig.java`
- Registered from `src/main/java/art/arcane/bibliocraft/BiblioCraft.java` as `ModConfig.Type.COMMON`.
- Current config categories: `blocks_enabled`, `recipes`, `rendering`, `gameplay`, `keywords`.
- Config gate-consumption layer:
- `src/main/java/art/arcane/bibliocraft/config/BiblioFeatureToggles.java`
- Current runtime consumers:
- `src/main/java/art/arcane/bibliocraft/BiblioCraft.java` (creative tab filtering by toggle)
- `src/main/java/art/arcane/bibliocraft/block/PlaceholderEntityBlock.java` (block menu open toggle checks)
- `src/main/java/art/arcane/bibliocraft/item/MenuOpeningItem.java` (item menu open toggle checks)
- `src/main/java/art/arcane/bibliocraft/block/SeatBlock.java` (sitting, attachments, and redstone toggle checks)
- `src/main/java/art/arcane/bibliocraft/registry/ModMenus.java` (menu lookup toggle checks)
- `src/main/java/art/arcane/bibliocraft/recipe/EnchantedAtlasRecipe.java` (recipe availability toggle checks)
- Placeholder runtime classes:
- `src/main/java/art/arcane/bibliocraft/block/HorizontalFacingEntityBlock.java`
- `src/main/java/art/arcane/bibliocraft/block/ColorFacingEntityBlock.java`
- `src/main/java/art/arcane/bibliocraft/block/ColorMountedFacingEntityBlock.java`
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
- Block entity types: 33
- Menu types: 26
- Entity types: 1 (`seat_entity`)
- Enchantments: 2 (`deathcompassench`, `readingench`)
- Sound events: 17 (from legacy `sounds.json` keys)
- Sound assets: 1 `sounds.json` + 39 `.ogg` files migrated into active resources
- Packet registrations (placeholder): 33 (23 serverbound + 10 clientbound)
- Recipe placeholders: 1 serializer + 1 type (`enchantedatlas`)

### Current 1.20 Network + Recipe Skeleton Snapshot
- Network channel:
- `src/main/java/art/arcane/bibliocraft/network/ModNetwork.java`
- Legacy packet parity represented via no-payload placeholders:
- Serverbound placeholders: `atlas_wpt`, `type`, `type_flag`, `type_delete`, `type_update`, `paneler`, `recipe_craft`, `stock_title`, `stock_compass`, `clipboard`, `update_inv`, `atlas`, `measure`, `map_pin`, `rbook`, `rbook_load`, `paint_press`, `painting`, `painting_clear`.
- Clientbound placeholders: `drill_text`, `atlas_client`, `paneler_client`, `recipe_text`, `stock_log`, `open_book`, `desk_open_gui`, `atlas_swp_client`, `atlas_tgui`, `sound_player`.
- Recipe placeholder:
- `src/main/java/art/arcane/bibliocraft/registry/ModRecipes.java`
- `src/main/java/art/arcane/bibliocraft/recipe/EnchantedAtlasRecipe.java`
- Registered key: `bibliocraft:enchantedatlas` (serializer + recipe type placeholder).
- Current migrated datapack subset:
- `src/main/resources/data/bibliocraft/recipes/maptool.json`
- `src/main/resources/data/bibliocraft/recipes/compass.json`
- `src/main/resources/data/bibliocraft/recipes/slotted_book.json`
- `src/main/resources/data/bibliocraft/recipes/atlas_book.json`
- `src/main/resources/data/bibliocraft/recipes/enchantedatlas.json`
- Extended migrated datapack subset:
- `src/main/resources/data/bibliocraft/recipes/hand_drill.json`
- `src/main/resources/data/bibliocraft/recipes/tape.json`
- `src/main/resources/data/bibliocraft/recipes/tape_measure.json`
- `src/main/resources/data/bibliocraft/recipes/framing_saw.json`
- `src/main/resources/data/bibliocraft/recipes/framing_sheet.json`
- `src/main/resources/data/bibliocraft/recipes/framing_board.json`
- `src/main/resources/data/bibliocraft/recipes/plumb_line.json`
- `src/main/resources/data/bibliocraft/recipes/biblio_clipboard.json`
- `src/main/resources/data/bibliocraft/recipes/big_book.json`
- `src/main/resources/data/bibliocraft/recipes/biblio_red_book.json`
- `src/main/resources/data/bibliocraft/recipes/stockroom_catalog.json`
- `src/main/resources/data/bibliocraft/recipes/biblio_glasses.json`
- Consolidated furniture migrated subset:
- `src/main/resources/data/bibliocraft/recipes/bookcase.json`
- `src/main/resources/data/bibliocraft/recipes/shelf.json`
- `src/main/resources/data/bibliocraft/recipes/table.json`
- `src/main/resources/data/bibliocraft/recipes/seat.json`
- `src/main/resources/data/bibliocraft/recipes/seatback1.json`
- `src/main/resources/data/bibliocraft/recipes/seatback2.json`
- `src/main/resources/data/bibliocraft/recipes/seatback3.json`
- `src/main/resources/data/bibliocraft/recipes/seatback4.json`
- `src/main/resources/data/bibliocraft/recipes/seatback5.json`
- Supporting block migrated subset:
- `src/main/resources/data/bibliocraft/recipes/case.json`
- `src/main/resources/data/bibliocraft/recipes/map_frame.json`
- `src/main/resources/data/bibliocraft/recipes/fancy_sign.json`
- `src/main/resources/data/bibliocraft/recipes/fancy_workbench.json`
- `src/main/resources/data/bibliocraft/recipes/potion_shelf.json`
- `src/main/resources/data/bibliocraft/recipes/tool_rack.json`
- `src/main/resources/data/bibliocraft/recipes/armor_stand.json`
- `src/main/resources/data/bibliocraft/recipes/framed_chest.json`
- Extended block/workstation/color migrated subset:
- `src/main/resources/data/bibliocraft/recipes/bell.json`
- `src/main/resources/data/bibliocraft/recipes/clock.json`
- `src/main/resources/data/bibliocraft/recipes/cookie_jar.json`
- `src/main/resources/data/bibliocraft/recipes/desk.json`
- `src/main/resources/data/bibliocraft/recipes/dinner_plate.json`
- `src/main/resources/data/bibliocraft/recipes/disc_rack.json`
- `src/main/resources/data/bibliocraft/recipes/furniture_paneler.json`
- `src/main/resources/data/bibliocraft/recipes/label.json`
- `src/main/resources/data/bibliocraft/recipes/lamp_gold.json`
- `src/main/resources/data/bibliocraft/recipes/lamp_iron.json`
- `src/main/resources/data/bibliocraft/recipes/lantern_gold.json`
- `src/main/resources/data/bibliocraft/recipes/lantern_iron.json`
- `src/main/resources/data/bibliocraft/recipes/painting_frame_borderless.json`
- `src/main/resources/data/bibliocraft/recipes/painting_frame_flat.json`
- `src/main/resources/data/bibliocraft/recipes/painting_frame_simple.json`
- `src/main/resources/data/bibliocraft/recipes/painting_frame_middle.json`
- `src/main/resources/data/bibliocraft/recipes/painting_frame_fancy.json`
- `src/main/resources/data/bibliocraft/recipes/painting_press.json`
- `src/main/resources/data/bibliocraft/recipes/printing_press.json`
- `src/main/resources/data/bibliocraft/recipes/sword_pedestal.json`
- `src/main/resources/data/bibliocraft/recipes/typesetting_table.json`
- `src/main/resources/data/bibliocraft/recipes/typewriter.json`
- `src/main/resources/data/bibliocraft/recipes/biblio_chase.json`
- `src/main/resources/data/bibliocraft/recipes/biblio_drill.json`
- Added mapped legacy-output completion recipe:
- `src/main/resources/data/bibliocraft/recipes/painting_canvas.json`
- Procedural/variant obtainability subset:
- `src/main/resources/data/bibliocraft/recipes/atlas_plate.json`
- `src/main/resources/data/bibliocraft/recipes/clipboard_block.json`
- `src/main/resources/data/bibliocraft/recipes/death_compass.json`
- `src/main/resources/data/bibliocraft/recipes/print_plate.json`
- `src/main/resources/data/bibliocraft/recipes/enchanted_plate.json`
- `src/main/resources/data/bibliocraft/recipes/recipe_book.json`
- `src/main/resources/data/bibliocraft/recipes/marker_pole.json`
- `src/main/resources/data/bibliocraft/recipes/monocle.json`
- `src/main/resources/data/bibliocraft/recipes/tinted_glasses.json`
- Current migrated datapack recipe count: 74
- Mapped legacy JSON output coverage: 58 / 58 outputs represented (consolidated-variant strategy applied).
- Current registry-id recipe output coverage: 65 / 68 ids (remaining intentional non-craftable surfaces: `bookcase_creative`, `biblio_creative_lock`, `tester_item`).
- First-pass block loot-table subset:
- Path: `src/main/resources/data/bibliocraft/loot_tables/blocks/*.json`
- Count: 37 files (one per registered block id).
- Current loot strategy: self-drop + `minecraft:survives_explosion` placeholder condition.
- Colorized block persistence override:
- `lamp_*`, `lantern_*`, `typewriter`, and `sword_pedestal` loot tables now also apply `minecraft:copy_state` for `color` so dropped items preserve state via `BlockStateTag`.
- Deferred metadata-variant recipe split:
- `monocle`, `tinted_glasses` currently output base `biblio_glasses`; split to true subtype/state outputs when glasses variant support exists.
- Deferred variant-splitting follow-up:
- wood/framed furniture recipe families are currently merged into consolidated recipes until block/item wood-variant depth support is implemented.
- supporting block recipe families are also merged into consolidated recipes until block/item wood-variant depth support is implemented.
- block loot-table files are currently generic self-drop placeholders; block-entity/state-specific drop parity remains deferred.
- `EnchantedAtlasRecipe` behavior baseline:
- 3x3 pattern requires atlas center + 4 ender pearls + 2 waypoint compasses + 2 enchanted books.
- Output atlas currently includes placeholder marker NBT (`bibliocraft:death_compass_enabled=true`) until full enchantment parity is ported.

### Current 1.20 Event Hook + Seat Snapshot
- Common Forge event subscriber:
- `src/main/java/art/arcane/bibliocraft/event/CommonGameplayEvents.java`
- Placeholder legacy hook coverage:
- death flow (`LivingDeathEvent`) captures pending player death position.
- spawn/join flow (`EntityJoinLevelEvent`) consumes pending death marker (placeholder waypoint handoff hook).
- item toss flow (`ItemTossEvent`) closes container for legacy packet-heavy item set.
- ordinary seat block use now handles riding and attachments. The common interaction hook allows the seat to handle sneak-use.
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
- `fancy_workbench`, `painting_press`, `painting_frame_borderless`, `painting_frame_flat`, `painting_frame_simple`, `painting_frame_middle`, `painting_frame_fancy`, `clipboard`, `typesetting_table`, `printing_press`, `map_frame`.
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

## Block Entity Registrations (33 active registrations)
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

The Forge port uses one creative tab per installed wood source and the existing 27 framed furniture items. A variant stores the companion mod's plank block ID in `FurnitureMaterial.panel`. The client uses the installed plank model's particle sprite on the original furniture quads. The same material survives menu sync, pick-block, loot, and world saves. A missing source block renders the default frame and leaves the saved panel ID intact.

`src/main/resources/bibliocraft/bibliowoods.json` lists the supported plank and slab families. `generateBiblioWoodsRecipes` builds the recipes from the current base layouts and keeps their feature conditions. The dependent recipes require the matching framed bookcase, label, borderless frame, or seat back. Fancy frames inherit their panel from the existing generic framed recipe. The creative bookcase remains available in creative. Forge item-existence conditions omit recipes when their registered ingredients or plank textures are unavailable.

### `BiblioWoodsBoP`
- File: `old-1.12.2/src/main/java/jds/bibliowoods/bop/BiblioWoodsBoP.java`
- Mod ID: `bibliowoodsbop`
- Version: `2.0`
- Integrates 16 BoP wood textures through `RegisterCustomFramedBlocks`
- The 1.20.1 Forge build has 13 registered plank-and-slab families, including 10 original families and Dead, Empyreal, and Maple. Sacred Oak, BoP Cherry, Ethereal, BoP Mangrove, Ebony, and Eucalyptus have no corresponding block IDs in this build. Vanilla Cherry and Mangrove already have core furniture recipes. [Biomes O' Plenty 19.0.0.91](https://modrinth.com/mod/biomes-o-plenty/version/qlLkvQuH).

### `BiblioWoodsForestry`
- File: `old-1.12.2/src/main/java/jds/bibliowoods/forestry/BiblioWoodsForestry.java`
- Mod ID: `bibliowoodsforestry`
- Version: `2.0`
- Integrates 29 Forestry wood textures through `RegisterCustomFramedBlocks`
- The 1.20.1 integration covers the 43 regular Forestry CE wood families, including the original 29 with `acacia_desert` and `hill_cherry` as the current block names. Fireproof planks can be applied in the furniture paneler. [Forestry CE 2.11.2](https://modrinth.com/mod/forestry-community-edition/version/dvKfwvis), [Forestry Cherry rename](https://modrinth.com/mod/forestry-community-edition/version/2.0.0).

### `BiblioWoodsNatura`
- File: `old-1.12.2/src/main/java/jds/bibliowoods/natura/BiblioWoodsNatura.java`
- Mod ID: `bibliowoodsnatura`
- Version: `2.0`
- Integrates 13 Natura wood textures through `RegisterCustomFramedBlocks`
- Both Forge 1.20.1 ports register the same 13 source IDs. Those releases both use the `natura` mod ID and are alternate installations. [Natura Port 0.1.0](https://www.curseforge.com/minecraft/mc-mods/natura-port/files/8704184), [Natura (Unofficial Port) 1.0.1](https://www.curseforge.com/minecraft/mc-mods/natura-unofficial-port/files/9339478).

### `BiblioWoodsBotania`
- File: `old-1.12.2/src/main/java/jds/bibliowoods/botania/BiblioWoodsBotania.java`
- Mod ID: `bibliowoodsbotania`
- Version: `2.0`
- The bundled add-on only probes block lookup. Compatible Botania blocks can use the general furniture paneler.

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

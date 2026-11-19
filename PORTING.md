# BiblioCraft 1.20.1 Port Plan

## Working Rules
- Finish and verify one feature family at a time against the bundled 1.12.2 code and assets.
- Always read `PORTING_MEMORY.md` before coding.
- Use `PORTING_MANIFEST.md` as the source index for old 1.12.2 locations.
- Keep this file updated whenever phase scope changes.

## Active Forge 1.20.1 Parity Checklist

| Legacy feature family | Implementation | Current verification |
|---|---|---|
| Shelf interaction, four visible slots, menu, and drops | Shelf stores and syncs real stacks, restores the legacy screen, responds to neighbor changes, and renders stored items. | Four Forge GameTests cover click transfer, matching hitboxes in all facings, stack limits, save/load, client update tags, drops, and menu shift-click. The isolated production client has opened the screen and shift-clicked a displayed stack. Both drills move the shelf and its display through the original three depths and rotate it on sneak-use. |
| Furniture model pipeline and variants | Shelves, bookcases, racks, display cases, furniture panelers, writing desks, armor stands, tables, seats, labels, fancy signs, clocks, framed chests, fancy workbenches, and painting frames each share one legacy OBJ across eleven vanilla woods and a framed variant. Framed furniture stores its panel's source block ID. | Isolated clients verified material variants, authored geometry, and live changes across the furniture cycles below. |
| Display cases | Both mounts use the original open and closed lid meshes with cutout glass. Sneak-click operates the lid and plays its sound. The case holds one displayed stack, supports carpet lining and the legacy menu, and powers neighbors when displaying a redstone block. | Five Forge GameTests cover mounts, interactions, persistence, menus, redstone, drops, and all twelve materials. The muted client rendered both mounts in both lid states, toggled a case open, removed its exhibit, replaced its lining, and opened the original screen. Both drills switch the mount and rotate the case while keeping its lining, exhibit, and lid state. |
| Bookcases and creative bookcases | Sixteen saveable slots, direct insertion and sneak removal, a filtered menu, enchanting power, directional redstone books, and generated written books on creative bookcase placement. Model data controls each authored book mesh. | Five Forge GameTests cover all slots in all four rotations, recipes, saved data, drops, menus, enchanting, redstone through solid blocks, and generated books. The muted client showed the correct mesh appearing when a player placed a book directly into its slot on a brick-paneled bookcase. It also opened the legacy menu and rendered wood variants in the world and hotbar. Both drills shift the model and rotate the opening, and a redstone book updates the old and new sides. |
| Potion shelves and tool racks | Twelve-slot, two-sided potion shelves and four-slot tool racks use their legacy screens, shared inventory persistence, and live item rendering. | Four Forge GameTests cover all rotations, both potion shelf faces, filtered menus, effect data, drops, and all 24 rack recipes. The muted client showed a potion appearing in the clicked compartment and a sword dropping from the clicked rack slot. Both menus and the wood and framed variants rendered correctly. Both drills move the original mesh, outline, and contents together and rotate them on sneak-use. |
| Cookie jar | Eight persistent slots and the legacy menu. A one-second redstone pulse starts when the menu opens. The original cookie geometry fills the jar one piece per occupied slot. | Three Forge GameTests cover saved contents, drops, the pulse through a solid block, and replacement mid-pulse. The muted client rendered empty and full jars with clear glass and showed a second cookie mesh after a live shift-click. |
| Dinner plate | Three persistent food slots, the original menu, one or three visible food piles, and direct serving with vanilla food effects and container returns. The four-click meal sequence belongs to each diner at each plate. | Three Forge GameTests cover per-player serving, hunger, bowls, menus, inventory sync, drops, and both quartz slab recipes. The muted client rendered both pile layouts, opened the original menu, and consumed a meal with a bowl returned and hunger restored. |
| Disc rack | Nine persistent slots display records and compatible items. The original OBJ mounts on floors, ceilings, and walls. The hand drill and powered drill rotate the rack and change wall layout. | Three Forge GameTests cover all nine grooves in all sixteen mount and facing combinations, legacy save defaults, both drill interactions, menus, and drops. The muted client rendered all four mounts, drilled an upright rack horizontal, and placed a record into the clicked groove. |
| Sword pedestal | The original stone and trim mesh supports sixteen wool colors in the world and inventory. A persistent one-sword slot drives the upright display and full redstone power. Both drills rotate the pedestal on sneak-use. | Three Forge GameTests cover insertion, ejection, sync, redstone through stone, automation, dyeing, rotation, drops, and all sixteen wool and dye colors. The muted client verified the blade pose, visible world and hotbar trim, live insertion, dyeing, drilling, and ejection. The trim, facing, and stored blades survived reload. |
| Writing desk | Ten persistent slots back the original inventory screen, direct book stacks, work surface, and carpet. Vanilla books, recipe books, and Big Books can be edited, signed, and read at the desk. Clipboards can be edited in the writing slot. The original models show the active contents and connected desk sections. A filled map renders on the surface. The desk uses wooden sounds and axe mining. | Five Forge GameTests cover four facings, recipes and materials, signed-book saves, connected rows, drops, and typewriter delivery. The muted client rendered the joined sections, books, colored carpet, and a filled map, opened the original inventory screen, and saved and signed two pages in place. The signed book, selected page, map, and delivered typewriter book survived a world reopen. The recipe book retained signed notes. The Big Book preserves its bookmark while on the desk and supports concurrent edits. Two muted clients opened and merged changes to a clipboard in the writing slot. |
| Armor stand | Both halves share four wearables, the original two-column inventory screen, and material and lock state. A sneak-click swaps one matching piece or the full set when powered. The original OBJ renders with worn equipment in the world and has eleven wood variants plus framed. | Five Forge GameTests cover the equipment swaps, two-column menu, saved and synced wearables, both halves, safe placement upgrades, locks, material drops, and all twelve recipes. The muted client rendered armor and head items, opened the original menu, swapped a full set, placed and drilled a cherry stand, and reopened the world with both halves and the dyed helmet intact. Mining the upper half with an axe returned exactly one stand and the helmet with its color. |
| Tables | Automatic joins restore the original corner legs, square and beveled tops, and cloth drapes. Three persistent slots hold a displayed stack, tabletop cloth, and floor carpet. The one-slot menu, filled maps, two-axis display rotation, both drills, and eleven woods plus framed are implemented. | Five Forge GameTests cover neighboring shapes, display and fabrics, menus, tools, map and state persistence, twelve recipes, furniture material, and drops. The muted client displayed joined T-shaped and 2×2 tables, applied magenta cloth separately from lime floor carpet, used the original menu and side drill, and displayed terrain from a newly made map. The display angle, fabrics, and map survived a world reopen. Live placement and survival break joined and detached neighboring tables. Both fabrics came back as separate items. |
| Desk bell | Right-click and rising analog redstone signals play the original bell sound. The bell remembers the last sampled strength and respects the Lock and Key for player clicks. The original OBJ renders in the world and hotbar. | Two Forge GameTests verify clicks, locks, drops, signal changes through stone, and reload. The muted client received the sound event after both a direct click and a lever click through stone. |
| Gold and iron lamps and lanterns | The original OBJ groups select floor, wall, or ceiling hardware. Lamps and lanterns emit full light and keep all sixteen legacy shades through crafting, inventory, placement, dyeing, pick-block, and drops. Both drills change the mount or rotate the light. Lanterns show their wick flame. | Two Forge GameTests check all 64 recolor outputs, recipe sync, drilling, saved locks, light emission, and drops. The muted client showed both metals in all three mounts, the corrected palette in the world and hotbar, and lit wicks inside the glass. Dye and drill changes survived a reload, and pick-block and placement preserved a lime lantern. |
| Seats and benches | The original stool, five backs, cushions, floor carpet and bench pieces respond to direct clicks and both drills. Players and nearby named mobs can sit. Occupied seats emit power with the chair redstone option enabled. Seats and backs have eleven wood variants plus framed, with independent frame panels. | Five Forge GameTests cover attachments, bench geometry and detachment, rider exclusivity, power through stone, named riders, material drops, and all 72 recipes. The muted client displayed all five backs, attached both fabrics, joined a three-seat bench by drill, and verified that a player and a named pig powered their lamps. Two muted clients on a clean dedicated Forge server reopened the saved bench and named rider, observed the same player and lamp, and took turns occupying the seat. Breaking one section in survival detached its neighbor and returned the seat, back, and cushion separately. |
| Labels | The original wood frame, one- or three-icon layout, and three-slot inventory screen are restored for all twelve materials. Survival removal returns the material-specific label and stored stacks. | Forge GameTests cover menu transfers, saved stacks, update tags, drops, and material variants. The muted client inserted three stacks in a cherry label and showed both icon layouts. All three stacks survived a dedicated server restart and dropped separately from the cherry label when mined. |
| Fancy signs | The original floor, wall, and hanging meshes support three drill positions. Sneak-use opens the editor for formatted text, per-line sizing, and two independently positioned, scaled, and rotated icons. Harvest packs the text, material, and displayed items into the sign. | Forge GameTests cover all placements and hitboxes, material-aware crafting, both drills, item conservation, persistence, wire encoding, and editor access. Two muted clients edited different lines concurrently and saw both changes. The text and icons followed the board through two drill shifts and a rotation. Reload preserved both icons and the formatting. A survival player moved the packed sign and crafted a new sign with its original custom panel. |
| Clocks | Two wall clocks join into the original grandfather case with either drill. Both cases show world time with the legacy dial, moving hands, and pendulum. The original editor programs tick sounds, 48 half-hour chime and redstone slots, and two-second or continuous output. Both halves of a grandfather clock use the upper schedule and keep their own settings if separated. | Five Forge GameTests cover join, shift, separation, pulse and solid power through stone, scheduled sounds, editor access, all twelve recipes, save tags, and drops. Two muted clients verified the moving parts, concurrent edits, sound events, and both output modes. Dedicated server reload kept both lamps lit with the saved schedule. A survival player mined and replaced the upper clock with its frame and settings intact while the lower clock reverted to its own state. The live editor applied the hourly and six-hour presets. |
| Framing tools, paneler, and custom textures | Framing saw returns after cutting parts. The paneler applies a source block to framed furniture through its legacy screen and top slots. Its OBJ shows the stock from the saved input count and the renderer previews the furniture. | Forge checked the saw remainder, a custom panel on the shelf, paneler slot transfers, input consumption, and stale result handling. The isolated client shift-clicked a brick-paneled shelf from the menu and placed it normally with the correct texture. Adoption by remaining furniture is pending. |
| Framed chests | Each chest has 27 slots and its own material and automatic item label. Either drill joins adjacent aligned chests into a 54-slot menu with the original moving lids and seam latch. The two halves keep separate inventories on removal, and locking either half protects shared menu access. | Five Forge GameTests cover all orientations, synchronized viewer counts, label aggregation, locks, persistence, drops, and all twelve recipes. Two muted clients verified moving joined lids, shared menu updates, item labels, and guest denial through either half. A dedicated server restart preserved the join, contents, and lock. A survival player broke the unlocked half and recovered its chest and items. Live crafting carried a custom panel from a label to a chest. |
| Stockroom catalog and waypoint compass | Crouch-use records individual inventories. The original catalog pages show names, sorted totals, per-container counts, and locations. A portable catalog title, holder-only markers, and selection of up to eight carried waypoint compasses are supported. The compass editor and all 36 original needle angles are restored. | Four Forge GameTests cover save tags, locked and unloaded containers, distinct double chest halves, paged data, large counts, and cardinal needle directions. Two muted clients tested registration, saved titles, live catalog refresh, name and count sorting, two summary pages, private stock totals, and assigning a location to the selected compass. The client also showed the original location particles and verified manual waypoint entry and visible needle changes. The catalog title and compass targets survived a dedicated server restart. Removing a catalog entry preserved the contents, and dropping and recollecting the compass preserved its target. |
| Fancy workbench and recipe book | The original bench shows eight individual book meshes and supports twelve materials, both drills, and two adjacent bookcase panels. Each viewer has an independent crafting grid. A plain or unsigned recipe book records the live recipe. A recorded book shows ingredients, loads complete batches, and can be annotated and signed in hand or at the desk. Portable crafting uses live recipes and returns remainders. | Five Forge GameTests cover materials, facings, saved mesh masks, shared bookcase and book slots, independent crafting, item conservation, and bucket remainders. Two muted clients shared a recorded book, loaded a separate grid, crafted multiple batches, and moved items through a side bookcase. Closing returned the sample ingredients. The muted client crafted from a held book, signed saved notes at the desk, and verified guest denial after locking the bench. A clean server restart preserved the lock, material, bench contents, and signed book at the desk. Survival mining returned the framed bench and both types of stored books. Live crafting inherited a custom bookcase panel. |
| Big Book | The original parchment and chapter-tab textures support up to 256 pages and eight two-line chapter links. Per-line formats, colors, and sizes are saved. Books can be edited and signed in hand or at the writing desk, and signed copies remain readable with saved bookmarks. | Five Forge GameTests cover the original NBT layout, two-player desk edits, stale sources, signing, large Unicode pages, book drops, and the blank-book recipe. Two muted clients verified chapter jumps, saved color and bold formatting, edits to separate lines, and revocation of an open reader when a desk was locked. The client kept the typed signing title through a window resize and confirmed that signed page turns update the bookmark. A signed book kept its notes and bookmark through a dedicated server restart, survival harvest and pickup, and re-placement on a desk. |
| Reading glasses and Reading enchantment | Three wearable variants retain their original sprites, armor textures, recipes, and item ID. Reading appears on books at the enchantment table and applies to helmets at an anvil. The readout follows the aimed slot, shows item and book details and typesetting costs, and respects locks and private library selections. | Four Forge GameTests cover all three crafting recipes and variant saves, the original enchanting range, helmet-only anvil application, all four shelf and label facings, and library privacy. Two muted clients showed the original sprites and three worn textures. They saw a shelf's live stack count, then verified that locking it hid the text from the guest. The stands retained all three variants after a dedicated restart. The clients saw the 40-level typesetting readout, applied Reading to an iron helmet at an anvil, and verified that equipping it enabled the shelf readout. |
| Tape measure, marker poles, and plumb line | A tape keeps its start point and mode on the stack. The original distance overlay updates while moving, and the second click reports the axes or absolute distance and removes that tape's pole. Poles render the original mesh against all six faces. The plumb line measures the adjacent column down to solid ground, including through water. | Four Forge GameTests cover independent measurements, legacy distances, stack saves, all six mounts, temporary and permanent loot, deferred cleanup, and the plumb line through water. Two muted clients saw floor, wall, and ceiling poles. Separate active measurements survived a dedicated restart and cleared independently. Dropping the active tape removed its ceiling pole. The muted client saw a 7 m depth at Y 124 through water and rotated a table display with the tape. The final JAR started, finished, and toggled mode once each during sustained right-clicks. |
| Clipboard | The handheld and desk editors use the original checklist art, nine tasks per page, three mark states, and up to 50 pages. Crouch-placement hangs the held clipboard on a wall. Front clicks mark tasks and turn existing pages on the original board mesh. Pickup and mining return the saved clipboard. | Four Forge GameTests cover held edits, page saves, stack replacement, desk locks and concurrent edits, all four wall facings, item conservation, and recipes. Two muted clients edited and displayed two pages, cycled a wall mark, and turned pages. A dedicated restart preserved both pages. The players retrieved the board and merged separate desk edits. Rehanging and mining it in survival returned exactly one item with both pages and edits intact. |
| Slotted book and redstone book | The legacy slotted book screen stores one item stack, edits its title and five description lines, and protects the held book. The redstone book explains its directional output and lets the player rename it. Both keep their legacy author tooltips, glint, and recipes. | Four Forge GameTests cover item conservation, held-stack guards, original recipes, and directional redstone after a save/load. The muted client stored a named damaged pickaxe and saved two description lines. It renamed a redstone book, canceled a second edit, and put it in slot 16. The second client saw the side lamp light while the front lamp stayed off. A dedicated restart preserved the books, lamp states, label, and pickaxe. The player extracted exactly one pickaxe with its name and damage intact. |
| Shared drills and tester item | Both drills restore the legacy depth, rotation, and mount actions across furniture and supported vanilla blocks. The join readout shows the result, and using a drill in the air clears its selection. The tester stores a sample and shows its display name, translation key, registry ID, and damage. | Six Forge GameTests verify all shifted shapes, powered bookcase rotation, case contents and locks, both halves of vanilla chests and doors, drill selection, and tester item conservation. Two muted clients verified shifted furniture and moving contents, a wall-mounted open display case, and rotation of a vanilla chest with all seven ingots still inside. The older bookcase reopened with its original facing and named redstone book. A dedicated restart kept the adjusted blocks and the named axe in the tester. The tester returned the one axe, and survival mining returned the shifted shelf and all three apples. |
| Typewriter | The original body, sliding carriage, and seven paper meshes show sixteen writing stages. Players and named mobs turn eight sheets into a signed book. Shell color survives crafting, dyeing, pick-block, and drops. A finished book can move to the writing desk below. | Four Forge GameTests cover inventory and automation, locks, save data, mob voices, all sixteen crafting and recoloring recipes, and drops. The muted client typed a book, watched a named pig finish another, verified the moving paper and carriage, and picked up and read the signed book after reopening the world. The desk tests and muted client verified automatic delivery. |
| Typesetting table and printing press | The original table slots save signed books and catalogs to a world library. The owner can share or discard titles, and a rising redstone signal stamps the selected title into a chase. Enchanted plates cost experience and have limited uses. The press prints from a plate, black dye or ink, and blank books. Its original arm and bed move while printing. | Five Forge GameTests cover rotated slots, author controls, book data, persisted progress, level costs, plate wear, sided automation, locking, and drops. Two muted clients watched published and withdrawn titles update an open guest library. A survival player stamped a plate, used the direct press slots, and saw the original arm and bed moving. The press resumed after a dedicated server restart. The player shift-clicked the copy out of the press and read the original text, and a hopper collected the next copy. A rotated table stamped a named Mending plate for the expected experience cost. The press printed three named Mending books before exhausting the plate. Both drills rotated the press in the live client. |
| Painting frames and press | The original press transfers bundled, vanilla, and custom resource-pack art onto a canvas as its lid closes. All five frame styles have eleven wood variants and a framed variant, with the original mitered and joined edges. Both drills edit joins or rotate the art. The frame editor adjusts the master corner, scale, resolution, aspect ratio, rotation, and borderless visibility. | Five Forge GameTests cover the transfer and its saved progress, reciprocal links and concave corners, concurrent frame edits, locking, hidden-frame collision, artwork metadata, item conservation, and all 60 frame recipes. Two muted clients verified the moving lid, three artwork sources, both drills, and a four-frame mural. The framed custom canvas and layout survived a dedicated server restart. Creative pick-block preserved the brick panel, and survival mining returned the frame and custom canvas separately. |
| Map frame and map tool | The original frame mounts on the wall, floor, and ceiling and joins adjacent frames on the same plane. It displays live terrain, rotates maps by quarter turns, and saves named, colored pins on the displayed map. Both drills return the filled map, and clicking a pin can set a waypoint compass. | Five Forge GameTests cover mounts and rotations, seams, item conservation, pin edits, compass targets, and all twelve material recipes. Two muted clients verified the original mounts and joined edges, live terrain updates, color selection, and a pin moving with the map while both players' compasses received the same world coordinates. The reobfuscated JAR reopened the edited pin after a dedicated server restart. A hand drill returned the filled map with the pin intact. |
| Atlas and atlas waypoints | The atlas holds 42 maps and six supplies. It opens the original inventory and transfer screens and a live map with neighboring sheets, colored waypoints, and stored compasses. Auto center selects the appropriate saved sheet for the current dimension and scale. Auto create makes a missing sheet from one blank map. Crouch-use copies a map to or from a frame and opens pin transfer when both show the same map. | Five Forge GameTests cover the saved inventory, slot limits, map creation across a seam, blank-map consumption, copied pins, compass targeting, and frame locks. Two muted clients verified the original inventory, copying maps through two frames, creating a pin, setting a stored compass, and sending pins in both directions. The production JAR reopened the same selected sheet, pins, compass, and map view after a dedicated server restart. The clients saw the original first-person cover and the third-person compass. Auto create used one blank for the east sheet. Returning west reselected the existing sheet, and the map arrows navigated between them. Dropping and picking up the atlas preserved its maps, waypoints, and supplies. |
| Atlas plates | Crouch-use on a populated atlas in the typesetting table stamps a single-use plate. It carries the filled sheets and pins. Printing consumes an empty atlas and leaves supplies in the original atlas. | Forge GameTests cover the level charge, inputs, a replaced blank atlas, and the saved job. In the muted clients a two-map atlas took five levels and one chase. The reobfuscated JAR resumed the saved press job with 171 ticks remaining and produced one copy containing both maps and their pins. |
| Death compass and its enchantment | Both original crafting layouts and the enchantment table can upgrade atlases. A tracked death grants one compass with its dimension and coordinates. The original death-compass face and moving needle use the shared compass geometry. Earlier atlases with the placeholder marker acquire the real enchantment. | Forge GameTests cover both recipes through the normal crafting type, atlas-only application, marker conversion, and once-only respawn delivery with saved coordinates. In the muted client, the crafting table upgraded the printed atlas. The player died in the Nether, disconnected while dead, and received one compass on respawning in the Overworld. The right-click readout showed the Nether coordinates and height. The original needle reversed when the player turned in the Nether, and the dropped atlas was recovered with the enchantment and both maps. |
| Lock and Key | Lock ownership is saved by UUID on ported inventory furniture. The owner can use and unlock it. Other players lose access to use, drills, and open menus. Locked furniture blocks survival mining and automated transfers. | Four Forge GameTests cover same-name players, menus, drills, mining, hopper and Forge transfers, and saved lock data. Two muted clients on a clean dedicated server verified menu closure, denied guest access, owner access, and survival break protection. The owner and contents survived a server restart. |
| Display lighting, config, recipes, and loot | Wooden furniture emits the brightest light from a stored block. Existing config keys control displayed light, recipes, placement, redstone behavior, and optional client renderers. The Lock and Key has its original opt-in gold recipe. | Seven new Forge GameTests cover light propagation and removal, recipe conditions, and actual drops for all 37 block IDs. The packaged JAR recovered light from a shelf saved by the prior build. Both muted clients saw the room light up, including the client with furniture renderers disabled. A player ejected, recovered, and replaced one named glowstone. The nearby block light went from 13 to 0 and back to 13. Restarting with lighting and redstone books disabled cleared both signals and preserved the stored items. The disabled shelf recipe was absent and the opt-in Lock and Key was craftable at a normal crafting table. |
| BiblioWoods and save compatibility | Legacy source and identifiers are mapped. | Supported 1.20.1 integrations and fixture upgrades for existing 1.20.1 worlds pending. Direct 1.12.2 world migration is separate. |

Legacy model fidelity is a primary acceptance criterion in each feature cycle. Compare the original OBJ parts, materials, UVs, and authored axes with the 1.12.2 state mapper and renderer. Check the world mesh, inventory and held models, state-driven parts, animation, transparent surfaces, displayed items, and material variants where applicable. Verify representative states and live transitions in an isolated muted Forge client. Confirm that selection shapes and click targets align with the rendered parts in every supported orientation.

For the shelf, the OBJ opens toward positive X in its base orientation. The existing `facing=north` state still uses that transform. Placement now picks the saved state from the intended opening, and the slots use the same orientation as the rendered geometry. Horizontal, mounted, and floor/wall geometry share corrected shape rotations in block coordinates.

## Initial Breadth Snapshot (2026-02-09)
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
- The initial pass relied on manually reported client visual checks.
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
- Item-held menu interaction breadth pass completed:
- Added `MenuOpeningItem` placeholder class for right-click item menu opens.
- Wired `atlas_book`, `slotted_book`, and `tester_item` to open their placeholder menus (`atlas`, `slotted_book`, `tester`) on main-hand use.
- Preserved stack-size parity baseline for menu-heavy books (`atlas_book`, `slotted_book` are single-stack).
- `./gradlew --no-daemon compileJava` passes after item-menu wiring.
- Sound asset migration breadth pass completed:
- Copied legacy `sounds.json` and 39 `.ogg` files into active resources under `assets/bibliocraft/`.
- Verified all 17 `ModSounds` `SoundEvent` ids match `sounds.json` keys one-to-one.
- `./gradlew --no-daemon processResources compileJava` passes after sound asset import.
- Config migration scaffold breadth pass completed:
- Added `BiblioConfig` (`ForgeConfigSpec`) with legacy-parity toggle surface (block/item enable flags, recipe toggles, render toggles, and gameplay keyword settings).
- Registered common config in mod bootstrap (`ModLoadingContext.registerConfig`).
- This is a breadth scaffold only; feature-level registry gating/behavior checks are still pending.
- `./gradlew --no-daemon compileJava` passes after config wiring.
- Config toggle runtime-consumption pass completed:
- Added `BiblioFeatureToggles` as the legacy gate mapping layer (`ResourceLocation` -> config boolean supplier).
- Applied block/item gate checks to runtime placeholder behavior:
- Creative tab output now filters block-items and standalone items by legacy toggle parity.
- Block interaction menu opens (`PlaceholderEntityBlock#use`) now respect block feature toggles.
- Item-held menu opens (`MenuOpeningItem#use`) now respect item feature toggles.
- Seat interaction hook (`CommonGameplayEvents#onSeatInteract`) now respects seat toggle parity.
- `./gradlew --no-daemon compileJava` passes after config-consumption wiring.
- Recipe data migration + runtime-gating pass completed:
- Added first representative migrated 1.20 datapack recipe family under `data/bibliocraft/recipes`:
- `maptool`, `compass`, `slotted_book`, `atlas_book`, and `enchantedatlas`.
- Upgraded `EnchantedAtlasRecipe` from no-op placeholder to a real 3x3 matcher/assembler:
- Requires atlas + 4 ender pearls + 2 waypoint compasses + 2 enchanted books.
- Adds placeholder marker NBT (`bibliocraft:death_compass_enabled=true`) to crafted atlas output.
- Gated `EnchantedAtlasRecipe.matches(...)` behind legacy parity config (`enableAtlas && enableDeathCompass && enableWaypointCompass`).
- Extended toggle checks to remaining placeholder runtime surfaces:
- `ModMenus.getMenuForBlock(...)` now returns `null` for disabled blocks.
- `CommonGameplayEvents.onItemToss(...)` now ignores disabled-item surfaces.
- `./gradlew --no-daemon processResources compileJava` passes after recipe/data/gating pass.
- Utility-item recipe migration breadth pass completed:
- Added second migrated 1.20 datapack recipe batch under `data/bibliocraft/recipes`:
- `hand_drill`, `tape`, `tape_measure`, `framing_saw`, `framing_sheet`, `framing_board`, `plumb_line`, `biblio_clipboard`, `big_book`, `biblio_red_book`, `stockroom_catalog`, `biblio_glasses`.
- Recipe conversions normalized old 1.12 ids and metadata to 1.20 naming/tag semantics:
- old `minecraft:dye` metadata replaced by `forge:dyes/*` tags where needed.
- old `minecraft:planks` metadata lists replaced by `minecraft:planks` tag ingredients.
- old legacy item ids (`handdrill`, `tapemeasure`, `framingsaw`, etc.) mapped to current 1.20 ids (`hand_drill`, `tape_measure`, `framing_saw`, etc.).
- `./gradlew --no-daemon processResources compileJava` passes after utility recipe batch.
- Core furniture block recipe migration breadth pass completed:
- Added consolidated 1.20 datapack recipes for wood/framed furniture surfaces:
- `bookcase`, `shelf`, `table`, `seat`, `seatback1`, `seatback2`, `seatback3`, `seatback4`, `seatback5`.
- Conversion strategy for missing metadata variants in current placeholder runtime:
- merged old per-wood + framed recipe families into single recipes using ingredient alternatives (`minecraft:planks`/`minecraft:wooden_slabs` and `framing_sheet`/`framing_board`).
- normalized old `wooden_pressure_plate` to `minecraft:wooden_pressure_plates` tag and old wool metadata to `minecraft:white_wool`.
- `./gradlew --no-daemon processResources compileJava` passes after furniture recipe batch.
- Supporting block recipe migration breadth pass completed:
- Added consolidated 1.20 datapack recipes for additional block families:
- `case`, `map_frame`, `fancy_sign`, `fancy_workbench`, `potion_shelf`, `tool_rack`, `armor_stand`, `framed_chest`.
- Conversion strategy reused consolidated wood/framed alternatives for current placeholder runtime:
- `minecraft:wooden_slabs` + `framing_board`, `minecraft:planks` + `framing_sheet`, plus normalized tag usage (`minecraft:signs`, `forge:dyes/black`).
- `./gradlew --no-daemon processResources compileJava` passes after supporting block recipe batch.
- Extended block + color-surface recipe migration breadth pass completed:
- Added additional consolidated 1.20 datapack recipes for remaining broad block/dependency surfaces:
- `bell`, `clock`, `cookie_jar`, `desk`, `dinner_plate`, `disc_rack`, `furniture_paneler`, `label`, `lamp_gold`, `lamp_iron`, `lantern_gold`, `lantern_iron`, `painting_frame_borderless`, `painting_frame_flat`, `painting_frame_simple`, `painting_frame_middle`, `painting_frame_fancy`, `painting_press`, `printing_press`, `sword_pedestal`, `typesetting_table`, `typewriter`, plus utility dependencies `biblio_chase`, `biblio_drill`.
- Added temporary placeholder recolor datapack flows for collapsed color-variant runtime ids:
- `lamp_gold_recolor`, `lamp_iron_recolor`, `lantern_gold_recolor`, `lantern_iron_recolor`, `sword_pedestal_recolor`, `typewriter_recolor`.
- `./gradlew --no-daemon processResources compileJava` passes after extended recipe batch.
- Legacy JSON-output recipe coverage pass completed:
- Added `painting_canvas` datapack recipe (`painting_canvas.json`) from legacy recipe set.
- Current mapped old JSON output coverage is now `58 / 58` outputs (with consolidated variant strategy still applied).
- Block loot-table breadth pass completed:
- Added first-pass block loot tables for all 37 registered blocks under `data/bibliocraft/loot_tables/blocks`.
- Current loot-table strategy is self-drop + `minecraft:survives_explosion` placeholders for survival smoke tests.
- `./gradlew --no-daemon processResources compileJava` passes after recipe + loot-table pass.
- Procedural/variant-recipe obtainability breadth pass completed:
- Added placeholder obtainability datapack recipes for non-creative, non-debug registered IDs that were still without outputs:
- `atlas_plate`, `clipboard` (from `biblio_clipboard`), `death_compass`, `print_plate`, `enchanted_plate`, `recipe_book`, `marker_pole`.
- Added deferred glasses variant recipe files from legacy set as placeholder no-op variant flows:
- `monocle` and `tinted_glasses` now map to `biblio_glasses` output in current collapsed-variant runtime.
- Current registry-ID recipe output coverage is now `65 / 68` IDs (remaining intentionally non-craftable: `bookcase_creative`, `biblio_creative_lock`, `tester_item`).
- `./gradlew --no-daemon processResources compileJava` passes after procedural/variant recipe pass.
- Typewriter/Sword pedestal color-state parity pass completed:
- Added `ColorFacingEntityBlock` (`facing + color`) and switched `typewriter` + `sword_pedestal` registrations to this block class.
- Added in-world dye application in `PlaceholderEntityBlock#use` for color-capable placeholders.
- Added 16-color OBJ material/model variants for both blocks plus `facing + color` blockstate variant maps.
- `./gradlew --no-daemon processResources compileJava` passes after color-state/model pass.
- Lamp/Lantern color-state + drop-persistence parity pass completed:
- Added `ColorMountedFacingEntityBlock` (`face + facing + color`) and switched `lamp_*` + `lantern_*` registrations to this block class.
- Expanded model/state coverage for mounted lights to 16 colors (gold/iron floor/wall/ceiling variants with color-specific MTL overrides).
- Rewrote `lamp_gold`, `lamp_iron`, `lantern_gold`, and `lantern_iron` blockstate files to `face + facing + color` variant grids.
- Added loot-table `minecraft:copy_state` for colorized blocks (`lamp_*`, `lantern_*`, `typewriter`, `sword_pedestal`) so break/re-place preserves color via `BlockStateTag`.
- `./gradlew --no-daemon processResources compileJava` passes after light color-state + persistence pass.
- Bounding-box correction pass completed for reported shape mismatches:
- Updated `table` to a multi-box monoleg profile (top slab + flare + stem + foot crossbars) instead of a single center post.
- Recalibrated `lamp_*` and `lantern_*` floor/wall/ceiling voxel shapes to match active OBJ extents, including dedicated wall/ceiling lantern shapes.
- Tightened additional high-traffic shapes to current visible-model extents (`typewriter`, `seat`, `clock`, `dinner_plate`).
- `./gradlew --no-daemon compileJava` passes after shape corrections.

## Phase 0 - Recon and Inventory (Complete)
- [x] Locate all block and item registration entry points.
- [x] Enumerate all registered blocks/items and their registry names.
- [x] Enumerate block entity registrations and mappings.
- [x] Enumerate enchantments, entity types, and networking packets.
- [x] Enumerate GUI/container topology.
- [x] Enumerate recipe/sound/resource inventory at high level.
- [x] Build initial manifest and memory files.

## Phase 1 - 1.20.1 Registry Skeleton
- [x] Create `DeferredRegister` sets for:
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
- [x] Boot a dedicated server with the current registry surface.

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
- [x] Typewriter/Sword pedestal color variants
- [ ] Recreate creative tab exposure in new tab system.
- [x] Add config-aware creative tab filtering for block-items and standalone items.

## Phase 3 - Block Entities + Menus Breadth
- [x] Register all legacy block entities in 1.20.1.
- [x] Provide a working factory for each registered block-entity type.
- [x] Register all menu types used by legacy GUI/container flow.
- [x] Create placeholder menu classes so open paths compile.
- [x] Wire placeholder block interaction paths to open registered placeholder menus.

## Phase 4 - Interaction + Network Breadth
- [x] Create packet channel and message registration equivalent to legacy packet list.
- [x] Stub packet handlers to no-op/log first.
- [x] Recreate core event bus hooks (death drop, toss, spawn, overlays, highlights) as placeholders.
- [x] Port entity seat registration and minimal behavior.
- [x] Add placeholder item-held menu open hooks for atlas/slotted/tester interaction flow.

## Phase 5 - Rendering + Data Breadth
- [ ] Stand up model/data pipeline for 1.20.1:
- [x] Blockstate + model JSON strategy for every block/item ID
- [x] Temporary/generated assets to make all IDs visible in game
- [x] Replace placeholder oak/paper visuals with legacy-asset smoke baseline
- [x] Restore first-pass OBJ item models for legacy custom-rendered tools (`atlas_book`, `compass`, `death_compass`, `maptool`, `painting_canvas`)
- [ ] Convert or replace OBJ-driven dynamic model system incrementally
- [ ] Recreate legacy dynamic transform/state behavior (old `ExtendedBlockState` + custom baked model logic) for off-axis/shifted models
- [x] Migrate sounds (`sounds.json` + `SoundEvent` registry).
- [x] Migrate representative atlas recipe family to 1.20 datapack format (`maptool`, `compass`, `slotted_book`, `atlas_book`, `enchantedatlas`).
- [x] Migrate representative utility-item recipe family to 1.20 datapack format (`hand_drill`, `tape`, `tape_measure`, `framing_*`, `plumb_line`, `biblio_clipboard`, `big_book`, `biblio_red_book`, `stockroom_catalog`, `biblio_glasses`).
- [x] Migrate representative core furniture recipe family to 1.20 datapack format (`bookcase`, `shelf`, `table`, `seat`, `seatback1..5`).
- [x] Migrate representative supporting block recipe family to 1.20 datapack format (`case`, `map_frame`, `fancy_sign`, `fancy_workbench`, `potion_shelf`, `tool_rack`, `armor_stand`, `framed_chest`).
- [x] Migrate extended block/workstation/color recipe family to 1.20 datapack format (`bell`, `clock`, `cookie_jar`, `desk`, `dinner_plate`, `disc_rack`, `furniture_paneler`, `label`, `lamp_*`, `lantern_*`, `painting_frame_*`, `painting_press`, `printing_press`, `sword_pedestal`, `typesetting_table`, `typewriter`, `biblio_chase`, `biblio_drill`).
- [x] Migrate remaining mapped legacy JSON output recipe (`painting_canvas`) to 1.20 datapack format.
- [x] Add first-pass placeholder obtainability recipes for non-creative/non-debug registered IDs still missing recipe outputs (`atlas_plate`, `clipboard`, `death_compass`, `print_plate`, `enchanted_plate`, `recipe_book`, `marker_pole`).
- [x] Add deferred glasses-variant legacy recipe files (`monocle`, `tinted_glasses`) as collapsed-variant placeholders.
- [x] Add first-pass block loot-table coverage for all registered block IDs (self-drop placeholders).
- [ ] Move legacy recipes into data pack format where needed.

## Phase 6 - Feature Depth Passes (Iterative)
- [ ] Inventory logic parity per block entity.
- [ ] Lock/key ownership rules.
- [x] Painting/canvas systems.
- [ ] Printing/typesetting systems.
- [ ] Atlas/compass systems.
- [ ] Rendering parity and performance.
- [x] Config migration strategy scaffold (legacy config booleans -> 1.20 config).

## Phase 7 - Add-on and Compatibility Modules
- [ ] Decide scope for BiblioWoods add-ons in 1.20.1.
- [ ] Re-implement framed texture variant support or replace with modern data-driven alternative.
- [ ] Add integration tests for optional-mod behavior.

## Near-Term Lookahead
- Verify supported BiblioWoods integrations with their actual Forge 1.20.1 dependencies.
- Audit earlier 1.20.1 saves and verify the packaged release on clean Forge and in Auram.

## Definition of Done
- [ ] Resolve each feature and model family in the active parity checklist against the bundled 1.12.2 source. Document any unavailable integration dependencies.
- [ ] Preserve registry IDs and existing Forge 1.20.1 world state through save and reload, including tested defaults for older placeholders.
- [ ] Verify normal player workflows, client model parity, and dedicated server behavior with the reobfuscated JAR on Forge 1.20.1.
- [ ] Smoke-test the packaged JAR in the Auram development environment.

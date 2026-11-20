# BiblioCraft Forge 1.20.1 Port

## What This Project Is
This repository ports **BiblioCraft** from Forge 1.12.2 to **Minecraft 1.20.1 on Forge**. It restores the original furniture models, display items, books, workstations, paintings, maps, and tools.

The old source is kept in:
- `old-1.12.2/`

The active 1.20.1 port code is in:
- `src/main/java/art/arcane/bibliocraft`
- `src/main/resources/assets/bibliocraft`

## Install and Build

Use Java 17 to run Minecraft 1.20.1. The port is tested on Forge 47.4.10. Place the same BiblioCraft JAR in the `mods` folder on each client and the dedicated server.

```bash
./gradlew jar
```

This produces the reobfuscated `build/libs/bibliocraft-1.0.1.jar`. The `build` task also copies the JAR to the configured local Auram instance.

BiblioWoods recipes and creative tabs appear for supported woods from installed companion mods. The [porting manifest](PORTING_MANIFEST.md) records supported versions and save formats. Earlier Forge 1.20.1 worlds can be opened with the port. Direct Forge 1.12.2 world conversion is a separate migration project.

## In-Game Guide

With Patchouli installed, craft **BiblioCraft Guide** from a vanilla book and green dye in any arrangement. Hold the guide and right-click to open it. The guide also appears in the BiblioCraft creative tab. BiblioCraft gameplay is available without Patchouli.

The item is `patchouli:guide_book` with the `patchouli:book` tag set to `bibliocraft:bibliocraft`. With administrator permissions, use this command.

```mcfunction
/give @s patchouli:guide_book{"patchouli:book":"bibliocraft:bibliocraft"}
```

The recipe and command have been verified with Patchouli 84.1 for Minecraft 1.20.1 Forge.

## Source-of-Truth Docs
Always use these files together:

1. `PORTING_MEMORY.md`
- Session memory and critical reminders.
- **Read this first** at the start of each session.

2. `PORTING.md`
- Phase plan and checklist status.
- Primary progress tracker.

3. `PORTING_MANIFEST.md`
- Legacy registry/index map (blocks, items, block entities, packet surfaces, source locations).
- Lookup/reference document for where things come from.

## How To Check Progress
Use these checks regularly:

1. Checklist progress
- Open `PORTING.md` and review each phase checklist.

2. Build health
```bash
./gradlew --no-daemon processResources compileJava
```

3. Data generation health (when touching generated assets/data)
```bash
./gradlew --no-daemon generateBiblioWoodsRecipes
```

4. Runtime checks
- Run Forge GameTests with `./gradlew runGameTestServer -PbibliocraftGameTestDir=/path/to/disposable/run`.
- For visual checks, set Minecraft's master volume to zero before launching the client inside an isolated graphical session or virtual display.

## Daily Workflow
1. Read `PORTING_MEMORY.md` -> `PORTING.md` -> `PORTING_MANIFEST.md`.
2. Pick the next feature or verification target from `PORTING.md`.
3. Use `PORTING_MANIFEST.md` to locate legacy source and registry IDs.
4. Implement in 1.20.1 code/resources.
5. Run build checks.
6. Update all three docs with what changed.

## Credits

Original BiblioCraft by Joseph D. Sinclair (Nuchaz). The original [MIT notice](old-1.12.2/LICENSE) is included at `META-INF/BIBLIOCRAFT-LICENSE.txt` in the JAR.

## Project Purpose
The near-term goal is a complete 1.20.1 registry and rendering surface for all legacy BiblioCraft content.

The long-term goal is behavior parity and stable gameplay parity with the 1.12.2 mod while using modern Forge 1.20.1 patterns.

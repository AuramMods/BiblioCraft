# BiblioCraft Forge 1.20.1 Port

## What This Project Is
This repository is an active port of **BiblioCraft** from **Forge 1.12.2** to **Forge 1.20.1**.

The old source is kept in:
- `old-1.12.2/`

The active 1.20.1 port code is in:
- `src/main/java/art/arcane/bibliocraft`
- `src/main/resources/assets/bibliocraft`

## Porting Strategy
The project is following a **breadth-first** strategy:
- Register and surface all legacy content first.
- Make everything load and render at a smoke-test level.
- Then iterate depth: behavior parity, dynamic rendering, block entities, menus, networking, etc.

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

## Current Snapshot (High Level)
As of the latest updates in the porting docs:
- Legacy surface mapped from 1.12.2.
- 37 legacy blocks registered in 1.20.1.
- 31 legacy standalone items registered in 1.20.1.
- Legacy OBJ/MTL/textures imported for smoke testing.
- First-pass non-full voxel shapes added for many blocks.
- Block items and key custom items are now 3D OBJ-based in inventory/hand.

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
./gradlew --no-daemon runData
```

4. Runtime visual smoke test
- `runClient` is intentionally manual/user-driven in this workflow.
- Do not assume immediate visual testing; coordinate before requesting it.

## Daily Workflow
1. Read `PORTING_MEMORY.md` -> `PORTING.md` -> `PORTING_MANIFEST.md`.
2. Pick the next breadth/depth target from `PORTING.md`.
3. Use `PORTING_MANIFEST.md` to locate legacy source and registry IDs.
4. Implement in 1.20.1 code/resources.
5. Run build checks.
6. Update all three docs with what changed.

## Project Purpose
The near-term goal is a complete 1.20.1 registry and rendering surface for all legacy BiblioCraft content.

The long-term goal is behavior parity and stable gameplay parity with the 1.12.2 mod while using modern Forge 1.20.1 patterns.

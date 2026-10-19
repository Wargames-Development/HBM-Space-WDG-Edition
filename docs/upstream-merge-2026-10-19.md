# Upstream merge compatibility — 19 October 2026

Target: `af4c62f8d9952c65a469ac64b2f5b3f4c690e278` (upstream space-travel-twopointfive).
Local parent: `ee241d23d` (WG 2.4.0, dynamic altitudes and WorldBorder compatibility).

## Resolutions

- Resolved 27 conflicted paths. Re-merged ClientProxy and ModEventHandler with normalized line endings, retaining WG station lifecycle, drive validation, authorization, and the prior removal of identity-specific privileges.
- Adopted upstream power/fluid ports while retaining launchpad ownership, WG station-drive processing, and orbital-launch authorization. Migrated the WG-only station-drive terminal to the same power ports and player-aware control interface.
- Retained ownership on explosion helpers, missile/turret projectiles, and new cargo-pod shrapnel. Combined upstream turret damage configuration with WG attribution. Chocolate-milk explosions now use upstream VNT processing with player attribution.
- Consolidated barrel/tank ownership in the new shared superclass, added ownership persistence across chunk reloads and corrosion conversion, and retained attribution on tank antimatter effects.
- Moved satellite launch altitude into EntityRocketBase for Soyuz and Lambda. Payload deployment runs on the server. Kept dynamic capsule reentry height and upstream negative-coordinate flooring; guarded capsule inventory access.
- Updated new cargo-pod spawn/departure heights for extended worlds and high docking pads. Updated Horizons delivery altitude. Existing rideable-rocket, atmospheric, and WorldBorder compatibility remains in place.
- Accepted removal of the unregistered legacy miner rocket and retired FOEQ achievement, and retained both DGK recipes and new artillery recipes. Kept Russian artillery range placeholders alongside new rocket-mode translations.

## Validation

- Java 8 `./gradlew compileJava --offline --no-daemon`: passed after downloading the missing declared dependencies once.
- Java 8 `./gradlew build --offline --no-daemon`: passed, including resource processing and reobfuscation. Existing compiler warnings remain. Gradle has no JUnit tests configured.
- `JAVA_HOME=<JDK path> python3 tools/test-spaceflight-compat.py`: 61 assertions passed, compiling the actual altitude helper and shared rocket base against minimal Minecraft test doubles. Covers 128/256/384/512 block worlds, null-world fallback, high cargo pads, deployment boundaries, client/server separation, and legacy payload NBT slots.
- Conflict-marker scan of source and the conflicted language file: clean.

## Required in-game checks before release

These have not been run in a live Forge client/server:

1. In both 256- and 384-block worlds, launch Soyuz, Lambda, and rideable rockets; confirm deployment, capsule descent, and WorldBorder transit. Request cargo to low and high pads, including negative coordinates.
2. With WGCore enabled, test permitted/denied turret, missile, chocolate-milk, and tank explosions across territory boundaries. Reload chunks/restart the server and repeat a tank ownership check.
3. Power the station-drive terminal and drive processor through new ports; validate authorized and denied orbit/Breach drives, orbital launching/docking, station fall recovery, and defeated-station cleanup.
4. Test fluid-tank input/output/buffer/locked modes and damaged tanks; confirm existing-world inventories/fluid and port reconnection after chunk reload.

Build and isolated checks establish compilation and the tested behavior, not complete modpack runtime compatibility. The merge is staged for review; no merge commit or push was made.

## Wider integration review

The follow-up screened the full changed-source inventory (703 Java paths at the start of this review) and inspected the integration-sensitive additions and altered call paths, including controls, radar commands, satellite delivery/production, port management, optional OpenComputers setup, registrations, and world migration. This is a targeted static compatibility review; it does not verify every recipe, asset, or gameplay outcome.

Additional fixes:

- Registered power/fluid port-manager cleanup with their owning tile. Unload/invalidation now releases module-owned nodes even for machine classes with no custom cleanup override. The existing furnace replacement guard is respected.
- Restored science-satellite assembler progress from NBT and restricted production ticks/data lookup to the server.
- Guarded the new RTTY targeting designator when its ray trace misses.
- Corrected the new bullet hitscan fence-post bounding-box argument order.
- Removed fixed Y=255 cable/fluid-connection limits. Actual World access uses its height; cached access handles its own bounds. This deliberately avoids IBlockAccess#getHeight, which the EFR+ source identifies as absent in Crucible's runtime interface.
- Restored existing fluid-tank multiblock migration after the superclass refactor and corrected cargo-dock migration to replace the old core metadata while preserving saved inventory/NBT.
- Preserved the retired TAINT_TRAILS configuration key as a read fallback, with ENTITY_TAINT_TRAILS taking precedence.

The installed `lib/WGCore-2.6.0_X193.jar` fluid inspection/output adapters were inspected with javap. Their MK2 receiver, pressure, transfer, tank-access, and multiblock-core reflection signatures remain present; executable checks cover those signatures. This verifies the API surface, not live WGCore territory decisions.

Validation added: `JAVA_HOME=<Java 8 JDK> python3 tools/test-integration-compat.py` passes 22 assertions against compiled HBM/Minecraft classes, covering port cleanup, furnace invalidation behavior, science NBT persistence, actual fence collision handling, cached extended-height cable access, configuration-key migration, and the WGCore fluid reflection contracts. The separate spaceflight check still passes 61 assertions.

The user explicitly chose to follow upstream removal of FENSU, HTRF4, and the old small turbine. Those removals are accepted upstream behavior, not restored legacy machines. The corresponding existing-world content must not be assumed preserved. Other upstream progression changes, including capacitor/diode redesigns, remain upstream behavior.

Final live QA must additionally cover chunk unload/reload with module-managed ports, old cargo-dock/fluid-tank upgrades on a world copy, science production across restart, RTTY misses, and optional OpenComputers present/absent. No live Forge/Crucible server or complete modpack was launched during this work.

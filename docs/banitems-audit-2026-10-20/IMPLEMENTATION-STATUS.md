# BanItems implementation — 20 October 2026

Implemented and installed into Release Testing for a full-pack smoke test. Nothing committed. No save files modified.

## Availability and reversibility

The curated profile omits 313 whole registry names (300 items and 13 blocks with their inventory forms). It filters 93 selected metadata variants while preserving all 44 explicitly retained variants and existing enum ordinals. Source classes and assets remain.

In hbm.cfg, category wdg_banitems, enableItemProfile controls the profile; disabledRegistrations and disabledMetadata allow individual curated entries to be restored on restart. disableSelectedBosses, disableSelectedBossRewards and disableSmeltingBonusItems are separate reversible settings. The four boss families are Hunter Chopper, BOTPrime, UFO and Radioactive Angel. Industrial drill and fire extinguisher remain outside the combat-gun expansion.

Recipe dependencies, ore dictionaries, inventories, grants, client lists and achievement icons are guarded. Removed entries in loot retain their original probability mass and produce empty draws. No vanilla substitutes or loot reweighting were added. See RECIPE-DECISIONS.md for six retained outputs requiring later recipe decisions.

## Validation

- Real Forge lifecycle: 9,767 assertions with profile enabled; 645 with it disabled.
- Policy checks: 16 passed. Existing integration checks: 24 passed.
- Offline release build passed; packaged profile verified.
- Actual pack CraftTweaker compiler/runtime adapter test passed for absent/restored registrations and exact/wildcard metadata, using mock game/recipe interfaces. Its standalone API initialization also logs an existing ILootFunction setter registration exception; the adapter compiled and executed successfully. Full-pack startup remains to be tested.
- Exported loot defaults identical across profile modes; 128 machine recipes removed by restrictions.
- 281 existing script removal rules migrated; markers, unrelated rules, retained ammo metadata and custom additions preserved. Original and staged hashes checked before installation.

## Installed update and rollback

INSTALLATION.json records the jar hash and backup paths. Backups are outside the active mods/scripts directories. To roll back while the game is closed, restore the jar backup to its original mods filename and restore the backed-up MineTweakerRecipeMaker.zs into scripts. Do not leave duplicate jars or script copies in those active folders.

Configuration reversal restores availability after restart, but cannot reconstruct selected blocks/items already removed from a subsequently saved world. Keep BanItems as the untouched reference; use a fresh disposable world for the smoke test.

## Next check

Boot Release Testing, create a fresh creative world, confirm selected weapons/blocks are absent and intentionally retained ammo remains, check a normal industrial recipe and retained micro-missile recipe, then save, quit and reopen that test world. Report any startup/script errors. Refresh EFR import capabilities and backporter target/template contracts after acceptance; old preflights describe the previous registry. Hold commits until this check passes.

## Follow-up: all canisters and Glitch

User requested all filled Disperser Canister variants and Glitch be removed as whole registrations. Empty canisters were already removed. The older canister metadata rule remains accepted for compatibility with saved configs, while the whole-item restriction now takes precedence. Saved pack config was backed up and amended with both new names; unrelated settings and scripts were preserved. FOLLOWUP-CANISTERS-GLITCH.json records the build hash and immediate rollback backup. The prior runtime smoke test was accepted; this follow-up still needs a quick NEI check. Recipe/loot decisions remain deferred, with no substitutions.

## Follow-up: five bottle-cap blocks and Bypass Round

Added exact restrictions block_cap metadata 1..5 and ammo_secret metadata 6. These shared registrations remain to preserve block_cap:0 and other ammo_secret variants. Creative/NEI listing, recipes, inventories, grants and player use follow the availability profile. Source enums and assets are intact. No placed blocks are automatically deleted by the mod; a separate migration writing a world copy is prepared and synthetically tested (PLACED-BOTTLECAP-MIGRATION.md). No source save modified. All six recipe replacement decisions are resolved by disabling their outputs.

## Restriction route cleanup — validated checkpoint

Nine selected NTM additions to Forge chest hooks now retain weight as empty draws. Pedestal assignment preserves the original single selection/RNG behavior and omits banned contents; inventory access, serialization, ticks and block drops are guarded. Restricted decorative dispatch no longer falls through from an empty chosen layout into unrelated rewards. With enableItemProfile=false, the previous dispatch fall-through is retained.

Real disposable Forge tests passed with 9,783 assertions enabled and 652 disabled, including all nine hook wrappers, pedestal exact-metadata save/read/tick paths, retained metadata, no reroll, and enabled/disabled decorative dispatch behavior. Policy16/integration24/migration tests and the offline release build passed. RESTRICTION-GUARDS.json records the new build. It has not replaced the currently installed test jar. No loot substitutions or weights changed. Replacement suggestions remain unapproved.

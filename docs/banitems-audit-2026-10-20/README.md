# BanItems audit and decision report

**Cross-mod suggestions:** [Release Testing food, materials and FMUR gun kits](loot-review/MODPACK-SUGGESTIONS.md). These are unapproved alternatives to the earlier NTM-only draft.

**Loot review (21 October):** [pool decisions and exact suggestions](loot-review/REVIEW.md), [additional grant routes](loot-review/SUPPLEMENTAL-ROUTES.md). Review only: no loot substitutions applied.

**Current status:** implemented and installed for smoke testing, uncommitted. See [implementation status](IMPLEMENTATION-STATUS.md), [installation record](INSTALLATION.json), and [recipe decisions](RECIPE-DECISIONS.md). The original audit below is historical evidence; its earlier suggestions do not authorize loot substitutions. No world data was modified.

## What was found

- Saved player: approximately (-1131, 4, -302). The selected rack is X=-1129, Y=4..6, Z=-306..-299.
- **22 chest halves / 11 inferred rack pairs, 513 occupied stacks, 424 distinct (registry, metadata) combinations, 287 HBM registry names.** Pair labels follow the physical rack order; inventory accounting is by each individual chest/slot, so adjacency cannot double-count a stack.
- All 513 saved IDs resolved using this world's FML ItemData item registry, not guessed from textures or numeric-ID conventions. Every parent has a matched source initializer and registration call; all 424 rows have a source en_US name/key.
- The nearby-region scan also found 10 chest halves far from the player/rack; their contents are **excluded**. This is not a whole-world claim that no other inventories exist. Player inventory and nested/container contents are not added to the ban selection.
- The exact saved NBT and numeric IDs are retained for traceability. Glyphid gland metadata 75/172 and dispersal-canister metadata 6 represent fluid identities, not tool wear; its in-game display prepends the fluid name to the base gland translation. Configured grenades also require their NBT identity.

## Item list and traceability

- [Readable complete item list](SELECTED-ITEMS.md) — all 424 registry/metadata rows, English names, language keys, fields and registration lines.
- [Spreadsheet item list](selected-items.csv) — includes classes, saved numeric IDs, occurrences, NBT counts and source-reference counts.
- [All 513 saved chest slots](chest-slots.csv); [full saved inventory](inventory.json).
- [Source references](source-references.csv) — 2769 lexical candidate references with file, line, category and explicit-field marker. These are navigation evidence, not a proof of every transitive/dynamic code path. In particular a comment, constructor dependency or achievement criterion is not automatically an item grant.
- [Audit manifest](AUDIT-MANIFEST.json) — source commit, source hashes, saved registry/world-region hashes and scope. The scanned save files remained byte-identical throughout the audit.

## Preserve your intentional gaps

**Do not remove enum entries, reorder ordinals, or unregister an entire ammunition parent.** Forge 1.7 registers parent Items; metadata subtypes are not separate registrations. True per-subtype registry removal is not supported. The selected subtypes need reversible availability/grant/use filtering while the parent and unselected types remain registered. A parent-level hidden-item setting alone is insufficient.

| Shared registry to KEEP registered | Selected metadata count | Known total | Unselected metadata to KEEP |
|---|---:|---:|---|
| `hbm:item.ammo_arty` | 2 | 10 | 0, 1, 2, 5, 6, 7, 8, 9 |
| `hbm:item.ammo_himars` | 4 | 8 | 0, 1, 2, 3 |
| `hbm:item.ammo_shell` | 3 | 5 | 0, 1 |
| `hbm:item.ammo_standard` | 74 | 96 | 0, 1, 2, 3, 20, 21, 24, 25, 33, 34, 50, 51, 52, 58, 61, 63, 64, 65, 73, 74, 75, 95 |
| `hbm:item.bomb_caller` | 1 | 9 | 0, 1, 2, 3, 5, 6, 7, 8 |

[Exact keep-list: 36 known unselected ammunition variants plus eight airstrike-caller types](unselected-variants-KEEP.csv), including ordinary 9mm/5.56mm/.50 BMG rounds, selected rockets/fuels, normal mini-nukes and standard artillery shells. KEEP means not selected in the chest rack; it is not a balance recommendation. Other unlisted items also stay outside the selection.

Also keep the airstrike caller parent: only metadata 4 (atomic bomb) was selected. Types 0..3 and 5..8 remain unselected. Keep the dispersal-canister parent: only fluid metadata 6 was selected; other fluid contents remain outside the selection. ItemDisperser display names combine the base en_US value with the fluid label; the catalogue records the base key and exact fluid metadata without guessing runtime labels.

Other decisions before implementation:

1. **Configured grenades:** 90 distinct shell/filling/fuze/extra combinations share `hbm:item.grenade_universal`. [Exact configurations](selected-grenade-configurations.json). Default to preserving unlisted configurations; confirm whether you instead intend removal of this entire grenade Item. Do not match transient NBT such as equipped flags or animation timers.
2. **13 block items:** decide inventory-item suppression versus block-and-item registration removal. Both registry forms and placed-world references require separate handling. [Capture block collisions](structure-block-collisions.json) finds `broadcaster_pc` only in the already-excluded legacy corrupt-broadcaster capture; none of the other 12 occur as placed blocks in this captured set. This does not prove they are unused by uncaptured templates or entities.
3. **Unlisted special weapons remain unselected.** Many upgraded/legendary gun registry names were not put in these chests; disabling a selected base gun may make an unselected upgrade recipe unreachable. Audit dependencies rather than automatically banning every member of a weapon family.

## Loot defaults and balancing — suggestions only

The current pack has **no active `hbmItemPools.json` override**. The inspected `_hbmItemPools.json` is an exported default template; source ItemPool.initialize supplies defaults. Merely editing the underscore file does not activate it. ItemPoolConfigJSON writes that template when the non-underscore override is absent; an active override replaces the pool map. Missing item names currently fall back to the registered NOTHING item, which is not a clean content-removal solution.

**153 default entries reference selected parent names across 32 pools.** This count includes KEEP metadata and entries needing NBT/block policy decisions, not 153 approved removals.

[Every affected default entry, quantities and weights](loot-entry-review.csv); [pool-level counts and weight totals](loot-pool-summary.csv). Provisional deletion would empty: `POOL_PILE_MAKESHIFT_GUN`, `POOL_PILE_MED_SYRINGE`, `POOL_WEAPONS`. These must not be shipped empty or silently filled with overpowered substitutes.

| Pool / situation | Suggested decision to review |
|---|---|
| POOL_AMMO | Keep the unselected 9mm and HE rocket entries. Simply deleting the others raises the HE rocket share from 3/69 (4.35%) to 3/23 (13.04%) per weighted draw. One conservative proposal is to put the removed 46 weight into the two retained 9mm entries (+23 each), leaving rocket weight 3, quantities and roll count unchanged. This preserves rocket rarity; it is not applied. |
| POOL_WEAPONS — all nine entries selected | Choose a basic bow/iron sword supply profile, or explicitly choose compatible retained/modpack weapons. Do not automatically substitute unlisted legendary NTM gun variants. Preserve common/rare tiers and roll counts. |
| POOL_PILE_MAKESHIFT_GUN — only entry selected | Choose a basic mundane weapon/salvage drop, or explicitly retire this loot source. Do not leave the pool empty. |
| POOL_PILE_MED_SYRINGE — all three entries selected | Choose retained basic medicine / bottles / paper or retire this medical pile source. Stronger healing or combat buffs are not automatic substitutes. |
| Vaults, meteorite treasure, submarine, red pedestal | Preserve rare-material/progression rewards and their rarity. Suggest replacements entry-by-entry; do not redistribute all removed weight into top-tier remaining loot. |
| Blueprint pools | No blanket edits. Blueprint progression should remain unless an individual disabled recipe or machine makes a specific blueprint obsolete. |

A pool's per-entry weight is relative, and quantities and the number of rolls also determine value. Keep those dimensions in the review; removing rows without rebalancing can make remaining rare loot much more common.

## Confirmed non-recipe grants and dependencies

| Route | Selected item / consequence | Evidence |
|---|---|---|
| UFO death | Grants selected `coin_ufo` to players within 200 blocks, alongside a boss achievement. Preserve achievement IDs; remove/replace the grant only after your decision. | EntityUFO.java:310 |
| BOTPrime worm death | Grants selected `coin_worm` to nearby players with the boss achievement. | EntityBOTPrimeHead.java:144 |
| Uranium-ingot smelting | 1/64 chance of selected `quartz_plutonium`, given directly or dropped if inventory is full. | ModEventHandler.java:1723..1726 |
| Glyphid Brenda / Behemoth drops | Selected gland fluid variants use PHEROMONE / SULFURIC_ACID, not durability. | EntityGlyphidBrenda.java:72; EntityGlyphidBehemoth.java:100 |
| Potion/cow conversion | Selected cheese is also dropped by a potion-driven cow interaction. | HbmPotion.java:165 |
| Nuclear creeper drop — KEEP | Uses `ammo_standard` NUKE_STANDARD, metadata 73, which is intentionally unselected. Do not disable this route by parent ID. | EntityCreeperNuclear.java:86 |
| Crafting achievement criterion | Selected gun_b92 is a criterion in AchievementHandler. That reference is not itself an item reward. Retarget/gate the criterion deliberately without deleting existing player achievement IDs. | AchievementHandler.java:19 |

Recipe, Ore Dictionary, crafting output, reverse/salvage recipe, upgrade, machine process, mob equipment, drop, event reward and consumption-container references are indexed in source-references.csv. Imported structure fixed inventories and native loot/config paths require their own handling; filtering chest pools alone is not an exhaustive grant policy.

## Recommended reversible implementation order

1. Freeze this exact name/metadata/NBT selection and KEEP list in a versioned modpack profile, with stable registry strings. No per-world or live registry mutation: Forge registration is process-wide and startup-based.
2. Keep Java objects/classes, textures and enum ordinals. Centralize the profile/predicates, using small registration/availability hooks so upstream updates cause fewer conflicts. No broad source deletion and no nulling fields that other content still references.
3. Fully unregister independent selected Items only after checking their references. Keep shared parents registered and block selected subtypes in creative/NEI, recipes, machine outputs, grants, drops and practical command/use routes. Inventory hiding alone is not enough.
4. Review input dependencies and recipe replacements with you. Skip selected outputs when approved, but do not silently choose substitute ingredients or delete unselected progression outputs just because they depend on selected content.
5. Agree on loot replacements before editing source defaults or pack overrides. Validate non-empty pools, registered stacks, permitted metadata/NBT and retained rarity/roll budgets. Handle fixed structure inventories separately from random pool rolls.
6. Define existing-save migration: retain backups and deliberate missing-mapping handling. Re-enabling registration restores source availability; it cannot restore stacks already discarded from saved inventories. Do not open production worlds with removed registries before migration behavior is proven on copies.
7. Automated isolated Forge startup plus recipe/loot audit, save/reopen, retained-ammo recipes and use, boss rewards, random smelting grants, mob equipment/drops, and a tiny structure/loot test. Test shared subtype enforcement rather than assuming registration removal handles it.
8. Refresh EFR capabilities/template registry and NTM source/JAR fingerprints/captures as required, then do a small backporter regression. Changes to registered names or the NTM JAR can invalidate current exact-provider and native-capture contracts.

## Where this fits in the previous roadmap

Structure placement is now complete for the selected scope: 39 modern families and 18 enabled legacy entries. Harsh lighting specifically on NTM structures remains an open defect. Before full Earth production conversion, close the content/loot policy, validate the final target pack/registry, test small imports, review remaining conversion limitations and final performance, then commit/release the validated repositories. Broad EFR crafting/feature expansion remains a separate later pass.

**Commit decision:** hold EFR/backporter commits during this audit because NTM registration/loot changes can alter their exact target contracts. No automatic NTM or loot edit is authorized by this report; replacements await your choices. Nothing has been committed.


## Additional captured/config evidence

[Native capture inventory references](captured-inventory-item-references.json)
contains 639 parent-name hits across nine family labels and multiple samples/
rotations: 370 in captured tile payloads and 269 in recorded loot-call evidence.
551 match selected metadata; the remaining parent-name hits must not be banned
by association. These counts are references, not 639 unique items or loot events,
and random loot evidence is not automatically a fixed structure inventory.
Distinguish those paths before rewriting templates or refreshing captures.

[Pack scripts/config reference hits](pack-config-reference-hits.csv) separately
indexes the installed instance's scripts, hbmConfig and hbmRecipes files. An
underscore-prefixed generated template is not treated as an active override.
Loader order/activation and any cross-mod indirect lookups still need validation
in the implementation pass.

Two source aliases were resolved explicitly: registry
`hbm:item.cheese_quesadilla` is `ModItems.quesadilla`, and
`hbm:item.chopper` is `ModItems.spawn_chopper`. Registry names, language keys and
Java field names must not be assumed interchangeable.


The installed `scripts/MineTweakerRecipeMaker.zs` contains **311 matching source
references** to selected registry names. It is an active-script candidate, not an
underscore export. Its recipe removals/additions must be migrated alongside any
true unregistration so MineTweaker does not resolve missing `<hbm:...>` brackets
at load. Source-only NTM edits would miss this installed-pack dependency. Counts
are lexical references, not necessarily 311 distinct recipes or selected items.


Preserve unrelated existing MineTweaker restrictions on unselected types; KEEP
here means do not newly disable/unregister them, not that existing modpack recipe
removals should be undone. The existing shapeless micro-missile recipe uses
`ammo_standard:75`, which is in your KEEP list; it must not be broken by a
parent-wide ammunition registration filter.

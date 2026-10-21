**Checkpoint update:** the restriction route guards and profile-aware decorative dispatch described below have now been implemented and validated. Earlier findings are retained as audit history; replacement items and gun-kit handling remain unapproved/unimplemented. See ../RESTRICTION-GUARDS.json.

# Additional loot routes — review only

These are separate from the 51 ItemPool definitions and are not included in the 132 removed pool-entry count. No replacements or guard fixes applied.

## NTM additions to vanilla/Forge chest pools

Nine selected additions among thirteen raw additions in MainRegistry.java. Proposed weights match the original additions; no new vanilla items. Effective percentages require the full target pack because other mods contribute to these pools.

| Chest category | Removed NTM addition | Original quantity / weight | Suggested NTM replacement | New quantity |
| --- | --- | --- | --- | --- |
| VILLAGE_BLACKSMITH | ShiningArmor™ Armor Polish (hbm:item.armor_polish) | 1–1 / w3 | hbm:item.plate_steel:0 | 1–2 |
| VILLAGE_BLACKSMITH | Toxic Soapy Water (hbm:item.bathwater) | 1–1 / w1 | hbm:item.can_empty:0 | 1–1 |
| MINESHAFT_CORRIDOR | Toxic Soapy Water (hbm:item.bathwater) | 1–1 / w1 | hbm:item.can_empty:0 | 1–1 |
| MINESHAFT_CORRIDOR | Serum (hbm:item.serum) | 1–1 / w5 | hbm:item.cell_empty:0 | 1–1 |
| DUNGEON_CHEST | Heart Piece (hbm:item.heart_piece) | 1–1 / w1 | hbm:item.rag:0 | 1–2 |
| PYRAMID_DESERT_CHEST | Heart Piece (hbm:item.heart_piece) | 1–1 / w1 | hbm:item.rag:0 | 1–2 |
| PYRAMID_JUNGLE_CHEST | Heart Piece (hbm:item.heart_piece) | 1–1 / w1 | hbm:item.rag:0 | 1–2 |
| DUNGEON_CHEST | Bottle of Scrumpy (hbm:item.scrumpy) | 1–1 / w1 | hbm:item.can_empty:0 | 1–1 |
| PYRAMID_DESERT_CHEST | Bottle of Scrumpy (hbm:item.scrumpy) | 1–1 / w1 | hbm:item.can_empty:0 | 1–1 |

## Fixed decorative grants

### LOOT_CAPNUKE / lootCapNuke

Source: src/main/java/com/hbm/util/LootGenerator.java:106

Removed: 2 x Stimpak, arranged as two single-item props

Suggested: 2 x Empty Metal Syringe, same prop positions

Frequency: Fixed within that decorative layout

### LOOT_FLAREGUN / lootFlareGun

Source: src/main/java/com/hbm/util/LootGenerator.java:233

Removed: 1 x Flare Gun

Suggested: 1 x Copper Coil; preserve the existing retained flare ammunition quantities and subtype choices

Frequency: Fixed within that decorative layout

### Red-room Trenchmaster armour branch

Source: src/main/java/com/hbm/blocks/generic/BlockKeyhole.java:216

Removed: Four Trenchmaster armour pieces

Suggested: Two 1x Steel Plate props and two 1x Rag props. Keep the separate retained NCR armour branch probability unchanged. Alternatively leave that branch deliberately empty.

Frequency: Source uses a 1/20 armour branch, then 4/5 Trenchmaster selection (4% overall); NCR branch 1% overall

## Required route hardening

- Apply the no-drop wrapper to selected NTM ChestGenHooks entries; preserve every retained hook.
- Use a filtered nullable stack for pedestal assignment, read/write and interaction. Do not reroll until a kept reward appears.
- Make LootGenerator.applyLoot dispatch explicit after confirming intended behavior. Its existing switch has no breaks and handlers generally check items.isEmpty(); empty results can fall through into unrelated layouts. Keep this separate from loot balancing decisions.
- Revalidate native structure captures after implementation. Existing populated containers retain their stored contents; changing future loot does not reroll them.

This is a bounded review of confirmed paths, not a claim that every dynamic or future grant in NTM has been exhaustively proven safe.

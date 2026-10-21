**Checkpoint update:** the restriction route guards and profile-aware decorative dispatch described below have now been implemented and validated. Earlier findings are retained as audit history; replacement items and gun-kit handling remain unapproved/unimplemented. See ../RESTRICTION-GUARDS.json.

# NTM loot decisions — review only

51 default pools; 31 affected; 132 disabled entries. No substitutions applied; no pack files modified.

No active hbmItemPools.json override. Pack underscore template matches tested defaults. Editing _hbmItemPools.json alone has no effect. Active hbmItemPools.json replaces the entire pool map, so later installation must preserve all 51 pools.

All 51 initialized NTM ItemPool definitions; all 31 affected pools. Source usages include non-structure drops. Separate vanilla ChestGenHooks and direct/decorative grants are noted in the report; no claim all loot routes are covered by these 51 pools. AE2 loot tables are unchanged.

## Recommended policy

Retain every allowed original row, NBT, quantity and weight. Replace each removed row with a modest retained NTM item at the same weight, only after approval. No new vanilla substitutions, no extra nuclear ammunition, no increased blueprint/fuel/rare-circuit weights. Existing vanilla entries remain. Exact suggestions below are provisional, not a ready-to-install config. Recipients of replacement weight become more common; those increases are listed in LOOT-REVIEW.json.

## Important route gaps before rollout

- BlockKeyhole.spawnPedestalItem assigns content.theItemId.copy() directly, bypassing EmptyLootEntry generation. BlockPedestal.TileEntityPedestal read/write currently lacks a BanItems guard. Fix the restricted-item path without rerolling before treating red-room rewards as safe. This is separate from choosing replacement items.
- MainRegistry.java:348–360 registers 13 raw Forge ChestGenHooks entries. Several selected rewards (armor polish, bathwater, serum, heart pieces, scrumpy) bypass the weighted wrapper. Preserve vanilla hooks but guard selected NTM additions before release. Inventory cleanup is not a substitute for correct generation.
- Decorative LootGenerator methods include fixed direct grants as well as pools (for example two Stimpaks in lootCapNuke). Fixed grants need explicit proposals too; they are not part of the pool-weight totals. Existing BlockLoot.addItem filters them. See SUPPLEMENTAL-ROUTES.md for fixed-grant and vanilla-hook suggestions. Do not infer all fixed grants are balanced by this audit.
- All changes affect future generation/first-time deferred loot population; existing populated chests are not automatically rerolled. Refresh EFR/backporter captures and contracts after the approved profile is installed.

## Pool overview

| Pool | Disabled rows / total | Disabled selection weight | No nonzero output in 8 draws | Suggested role |
| --- | ---: | ---: | ---: | --- |
| POOL_PILE_MAKESHIFT_GUN | 1/1 | 100.00% | 100.0000% | Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased. |
| POOL_PILE_MED_SYRINGE | 3/3 | 100.00% | 100.0000% | Decorative medicine piles: all drug entries are banned. Empty metal syringes preserve the medical prop theme without adding healing or buffs. |
| POOL_WEAPONS | 9/9 | 100.00% | 100.0000% | C130 WEAPONS supply drops use this pool (1–2 rolls); it is also embedded in military-base, beached-patrol and crane resources. Proposed conversion is salvage loot across those shared uses. No replacement guns or automatic transfer into retained ammunition. |
| POOL_RED_PEDESTAL | 17/25 | 82.50% | 21.4601% | Special reward room: these mundane electronics are a conservative placeholder proposal, likely underwhelming. Prefer a deliberate reward redesign. Existing rare rewards keep their weights. Direct pedestal assignment also needs a restriction guard before rollout. |
| POOL_PILE_SUPPLIES | 6/7 | 81.25% | 18.9927% | Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased. |
| POOL_SODA | 6/7 | 78.72% | 14.7513% | Vending-machine soda drops: proposed removed drinks become empty cans. Nuka Cola stays at its original 10/47 chance; this is salvage rather than a new beverage. A full drink selection needs another mod or a separate content decision. |
| POOL_SNACKS | 4/5 | 75.00% | 10.0113% | Vending-machine snack drops: raw beans replace three removed foods; one removed chocolate entry becomes a Twinkie. Twinkie rises from 25% to 50%; quantities remain one for it. Confirm that increase. |
| POOL_AMMO | 7/10 | 66.67% | 3.9018% | C130 WEAPONS supply drops use six rolls of this pool. Preserve the intentionally retained ammunition; replace only removed entries with small salvage quantities. |
| POOL_VAULT_UNBREAKABLE | 4/6 | 66.67% | 3.9018% | Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased. |
| POOL_SUPPLIES | 2/8 | 60.34% | 1.7584% | Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased. |
| POOL_VAULT_RUSTY | 6/13 | 53.33% | 0.6546% | Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased. |
| POOL_PILE_GEAR | 6/11 | 47.06% | 0.2405% | Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased. |
| POOL_PILE_MED_PILLS | 1/4 | 37.50% | 0.0391% | Keep RadAway, Rad-X and SIOX weights unchanged; replace removed blood bags with rags rather than making retained medicines more common. |
| POOL_PILE_HIVE | 11/23 | 36.55% | 0.0318% | Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased. |
| POOL_VAULT_STANDARD | 4/11 | 36.36% | 0.0306% | Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased. |
| POOL_VAULT_REINFORCED | 4/12 | 33.33% | 0.0152% | Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased. |
| POOL_METEORITE_TREASURE | 4/19 | 25.48% | 0.0018% | NTM meteorite treasure block, not AE2 meteorite sky-chest loot. Keep starmetal, blueprint and gemstone chances unchanged. |
| POOL_OFFICE_TRASH | 4/17 | 19.23% | 0.0002% | Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased. |
| POOL_EXPENSIVE | 7/30 | 17.44% | 0.0001% | Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased. |
| POOL_VAULT_LOCKERS | 4/26 | 17.28% | 0.0001% | Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased. |
| POOL_PILE_CAPS | 2/3 | 16.67% | 0.0001% | Only normal Nuka caps remain allowed. Proposal adds 1–2 normal caps per formerly banned roll rather than four special caps; expected caps per draw rise from 3.333 to 3.583. |
| POOL_PILE_NUKE_STORAGE | 1/3 | 14.29% | 0.0000% | Retained nuclear ammunition weights stay unchanged. Removed cluster ammunition becomes 1–3 scrap; no extra mini/micro nukes. |
| POOL_VERTIBIRD | 5/22 | 12.50% | 0.0000% | Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased. |
| POOL_GENERIC | 5/30 | 10.92% | 0.0000% | Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased. |
| POOL_SUBMARINE | 2/21 | 9.84% | 0.0000% | Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased. |
| POOL_FILING_CABINET | 2/11 | 3.55% | 0.0000% | Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased. |
| POOL_NUKE_TRASH | 1/11 | 2.78% | 0.0000% | Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased. |
| POOL_ANTENNA | 1/23 | 1.98% | 0.0000% | Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased. |
| POOL_NUKE_MISC | 1/24 | 1.49% | 0.0000% | Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased. |
| POOL_PILE_OF_GARBAGE | 1/23 | 1.22% | 0.0013% | Preserve the old 0–1 quantity range when replacing the removed canned food with an empty can; this pool intentionally retains zero-quantity rolls. |
| POOL_VAULT_LAB | 1/16 | 1.06% | 0.0000% | Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased. |

Eight draws are a comparison scenario, not each structure’s actual roll count. C130 weapons use 1–2 weapon rolls plus six ammo rolls; C130 supplies use five supply rolls. Medicine piles use four syringe rolls plus one pill roll. Vending drops are single draws. q^n cannot be presented as an entire crate’s empty chance when sources mix pools.

## Exact suggestions by pool

### POOL_PILE_MAKESHIFT_GUN

Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased.

Used by: src/main/java/com/hbm/util/LootGenerator.java:147; src/main/resources/assets/hbm/structures/tower_base.nbt; src/main/resources/assets/hbm/structures/military_base.nbt; src/main/resources/assets/hbm/structures/lighthouse.nbt; src/main/resources/assets/hbm/structures/falloutshelter_house.nbt; src/main/resources/assets/hbm/structures/radio_house.nbt; src/main/resources/assets/hbm/structures/test-rot.nbt.

Retained: .

| Removed entry | Old quantity | Weight | Suggested retained NTM replacement | New quantity |
| --- | ---: | ---: | --- | ---: |
| Lever Action Shotgun [hbm:item.gun_maresleg:0] | 1–1 | 10 | Scrap [hbm:item.scrap:0] | 1–3 |

12,000 simulated 27-slot chests, 8 rolls: current empty 100.0%, proposal empty 0.0%; mean items 0.0 -> 14.061. Projection only.

### POOL_PILE_MED_SYRINGE

Decorative medicine piles: all drug entries are banned. Empty metal syringes preserve the medical prop theme without adding healing or buffs.

Used by: src/main/java/com/hbm/util/LootGenerator.java:117; src/main/resources/assets/hbm/structures/military_base.nbt; src/main/resources/assets/hbm/structures/abandoned_arcticstation.nbt; src/main/resources/assets/hbm/structures/falloutshelter_house.nbt; src/main/resources/assets/hbm/structures/laboratory.nbt; src/main/resources/assets/hbm/structures/radio_house.nbt; src/main/resources/assets/hbm/structures/munbase/base.nbt; src/main/resources/assets/hbm/structures/meteor/room10/room-dragon.nbt.

Retained: .

| Removed entry | Old quantity | Weight | Suggested retained NTM replacement | New quantity |
| --- | ---: | ---: | --- | ---: |
| Stimpak [hbm:item.syringe_metal_stimpak:0] | 1–1 | 10 | Metal Syringe [hbm:item.syringe_metal_empty:0] | 1–1 |
| Med-X [hbm:item.syringe_metal_medx:0] | 1–1 | 5 | Metal Syringe [hbm:item.syringe_metal_empty:0] | 1–1 |
| Psycho [hbm:item.syringe_metal_psycho:0] | 1–1 | 5 | Metal Syringe [hbm:item.syringe_metal_empty:0] | 1–1 |

12,000 simulated 27-slot chests, 8 rolls: current empty 100.0%, proposal empty 0.0%; mean items 0.0 -> 7.036. Projection only.

### POOL_WEAPONS

C130 WEAPONS supply drops use this pool (1–2 rolls); it is also embedded in military-base, beached-patrol and crane resources. Proposed conversion is salvage loot across those shared uses. No replacement guns or automatic transfer into retained ammunition.

Used by: src/main/java/com/hbm/entity/logic/EntityC130.java:54; src/main/resources/assets/hbm/structures/military_base.nbt; src/main/resources/assets/hbm/structures/beached_patrol.nbt; src/main/resources/assets/hbm/structures/crane.nbt.

Retained: .

| Removed entry | Old quantity | Weight | Suggested retained NTM replacement | New quantity |
| --- | ---: | ---: | --- | ---: |
| Break-Action Revolver [hbm:item.gun_light_revolver:0] | 1–1 | 100 | Scrap [hbm:item.scrap:0] | 1–3 |
| Lever Action Rifle [hbm:item.gun_henry:0] | 1–1 | 100 | Steel Plate [hbm:item.plate_steel:0] | 1–2 |
| Lever Action Shotgun [hbm:item.gun_maresleg:0] | 1–1 | 100 | Copper Coil [hbm:item.coil_copper:0] | 1–2 |
| Grease Gun [hbm:item.gun_greasegun:0] | 1–1 | 100 | Scrap [hbm:item.scrap:0] | 1–3 |
| Carbine [hbm:item.gun_carbine:0] | 1–1 | 50 | Steel Plate [hbm:item.plate_steel:0] | 1–2 |
| Heavy Revolver [hbm:item.gun_heavy_revolver:0] | 1–1 | 50 | Copper Coil [hbm:item.coil_copper:0] | 1–2 |
| Panzerschreck [hbm:item.gun_panzerschreck:0] | 1–1 | 20 | Scrap [hbm:item.scrap:0] | 1–3 |
| An Old Classic [hbm:item.gun_double_barrel:0] | 1–1 | 10 | Steel Plate [hbm:item.plate_steel:0] | 1–2 |
| N I 4 N I [hbm:item.gun_n_i_4_n_i:0] | 1–1 | 1 | Copper Coil [hbm:item.coil_copper:0] | 1–2 |

12,000 simulated 27-slot chests, 8 rolls: current empty 100.0%, proposal empty 0.0%; mean items 0.0 -> 12.03. Projection only.

### POOL_RED_PEDESTAL

Special reward room: these mundane electronics are a conservative placeholder proposal, likely underwhelming. Prefer a deliberate reward redesign. Existing rare rewards keep their weights. Direct pedestal assignment also needs a restriction guard before rollout.

Used by: src/main/java/com/hbm/blocks/generic/BlockKeyhole.java:239.

Retained: Ace of Spades [hbm:item.card_aos:0] x1–1 (w5); Queen of Spades [hbm:item.card_qos:0] x1–1 (w5); Alexandrite [hbm:item.gem_alexandrite:0] x1–1 (w5); Boxcar [hbm:tile.boxcar:0] x1–1 (w5); The Book of Boxcars [hbm:item.book_of_:0] x1–1 (w5); Folly Part [hbm:item.item_secret:4] x1–1 (w1); Two Nickels [hbm:item.weapon_mod_special:16] x1–1 (w1); Two Gold Doubloons [hbm:item.weapon_mod_special:17] x1–1 (w1).

| Removed entry | Old quantity | Weight | Suggested retained NTM replacement | New quantity |
| --- | ---: | ---: | --- | ---: |
| Ballistic Gauntlet [hbm:item.ballistic_gauntlet:0] | 1–1 | 10 | Vacuum Tube [hbm:item.circuit:0] | 1–1 |
| ShiningArmor™ Armor Polish [hbm:item.armor_polish:0] | 1–1 | 10 | Copper Coil [hbm:item.coil_copper:0] | 1–1 |
| Velvet Band-Aid [hbm:item.bandaid:0] | 1–1 | 10 | Steel Plate [hbm:item.plate_steel:0] | 1–1 |
| Serum [hbm:item.serum:0] | 1–1 | 10 | Vacuum Tube [hbm:item.circuit:0] | 1–1 |
| Plutonic Quartz [hbm:item.quartz_plutonium:0] | 1–1 | 10 | Copper Coil [hbm:item.coil_copper:0] | 1–1 |
| Morning Glory [hbm:item.morning_glory:0] | 1–1 | 10 | Steel Plate [hbm:item.plate_steel:0] | 1–1 |
| Bottle of Spider Milk [hbm:item.spider_milk:0] | 1–1 | 10 | Vacuum Tube [hbm:item.circuit:0] | 1–1 |
| 林 Ink [hbm:item.ink:0] | 1–1 | 10 | Copper Coil [hbm:item.coil_copper:0] | 1–1 |
| Heart Container [hbm:item.heart_container:0] | 1–1 | 10 | Steel Plate [hbm:item.plate_steel:0] | 1–1 |
| Black Diamond [hbm:item.black_diamond:0] | 1–1 | 10 | Vacuum Tube [hbm:item.circuit:0] | 1–1 |
| Bottle of Scrumpy [hbm:item.scrumpy:0] | 1–1 | 10 | Copper Coil [hbm:item.coil_copper:0] | 1–1 |
| Wild Pegasus Dry Whiskey [hbm:item.wild_p:0] | 1–1 | 5 | Steel Plate [hbm:item.plate_steel:0] | 1–1 |
| Starmetal Sword [hbm:item.starmetal_sword:0] | 1–1 | 5 | Vacuum Tube [hbm:item.circuit:0] | 1–1 |
| Health Pipe [hbm:item.crackpipe:0] | 1–1 | 5 | Copper Coil [hbm:item.coil_copper:0] | 1–1 |
| Shield Infusion [hbm:item.flask_infusion:0] | 1–1 | 5 | Steel Plate [hbm:item.plate_steel:0] | 1–1 |
| Hangman [hbm:item.gun_hangman:0] | 1–1 | 1 | Vacuum Tube [hbm:item.circuit:0] | 1–1 |
| South Star [hbm:item.gun_mas36:0] | 1–1 | 1 | Copper Coil [hbm:item.coil_copper:0] | 1–1 |

12,000 simulated 27-slot chests, 8 rolls: current empty 21.192%, proposal empty 0.0%; mean items 1.366 -> 7.036. Projection only.

### POOL_PILE_SUPPLIES

Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased.

Used by: src/main/java/com/hbm/util/LootGenerator.java:296; src/main/resources/assets/hbm/structures/crane_mod.nbt.

Retained: Antidote [hbm:item.syringe_antidote:0] x1–2 (w30).

| Removed entry | Old quantity | Weight | Suggested retained NTM replacement | New quantity |
| --- | ---: | ---: | --- | ---: |
| Grenade [hbm:item.grenade_universal:0] | 3–5 | 10 | Steel Plate [hbm:item.plate_steel:0] | 1–2 |
| Grenade [hbm:item.grenade_universal:0] | 3–5 | 10 | Copper Coil [hbm:item.coil_copper:0] | 1–2 |
| Grenade [hbm:item.grenade_universal:0] | 3–5 | 10 | Scrap [hbm:item.scrap:0] | 1–3 |
| Stimpak [hbm:item.syringe_metal_stimpak:0] | 3–5 | 30 | Metal Syringe [hbm:item.syringe_metal_empty:0] | 1–1 |
| Psycho [hbm:item.syringe_metal_psycho:0] | 3–5 | 30 | Metal Syringe [hbm:item.syringe_metal_empty:0] | 1–1 |
| Ammo Container [hbm:item.ammo_container:0] | 2–3 | 40 | Scrap [hbm:item.scrap:0] | 1–3 |

12,000 simulated 27-slot chests, 8 rolls: current empty 18.575%, proposal empty 0.0%; mean items 2.189 -> 10.34. Projection only.

### POOL_SODA

Vending-machine soda drops: proposed removed drinks become empty cans. Nuka Cola stays at its original 10/47 chance; this is salvage rather than a new beverage. A full drink selection needs another mod or a separate content decision.

Used by: src/main/java/com/hbm/blocks/machine/BlockVendingMachine.java:61.

Retained: Bottle of Nuka Cola [hbm:item.bottle_nuka:0] x1–1 (w10).

| Removed entry | Old quantity | Weight | Suggested retained NTM replacement | New quantity |
| --- | ---: | ---: | --- | ---: |
| Bottle of Nuka Cherry [hbm:item.bottle_cherry:0] | 1–1 | 5 | Empty Can [hbm:item.can_empty:0] | 1–1 |
| Bottle of Nuka Cola Quantum [hbm:item.bottle_quantum:0] | 1–1 | 1 | Empty Can [hbm:item.can_empty:0] | 1–1 |
| Bepis [hbm:item.can_bepis:0] | 1–1 | 10 | Empty Can [hbm:item.can_empty:0] | 1–1 |
| Black Mesa Luna - Dark Cola [hbm:item.can_luna:0] | 1–1 | 10 | Empty Can [hbm:item.can_empty:0] | 1–1 |
| MUG Root Beer [hbm:item.can_mug:0] | 1–1 | 10 | Empty Can [hbm:item.can_empty:0] | 1–1 |
| Dr>Breens Private Reserve [hbm:item.can_breen:0] | 1–1 | 1 | Empty Can [hbm:item.can_empty:0] | 1–1 |

12,000 simulated 27-slot chests, 8 rolls: current empty 14.783%, proposal empty 0.0%; mean items 1.651 -> 7.029. Projection only.

### POOL_SNACKS

Vending-machine snack drops: raw beans replace three removed foods; one removed chocolate entry becomes a Twinkie. Twinkie rises from 25% to 50%; quantities remain one for it. Confirm that increase.

Used by: src/main/java/com/hbm/blocks/machine/BlockVendingMachine.java:62.

Retained: Twinkie [hbm:item.twinkie:0] x1–1 (w10).

| Removed entry | Old quantity | Weight | Suggested retained NTM replacement | New quantity |
| --- | ---: | ---: | --- | ---: |
| MRE [hbm:item.definitelyfood:0] | 1–1 | 10 | Raw Coffee Bean [hbm:item.bean_raw:0] | 1–2 |
| Canned Beef [hbm:item.canned_conserve:0] | 1–1 | 5 | Raw Coffee Bean [hbm:item.bean_raw:0] | 1–2 |
| Astronaut Food Tube [hbm:item.canned_conserve:9] | 1–1 | 5 | Raw Coffee Bean [hbm:item.bean_raw:0] | 1–2 |
| Ithis-Brand Radium Chocolate [hbm:item.chocolate:0] | 1–1 | 10 | Twinkie [hbm:item.twinkie:0] | 1–1 |

12,000 simulated 27-slot chests, 8 rolls: current empty 9.675%, proposal empty 0.0%; mean items 1.939 -> 8.788. Projection only.

### POOL_AMMO

C130 WEAPONS supply drops use six rolls of this pool. Preserve the intentionally retained ammunition; replace only removed entries with small salvage quantities.

Used by: src/main/java/com/hbm/entity/logic/EntityC130.java:55; src/main/resources/assets/hbm/structures/factory.nbt; src/main/resources/assets/hbm/structures/aircraft_carrier.nbt; src/main/resources/assets/hbm/structures/laboratory.nbt; src/main/resources/assets/hbm/structures/crane.nbt; src/main/resources/assets/hbm/structures/radio_house.nbt.

Retained: 9mm Round (Soft Point) [hbm:item.ammo_standard:20] x12–12 (w10); 9mm Round (Full Metal Jacket) [hbm:item.ammo_standard:21] x6–6 (w10); Rocket, High-Explosive [hbm:item.ammo_standard:58] x1–1 (w3).

| Removed entry | Old quantity | Weight | Suggested retained NTM replacement | New quantity |
| --- | ---: | ---: | --- | ---: |
| .357 Magnum Round (Soft Point) [hbm:item.ammo_standard:5] | 12–12 | 10 | Scrap [hbm:item.scrap:0] | 1–2 |
| .357 Magnum Round (Full Metal Jacket) [hbm:item.ammo_standard:6] | 6–6 | 10 | Steel Plate [hbm:item.plate_steel:0] | 1–1 |
| .44 Magnum Round (Soft Point) [hbm:item.ammo_standard:11] | 12–12 | 5 | Copper Coil [hbm:item.coil_copper:0] | 1–1 |
| .44 Magnum Round (Full Metal Jacket) [hbm:item.ammo_standard:12] | 6–6 | 5 | Scrap [hbm:item.scrap:0] | 1–2 |
| 7.62mm Round (Soft Point) [hbm:item.ammo_standard:28] | 6–6 | 5 | Steel Plate [hbm:item.plate_steel:0] | 1–1 |
| 12 Gauge Black Powder Buckshot [hbm:item.ammo_standard:41] | 6–6 | 10 | Copper Coil [hbm:item.coil_copper:0] | 1–1 |
| Ammo Container [hbm:item.ammo_container:0] | 1–1 | 1 | Scrap [hbm:item.scrap:0] | 1–2 |

12,000 simulated 27-slot chests, 8 rolls: current empty 4.133%, proposal empty 0.0%; mean items 20.443 -> 24.134. Projection only.

### POOL_VAULT_UNBREAKABLE

Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased.

Used by: src/main/java/com/hbm/lib/HbmWorldGen.java:438.

Retained: Mini Nuke, Demolition [hbm:item.ammo_standard:74] x2–3 (w1); Military Grade Circuit Board [hbm:item.circuit:10] x6–12 (w1).

| Removed entry | Old quantity | Weight | Suggested retained NTM replacement | New quantity |
| --- | ---: | ---: | --- | ---: |
| Ammo Container [hbm:item.ammo_container:0] | 3–6 | 1 | Scrap [hbm:item.scrap:0] | 1–3 |
| Carbine [hbm:item.gun_carbine:0] | 1–1 | 1 | Steel Plate [hbm:item.plate_steel:0] | 1–2 |
| 7.62mm Round (Depleted Uranium) [hbm:item.ammo_standard:32] | 16–32 | 1 | Copper Coil [hbm:item.coil_copper:0] | 1–2 |
| Congo Lake [hbm:item.gun_congolake:0] | 1–1 | 1 | Steel Plate [hbm:item.plate_steel:0] | 1–2 |

12,000 simulated 27-slot chests, 8 rolls: current empty 3.717%, proposal empty 0.0%; mean items 14.656 -> 21.064. Projection only.

### POOL_SUPPLIES

Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased.

Used by: src/main/java/com/hbm/entity/logic/EntityC130.java:50; src/main/resources/assets/hbm/structures/military_base.nbt; src/main/resources/assets/hbm/structures/submarine.nbt; src/main/resources/assets/hbm/structures/beached_patrol.nbt; src/main/resources/assets/hbm/structures/abandoned_arcticstation.nbt; src/main/resources/assets/hbm/structures/falloutshelter_house.nbt.

Retained: Iodine Pill [hbm:item.pill_iodine:0] x1–2 (w2); Canister: [hbm:item.canister_full:20] x1–4 (w5); Diesel Generator [hbm:tile.machine_diesel:0] x1–1 (w1); Handheld Geiger Counter [hbm:item.geiger_counter:0] x1–1 (w2); First Aid Kit [hbm:item.med_bag:0] x1–1 (w3); RadAway [hbm:item.radaway:0] x1–5 (w10).

| Removed entry | Old quantity | Weight | Suggested retained NTM replacement | New quantity |
| --- | ---: | ---: | --- | ---: |
| MRE [hbm:item.definitelyfood:0] | 3–10 | 25 | Twinkie [hbm:item.twinkie:0] | 1–2 |
| Stimpak [hbm:item.syringe_metal_stimpak:0] | 1–3 | 10 | Cloth [hbm:item.rag:0] | 1–2 |

12,000 simulated 27-slot chests, 8 rolls: current empty 1.7%, proposal empty 0.0%; mean items 6.801 -> 12.622. Projection only.

### POOL_VAULT_RUSTY

Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased.

Used by: src/main/java/com/hbm/world/gen/component/SiloComponent.java:484; src/main/java/com/hbm/lib/HbmWorldGen.java:426; src/main/resources/assets/hbm/structures/military_base.nbt.

Retained: item.ingotGold.name [minecraft:gold_ingot:0] x3–14 (w1); Bobby Pin [hbm:item.pin:0] x8–8 (w1); Cobalt Ingot [hbm:item.ingot_cobalt:0] x4–12 (w1); .50 BMG Round (Full Metal Jacket) [hbm:item.ammo_standard:34] x24–48 (w1); Microchip [hbm:item.circuit:6] x3–6 (w1); M65-Z Gas Mask [hbm:item.gas_mask_m65:0] x1–1 (w1); item.diamond.name [minecraft:diamond:0] x1–2 (w1).

| Removed entry | Old quantity | Weight | Suggested retained NTM replacement | New quantity |
| --- | ---: | ---: | --- | ---: |
| Heavy Revolver [hbm:item.gun_heavy_revolver:0] | 1–1 | 2 | Scrap [hbm:item.scrap:0] | 1–3 |
| .22 Submachine Gun [hbm:item.gun_am180:0] | 1–1 | 1 | Steel Plate [hbm:item.plate_steel:0] | 1–2 |
| Bottle of Nuka Cola Quantum [hbm:item.bottle_quantum:0] | 1–3 | 1 | Empty Can [hbm:item.can_empty:0] | 1–1 |
| 9mm Round (Jacketed Hollow Point) [hbm:item.ammo_standard:22] | 48–64 | 2 | Copper Coil [hbm:item.coil_copper:0] | 1–2 |
| Grenade [hbm:item.grenade_universal:0] | 1–1 | 1 | Steel Plate [hbm:item.plate_steel:0] | 1–2 |
| Grenade [hbm:item.grenade_universal:0] | 1–1 | 1 | Copper Coil [hbm:item.coil_copper:0] | 1–2 |

12,000 simulated 27-slot chests, 8 rolls: current empty 0.633%, proposal empty 0.0%; mean items 33.834 -> 37.036. Projection only.

### POOL_PILE_GEAR

Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased.

Used by: src/main/java/com/hbm/util/LootGenerator.java:283; src/main/resources/assets/hbm/structures/crane_mod.nbt.

Retained: High-Tech Bomb Defusing Device [hbm:item.defuser:0] x1–1 (w40); Screwdriver [hbm:item.screwdriver:0] x1–1 (w30); Small Weapon Steel Casing [hbm:item.casing:2] x1–4 (w30); Small Gunmetal Casing [hbm:item.casing:0] x3–8 (w40); Plastic Shotshell Casing [hbm:item.casing:5] x3–8 (w40).

| Removed entry | Old quantity | Weight | Suggested retained NTM replacement | New quantity |
| --- | ---: | ---: | --- | ---: |
| Stylish Flask [hbm:item.canteen_vodka:0] | 1–1 | 40 | Empty Can [hbm:item.can_empty:0] | 1–1 |
| Canned Beef [hbm:item.canned_conserve:0] | 2–5 | 40 | Raw Coffee Bean [hbm:item.bean_raw:0] | 1–2 |
| Taurun Helmet [hbm:item.taurun_helmet:0] | 1–1 | 20 | Cloth [hbm:item.rag:0] | 1–2 |
| Taurun Chestplate [hbm:item.taurun_plate:0] | 1–1 | 20 | Steel Plate [hbm:item.plate_steel:0] | 1–2 |
| Taurun Leggings [hbm:item.taurun_legs:0] | 1–1 | 20 | Cloth [hbm:item.rag:0] | 1–2 |
| Taurun Boots [hbm:item.taurun_boots:0] | 1–1 | 20 | Steel Plate [hbm:item.plate_steel:0] | 1–2 |

12,000 simulated 27-slot chests, 8 rolls: current empty 0.283%, proposal empty 0.0%; mean items 12.878 -> 16.642. Projection only.

### POOL_PILE_MED_PILLS

Keep RadAway, Rad-X and SIOX weights unchanged; replace removed blood bags with rags rather than making retained medicines more common.

Used by: src/main/java/com/hbm/util/LootGenerator.java:118; src/main/resources/assets/hbm/structures/military_base.nbt; src/main/resources/assets/hbm/structures/abandoned_arcticstation.nbt; src/main/resources/assets/hbm/structures/falloutshelter_house.nbt; src/main/resources/assets/hbm/structures/laboratory.nbt; src/main/resources/assets/hbm/structures/radio_house.nbt; src/main/resources/assets/hbm/structures/munbase/base.nbt; src/main/resources/assets/hbm/structures/meteor/room10/room-dragon.nbt.

Retained: RadAway [hbm:item.radaway:0] x1–1 (w10); Rad-X [hbm:item.radx:0] x1–1 (w10); SiOX Cancer Medication [hbm:item.siox:0] x1–1 (w5).

| Removed entry | Old quantity | Weight | Suggested retained NTM replacement | New quantity |
| --- | ---: | ---: | --- | ---: |
| Blood Bag [hbm:item.iv_blood:0] | 1–1 | 15 | Cloth [hbm:item.rag:0] | 1–2 |

12,000 simulated 27-slot chests, 8 rolls: current empty 0.05%, proposal empty 0.0%; mean items 4.627 -> 8.353. Projection only.

### POOL_PILE_HIVE

Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased.

Used by: src/main/java/com/hbm/util/LootGenerator.java:197; src/main/resources/assets/hbm/structures/crane_mod.nbt; src/main/resources/assets/hbm/structures/test-rot.nbt.

Retained: item.ingotIron.name [minecraft:iron_ingot:0] x1–3 (w10); Steel Ingot [hbm:item.ingot_steel:0] x1–2 (w10); Aluminium Ingot [hbm:item.ingot_aluminium:0] x1–2 (w10); Scrap [hbm:item.scrap:0] x3–6 (w10); M65-Z Gas Mask [hbm:item.gas_mask_m65:0] x1–1 (w10); Steel Chestplate [hbm:item.steel_plate:0] x1–1 (w5); Steel Leggings [hbm:item.steel_legs:0] x1–1 (w5); Steel Pickaxe [hbm:item.steel_pickaxe:0] x1–1 (w5); Steel Shovel [hbm:item.steel_shovel:0] x1–1 (w5); Bottle of Nuka Cola [hbm:item.bottle_nuka:0] x1–2 (w20); Glyphid Egg [hbm:item.egg_glyphid:0] x1–3 (w30); item.expBottle.name [minecraft:experience_bottle:0] x1–3 (w5).

| Removed entry | Old quantity | Weight | Suggested retained NTM replacement | New quantity |
| --- | ---: | ---: | --- | ---: |
| Lever Action Shotgun [hbm:item.gun_maresleg:0] | 1–1 | 5 | Scrap [hbm:item.scrap:0] | 1–3 |
| Break-Action Revolver [hbm:item.gun_light_revolver:0] | 1–1 | 1 | Steel Plate [hbm:item.plate_steel:0] | 1–2 |
| Grenade [hbm:item.grenade_universal:0] | 1–2 | 5 | Cloth [hbm:item.rag:0] | 1–2 |
| Grenade [hbm:item.grenade_universal:0] | 1–2 | 3 | Copper Coil [hbm:item.coil_copper:0] | 1–2 |
| 12 Gauge Buckshot [hbm:item.ammo_standard:44] | 4–4 | 10 | Steel Plate [hbm:item.plate_steel:0] | 1–2 |
| .357 Magnum Round (Soft Point) [hbm:item.ammo_standard:5] | 6–12 | 10 | Scrap [hbm:item.scrap:0] | 1–3 |
| 40mm Grenade, High-Explosive [hbm:item.ammo_standard:53] | 1–1 | 2 | Empty Can [hbm:item.can_empty:0] | 1–1 |
| Bottle of Nuka Cola Quantum [hbm:item.bottle_quantum:0] | 1–2 | 1 | Empty Can [hbm:item.can_empty:0] | 1–1 |
| MRE [hbm:item.definitelyfood:0] | 5–12 | 20 | Raw Coffee Bean [hbm:item.bean_raw:0] | 1–2 |
| Stimpak [hbm:item.syringe_metal_stimpak:0] | 1–1 | 5 | Metal Syringe [hbm:item.syringe_metal_empty:0] | 1–1 |
| Blood Bag [hbm:item.iv_blood:0] | 1–1 | 10 | Cloth [hbm:item.rag:0] | 1–2 |

12,000 simulated 27-slot chests, 8 rolls: current empty 0.025%, proposal empty 0.0%; mean items 8.421 -> 12.026. Projection only.

### POOL_VAULT_STANDARD

Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased.

Used by: src/main/java/com/hbm/lib/HbmWorldGen.java:430.

Retained: Desh Ingot [hbm:item.ingot_desh:0] x2–6 (w1); Desh Blend [hbm:item.powder_desh_mix:0] x1–5 (w1); item.diamond.name [minecraft:diamond:0] x3–6 (w1); Mini Nuke [hbm:item.ammo_standard:73] x1–1 (w1); Yellowcake [hbm:item.powder_yellowcake:0] x16–24 (w1); Vacuum Tube [hbm:item.circuit:0] x12–16 (w1); Microchip [hbm:item.circuit:6] x2–6 (w1).

| Removed entry | Old quantity | Weight | Suggested retained NTM replacement | New quantity |
| --- | ---: | ---: | --- | ---: |
| Ammo Container [hbm:item.ammo_container:0] | 1–1 | 1 | Scrap [hbm:item.scrap:0] | 1–3 |
| Grenade [hbm:item.grenade_universal:0] | 1–1 | 1 | Steel Plate [hbm:item.plate_steel:0] | 1–2 |
| Grenade [hbm:item.grenade_universal:0] | 1–6 | 1 | Copper Coil [hbm:item.coil_copper:0] | 1–2 |
| Uzi [hbm:item.gun_uzi:0] | 1–1 | 1 | Steel Plate [hbm:item.plate_steel:0] | 1–2 |

12,000 simulated 27-slot chests, 8 rolls: current empty 0.025%, proposal empty 0.0%; mean items 33.932 -> 36.377. Projection only.

### POOL_VAULT_REINFORCED

Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased.

Used by: src/main/java/com/hbm/lib/HbmWorldGen.java:434.

Retained: Desh Ingot [hbm:item.ingot_desh:0] x6–16 (w1); Energy Powder [hbm:item.powder_power:0] x1–5 (w1); Satellite ID-Chip [hbm:item.sat_chip:0] x1–1 (w1); item.diamond.name [minecraft:diamond:0] x5–9 (w1); Mini Nuke [hbm:item.ammo_standard:73] x1–3 (w1); Yellowcake [hbm:item.powder_yellowcake:0] x26–42 (w1); Microchip [hbm:item.circuit:6] x18–32 (w1); Integrated Circuit Board [hbm:item.circuit:9] x6–12 (w1).

| Removed entry | Old quantity | Weight | Suggested retained NTM replacement | New quantity |
| --- | ---: | ---: | --- | ---: |
| Ammo Container [hbm:item.ammo_container:0] | 1–4 | 1 | Scrap [hbm:item.scrap:0] | 1–3 |
| Grenade [hbm:item.grenade_universal:0] | 1–2 | 1 | Steel Plate [hbm:item.plate_steel:0] | 1–2 |
| Grenade [hbm:item.grenade_universal:0] | 1–1 | 1 | Copper Coil [hbm:item.coil_copper:0] | 1–2 |
| Heavy Revolver [hbm:item.gun_heavy_revolver:0] | 1–1 | 1 | Steel Plate [hbm:item.plate_steel:0] | 1–2 |

12,000 simulated 27-slot chests, 8 rolls: current empty 0.017%, proposal empty 0.0%; mean items 56.678 -> 57.931. Projection only.

### POOL_METEORITE_TREASURE

NTM meteorite treasure block, not AE2 meteorite sky-chest loot. Keep starmetal, blueprint and gemstone chances unchanged.

Used by: src/main/java/com/hbm/blocks/generic/BlockMeteoriteTreasure.java:25; src/main/resources/assets/hbm/structures/meteor/room10/headloot/loot-fallback.nbt; src/main/resources/assets/hbm/structures/meteor/room10/headloot/loot-chest.nbt.

Retained: Cobalt Pickaxe [hbm:item.cobalt_pickaxe:0] x1–1 (w10); Zirconium Cube [hbm:item.ingot_zirconium:0] x1–16 (w10); Niobium Ingot [hbm:item.ingot_niobium:0] x1–16 (w10); Cobalt Ingot [hbm:item.ingot_cobalt:0] x1–16 (w10); Boron Ingot [hbm:item.ingot_boron:0] x1–16 (w10); Starmetal Ingot [hbm:item.ingot_starmetal:0] x1–1 (w5); Gold Crystals [hbm:item.crystal_gold:0] x1–4 (w10); Vacuum Tube [hbm:item.circuit:0] x4–8 (w10); Microchip [hbm:item.circuit:6] x2–4 (w10); Conserve Crate [hbm:tile.crate_can:0] x1–3 (w10); Herbal Paste [hbm:item.pill_herbal:0] x1–2 (w10); Silo Launch Code Piece [hbm:item.launch_code_piece:0] x1–1 (w5); Glyphid Egg [hbm:item.egg_glyphid:0] x1–1 (w5); Alexandrite [hbm:item.gem_alexandrite:0] x1–1 (w1); Blueprint Booklet [hbm:item.blueprint_folder:1] x1–1 (w1).

| Removed entry | Old quantity | Weight | Suggested retained NTM replacement | New quantity |
| --- | ---: | ---: | --- | ---: |
| MRE [hbm:item.definitelyfood:0] | 16–32 | 25 | Raw Coffee Bean [hbm:item.bean_raw:0] | 1–2 |
| Serum [hbm:item.serum:0] | 1–1 | 5 | Cloth [hbm:item.rag:0] | 1–2 |
| Heart Piece [hbm:item.heart_piece:0] | 1–1 | 5 | Empty Cell [hbm:item.cell_empty:0] | 1–1 |
| Bottle of Scrumpy [hbm:item.scrumpy:0] | 1–1 | 5 | Empty Can [hbm:item.can_empty:0] | 1–1 |

12,000 simulated 27-slot chests, 8 rolls: current empty 0.0%, proposal empty 0.0%; mean items 23.951 -> 25.674. Projection only.

### POOL_OFFICE_TRASH

Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased.

Used by: src/main/java/com/hbm/world/gen/component/OfficeFeatures.java:213; src/main/java/com/hbm/world/gen/component/OfficeFeatures.java:540; src/main/java/com/hbm/world/gen/component/SiloComponent.java:474; src/main/java/com/hbm/world/gen/component/CivilianFeatures.java:958; src/main/resources/assets/hbm/structures/oil_rig.nbt; src/main/resources/assets/hbm/structures/tower_base.nbt; src/main/resources/assets/hbm/structures/crane_mod.nbt; src/main/resources/assets/hbm/structures/submarine.nbt.

Retained: item.paper.name [minecraft:paper:0] x1–12 (w10); item.book.name [minecraft:book:0] x1–3 (w4); Twinkie [hbm:item.twinkie:0] x1–2 (w6); Raw Coffee Bean [hbm:item.bean_raw:0] x1–4 (w4); Tea Seeds [hbm:item.teaseeds:0] x1–4 (w3); Political Topic [hbm:item.flame_politics:0] x1–1 (w2); Ring Pull [hbm:item.ring_pull:0] x1–1 (w4); Empty Can [hbm:item.can_empty:0] x1–1 (w2); Nuka Cola Bottle Cap [hbm:item.cap_nuka:0] x1–16 (w2); Guide Book [hbm:item.book_guide:3] x1–1 (w1); IBM Personal Computer 300PL [hbm:tile.deco_computer:0] x1–1 (w1); Blueprint Booklet [hbm:item.blueprint_folder:0] x1–1 (w1); Vending Machine Token [hbm:item.coin_token:0] x1–1 (w2).

| Removed entry | Old quantity | Weight | Suggested retained NTM replacement | New quantity |
| --- | ---: | ---: | --- | ---: |
| Coffee [hbm:item.coffee:0] | 1–1 | 4 | Raw Coffee Bean [hbm:item.bean_raw:0] | 1–2 |
| 'Creature' Energy Drink [hbm:item.can_creature:0] | 1–2 | 2 | Empty Can [hbm:item.can_empty:0] | 1–1 |
| 'Smart' Energy Drink [hbm:item.can_smart:0] | 1–3 | 2 | Empty Can [hbm:item.can_empty:0] | 1–1 |
| 'Dr. Sugar' Soft Drink [hbm:item.can_mrsugar:0] | 1–2 | 2 | Empty Can [hbm:item.can_empty:0] | 1–1 |

12,000 simulated 27-slot chests, 8 rolls: current empty 0.0%, proposal empty 0.0%; mean items 17.906 -> 19.197. Projection only.

### POOL_EXPENSIVE

Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased.

Used by: src/main/java/com/hbm/world/dungeon/Spaceship.java:631; src/main/java/com/hbm/world/dungeon/DesertAtom002.java:1745; src/main/java/com/hbm/world/dungeon/Barrel.java:157; src/main/java/com/hbm/world/dungeon/Silo.java:394; src/main/java/com/hbm/world/gen/component/OfficeFeatures.java:546; src/main/java/com/hbm/world/gen/component/SiloComponent.java:926; src/main/java/com/hbm/world/gen/component/SiloComponent.java:1223; src/main/resources/assets/hbm/structures/factory.nbt.

Retained: Chlorine Pinwheel [hbm:item.chlorine_pinwheel:0] x1–1 (w1); Vacuum Tube [hbm:item.circuit:0] x1–1 (w4); Analog Circuit Board [hbm:item.circuit:8] x1–1 (w3); Microchip [hbm:item.circuit:6] x1–1 (w2); Gun Oil [hbm:item.gun_kit_1:0] x1–3 (w6); Gun Repair Kit [hbm:item.gun_kit_2:0] x1–2 (w3); Rocket, High-Explosive [hbm:item.ammo_standard:58] x1–4 (w5); 26mm Signal Flare (Supply Airdrop) [hbm:item.ammo_standard:51] x1–1 (w5); 26mm Signal Flare (Weapon Airdrop) [hbm:item.ammo_standard:52] x1–1 (w3); Stealth Device [hbm:item.stealth_boy:0] x1–1 (w2); Lithium-Ion Battery [hbm:item.battery_pack:2] x1–1 (w1); Fusion Core [hbm:item.fusion_core:0] x1–1 (w4); Bottle of Nuka Cola [hbm:item.bottle_nuka:0] x1–3 (w6); Explosive Barrel [hbm:tile.red_barrel:0] x1–1 (w6); Canister: [hbm:item.canister_full:20] x1–2 (w2); Canister: [hbm:item.canister_full:27] x1–2 (w3); M65-Z Gas Mask [hbm:item.gas_mask_m65:60] x1–1 (w5); Airstrike Designator [hbm:item.bomb_caller:0] x1–1 (w2); Airstrike Designator [hbm:item.bomb_caller:1] x1–1 (w1); Airstrike Designator [hbm:item.bomb_caller:2] x1–1 (w1); Gas Mask Filter [hbm:item.gas_mask_filter:0] x1–1 (w4); Silo Launch Code Piece [hbm:item.launch_code_piece:0] x1–1 (w1); Blueprint Booklet [hbm:item.blueprint_folder:1] x1–1 (w1).

| Removed entry | Old quantity | Weight | Suggested retained NTM replacement | New quantity |
| --- | ---: | ---: | --- | ---: |
| Panzerschreck [hbm:item.gun_panzerschreck:0] | 1–1 | 4 | Steel Plate [hbm:item.plate_steel:0] | 1–2 |
| Grenade [hbm:item.grenade_universal:0] | 1–1 | 2 | Copper Coil [hbm:item.coil_copper:0] | 1–2 |
| Grenade [hbm:item.grenade_universal:0] | 1–3 | 3 | Steel Plate [hbm:item.plate_steel:0] | 1–2 |
| Grenade Extra - Triplex [hbm:item.grenade_extra:3] | 1–1 | 1 | Scrap [hbm:item.scrap:0] | 1–3 |
| AWESOME [hbm:item.syringe_awesome:0] | 1–1 | 1 | Metal Syringe [hbm:item.syringe_metal_empty:0] | 1–1 |
| Bottle of Nuka Cola Quantum [hbm:item.bottle_quantum:0] | 1–1 | 3 | Empty Can [hbm:item.can_empty:0] | 1–1 |
| An Old Classic [hbm:item.gun_double_barrel:0] | 1–1 | 1 | Scrap [hbm:item.scrap:0] | 1–3 |

12,000 simulated 27-slot chests, 8 rolls: current empty 0.0%, proposal empty 0.0%; mean items 7.808 -> 9.362. Projection only.

### POOL_VAULT_LOCKERS

Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased.

Used by: src/main/java/com/hbm/world/gen/component/SiloComponent.java:923; src/main/java/com/hbm/world/gen/component/SiloComponent.java:925; src/main/java/com/hbm/world/gen/component/SiloComponent.java:927; src/main/java/com/hbm/world/gen/component/SiloComponent.java:928; src/main/java/com/hbm/world/gen/component/BunkerComponents.java:382; src/main/java/com/hbm/world/gen/component/BunkerComponents.java:383; src/main/java/com/hbm/world/gen/component/BunkerComponents.java:385; src/main/java/com/hbm/world/gen/component/BunkerComponents.java:386.

Retained: Casual Hoodie [hbm:item.robes_helmet:0] x1–1 (w1); Casual Sweater [hbm:item.robes_plate:0] x1–1 (w1); Casual Pants [hbm:item.robes_legs:0] x1–1 (w1); Field Boots [hbm:item.robes_boots:0] x1–1 (w1); Damn Stylish Ballistic Jacket [hbm:item.jackt:0] x1–1 (w1); Damn Stylish Ballistic Jacket 2: Tokyo Drift [hbm:item.jackt2:0] x1–1 (w1); M65-Z Gas Mask [hbm:item.gas_mask_m65:0] x1–1 (w2); Half Mask [hbm:item.gas_mask_mono:0] x1–1 (w2); Protection Goggles [hbm:item.goggles:0] x1–1 (w2); Gas Mask Filter [hbm:item.gas_mask_filter:0] x1–1 (w4); Own Opinion [hbm:item.flame_opinion:0] x1–3 (w5); Conspiracy Theory [hbm:item.flame_conspiracy:0] x1–3 (w5); Political Topic [hbm:item.flame_politics:0] x1–3 (w5); Gun Oil [hbm:item.gun_kit_1:0] x1–1 (w3); Cloth [hbm:item.rag:0] x1–3 (w5); item.paper.name [minecraft:paper:0] x1–6 (w7); item.clock.name [minecraft:clock:0] x1–1 (w3); item.book.name [minecraft:book:0] x1–5 (w10); item.expBottle.name [minecraft:experience_bottle:0] x1–3 (w1); Blueprint Booklet [hbm:item.blueprint_folder:0] x1–1 (w1); Blueprint Booklet [hbm:item.blueprint_folder:1] x1–1 (w1); Vending Machine Token [hbm:item.coin_token:0] x1–1 (w5).

| Removed entry | Old quantity | Weight | Suggested retained NTM replacement | New quantity |
| --- | ---: | ---: | --- | ---: |
| MRE [hbm:item.definitelyfood:0] | 2–7 | 5 | Raw Coffee Bean [hbm:item.bean_raw:0] | 1–2 |
| FFI-Brand Cigarette [hbm:item.cigarette:0] | 1–8 | 5 | Cloth [hbm:item.rag:0] | 1–2 |
| ShiningArmor™ Armor Polish [hbm:item.armor_polish:0] | 1–1 | 3 | Steel Plate [hbm:item.plate_steel:0] | 1–2 |
| Ammo Container [hbm:item.ammo_container:0] | 1–1 | 1 | Scrap [hbm:item.scrap:0] | 1–3 |

12,000 simulated 27-slot chests, 8 rolls: current empty 0.0%, proposal empty 0.0%; mean items 11.099 -> 12.755. Projection only.

### POOL_PILE_CAPS

Only normal Nuka caps remain allowed. Proposal adds 1–2 normal caps per formerly banned roll rather than four special caps; expected caps per draw rise from 3.333 to 3.583.

Used by: src/main/java/com/hbm/util/LootGenerator.java:133; src/main/resources/assets/hbm/structures/crane_mod.nbt; src/main/resources/assets/hbm/structures/duna-comms.nbt; src/main/resources/assets/hbm/structures/meteor/room10/room-dragon.nbt.

Retained: Nuka Cola Bottle Cap [hbm:item.cap_nuka:0] x4–4 (w20).

| Removed entry | Old quantity | Weight | Suggested retained NTM replacement | New quantity |
| --- | ---: | ---: | --- | ---: |
| Nuka Cola Quantum Bottle Cap [hbm:item.cap_quantum:0] | 4–4 | 3 | Nuka Cola Bottle Cap [hbm:item.cap_nuka:0] | 1–2 |
| S~Cola Bottle Cap [hbm:item.cap_sparkle:0] | 4–4 | 1 | Nuka Cola Bottle Cap [hbm:item.cap_nuka:0] | 1–2 |

12,000 simulated 27-slot chests, 8 rolls: current empty 0.0%, proposal empty 0.0%; mean items 23.968 -> 25.206. Projection only.

### POOL_PILE_NUKE_STORAGE

Retained nuclear ammunition weights stay unchanged. Removed cluster ammunition becomes 1–3 scrap; no extra mini/micro nukes.

Used by: src/main/java/com/hbm/util/LootGenerator.java:169.

Retained: Mini Nuke [hbm:item.ammo_standard:73] x1–1 (w50); Mini Nuke, High Yield [hbm:item.ammo_standard:75] x1–1 (w10).

| Removed entry | Old quantity | Weight | Suggested retained NTM replacement | New quantity |
| --- | ---: | ---: | --- | ---: |
| Mini Nuke, Tiny Tots [hbm:item.ammo_standard:76] | 1–1 | 10 | Scrap [hbm:item.scrap:0] | 1–3 |

12,000 simulated 27-slot chests, 8 rolls: current empty 0.0%, proposal empty 0.0%; mean items 6.142 -> 8.049. Projection only.

### POOL_VERTIBIRD

Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased.

Used by: src/main/java/com/hbm/world/gen/component/SiloComponent.java:254; src/main/resources/assets/hbm/structures/crashed-vertibird.nbt; src/main/resources/assets/hbm/structures/aircraft_carrier.nbt; src/main/resources/assets/hbm/structures/vertibird.nbt.

Retained: T-51b Power Armor Helmet [hbm:item.t51_helmet:0] x1–1 (w15); T-51b Power Armor Chestplate [hbm:item.t51_plate:0] x1–1 (w15); T-51b Power Armor Leggings [hbm:item.t51_legs:0] x1–1 (w15); T-51b Power Armor Boots [hbm:item.t51_boots:0] x1–1 (w15); Fusion Core [hbm:item.fusion_core:0] x1–1 (w10); Gun Oil [hbm:item.gun_kit_1:0] x2–3 (w4); 26mm Signal Flare (Weapon Airdrop) [hbm:item.ammo_standard:52] x1–1 (w5); Uranium-235 Rod [hbm:item.rod:6] x1–1 (w2); Uranium Fuel Billet [hbm:item.billet_uranium_fuel:0] x1–1 (w2); Ingot of Uranium Fuel [hbm:item.ingot_uranium_fuel:0] x1–1 (w2); Bottle of Nuka Cola [hbm:item.bottle_nuka:0] x1–3 (w6); Stealth Device [hbm:item.stealth_boy:0] x1–1 (w7); M65-Z Gas Mask [hbm:item.gas_mask_m65:0] x1–1 (w5); Gas Mask Filter [hbm:item.gas_mask_filter:0] x1–1 (w5); Airstrike Designator [hbm:item.bomb_caller:0] x1–1 (w1); Airstrike Designator [hbm:item.bomb_caller:1] x1–1 (w1); Airstrike Designator [hbm:item.bomb_caller:2] x1–1 (w2).

| Removed entry | Old quantity | Weight | Suggested retained NTM replacement | New quantity |
| --- | ---: | ---: | --- | ---: |
| Break-Action Revolver [hbm:item.gun_light_revolver:0] | 1–1 | 4 | Scrap [hbm:item.scrap:0] | 1–3 |
| .357 Magnum Round (Full Metal Jacket) [hbm:item.ammo_standard:6] | 1–24 | 4 | Copper Coil [hbm:item.coil_copper:0] | 1–2 |
| 40mm Grenade, High-Explosive [hbm:item.ammo_standard:53] | 1–6 | 3 | Steel Plate [hbm:item.plate_steel:0] | 1–2 |
| Bottle of Nuka Cola Quantum [hbm:item.bottle_quantum:0] | 1–1 | 3 | Empty Can [hbm:item.can_empty:0] | 1–1 |
| Grenade [hbm:item.grenade_universal:0] | 1–2 | 2 | Steel Plate [hbm:item.plate_steel:0] | 1–2 |

12,000 simulated 27-slot chests, 8 rolls: current empty 0.0%, proposal empty 0.0%; mean items 6.877 -> 8.08. Projection only.

### POOL_GENERIC

Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased.

Used by: src/main/java/com/hbm/world/dungeon/DesertAtom002.java:1153; src/main/java/com/hbm/world/dungeon/DesertAtom002.java:1174; src/main/java/com/hbm/world/dungeon/Silo.java:1646; src/main/java/com/hbm/world/gen/component/CivilianFeatures.java:123; src/main/java/com/hbm/world/gen/component/CivilianFeatures.java:400; src/main/java/com/hbm/world/gen/component/CivilianFeatures.java:959.

Retained: item.bread.name [minecraft:bread:0] x1–5 (w8); Twinkie [hbm:item.twinkie:0] x1–3 (w6); item.ingotIron.name [minecraft:iron_ingot:0] x2–6 (w10); Steel Ingot [hbm:item.ingot_steel:0] x2–5 (w7); Beryllium Ingot [hbm:item.ingot_beryllium:0] x1–2 (w4); Titanium Ingot [hbm:item.ingot_titanium:0] x1–1 (w3); Vacuum Tube [hbm:item.circuit:0] x1–1 (w5); 26mm Signal Flare (Supply Airdrop) [hbm:item.ammo_standard:51] x1–1 (w1); Gun Oil [hbm:item.gun_kit_1:0] x1–3 (w4); Small Gunmetal Casing [hbm:item.casing:0] x4–10 (w3); Black Powder Shotshell Casing [hbm:item.casing:4] x4–10 (w3); Cordite [hbm:item.cordite:0] x4–6 (w5); Redstone Battery [hbm:item.battery_pack:0] x1–1 (w1); Scrap [hbm:item.scrap:0] x1–3 (w10); Dust [hbm:item.dust:0] x2–4 (w9); Hbm's Own Self-Made Bottle Opener [hbm:item.bottle_opener:0] x1–1 (w2); Bottle of Nuka Cola [hbm:item.bottle_nuka:0] x1–3 (w4); Stealth Device [hbm:item.stealth_boy:0] x1–1 (w1); Nuka Cola Bottle Cap [hbm:item.cap_nuka:0] x1–15 (w7); Canister: [hbm:item.canister_full:20] x1–2 (w2); Canister: [hbm:item.canister_full:27] x1–2 (w3); M65-Z Gas Mask [hbm:item.gas_mask_m65:60] x1–1 (w2); Gas Mask Filter [hbm:item.gas_mask_filter:0] x1–1 (w3); Blueprint Booklet [hbm:item.blueprint_folder:0] x1–1 (w1); Vending Machine Token [hbm:item.coin_token:0] x1–1 (w2).

| Removed entry | Old quantity | Weight | Suggested retained NTM replacement | New quantity |
| --- | ---: | ---: | --- | ---: |
| Break-Action Revolver [hbm:item.gun_light_revolver:0] | 1–1 | 3 | Scrap [hbm:item.scrap:0] | 1–3 |
| .357 Magnum Round (Soft Point) [hbm:item.ammo_standard:5] | 2–6 | 4 | Steel Plate [hbm:item.plate_steel:0] | 1–2 |
| 12 Gauge Black Powder Buckshot [hbm:item.ammo_standard:41] | 3–6 | 3 | Copper Coil [hbm:item.coil_copper:0] | 1–2 |
| Lever Action Shotgun [hbm:item.gun_maresleg:0] | 1–1 | 1 | Scrap [hbm:item.scrap:0] | 1–3 |
| Bottle of Nuka Cherry [hbm:item.bottle_cherry:0] | 1–1 | 2 | Empty Can [hbm:item.can_empty:0] | 1–1 |

12,000 simulated 27-slot chests, 8 rolls: current empty 0.0%, proposal empty 0.0%; mean items 19.226 -> 20.155. Projection only.

### POOL_SUBMARINE

Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased.

Used by: src/main/resources/assets/hbm/structures/nuke-sub.nbt.

Retained: Uranium-235 Nugget [hbm:item.nugget_u235:0] x3–12 (w5); Plutonium-238 Nugget [hbm:item.nugget_pu238:0] x3–12 (w5); Radium-226 Nugget [hbm:item.nugget_ra226:0] x3–6 (w5); Oxygen Pinwheel [hbm:item.oxy_pinwheel:0] x1–1 (w1); M65-Z Gas Mask [hbm:item.gas_mask_m65:0] x1–1 (w2); Half Mask [hbm:item.gas_mask_mono:0] x1–1 (w2); Gas Mask Filter [hbm:item.gas_mask_filter:0] x1–1 (w2); Iodine Powder [hbm:item.powder_iodine:0] x1–1 (w1); Bromine Powder [hbm:item.powder_bromine:0] x1–2 (w1); Hazmat Kit [hbm:item.hazmat_kit:0] x1–1 (w2); Radioactive Barrel [hbm:tile.yellow_barrel:0] x1–3 (w3); Neptunium Powder [hbm:item.powder_neptunium:0] x1–1 (w1); MEU PWR Fuel Rod [hbm:item.pwr_fuel:0] x1–3 (w3); MEP PWR Fuel Rod [hbm:item.pwr_fuel:6] x1–3 (w3); HEU-235 PWR Fuel Rod [hbm:item.pwr_fuel:2] x1–3 (w4); item.paper.name [minecraft:paper:0] x1–6 (w5); item.clock.name [minecraft:clock:0] x1–1 (w3); item.compass.name [minecraft:compass:0] x1–1 (w3); item.book.name [minecraft:book:0] x1–5 (w4).

| Removed entry | Old quantity | Weight | Suggested retained NTM replacement | New quantity |
| --- | ---: | ---: | --- | ---: |
| Carbine [hbm:item.gun_carbine:0] | 1–1 | 2 | Steel Plate [hbm:item.plate_steel:0] | 1–2 |
| 7.62mm Round (Full Metal Jacket) [hbm:item.ammo_standard:29] | 8–32 | 4 | Copper Coil [hbm:item.coil_copper:0] | 1–2 |

12,000 simulated 27-slot chests, 8 rolls: current empty 0.0%, proposal empty 0.0%; mean items 20.134 -> 20.916. Projection only.

### POOL_FILING_CABINET

Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased.

Used by: src/main/java/com/hbm/world/gen/component/OfficeFeatures.java:532; src/main/java/com/hbm/world/gen/component/OfficeFeatures.java:533; src/main/java/com/hbm/world/gen/component/OfficeFeatures.java:534; src/main/java/com/hbm/world/gen/component/OfficeFeatures.java:535; src/main/java/com/hbm/world/gen/component/OfficeFeatures.java:536; src/main/java/com/hbm/world/gen/component/OfficeFeatures.java:537; src/main/java/com/hbm/world/gen/component/OfficeFeatures.java:538; src/main/java/com/hbm/world/gen/component/OfficeFeatures.java:544.

Retained: item.paper.name [minecraft:paper:0] x1–12 (w240); item.book.name [minecraft:book:0] x1–3 (w90); item.emptyMap.name [minecraft:map:0] x1–1 (w50); item.writingBook.name [minecraft:writable_book:0] x1–1 (w30); Dust [hbm:item.dust:0] x1–1 (w40); Tiny Pile of Dust [hbm:item.dust_tiny:0] x1–3 (w75); Screwdriver [hbm:item.screwdriver:0] x1–1 (w10); Blueprint Booklet [hbm:item.blueprint_folder:0] x1–1 (w5); Vending Machine Token [hbm:item.coin_token:0] x1–1 (w30).

| Removed entry | Old quantity | Weight | Suggested retained NTM replacement | New quantity |
| --- | ---: | ---: | --- | ---: |
| FFI-Brand Cigarette [hbm:item.cigarette:0] | 1–16 | 20 | Cloth [hbm:item.rag:0] | 1–2 |
| 林 Ink [hbm:item.ink:0] | 1–1 | 1 | Dust [hbm:item.dust:0] | 1–1 |

12,000 simulated 27-slot chests, 8 rolls: current empty 0.0%, proposal empty 0.0%; mean items 24.574 -> 24.862. Projection only.

### POOL_NUKE_TRASH

Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased.

Used by: src/main/java/com/hbm/world/dungeon/DesertAtom002.java:1366; src/main/java/com/hbm/world/gen/component/SiloComponent.java:1220; src/main/java/com/hbm/world/gen/component/CivilianFeatures.java:574; src/main/resources/assets/hbm/structures/nuke-sub.nbt; src/main/resources/assets/hbm/structures/dresbmk/incap.nbt.

Retained: Uranium-238 Nugget [hbm:item.nugget_u238:0] x3–12 (w5); Plutonium-240 Nugget [hbm:item.nugget_pu240:0] x3–8 (w5); Neptunium Nugget [hbm:item.nugget_neptunium:0] x1–4 (w3); Uranium-238 Rod [hbm:item.rod:8] x1–1 (w3); Uranium-238 Dual Rod [hbm:item.rod_dual:8] x1–1 (w3); Uranium-238 Quad Rod [hbm:item.rod_quad:8] x1–1 (w3); M65-Z Gas Mask [hbm:item.gas_mask_m65:60] x1–1 (w5); Hazmat Kit [hbm:item.hazmat_kit:0] x1–1 (w1); Gas Mask Filter [hbm:item.gas_mask_filter:0] x1–1 (w5); Radioactive Barrel [hbm:tile.yellow_barrel:0] x1–1 (w2).

| Removed entry | Old quantity | Weight | Suggested retained NTM replacement | New quantity |
| --- | ---: | ---: | --- | ---: |
| Bottle of Nuka Cola Quantum [hbm:item.bottle_quantum:0] | 1–1 | 1 | Empty Can [hbm:item.can_empty:0] | 1–1 |

12,000 simulated 27-slot chests, 8 rolls: current empty 0.0%, proposal empty 0.0%; mean items 18.457 -> 18.568. Projection only.

### POOL_ANTENNA

Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased.

Used by: src/main/java/com/hbm/world/dungeon/Antenna.java:92; src/main/java/com/hbm/world/gen/component/BunkerComponents.java:152; src/main/java/com/hbm/world/gen/component/CivilianFeatures.java:262; src/main/resources/assets/hbm/structures/aircraft_carrier.nbt.

Retained: Twinkie [hbm:item.twinkie:0] x1–3 (w4); Steel Ingot [hbm:item.ingot_steel:0] x1–2 (w7); Minecraft Grade Copper Ingot [hbm:item.ingot_red_copper:0] x1–1 (w4); Titanium Ingot [hbm:item.ingot_titanium:0] x1–3 (w5); %s Wire [hbm:item.wire_fine:31] x2–3 (w7); Vacuum Tube [hbm:item.circuit:0] x1–1 (w4); Capacitor [hbm:item.circuit:1] x1–1 (w2); Redstone Battery [hbm:item.battery_pack:0] x1–1 (w1); Iodine Powder [hbm:item.powder_iodine:0] x1–1 (w1); Bromine Powder [hbm:item.powder_bromine:0] x1–1 (w1); Steel Pole [hbm:tile.steel_poles:0] x1–4 (w8); Steel Scaffold [hbm:tile.steel_scaffold:0] x1–3 (w8); Antenna Top [hbm:tile.pole_top:0] x1–1 (w4); Satellite Dish [hbm:tile.pole_satellite_receiver:0] x1–1 (w7); Scrap [hbm:item.scrap:0] x1–3 (w10); Dust [hbm:item.dust:0] x2–4 (w9); Hbm's Own Self-Made Bottle Opener [hbm:item.bottle_opener:0] x1–1 (w2); Bottle of Nuka Cola [hbm:item.bottle_nuka:0] x1–3 (w4); Stealth Device [hbm:item.stealth_boy:0] x1–1 (w1); Nuka Cola Bottle Cap [hbm:item.cap_nuka:0] x1–15 (w7); Airstrike Designator [hbm:item.bomb_caller:0] x1–1 (w1); Gas Mask Filter [hbm:item.gas_mask_filter:0] x1–1 (w2).

| Removed entry | Old quantity | Weight | Suggested retained NTM replacement | New quantity |
| --- | ---: | ---: | --- | ---: |
| Bottle of Nuka Cherry [hbm:item.bottle_cherry:0] | 1–1 | 2 | Empty Can [hbm:item.can_empty:0] | 1–1 |

12,000 simulated 27-slot chests, 8 rolls: current empty 0.0%, proposal empty 0.0%; mean items 15.59 -> 15.649. Projection only.

### POOL_NUKE_MISC

Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased.

Used by: src/main/java/com/hbm/world/dungeon/DesertAtom001.java:194; src/main/java/com/hbm/world/dungeon/DesertAtom003.java:443; src/main/resources/assets/hbm/structures/dresbmk/hall/starbmk.nbt.

Retained: Uranium-235 Nugget [hbm:item.nugget_u235:0] x3–12 (w5); Plutonium-238 Nugget [hbm:item.nugget_pu238:0] x3–12 (w5); Radium-226 Nugget [hbm:item.nugget_ra226:0] x3–6 (w5); Uranium-235 Rod [hbm:item.rod:6] x1–1 (w3); Uranium-235 Dual Rod [hbm:item.rod_dual:6] x1–1 (w3); Uranium-235 Quad Rod [hbm:item.rod_quad:6] x1–1 (w3); ZIRNOX Uranium Fuel Rod [hbm:item.rod_zirnox:1] x1–1 (w4); ZIRNOX MOX Fuel Rod [hbm:item.rod_zirnox:4] x1–1 (w4); ZIRNOX Lithium Rod [hbm:item.rod_zirnox:9] x1–1 (w3); ZIRNOX Thorium Fuel Rod [hbm:item.rod_zirnox:3] x1–1 (w3); Thorium Fuel Dual Rod [hbm:item.rod_dual:5] x1–1 (w3); ZIRNOX Tritium Rod [hbm:item.rod_zirnox_tritium:0] x1–1 (w1); ZIRNOX Uranium-233 Fuel Rod [hbm:item.rod_zirnox:6] x1–1 (w1); ZIRNOX Uranium-235 Fuel Rod [hbm:item.rod_zirnox:7] x1–1 (w1); Plutonium-238 RTG Pellet [hbm:item.pellet_rtg:0] x1–1 (w3); Thorium Powder [hbm:item.powder_thorium:0] x1–1 (w1); Neptunium Powder [hbm:item.powder_neptunium:0] x1–1 (w1); Strontium Powder [hbm:item.powder_strontium:0] x1–1 (w1); Cobalt Powder [hbm:item.powder_cobalt:0] x1–1 (w1); M65-Z Gas Mask [hbm:item.gas_mask_m65:60] x1–1 (w5); Hazmat Kit [hbm:item.hazmat_kit:0] x1–1 (w2); Gas Mask Filter [hbm:item.gas_mask_filter:0] x1–1 (w5); Radioactive Barrel [hbm:tile.yellow_barrel:0] x1–3 (w3).

| Removed entry | Old quantity | Weight | Suggested retained NTM replacement | New quantity |
| --- | ---: | ---: | --- | ---: |
| Bottle of Nuka Cola Quantum [hbm:item.bottle_quantum:0] | 1–1 | 1 | Empty Can [hbm:item.can_empty:0] | 1–1 |

12,000 simulated 27-slot chests, 8 rolls: current empty 0.0%, proposal empty 0.0%; mean items 15.865 -> 15.988. Projection only.

### POOL_PILE_OF_GARBAGE

Preserve the old 0–1 quantity range when replacing the removed canned food with an empty can; this pool intentionally retains zero-quantity rolls.

Used by: src/main/java/com/hbm/util/LootGenerator.java:257; src/main/resources/assets/hbm/structures/ntmruinsJ.nbt; src/main/resources/assets/hbm/structures/ntmruinsH.nbt; src/main/resources/assets/hbm/structures/military_base.nbt; src/main/resources/assets/hbm/structures/crane_mod.nbt; src/main/resources/assets/hbm/structures/abandoned_arcticstation.nbt; src/main/resources/assets/hbm/structures/ntmruinsB.nbt; src/main/resources/assets/hbm/structures/ntmruinsC.nbt.

Retained: %s Pipe [hbm:item.pipe:2600] x0–2 (w20); Scrap [hbm:item.scrap:0] x1–5 (w20); Dust [hbm:item.dust:0] x1–3 (w40); Tiny Pile of Dust [hbm:item.dust_tiny:0] x1–7 (w40); Cement [hbm:item.powder_cement:0] x1–6 (w40); Lead Nugget [hbm:item.nugget_lead:0] x0–3 (w20); %s Wire [hbm:item.wire_fine:8200] x1–2 (w20); Wood Ash [hbm:item.powder_ash:0] x0–1 (w15); Lead Plate [hbm:item.plate_lead:0] x0–1 (w15); item.string.name [minecraft:string:0] x0–1 (w15); %s Bolt [hbm:item.bolt:8200] x0–2 (w15); Bobby Pin [hbm:item.pin:0] x0–2 (w15); Nuka Cola Bottle Cap [hbm:item.cap_nuka:0] x0–8 (w15); Iron Plate [hbm:item.plate_iron:0] x0–2 (w15); Pile of Fallout [hbm:item.fallout:0] x0–2 (w15); Heating Coil [hbm:item.coil_tungsten:0] x0–2 (w15); Empty Can [hbm:item.can_empty:0] x0–1 (w15); Asbestos Sheet [hbm:item.ingot_asbestos:0] x0–1 (w15); Metal Syringe [hbm:item.syringe_metal_empty:0] x0–1 (w15); Empty Syringe [hbm:item.syringe_empty:0] x0–1 (w15); The Manual Override [hbm:item.weapon_pipe_lead:0] x0–1 (w5); Motor [hbm:item.motor:0] x0–1 (w5).

| Removed entry | Old quantity | Weight | Suggested retained NTM replacement | New quantity |
| --- | ---: | ---: | --- | ---: |
| Canned Mystery Meat [hbm:item.canned_conserve:2] | 0–1 | 5 | Empty Can [hbm:item.can_empty:0] | 0–1 |

12,000 simulated 27-slot chests, 8 rolls: current empty 0.025%, proposal empty 0.017%; mean items 12.28 -> 12.321. Projection only.

### POOL_VAULT_LAB

Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased.

Used by: src/main/java/com/hbm/world/gen/component/SiloComponent.java:470; src/main/java/com/hbm/world/gen/component/SiloComponent.java:1025; src/main/java/com/hbm/world/gen/component/BunkerComponents.java:709; src/main/resources/assets/hbm/structures/duna-comms.nbt; src/main/resources/assets/hbm/structures/aircraft_carrier.nbt; src/main/resources/assets/hbm/structures/munbase/base.nbt.

Retained: Blowtorch [hbm:item.blowtorch:0] x1–1 (w4); Laboratory Glassware [hbm:item.chemistry_set:0] x1–1 (w15); Screwdriver [hbm:item.screwdriver:0] x1–1 (w10); Tiny Drop of Mercury [hbm:item.nugget_mercury_tiny:0] x1–1 (w3); Activated Carbon Filter [hbm:item.filter_coal:0] x1–1 (w5); Dust [hbm:item.dust:0] x1–3 (w25); item.paper.name [minecraft:paper:0] x1–2 (w15); Empty Cell [hbm:item.cell_empty:0] x1–1 (w5); item.glassBottle.name [minecraft:glass_bottle:0] x1–1 (w5); Iodine Powder [hbm:item.powder_iodine:0] x1–1 (w1); Bromine Powder [hbm:item.powder_bromine:0] x1–1 (w1); Cobalt Powder [hbm:item.powder_cobalt:0] x1–1 (w1); Neodymium Powder [hbm:item.powder_neodymium:0] x1–1 (w1); Boron Powder [hbm:item.powder_boron:0] x1–1 (w1); Blueprint Booklet [hbm:item.blueprint_folder:1] x1–1 (w1).

| Removed entry | Old quantity | Weight | Suggested retained NTM replacement | New quantity |
| --- | ---: | ---: | --- | ---: |
| Morning Glory [hbm:item.morning_glory:0] | 1–1 | 1 | Empty Cell [hbm:item.cell_empty:0] | 1–1 |

12,000 simulated 27-slot chests, 8 rolls: current empty 0.0%, proposal empty 0.0%; mean items 9.394 -> 9.447. Projection only.

## Validation

Disposable Forge profile checks passed (9,767 enabled; 645 disabled). Runtime names/metadata classify the exact current profile. Draft validation confirms every suggested item is a retained registered NTM identity present in an existing pool, quantities fit its stack size, no replacement uses banned metadata, and each pool total weight stays unchanged. Exported native default loot files remain identical between profile modes. No progression-cost equivalence or actual in-game chest test has been claimed.

Lost weight is per-draw selection probability. q^8 is exact probability of no nonzero output in eight independent draws, not a guarantee for real inventories. 27-slot simulation includes zero counts, stack splitting and slot overwrites; actual containers, counts and multi-pool mixtures differ. These are projections, not full progression balance proof.

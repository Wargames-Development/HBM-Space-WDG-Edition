**Checkpoint update:** the restriction route guards and profile-aware decorative dispatch described below have now been implemented and validated. Earlier findings are retained as audit history; replacement items and gun-kit handling remain unapproved/unimplemented. See ../RESTRICTION-GUARDS.json.

# Release Testing: cross-mod loot suggestions

Review draft only. No loot changes, commits, or gun-kit implementation applied. This supersedes the earlier NTM-only replacement suggestions as an alternative for discussion, not approval.

## What was checked

Installed pack includes FMUR X170 with the BFMC Gun Pack, HarvestCraft, AE2, EFR+, NTM and other mods. Proposed IDs exist in the saved Backport Template Earth item registry. Gun/ammo relationships and magazine capacity are from the actual installed content text files. AE2 metadata comes from the installed jar bytecode, not enum ordinal guesses. Matching an ID in a saved registry is not a live firing test.

Version detail: Fresh Water and Bubbly Water are plain crafting ingredients in this HarvestCraft jar, so they are not proposed as drinks. Actual juice/tea/coffee/soda items are used. All prepared food outcomes in this draft grant one item; nutrition and potion effects still need an in-pack balance check.

## Recommended distribution

| Location | Suggested additions | Quantity per selected outcome |
| --- | --- | --- |
| Offices, houses, filing cabinets | HarvestCraft coffee, tea, juice, Trail Mix; rags and occasional chem light/candle | 1 drink or 1–2 small supplies |
| Factories, labs, antennas | Existing NTM materials plus raw AE2 Certus, Certus Dust, Silicon; occasional EFR lantern | 1–2 materials or 1 lantern |
| Military sites, carrier/aircraft wrecks, armories | Rare FMUR gun kits, matching ordinary magazines, jerky and chem lights | 1 gun + a small matching ammo supply; loose magazines 1 |
| Bunkers, shelters, supply drops | HarvestCraft jerky/Trail Mix/juice, NTM rags and empty syringes; modest military supplies in weapon-specific pools | 1–2 food/supplies |
| Soda/snack vending | HarvestCraft sodas, juice, Lemon-Lime Soda, Trail Mix, jerky, sandwich, Cheese on Toast | 1 consumable |
| Locked high-tier vaults | Standard M4/AK kit possibilities and small Fluix/Silicon drops; original rare weights unchanged | 1 kit or 1–2 AE2 materials |

## Verified FMUR pairs

One magazine is an item containing multiple rounds, not one bullet. Start magazines at damage 0. Proposed kits contain one unloaded gun plus the matching items below; loaded-gun NBT is an alternative requiring separate validation.

| Gun | Registry ID | Matching ammunition ID | Proposed ammo with gun |
| --- | --- | --- | --- |
| M9 | `flansmod:m9` | `flansmod:9x1915rdmag` | 1 item(s), 15 rounds |
| M45A1 | `flansmod:m45a1` | `flansmod:45acp8rdmag` | 1 item(s), 8 rounds |
| 870 MCS | `flansmod:870mcs` | `flansmod:12gaugebuckshot` | 6 item(s), 6 rounds |
| M4A1 | `flansmod:m4a1` | `flansmod:556x4530rdmag` | 1 item(s), 30 rounds |
| AK-74M | `flansmod:ak74m` | `flansmod:545x3930rdmag` | 1 item(s), 30 rounds |
| MP5SD | `flansmod:mp5sd` | `flansmod:9x1930rdmag` | 1 item(s), 30 rounds |

The 870 MCS has seven ammo slots; the proposed six ordinary buckshot shells are loose ammunition, not six magazines. MP5SD is the rarer suppressed option. No attachments or damage/wear randomization are assumed.

**Implementation requirement:** a normal ItemPool JSON entry returns one ItemStack. It cannot grant a gun plus separate ammo stacks atomically. These kit suggestions require small paired-loot support or validated preloaded-gun NBT; otherwise independent weapon/ammo rolls can mismatch. Do not paste this draft into hbmItemPools.json.

## Complete proposed replacement budgets

Weights below divide only the old removed entries’ combined weight. They may split/merge those removed rows, but each pool keeps its original total weight. Every allowed original row remains as-is. Percentages are per draw from that entire pool.

### POOL_OFFICE_TRASH

Replacement budget: **10 of 52 total weight**. Suggested gun-kit chance: **0.0% per draw**.

| New outcome | Weight | Chance in full pool |
| --- | ---: | ---: |
| 1 x Cup of Coffee (`harvestcraft:coffeeItem:0) | 4 | 7.692% |
| 1 x Cup of Tea (`harvestcraft:teaItem:0) | 2 | 3.846% |
| 1 x Apple Juice (`harvestcraft:applejuiceItem:0) | 2 | 3.846% |
| 1 x Trail Mix (`harvestcraft:trailmixItem:0) | 2 | 3.846% |

### POOL_FILING_CABINET

Replacement budget: **21 of 591 total weight**. Suggested gun-kit chance: **0.0% per draw**.

| New outcome | Weight | Chance in full pool |
| --- | ---: | ---: |
| 1–2 x Rag (`hbm:item.rag:0) | 15 | 2.538% |
| 1–2 x Green Chem Light (`flansmod:chemlight_green:0) | 5 | 0.846% |
| 1 x Candle (`etfuturum:candle:0) | 1 | 0.169% |

### POOL_ANTENNA

Replacement budget: **2 of 101 total weight**. Suggested gun-kit chance: **0.0% per draw**.

| New outcome | Weight | Chance in full pool |
| --- | ---: | ---: |
| 1 x Cup of Coffee (`harvestcraft:coffeeItem:0) | 1 | 0.990% |
| 1–2 x Certus Quartz Dust (`appliedenergistics2:item.ItemMultiMaterial:2) | 1 | 0.990% |

### POOL_GENERIC

Replacement budget: **13 of 119 total weight**. Suggested gun-kit chance: **2.521% per draw**.

| New outcome | Weight | Chance in full pool |
| --- | ---: | ---: |
| M9 + 1 x 9x19mm 15-Round Magazine (15 rounds) | 2 | 1.681% |
| 870 MCS + 6 x 12 Gauge BuckShot (6 rounds) | 1 | 0.840% |
| 1 x 9x19mm 15-round Magazine (`flansmod:9x1915rdmag:0) | 3 | 2.521% |
| 4–8 x 12 Gauge Buckshot (`flansmod:12gaugebuckshot:0) | 2 | 1.681% |
| 1 x Beef Jerky (`harvestcraft:beefjerkyItem:0) | 3 | 2.521% |
| 1 x Apple Juice (`harvestcraft:applejuiceItem:0) | 2 | 1.681% |

Paired kit handling needed. Independent ammo rolls do not guarantee compatibility; keep loose-magazine rows separate from guaranteed kit ammunition.

Gun kit rate 3/119 (~2.52%) per pool draw. Eight independent draws mean ~18.48% chance of at least one kit selection before slot overwrite or multi-item bundle insertion; not a promised final chest rate.

### POOL_VAULT_RUSTY

Replacement budget: **8 of 15 total weight**. Suggested gun-kit chance: **6.667% per draw**.

| New outcome | Weight | Chance in full pool |
| --- | ---: | ---: |
| 1 x Beef Jerky (`harvestcraft:beefjerkyItem:0) | 3 | 20.000% |
| 1–2 x Steel Plate (`hbm:item.plate_steel:0) | 2 | 13.333% |
| 1–2 x Certus Quartz Dust (`appliedenergistics2:item.ItemMultiMaterial:2) | 1 | 6.667% |
| M9 + 1 x 9x19mm 15-Round Magazine (15 rounds) | 1 | 6.667% |
| 1 x 9x19mm 15-round Magazine (`flansmod:9x1915rdmag:0) | 1 | 6.667% |

Paired kit handling needed. Independent ammo rolls do not guarantee compatibility; keep loose-magazine rows separate from guaranteed kit ammunition.

### POOL_VAULT_STANDARD

Replacement budget: **4 of 11 total weight**. Suggested gun-kit chance: **9.091% per draw**.

| New outcome | Weight | Chance in full pool |
| --- | ---: | ---: |
| M9 + 1 x 9x19mm 15-Round Magazine (15 rounds) | 1 | 9.091% |
| 1 x 5.56x45mm 30-round Magazine (`flansmod:556x4530rdmag:0) | 1 | 9.091% |
| 1–2 x Certus Quartz Crystal (`appliedenergistics2:item.ItemMultiMaterial:0) | 1 | 9.091% |
| 1 x Beef Jerky (`harvestcraft:beefjerkyItem:0) | 1 | 9.091% |

Paired kit handling needed. Independent ammo rolls do not guarantee compatibility; keep loose-magazine rows separate from guaranteed kit ammunition.

### POOL_VAULT_REINFORCED

Replacement budget: **4 of 12 total weight**. Suggested gun-kit chance: **8.333% per draw**.

| New outcome | Weight | Chance in full pool |
| --- | ---: | ---: |
| M4A1 + 1 x 5.56x45mm 30-Round Magazine (30 rounds) | 1 | 8.333% |
| 1–2 x Silicon (`appliedenergistics2:item.ItemMultiMaterial:5) | 1 | 8.333% |
| 1 x Beef Jerky (`harvestcraft:beefjerkyItem:0) | 1 | 8.333% |
| 1–2 x Green Chem Light (`flansmod:chemlight_green:0) | 1 | 8.333% |

Paired kit handling needed. Independent ammo rolls do not guarantee compatibility; keep loose-magazine rows separate from guaranteed kit ammunition.

### POOL_VAULT_UNBREAKABLE

Replacement budget: **4 of 6 total weight**. Suggested gun-kit chance: **33.333% per draw**.

| New outcome | Weight | Chance in full pool |
| --- | ---: | ---: |
| M4A1 + 1 x 5.56x45mm 30-Round Magazine (30 rounds) | 1 | 16.667% |
| AK-74M + 1 x 5.45x39mm 30-Round Magazine (30 rounds) | 1 | 16.667% |
| 1 x Fluix Crystal (`appliedenergistics2:item.ItemMultiMaterial:7) | 1 | 16.667% |
| 1–2 x Silicon (`appliedenergistics2:item.ItemMultiMaterial:5) | 1 | 16.667% |

Paired kit handling needed. Independent ammo rolls do not guarantee compatibility; keep loose-magazine rows separate from guaranteed kit ammunition.

### POOL_VAULT_LAB

Replacement budget: **1 of 94 total weight**. Suggested gun-kit chance: **0.0% per draw**.

| New outcome | Weight | Chance in full pool |
| --- | ---: | ---: |
| 1–2 x Silicon (`appliedenergistics2:item.ItemMultiMaterial:5) | 1 | 1.064% |

### POOL_VERTIBIRD

Replacement budget: **16 of 128 total weight**. Suggested gun-kit chance: **2.344% per draw**.

| New outcome | Weight | Chance in full pool |
| --- | ---: | ---: |
| M9 + 1 x 9x19mm 15-Round Magazine (15 rounds) | 2 | 1.562% |
| M4A1 + 1 x 5.56x45mm 30-Round Magazine (30 rounds) | 1 | 0.781% |
| 1 x 5.56x45mm 30-round Magazine (`flansmod:556x4530rdmag:0) | 3 | 2.344% |
| 1 x Beef Jerky (`harvestcraft:beefjerkyItem:0) | 5 | 3.906% |
| 1–2 x Copper Coil (`hbm:item.coil_copper:0) | 3 | 2.344% |
| 1–2 x Green Chem Light (`flansmod:chemlight_green:0) | 2 | 1.562% |

Paired kit handling needed. Independent ammo rolls do not guarantee compatibility; keep loose-magazine rows separate from guaranteed kit ammunition.

### POOL_EXPENSIVE

Replacement budget: **15 of 86 total weight**. Suggested gun-kit chance: **2.326% per draw**.

| New outcome | Weight | Chance in full pool |
| --- | ---: | ---: |
| M4A1 + 1 x 5.56x45mm 30-Round Magazine (30 rounds) | 1 | 1.163% |
| AK-74M + 1 x 5.45x39mm 30-Round Magazine (30 rounds) | 1 | 1.163% |
| 1 x 5.56x45mm 30-round Magazine (`flansmod:556x4530rdmag:0) | 2 | 2.326% |
| 1 x 5.45x39mm 30-round Magazine (`flansmod:545x3930rdmag:0) | 2 | 2.326% |
| 1–2 x Certus Quartz Crystal (`appliedenergistics2:item.ItemMultiMaterial:0) | 3 | 3.488% |
| 1–2 x Silicon (`appliedenergistics2:item.ItemMultiMaterial:5) | 2 | 2.326% |
| 1 x Beef Jerky (`harvestcraft:beefjerkyItem:0) | 2 | 2.326% |
| 1 x Lantern (`etfuturum:lantern:0) | 2 | 2.326% |

Paired kit handling needed. Independent ammo rolls do not guarantee compatibility; keep loose-magazine rows separate from guaranteed kit ammunition.

### POOL_SUBMARINE

Replacement budget: **6 of 61 total weight**. Suggested gun-kit chance: **1.639% per draw**.

| New outcome | Weight | Chance in full pool |
| --- | ---: | ---: |
| M9 + 1 x 9x19mm 15-Round Magazine (15 rounds) | 1 | 1.639% |
| 1 x 9x19mm 15-round Magazine (`flansmod:9x1915rdmag:0) | 1 | 1.639% |
| 1 x Empty Cell (`hbm:item.cell_empty:0) | 2 | 3.279% |
| 1–2 x Silicon (`appliedenergistics2:item.ItemMultiMaterial:5) | 2 | 3.279% |

Paired kit handling needed. Independent ammo rolls do not guarantee compatibility; keep loose-magazine rows separate from guaranteed kit ammunition.

### POOL_NUKE_TRASH

Replacement budget: **1 of 36 total weight**. Suggested gun-kit chance: **0.0% per draw**.

| New outcome | Weight | Chance in full pool |
| --- | ---: | ---: |
| 1 x Lemon-Lime Soda (`harvestcraft:lemonlimesodaItem:0) | 1 | 2.778% |

### POOL_NUKE_MISC

Replacement budget: **1 of 67 total weight**. Suggested gun-kit chance: **0.0% per draw**.

| New outcome | Weight | Chance in full pool |
| --- | ---: | ---: |
| 1 x Empty Cell (`hbm:item.cell_empty:0) | 1 | 1.493% |

### POOL_SUPPLIES

Replacement budget: **35 of 58 total weight**. Suggested gun-kit chance: **0.0% per draw**.

| New outcome | Weight | Chance in full pool |
| --- | ---: | ---: |
| 1 x Trail Mix (`harvestcraft:trailmixItem:0) | 12 | 20.690% |
| 1 x Beef Jerky (`harvestcraft:beefjerkyItem:0) | 8 | 13.793% |
| 1 x Grape Juice (`harvestcraft:grapejuiceItem:0) | 8 | 13.793% |
| 1–2 x Rag (`hbm:item.rag:0) | 4 | 6.897% |
| 1–2 x Candle (`etfuturum:candle:0) | 3 | 5.172% |

### POOL_SODA

Replacement budget: **37 of 47 total weight**. Suggested gun-kit chance: **0.0% per draw**.

| New outcome | Weight | Chance in full pool |
| --- | ---: | ---: |
| 1 x Cola Soda (`harvestcraft:colasodaItem:0) | 12 | 25.532% |
| 1 x Cherry Soda (`harvestcraft:cherrysodaItem:0) | 8 | 17.021% |
| 1 x Ginger Soda (`harvestcraft:gingersodaItem:0) | 6 | 12.766% |
| 1 x Apple Juice (`harvestcraft:applejuiceItem:0) | 6 | 12.766% |
| 1 x Lemon-Lime Soda (`harvestcraft:lemonlimesodaItem:0) | 5 | 10.638% |

Retained Nuka Cola weight stays 10/47 (~21.28%). All new drinks are one per draw.

### POOL_SNACKS

Replacement budget: **30 of 40 total weight**. Suggested gun-kit chance: **0.0% per draw**.

| New outcome | Weight | Chance in full pool |
| --- | ---: | ---: |
| 1 x Trail Mix (`harvestcraft:trailmixItem:0) | 12 | 30.000% |
| 1 x Beef Jerky (`harvestcraft:beefjerkyItem:0) | 8 | 20.000% |
| 1 x Chicken Sandwich (`harvestcraft:chickensandwichItem:0) | 6 | 15.000% |
| 1 x Cheese on Toast (`harvestcraft:cheeseontoastItem:0) | 4 | 10.000% |

Retained Twinkie weight stays 10/40 (25%). Replacement food quantities are one each; AppleCore/HarvestCraft nutrition balance still needs review.

### POOL_AMMO

Replacement budget: **46 of 69 total weight**. Suggested gun-kit chance: **0.0% per draw**.

| New outcome | Weight | Chance in full pool |
| --- | ---: | ---: |
| 1 x 9x19mm 15-round Magazine (`flansmod:9x1915rdmag:0) | 12 | 17.391% |
| 1 x .45 ACP 8-round Magazine (`flansmod:45acp8rdmag:0) | 6 | 8.696% |
| 1 x 5.56x45mm 30-round Magazine (`flansmod:556x4530rdmag:0) | 8 | 11.594% |
| 1 x 5.45x39mm 30-round Magazine (`flansmod:545x3930rdmag:0) | 8 | 11.594% |
| 4–8 x 12 Gauge Buckshot (`flansmod:12gaugebuckshot:0) | 8 | 11.594% |
| 1 x 9x19mm 30-round Magazine (`flansmod:9x1930rdmag:0) | 4 | 5.797% |

### POOL_WEAPONS

Replacement budget: **531 of 531 total weight**. Suggested gun-kit chance: **29.944% per draw**.

| New outcome | Weight | Chance in full pool |
| --- | ---: | ---: |
| M9 + 1 x 9x19mm 15-Round Magazine (15 rounds) | 60 | 11.299% |
| M45A1 + 1 x 45ACP 8-Round Magazine (8 rounds) | 30 | 5.650% |
| 870 MCS + 6 x 12 Gauge BuckShot (6 rounds) | 40 | 7.533% |
| M4A1 + 1 x 5.56x45mm 30-Round Magazine (30 rounds) | 12 | 2.260% |
| AK-74M + 1 x 5.45x39mm 30-Round Magazine (30 rounds) | 12 | 2.260% |
| MP5SD + 1 x 9x19mm 30-Round Magazine (30 rounds) | 5 | 0.942% |
| 1 x 9x19mm 15-round Magazine (`flansmod:9x1915rdmag:0) | 60 | 11.299% |
| 4–8 x 12 Gauge Buckshot (`flansmod:12gaugebuckshot:0) | 30 | 5.650% |
| 1 x 5.56x45mm 30-round Magazine (`flansmod:556x4530rdmag:0) | 20 | 3.766% |
| 1 x 5.45x39mm 30-round Magazine (`flansmod:545x3930rdmag:0) | 20 | 3.766% |
| 1 x Beef Jerky (`harvestcraft:beefjerkyItem:0) | 60 | 11.299% |
| 1–2 x Steel Plate (`hbm:item.plate_steel:0) | 50 | 9.416% |
| 1–3 x Scrap (`hbm:item.scrap:0) | 80 | 15.066% |
| 1–2 x Green Chem Light (`flansmod:chemlight_green:0) | 30 | 5.650% |
| 1 x Empty Metal Syringe (`hbm:item.syringe_metal_empty:0) | 22 | 4.143% |

Paired kit handling needed. Independent ammo rolls do not guarantee compatibility; keep loose-magazine rows separate from guaranteed kit ammunition.

C130 uses 1–2 rolls plus six POOL_AMMO rolls; embedded structure uses vary. Gun kit rate is 159/531 (~29.94%), not a percent per whole chest. C130 averages ~0.45 kit per weapon payload from these rolls.

### POOL_PILE_CAPS

Replacement budget: **4 of 24 total weight**. Suggested gun-kit chance: **0.0% per draw**.

| New outcome | Weight | Chance in full pool |
| --- | ---: | ---: |
| 1–2 x Nuka Cola Bottle Cap (`hbm:item.cap_nuka:0) | 4 | 16.667% |

### POOL_PILE_OF_GARBAGE

Replacement budget: **5 of 410 total weight**. Suggested gun-kit chance: **0.0% per draw**.

| New outcome | Weight | Chance in full pool |
| --- | ---: | ---: |
| 0–1 x Empty Can (`hbm:item.can_empty:0) | 5 | 1.220% |

### POOL_PILE_HIVE

Replacement budget: **72 of 197 total weight**. Suggested gun-kit chance: **1.015% per draw**.

| New outcome | Weight | Chance in full pool |
| --- | ---: | ---: |
| 1 x Beef Jerky (`harvestcraft:beefjerkyItem:0) | 20 | 10.152% |
| 1 x Trail Mix (`harvestcraft:trailmixItem:0) | 15 | 7.614% |
| 1 x 9x19mm 15-round Magazine (`flansmod:9x1915rdmag:0) | 10 | 5.076% |
| M9 + 1 x 9x19mm 15-Round Magazine (15 rounds) | 2 | 1.015% |
| 1–2 x Steel Plate (`hbm:item.plate_steel:0) | 10 | 5.076% |
| 1–2 x Rag (`hbm:item.rag:0) | 10 | 5.076% |
| 1 x Lantern (`etfuturum:lantern:0) | 5 | 2.538% |

Paired kit handling needed. Independent ammo rolls do not guarantee compatibility; keep loose-magazine rows separate from guaranteed kit ammunition.

### POOL_PILE_GEAR

Replacement budget: **160 of 340 total weight**. Suggested gun-kit chance: **2.941% per draw**.

| New outcome | Weight | Chance in full pool |
| --- | ---: | ---: |
| M9 + 1 x 9x19mm 15-Round Magazine (15 rounds) | 5 | 1.471% |
| 870 MCS + 6 x 12 Gauge BuckShot (6 rounds) | 5 | 1.471% |
| 1 x 9x19mm 15-round Magazine (`flansmod:9x1915rdmag:0) | 20 | 5.882% |
| 4–8 x 12 Gauge Buckshot (`flansmod:12gaugebuckshot:0) | 20 | 5.882% |
| 1–2 x Steel Plate (`hbm:item.plate_steel:0) | 30 | 8.824% |
| 1–2 x Rag (`hbm:item.rag:0) | 30 | 8.824% |
| 1 x Beef Jerky (`harvestcraft:beefjerkyItem:0) | 30 | 8.824% |
| 1 x Lantern (`etfuturum:lantern:0) | 20 | 5.882% |

Paired kit handling needed. Independent ammo rolls do not guarantee compatibility; keep loose-magazine rows separate from guaranteed kit ammunition.

### POOL_PILE_MED_PILLS

Replacement budget: **15 of 40 total weight**. Suggested gun-kit chance: **0.0% per draw**.

| New outcome | Weight | Chance in full pool |
| --- | ---: | ---: |
| 1 x Grape Juice (`harvestcraft:grapejuiceItem:0) | 5 | 12.500% |
| 1–2 x Rag (`hbm:item.rag:0) | 10 | 25.000% |

### POOL_PILE_MED_SYRINGE

Replacement budget: **20 of 20 total weight**. Suggested gun-kit chance: **0.0% per draw**.

| New outcome | Weight | Chance in full pool |
| --- | ---: | ---: |
| 1 x Empty Metal Syringe (`hbm:item.syringe_metal_empty:0) | 10 | 50.000% |
| 1–2 x Rag (`hbm:item.rag:0) | 6 | 30.000% |
| 1 x Grape Juice (`harvestcraft:grapejuiceItem:0) | 4 | 20.000% |

### POOL_PILE_MAKESHIFT_GUN

Replacement budget: **10 of 10 total weight**. Suggested gun-kit chance: **20.0% per draw**.

| New outcome | Weight | Chance in full pool |
| --- | ---: | ---: |
| M9 + 1 x 9x19mm 15-Round Magazine (15 rounds) | 1 | 10.000% |
| 870 MCS + 6 x 12 Gauge BuckShot (6 rounds) | 1 | 10.000% |
| 1–2 x Steel Plate (`hbm:item.plate_steel:0) | 4 | 40.000% |
| 1–3 x Scrap (`hbm:item.scrap:0) | 4 | 40.000% |

Paired kit handling needed. Independent ammo rolls do not guarantee compatibility; keep loose-magazine rows separate from guaranteed kit ammunition.

### POOL_PILE_NUKE_STORAGE

Replacement budget: **10 of 70 total weight**. Suggested gun-kit chance: **0.0% per draw**.

| New outcome | Weight | Chance in full pool |
| --- | ---: | ---: |
| 1 x Beef Jerky (`harvestcraft:beefjerkyItem:0) | 4 | 5.714% |
| 1–2 x Steel Plate (`hbm:item.plate_steel:0) | 4 | 5.714% |
| 1–2 x Green Chem Light (`flansmod:chemlight_green:0) | 2 | 2.857% |

### POOL_PILE_SUPPLIES

Replacement budget: **130 of 160 total weight**. Suggested gun-kit chance: **0.0% per draw**.

| New outcome | Weight | Chance in full pool |
| --- | ---: | ---: |
| 1 x Beef Jerky (`harvestcraft:beefjerkyItem:0) | 30 | 18.750% |
| 1 x Grape Juice (`harvestcraft:grapejuiceItem:0) | 25 | 15.625% |
| 1 x 9x19mm 15-round Magazine (`flansmod:9x1915rdmag:0) | 20 | 12.500% |
| 1 x 5.56x45mm 30-round Magazine (`flansmod:556x4530rdmag:0) | 10 | 6.250% |
| 1 x 5.45x39mm 30-round Magazine (`flansmod:545x3930rdmag:0) | 10 | 6.250% |
| 4–8 x 12 Gauge Buckshot (`flansmod:12gaugebuckshot:0) | 10 | 6.250% |
| 1–2 x Rag (`hbm:item.rag:0) | 15 | 9.375% |
| 1 x Lantern (`etfuturum:lantern:0) | 10 | 6.250% |

### POOL_VAULT_LOCKERS

Replacement budget: **14 of 81 total weight**. Suggested gun-kit chance: **0.0% per draw**.

| New outcome | Weight | Chance in full pool |
| --- | ---: | ---: |
| 1 x Beef Jerky (`harvestcraft:beefjerkyItem:0) | 5 | 6.173% |
| 1 x Trail Mix (`harvestcraft:trailmixItem:0) | 4 | 4.938% |
| 1–2 x Green Chem Light (`flansmod:chemlight_green:0) | 3 | 3.704% |
| 1 x Lantern (`etfuturum:lantern:0) | 2 | 2.469% |

### POOL_METEORITE_TREASURE

Replacement budget: **40 of 157 total weight**. Suggested gun-kit chance: **0.0% per draw**.

| New outcome | Weight | Chance in full pool |
| --- | ---: | ---: |
| 1–2 x Certus Quartz Crystal (`appliedenergistics2:item.ItemMultiMaterial:0) | 8 | 5.096% |
| 1–2 x Certus Quartz Dust (`appliedenergistics2:item.ItemMultiMaterial:2) | 6 | 3.822% |
| 1 x Beef Jerky (`harvestcraft:beefjerkyItem:0) | 12 | 7.643% |
| 1 x Trail Mix (`harvestcraft:trailmixItem:0) | 10 | 6.369% |
| 1 x Lantern (`etfuturum:lantern:0) | 4 | 2.548% |

### POOL_RED_PEDESTAL

Replacement budget: **132 of 160 total weight**. Suggested gun-kit chance: **6.25% per draw**.

| New outcome | Weight | Chance in full pool |
| --- | ---: | ---: |
| 2–4 x Certus Quartz Crystal (`appliedenergistics2:item.ItemMultiMaterial:0) | 24 | 15.000% |
| 1 x Fluix Crystal (`appliedenergistics2:item.ItemMultiMaterial:7) | 12 | 7.500% |
| 2–4 x Silicon (`appliedenergistics2:item.ItemMultiMaterial:5) | 20 | 12.500% |
| M9 + 1 x 9x19mm 15-Round Magazine (15 rounds) | 4 | 2.500% |
| 870 MCS + 6 x 12 Gauge BuckShot (6 rounds) | 4 | 2.500% |
| M4A1 + 1 x 5.56x45mm 30-Round Magazine (30 rounds) | 2 | 1.250% |
| 3–6 x Scrap (`hbm:item.scrap:0) | 20 | 12.500% |
| 2–4 x Steel Plate (`hbm:item.plate_steel:0) | 20 | 12.500% |
| 1 x Lantern (`etfuturum:lantern:0) | 12 | 7.500% |
| 1 x Trail Mix (`harvestcraft:trailmixItem:0) | 14 | 8.750% |

Paired kit handling needed. Independent ammo rolls do not guarantee compatibility; keep loose-magazine rows separate from guaranteed kit ammunition.

Hold for separate approval: conservative placeholder redesign for a special reward room, not a claim these common supplies match its intended reward value.

## Separate vanilla chest additions and fixed loot

The earlier nine selected NTM vanilla-hook additions can be replaced at the same original weights with: armour polish -> one Steel Plate; bathwater -> one HarvestCraft Grape Juice; serum -> one AE2 Silicon; heart pieces -> one HarvestCraft Trail Mix; scrumpy -> one HarvestCraft Apple Juice. Retain other original hook entries. Full-pack hook percentages are not measured because other mods contribute.

Fixed layouts: two Stimpaks in LOOT_CAPNUKE -> two empty metal syringes; the removed flare gun -> one M9 kit (retained flare ammunition must not be silently removed; decide whether to preserve that loose ammo or redesign the layout separately); the removed Trenchmaster branch -> two steel plates plus two rags, preserving the separate retained NCR armour branch frequency.

## Decisions and remaining checks

1. Approve or adjust the civilian food/drink and technical-material groups.
2. Approve the FMUR shortlist and gun-kit rates; decide whether MP5SD should be included or held back.
3. Decide the red-room special rewards separately; its draft is deliberately conservative and may be underwhelming.
4. Then implement paired kits and fix the confirmed pedestal, raw vanilla-hook and decorative dispatch paths. Preserve config-off behavior.
5. Validate actual FMUR loot generation/reload/fire, nutrition and source-template contracts before committing. No current save or loot table was modified.

All exact IDs, original replacement budgets, proposals and source-content hashes are in MODPACK-DRAFT.json.

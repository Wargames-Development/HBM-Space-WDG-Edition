User decision: disable the five bottle-cap block outputs and .50 BMG Bypass Round too. The six recipes below therefore need no substitute ingredients. Historical recipe details follow. Loot decisions remain deferred.

# Recipe decisions resolved — corrected item identities

Correction: the earlier report incorrectly expanded cap as capacitor. These five ingredients are COLA BOTTLE CAPS; outputs are decorative blocks of bottle caps. They are not industrial electrical capacitors.

The restrictions remove 128 machine recipes in total. Most produce selected removed items. The six recipes below instead produce retained outputs but consume a removed ingredient. All six are Assembly Machine recipes, not shaped crafting recipes. No replacements have been chosen.

| Recipe | Original input | Original output |
| --- | --- | --- |
| ass.capquantum | 64 + 64 Nuka Cola Quantum Bottle Caps (hbm:item.cap_quantum) | 1 Block of Nuka Cola Quantum Bottle Caps (hbm:tile.block_cap:1) |
| ass.capsparkle | 64 + 64 S~Cola Bottle Caps (hbm:item.cap_sparkle) | 1 Block of S~Cola Bottle Caps (hbm:tile.block_cap:2) |
| ass.caprad | 64 + 64 S~Cola RAD Bottle Caps (hbm:item.cap_rad) | 1 Block of S~Cola RAD Bottle Caps (hbm:tile.block_cap:3) |
| ass.capfritz | 64 + 64 Fritz Cola Bottle Caps (hbm:item.cap_fritz) | 1 Block of Fritz Cola Bottle Caps (hbm:tile.block_cap:5) |
| ass.capkorl | 64 + 64 Korl Bottle Caps (hbm:item.cap_korl) | 1 Block of Korl Bottle Caps (hbm:tile.block_cap:4) |
| ass.50bmgbypass | 2 Large Weapon Steel Casings (hbm:item.casing:3), 24 dustAnySmokeless (ballistite or cordite), 1 Selenium Steel (hbm:item.item_secret:2), 1 Black Diamond (hbm:item.black_diamond) | 12 .50 BMG Bypass Round (hbm:item.ammo_secret:6); blueprint pool secret.psalm |

All five bottle-cap ingredients appear in the selected-items.csv extraction from chest -1129,4,-299. Black Diamond appears at -1129,5,-301. ammo_secret was not present in the chest selection and remains registered; do not infer this special ammunition was deliberately selected to keep. Its recipe became unavailable because Black Diamond was selected. No replacement catalyst has been applied.

Source: AssemblyMachineRecipes.java, original recipe definitions. BlockCap.java confirms bottle-cap block enum metadata and normal break drops of 128 bottle caps. en_US.lang confirms full display names. Original loot weights and quantities remain; a banned roll yields nothing without rerolling.

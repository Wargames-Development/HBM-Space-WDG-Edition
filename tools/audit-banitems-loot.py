#!/usr/bin/env python3
"""Read-only loot audit and unapproved suggestions. Never writes pack configs."""
import collections,csv,gzip,hashlib,json,math,random,re,zlib
from pathlib import Path
r=Path(__file__).resolve().parents[1];out=r/'docs/banitems-audit-2026-10-20/loot-review';out.mkdir(exist_ok=True)
pack=Path('/Users/rhysh/Library/Application Support/PrismLauncher/instances/Release Testing/minecraft')
runtime=json.loads((r/'build/banitems-tests/loot-audit-enabled.json').read_text());defaults=json.loads((r/'build/banitems-tests/disabled/hbmConfig/_hbmItemPools.json').read_text())
assert not (pack/'config/hbmConfig/hbmItemPools.json').exists(),'Custom active config needs separate audit'
assert defaults['pools']==json.loads((pack/'config/hbmConfig/_hbmItemPools.json').read_text())['pools'],'Pack template differs from tested defaults'
available={(e['registry'],e['meta']):e for rows in runtime.values() for e in rows if not e['disabled']}
def c(item,minimum=1,maximum=1,meta=0):
 e=dict(available[('hbm:'+item,meta)]);e.update(min=minimum,max=maximum,nbt=None);return e
scrap=c('item.scrap',1,3);plate=c('item.plate_steel',1,2);coil=c('item.coil_copper',1,2);rag=c('item.rag',1,2);can=c('item.can_empty');beans=c('item.bean_raw',1,2);snack=c('item.twinkie');syringe=c('item.syringe_metal_empty');cell=c('item.cell_empty');circuit=c('item.circuit');dust=c('item.dust')
choices={
 'POOL_OFFICE_TRASH':[beans,can,can,can], 'POOL_FILING_CABINET':[rag,dust],
 'POOL_VAULT_RUSTY':[scrap,plate,can,coil,plate,coil], 'POOL_VERTIBIRD':[scrap,coil,plate,can,plate],
 'POOL_VAULT_LAB':[cell], 'POOL_VAULT_UNBREAKABLE':[scrap,plate,coil,plate],
 'POOL_SUPPLIES':[c('item.twinkie',1,2),rag], 'POOL_PILE_CAPS':[c('item.cap_nuka',1,2)],
 'POOL_ANTENNA':[can], 'POOL_GENERIC':[scrap,plate,coil,scrap,can], 'POOL_NUKE_TRASH':[can],
 'POOL_PILE_HIVE':[scrap,plate,rag,coil,plate,scrap,can,can,beans,syringe,rag],
 'POOL_EXPENSIVE':[plate,coil,plate,scrap,syringe,can,scrap],
 'POOL_VAULT_STANDARD':[scrap,plate,coil,plate], 'POOL_PILE_OF_GARBAGE':[c('item.can_empty',0,1)],
 'POOL_NUKE_MISC':[can], 'POOL_WEAPONS':[scrap,plate,coil,scrap,plate,coil,scrap,plate,coil],
 'POOL_METEORITE_TREASURE':[beans,rag,cell,can], 'POOL_PILE_MED_PILLS':[rag],
 'POOL_SUBMARINE':[plate,coil], 'POOL_VAULT_REINFORCED':[scrap,plate,coil,plate],
 'POOL_PILE_GEAR':[can,beans,rag,plate,rag,plate], 'POOL_SODA':[can],
 'POOL_AMMO':[c('item.scrap',1,2),c('item.plate_steel'),c('item.coil_copper'),c('item.scrap',1,2),c('item.plate_steel'),c('item.coil_copper'),c('item.scrap',1,2)],
 'POOL_PILE_MED_SYRINGE':[syringe],
 'POOL_RED_PEDESTAL':[circuit,c('item.coil_copper'),c('item.plate_steel')],
 'POOL_PILE_MAKESHIFT_GUN':[scrap], 'POOL_PILE_NUKE_STORAGE':[scrap],
 'POOL_VAULT_LOCKERS':[beans,rag,plate,scrap], 'POOL_SNACKS':[beans,beans,beans,snack],
 'POOL_PILE_SUPPLIES':[plate,coil,scrap,syringe,syringe,scrap]
}
notes={
 'POOL_RED_PEDESTAL':'Special reward room: these mundane electronics are a conservative placeholder proposal, likely underwhelming. Prefer a deliberate reward redesign. Existing rare rewards keep their weights. Direct pedestal assignment also needs a restriction guard before rollout.',
 'POOL_WEAPONS':'C130 WEAPONS supply drops use this pool (1–2 rolls); it is also embedded in military-base, beached-patrol and crane resources. Proposed conversion is salvage loot across those shared uses. No replacement guns or automatic transfer into retained ammunition.',
 'POOL_AMMO':'C130 WEAPONS supply drops use six rolls of this pool. Preserve the intentionally retained ammunition; replace only removed entries with small salvage quantities.',
 'POOL_SODA':'Vending-machine soda drops: proposed removed drinks become empty cans. Nuka Cola stays at its original 10/47 chance; this is salvage rather than a new beverage. A full drink selection needs another mod or a separate content decision.',
 'POOL_SNACKS':'Vending-machine snack drops: raw beans replace three removed foods; one removed chocolate entry becomes a Twinkie. Twinkie rises from 25% to 50%; quantities remain one for it. Confirm that increase.',
 'POOL_PILE_MED_SYRINGE':'Decorative medicine piles: all drug entries are banned. Empty metal syringes preserve the medical prop theme without adding healing or buffs.',
 'POOL_PILE_MED_PILLS':'Keep RadAway, Rad-X and SIOX weights unchanged; replace removed blood bags with rags rather than making retained medicines more common.',
 'POOL_PILE_CAPS':'Only normal Nuka caps remain allowed. Proposal adds 1–2 normal caps per formerly banned roll rather than four special caps; expected caps per draw rise from 3.333 to 3.583.',
 'POOL_PILE_NUKE_STORAGE':'Retained nuclear ammunition weights stay unchanged. Removed cluster ammunition becomes 1–3 scrap; no extra mini/micro nukes.',
 'POOL_METEORITE_TREASURE':'NTM meteorite treasure block, not AE2 meteorite sky-chest loot. Keep starmetal, blueprint and gemstone chances unchanged.',
 'POOL_PILE_OF_GARBAGE':'Preserve the old 0–1 quantity range when replacing the removed canned food with an empty can; this pool intentionally retains zero-quantity rolls.',
}
# Evidence: exact source references, not a claim of exhaustive runtime reachability.
usages=collections.defaultdict(list)
pattern=re.compile(r'\bPOOL_[A-Z_]+\b')
for p in (r/'src/main/java').rglob('*.java'):
 if '/itempool/' in str(p):continue
 for number,line in enumerate(p.read_text(errors='replace').splitlines(),1):
  for name in set(pattern.findall(line)) & runtime.keys():usages[name].append({'file':str(p.relative_to(r)),'line':number,'context':line.strip(),'kind':'java'})
for p in (r/'src/main/resources').rglob('*'):
 if p.suffix not in ('.nbt','.schematic','.json'):continue
 raw=p.read_bytes()
 if raw.startswith(b'\x1f\x8b'):
  decoder=zlib.decompressobj(31);raw=decoder.decompress(raw)+decoder.unused_data
 for name in set(x.decode() for x in re.findall(rb'POOL_[A-Z_]+',raw)) & runtime.keys():usages[name].append({'file':str(p.relative_to(r)),'line':None,'context':'Pool identity embedded in resource; placement/count may be conditional','kind':'resource'})
# Trace decorative LOOT_* dispatch names back to the pools used by each method.
loot_source=(r/'src/main/java/com/hbm/util/LootGenerator.java').read_text()
methods=list(re.finditer(r'public static void (\w+)\(',loot_source))
method_pools={match[1]:set(pattern.findall(loot_source[match.start():methods[i+1].start() if i+1<len(methods) else len(loot_source)])) & runtime.keys() for i,match in enumerate(methods)}
aliases={alias:method_pools.get(method,set()) for alias,method in re.findall(r'case (LOOT_[A-Z_]+): (\w+)\(',loot_source)}
for p in (r/'src/main/resources').rglob('*'):
 if p.suffix not in ('.nbt','.schematic','.json'):continue
 raw=p.read_bytes()
 if raw.startswith(b'\x1f\x8b'):
  decoder=zlib.decompressobj(31);raw=decoder.decompress(raw)+decoder.unused_data
 for alias in set(x.decode() for x in re.findall(rb'LOOT_[A-Z_]+',raw)) & aliases.keys():
  for name in aliases[alias]:usages[name].append({'file':str(p.relative_to(r)),'line':None,'context':'Decorative dispatch '+alias+'; direct method reference, not exhaustive fall-through reachability','kind':'resource-via-dispatch'})
def fmt(e):
 name=re.sub(r'\u00a7.', '', e['display_name']);return name+' ['+e['registry']+':'+str(e['meta'])+']'
def empty_chance(rows):
 total=sum(e['weight'] for e in rows)
 return sum(e['weight']*(1 if e['disabled'] else (1/(e['max']-e['min']+1) if e['min']==0 else 0)) for e in rows)/total
# Fixed-seed 27-slot chest simulation, same weighted/quantity/split/random-slot semantics as vanilla.
def simulate(rows,rolls,trials=12000):
 rng=random.Random(20261021);total=sum(e['weight'] for e in rows);cum=[];n=0
 for e in rows:n+=e['weight'];cum.append(n)
 import bisect
 empty=0;items=0
 for _ in range(trials):
  slots=[0]*27
  for _ in range(rolls):
   e=rows[bisect.bisect_right(cum,rng.randrange(total))]
   if e['disabled']:continue
   count=rng.randint(e['min'],e['max'])
   if count<=e['max_stack_size']:slots[rng.randrange(27)]=count
   else:
    for _ in range(count):slots[rng.randrange(27)]=1
  empty+=not any(slots);items+=sum(slots)
 return {'rolls':rolls,'empty_chest_percent_estimate':round(100*empty/trials,3),'mean_total_items_after_slot_overwrites':round(items/trials,3),'trials':trials}
pools=[];allproposals=[]
for name,rows in runtime.items():
 removed=[(i,e) for i,e in enumerate(rows) if e['disabled']]
 if not removed:continue
 assert name in choices
 total=sum(e['weight'] for e in rows);lost=sum(e['weight'] for _,e in removed);proposed=[dict(e) for e in rows];changes=[]
 for j,(i,e) in enumerate(removed):
  replacement=dict(choices[name][j%len(choices[name])]);replacement.update(weight=e['weight'],disabled=False)
  proposed[i]=replacement
  assert replacement['registry'].startswith('hbm:') and not available[(replacement['registry'],replacement['meta'])]['disabled']
  assert replacement['min']<=replacement['max']<=replacement['max_stack_size']
  change={'pool':name,'entry_index':i,'before':e,'suggestion':replacement,'weight':e['weight'],'chance_percent':round(100*e['weight']/total,4),'decision':'UNAPPROVED'};changes.append(change);allproposals.append(change)
 assert sum(e['weight'] for e in proposed)==total
 # Existing identities receiving proposed weight are measured explicitly; unchanged rows do not imply unchanged aggregate identity odds.
 old=collections.Counter();new=collections.Counter()
 for e in rows:
  if not e['disabled']:old[(e['registry'],e['meta'])]+=e['weight']
 for e in proposed:new[(e['registry'],e['meta'])]+=e['weight']
 increases=[{'registry':k[0],'meta':k[1],'from_percent':round(100*old[k]/total,4),'to_percent':round(100*v/total,4)} for k,v in new.items() if v>old[k]]
 q=empty_chance(rows);qp=empty_chance(proposed)
 pools.append({'name':name,'total_entries':len(rows),'removed_entries':len(removed),'total_weight':total,'disabled_weight':lost,'lost_weight_percent':round(100*lost/total,4),'zero_quantity_inclusive_empty_draw_percent':round(100*q,4),'proposal_empty_draw_percent':round(100*qp,4),'no_nonzero_output_in_8_draws_percent':round(100*q**8,6),'proposal_no_nonzero_output_in_8_draws_percent':round(100*qp**8,6),'usage':usages[name],'retained':[e for e in rows if not e['disabled']],'changes':changes,'aggregate_identity_increases':increases,'note':notes.get(name,'Replace removed rewards with small, routine NTM salvage; preserve all other rows and weights. Existing rare reward odds are not increased.'),'simulated_27_slot_chests':{'current':simulate(rows,8),'proposal':simulate(proposed,8)}})
assert len(pools)==31 and len(runtime)==51
pools.sort(key=lambda p:(-p['lost_weight_percent'],p['name']))
report={'status':'REVIEW ONLY — no loot/config/code replacements applied','pool_count':len(runtime),'affected_pool_count':len(pools),'removed_entry_count':len(allproposals),'configuration':'No active hbmItemPools.json override. Pack underscore template matches tested defaults. Editing _hbmItemPools.json alone has no effect. Active hbmItemPools.json replaces the entire pool map, so later installation must preserve all 51 pools.','scope':'All 51 initialized NTM ItemPool definitions; all 31 affected pools. Source usages include non-structure drops. Separate vanilla ChestGenHooks and direct/decorative grants are noted in the report; no claim all loot routes are covered by these 51 pools. AE2 loot tables are unchanged.','math':'Lost weight is per-draw selection probability. q^8 is exact probability of no nonzero output in eight independent draws, not a guarantee for real inventories. 27-slot simulation includes zero counts, stack splitting and slot overwrites; actual containers, counts and multi-pool mixtures differ. These are projections, not full progression balance proof.','source_hashes':{str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in [r/'src/main/resources/assets/hbm/banitems-default.json',r/'build/banitems-tests/loot-audit-enabled.json',pack/'config/hbmConfig/_hbmItemPools.json']},'pools':pools,'unaffected_pools':sorted(set(runtime)-{p['name'] for p in pools})}
(out/'LOOT-REVIEW.json').write_text(json.dumps(report,indent=2)+'\n')
with (out/'ENTRY-PROPOSALS.csv').open('w') as f:
 w=csv.writer(f);w.writerow(['pool','entry_index','removed_item','removed_min','removed_max','weight','chance_percent','suggested_item','suggested_min','suggested_max','decision'])
 for a in allproposals:w.writerow([a['pool'],a['entry_index'],fmt(a['before']),a['before']['min'],a['before']['max'],a['weight'],a['chance_percent'],fmt(a['suggestion']),a['suggestion']['min'],a['suggestion']['max'],a['decision']])
with (out/'POOL-USAGES.csv').open('w') as f:
 w=csv.writer(f);w.writerow(['pool','file','line','kind','context'])
 for name in sorted(usages):
  for e in usages[name]:w.writerow([name,e['file'],e['line'],e['kind'],e['context']])
lines=['**Checkpoint update: selected Forge hook and pedestal guards, and restricted decorative dispatch, are now implemented and validated. Earlier route findings below are historical; replacement loot remains unapproved.**','', '# NTM loot decisions — review only','',f"51 default pools; 31 affected; {len(allproposals)} disabled entries. No substitutions applied; no pack files modified.",'',report['configuration'],'',report['scope'],'','## Recommended policy','','Retain every allowed original row, NBT, quantity and weight. Replace each removed row with a modest retained NTM item at the same weight, only after approval. No new vanilla substitutions, no extra nuclear ammunition, no increased blueprint/fuel/rare-circuit weights. Existing vanilla entries remain. Exact suggestions below are provisional, not a ready-to-install config. Recipients of replacement weight become more common; those increases are listed in LOOT-REVIEW.json.','','## Important route gaps before rollout','','- BlockKeyhole.spawnPedestalItem assigns content.theItemId.copy() directly, bypassing EmptyLootEntry generation. BlockPedestal.TileEntityPedestal read/write currently lacks a BanItems guard. Fix the restricted-item path without rerolling before treating red-room rewards as safe. This is separate from choosing replacement items.','- MainRegistry.java:348–360 registers 13 raw Forge ChestGenHooks entries. Several selected rewards (armor polish, bathwater, serum, heart pieces, scrumpy) bypass the weighted wrapper. Preserve vanilla hooks but guard selected NTM additions before release. Inventory cleanup is not a substitute for correct generation.','- Decorative LootGenerator methods include fixed direct grants as well as pools (for example two Stimpaks in lootCapNuke). Fixed grants need explicit proposals too; they are not part of the pool-weight totals. Existing BlockLoot.addItem filters them. See SUPPLEMENTAL-ROUTES.md for fixed-grant and vanilla-hook suggestions. Do not infer all fixed grants are balanced by this audit.','- All changes affect future generation/first-time deferred loot population; existing populated chests are not automatically rerolled. Refresh EFR/backporter captures and contracts after the approved profile is installed.','','## Pool overview','','| Pool | Disabled rows / total | Disabled selection weight | No nonzero output in 8 draws | Suggested role |','| --- | ---: | ---: | ---: | --- |']
for p in pools:lines.append(f"| {p['name']} | {p['removed_entries']}/{p['total_entries']} | {p['lost_weight_percent']:.2f}% | {p['no_nonzero_output_in_8_draws_percent']:.4f}% | {p['note']} |")
lines+=['','Eight draws are a comparison scenario, not each structure’s actual roll count. C130 weapons use 1–2 weapon rolls plus six ammo rolls; C130 supplies use five supply rolls. Medicine piles use four syringe rolls plus one pill roll. Vending drops are single draws. q^n cannot be presented as an entire crate’s empty chance when sources mix pools.','','## Exact suggestions by pool']
for p in pools:
 lines+=['',f"### {p['name']}",'',p['note'],'','Used by: '+('; '.join(f"{e['file']}"+(f":{e['line']}" if e['line'] else '') for e in p['usage'][:8]) or 'No direct source/resource reference found; indirect use remains possible.')+'.','',f"Retained: "+'; '.join(f"{fmt(e)} x{e['min']}–{e['max']} (w{e['weight']})" for e in p['retained'])+'.','','| Removed entry | Old quantity | Weight | Suggested retained NTM replacement | New quantity |','| --- | ---: | ---: | --- | ---: |']
 for a in p['changes']:lines.append(f"| {fmt(a['before'])} | {a['before']['min']}–{a['before']['max']} | {a['weight']} | {fmt(a['suggestion'])} | {a['suggestion']['min']}–{a['suggestion']['max']} |")
 a=p['simulated_27_slot_chests'];lines+=['',f"12,000 simulated 27-slot chests, 8 rolls: current empty {a['current']['empty_chest_percent_estimate']}%, proposal empty {a['proposal']['empty_chest_percent_estimate']}%; mean items {a['current']['mean_total_items_after_slot_overwrites']} -> {a['proposal']['mean_total_items_after_slot_overwrites']}. Projection only."]
lines+=['','## Validation','','Disposable Forge profile checks passed (9,767 enabled; 645 disabled). Runtime names/metadata classify the exact current profile. Draft validation confirms every suggested item is a retained registered NTM identity present in an existing pool, quantities fit its stack size, no replacement uses banned metadata, and each pool total weight stays unchanged. Exported native default loot files remain identical between profile modes. No progression-cost equivalence or actual in-game chest test has been claimed.','',report['math']]
(out/'REVIEW.md').write_text('\n'.join(lines)+'\n');print(json.dumps({'pools':len(runtime),'affected':len(pools),'removed_entries':len(allproposals),'all_disabled_pools':[p['name'] for p in pools if p['removed_entries']==p['total_entries']],'resource_usages':sum(e['kind']=='resource' for rows in usages.values() for e in rows),'report':str(out/'REVIEW.md')},indent=2))

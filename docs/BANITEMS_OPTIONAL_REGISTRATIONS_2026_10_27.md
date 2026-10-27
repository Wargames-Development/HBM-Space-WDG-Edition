# BanItems optional audited registrations

## Behaviour

The original disabledRegistrations validator used the default ban list as its
support catalogue. A name absent from that default list could not be selected,
even if the user knew its registry name. ModItems.wings_limp also registered
directly through GameRegistry, so accepting the name alone would not remove it.

The support catalogue now consists of the unchanged default names plus a separate
optionalRegistrations list in assets/hbm/banitems-default.json. This release adds
only hbm:item.wings_limp to the optional list, and routes its registration through
the existing availability wrapper. Its object and source code remain intact.
It remains enabled by default. Existing default bans, exact metadata selection,
boss policy and default loot weights/contents are not changed.

Additional arbitrary names are still rejected before registration. The error now
explains that a code hook/dependency audit is missing and tells the administrator
to remove the unsupported entry to recover startup. Silently accepting such names
would falsely imply an item had been removed. Optional support is an audit
extension mechanism, not a promise that every HBM item can be safely unregistered.

## Administrator use

Install the same updated HBM jar on server and clients. Stop the game/server
before changing registrations. In hbm.cfg, under wdg_banitems, add the following
line inside the existing disabledRegistrations list to remove Limp Wings:

    hbm:item.wings_limp

Keep the other selected names. Remove only that line and restart to register Limp
Wings again. The updated config comment lists additional optional supported names.

Deleting the whole config file/category regenerates the original default bans;
it does not restore all content. enableItemProfile=false disables the item and
metadata availability profile. Boss-spawn/reward controls are separate settings.
Re-registering an item cannot reconstruct inventory contents already lost from a
saved world. Keep backups before changing availability in existing saves.

## Dependency audit

Source references to wings_limp are its construction, registration, WingsMurk's
identity comparison and these anvil recipes:

- Tier-2 construction: 16 bones + 4 leather + 24 feathers -> Limp Wings.
- Smithing: Limp Wings + Tachyon Particle -> Murky Wings.

The existing anvil recipe filter removes recipes using disabled items and leaves
their original ingredients/results unchanged when availability is restored. There
are no default direct loot references to wings_limp in the source audit. No
replacement loot or ingredients have been selected or added by this change.

## Validation

Configuration regression checks cover default availability, adding/removing the
optional name and actionable rejection of an unsupported name. The harness now
runs classes rather than compileJava so its resource catalogue is refreshed too.

The disposable Forge lifecycle suite has three modes: original default profile,
profile disabled, and original profile plus the optional wings_limp ban. It checks
actual registry membership, retained internal identity, the construction recipe,
existing recipe/ore/loot safeguards and the unchanged default ban/metadata counts.
Results and the release build are recorded after completion below.

Completed validation: 21 policy assertions; lifecycle modes passed 9787, 656 and
9787 assertions respectively. The 313 default bans and 93 metadata entries match
HEAD exactly. Exported loot configurations also match in optional-ban mode.
Offline build passed, and javap confirms the release ModItems registration calls
the BanItems wrapper for wings_limp. The artifact contains its optional support
entry without adding it to the default ban list. Validation did not modify pack configs or installed jars. The checkpoint commit
was subsequently authorised by the user on 27 October; pushing was not requested.

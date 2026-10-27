# Wargames versioning

Use `major.minor.patch_Xbuild` for release jar names and reported versions.

- Major changes only when substantially changing the entire mod.
- Minor changes for a new feature; reset patch to zero.
- Patch changes for fixes and small changes.
- Build advances on every successful packaging invocation, independently of the three release components. Never reset the counter for a release bump.

EFR and DH automatically persist the next build number after successful packaging (including CI); failed builds, checks, configuration and dry runs do not consume it. Run one packaging invocation at a time in a checkout. Build through the repository root wrapper. Do not add branch names, git hashes, dirty markers or descriptive feature suffixes to release versions. Standard dev/sources classifiers identify different artifact types.

The three release components are deliberate edits in gradle.properties; the build does not guess feature versus fix from source changes. Configuration cache is disabled so a cached task graph cannot reuse a consumed build number.

WGMap keeps its existing version format and build policy. NTM keeps its upstream mod_version and mod_build_number untouched: only wdg_version is advanced manually, without a new automatic build counter. Leave disabled counters in other mods disabled.

This rebuild packages the existing optional BanItems support as WG2.4.1. Upstream remains 1.0.27, build 5852 (H261). Use the normal HBM-NTM archive name with no feature suffix.

Validation on 27 October 2026: normal `build` passed after resolving the configured OpenComputers API dependency online. The jar's mcmod.info and generated WDGVersion agree on `1.0.27_X5852_H261_WG2.4.1`. Its policy still exposes `hbm:item.wings_limp` as optional, absent from default bans, and ModItems retains the BanItems registration wrapper. Existing pack configuration determines whether Murky Wings is disabled. No installation or pack configuration edits were made. The existing gameplay/lifecycle suite was not rerun for this version-only rebuild.

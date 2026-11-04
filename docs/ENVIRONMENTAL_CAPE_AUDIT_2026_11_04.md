# Environmental effects and cape audit — 4 November 2026

Scope: the HBM geysers, Agent Orange/chlorine/cloud particles, their shared
chemical damage routines, gas block exposure callbacks, geyser/volcano shrapnel,
and the NTM cape override. WGCore source was inspected alongside HBM; changes
are confined to HBM.

## Findings and changes

- `TileEntityGeysir.chlorine()` spawns `EntityOrangeFX`, rather than
  `EntityChlorineFX`. That existing choice explains why a chlorine geyser can
  appear in an Agent Orange crash report. It remains unchanged.
- Orange particle impacts previously called WGCore and changed grass on the
  client. Impacts now modify terrain and query territory only on the server.
  Collision and permission coordinates use the same floored block position,
  including west/north of zero. Contamination stops after the first impact,
  skips unloaded/out-of-world blocks, and does not continue moving after expiry.
- Chlorine particles previously called the poison routine on both sides. The
  particle and the shared poison/PC/cloud damage helpers now reject client-side
  damage work. Gas damage is centered on the actual particle position.
- Chlorine, carbon monoxide, coal dust, asbestos and the radon/meltdown variants
  previously ran their exposure/filter callbacks on both sides. Those callbacks
  now run on the server. Existing exposure strengths and protection rules remain.
- Pink cloud receiver conversion previously changed blocks on the client and
  lacked contamination permission checks. Conversion now runs on the server,
  checks WGCore, and uses consistent floored coordinates.
- Geyser/volcano shrapnel previously checked player harm on the client, and only
  removed itself on an allowed terrain impact. It now handles damage on the
  server and consumes a mature projectile even when territory denies its impact.
  Entity hits no longer fall through to a terrain check at default hit coordinates.
  Each neighboring monoxide placement checks contamination permission.
- NTM capes were written into the player's cape texture indefinitely. The
  override now exists only during the cape render and restores the prior texture,
  including a null original. It preserves intervening texture updates from other
  mods/downloads and recovers on the next render if a previous post event was
  skipped. Rendering respects the player's synchronized hide-cape preference and
  the local Multiplayer Settings → Show Cape option immediately. The local option
  applies to the local player's ordinary cape as well as the NTM override; other
  players keep their own cape preferences.

The reported crash log was not available. The client-side WGCore calls are
confirmed source defects, but matching the original exception still requires its
stack trace. A ticking-entity message alone does not identify the cause.

## Verified natural sources — coordinated HBM WG2.5 / WGCore 2.8 update

Ownerless effects remain unknown by default. New world generation explicitly marks
chlorine geysers, Nether/Moho fire geysers and Moho volcano cores with persisted
source dimension/coordinates. Emitted Orange particles and shrapnel carry that
context. A moved/copied source tag does not qualify at a different location, and
an owned effect cannot borrow the natural exception. The source must still exist
and be loaded at its recorded position when the effect acts. Volcano growth marks
the replacement core at its new position.

WGCore's separate environmental API permits verified effects only when both source
and destination are unclaimed Wilderness. Claims and all other zones deny. The
`wgcore-natural-hazards.cfg` wilderness settings independently enable natural damage,
contamination and debris; defaults are true. Restart after editing that file. With
an older WGCore lacking the optional API, the exception is denied without crashing.
Without WGCore, verified sources retain ordinary NTM behavior.

Natural poison/shrapnel damage checks every affected entity. Natural Orange terrain
edits and volcano core direct edits use destination checks. Natural volcano/debris
explosions remain terrain-only and check each affected chunk; chunk results are
cached per explosion, and natural explosion rays stop at protected/unloaded chunks.
The ordinary owned-explosion and partial-damage path remains separate.

HBM WG2.5.1 also supports a server-authored `config/hbm-natural-sources.json`
allowlist. A verified Earth40 example is provided in `docs/examples`. It contains
22 chlorine geysers extracted from the reference conversion's accepted structure
records and checked against actual blocks/TileEntities. With that file installed,
a matching source is certified before its first loaded server effect and saved
normally; no commands or external tools are needed. Dimension, exact X/Y/Z, block
registry ID and source tile class must match. No chunks are forced to load, and
unlisted or replaced sources are not certified. Invalid/duplicate entries reject
the entire file. An explicit manual clear saves an opt-out so later chunk loads
do not silently recertify that tile from the allowlist.

Existing maps otherwise have no reliable historical provenance. They are not automatically
reclassified from a null owner or a block type. A level-4 operator can explicitly
certify a known natural source in loaded terrain:

```text
/ntmnatural certify <x> <y> <z>
/ntmnatural clear <x> <y> <z>
```

Commands operate in the sender's current dimension and accept only an existing
geyser or volcano core. Certification does not bypass WGCore's territory rules.
Unknown/uncertified old sources retain the fail-closed ownerless behavior.

Persistent gas and liquid blocks still use their existing block/Forge fluid physics;
they do not acquire provenance simply because they lack an owner. Existing liquid
flow and block exposure protections are separate from these verified-source APIs.
This update does not retrofit origin tracking to every gas/liquid block or audit
every natural hazard. In particular, verify secondary lava/gas propagation near
territory boundaries in a copied world before production rollout.

## Verification

Run with Java 8 and cached dependencies:

```sh
python3 tools/test-environmental-compat.py
```

The harness runs compiled Minecraft/HBM code against small in-memory worlds and
a recording WGCore facade. It checks client isolation, server contamination,
negative coordinates, denial/unloaded handling, verified/moved/owned natural sources, particle expiry, gas exposure
callbacks, protected shrapnel impacts, and cape restoration. It does not launch a
game or prove behavior against a running WGCore server.

Live QA should cover:

1. Chlorine geysers and thrown chlorine/Agent Orange in wilderness and claims,
   with and without WGCore; include negative chunk boundaries and unloaded edges.
2. Nether geyser/volcano debris hitting players and protected terrain; confirm
   denied impacts disappear and do not place gas across a claim boundary.
3. Pink cloud receivers in allowed and denied territory; check client/server sync.
4. Own cape off/on in third person, inventory and after reconnect; repeat with
   an NTM-eligible account, an ordinary cape, and no ordinary cape.
5. Another player's cape off/on from their own settings; verify observers follow
   that preference and disabling your own cape does not hide theirs.
6. Relevant installed cape/render mods, since custom renderers may bypass Forge's
   normal `RenderPlayerEvent.Specials` cape path.

Additional live QA: generate fresh geysers/Moho cores, certify a known old source,
then test wilderness → claim/safe-zone boundaries, claim the source itself,
remove/move it, restart, and repeat with each environmental switch disabled.

## Reported live validation — 4 November 2026

The trailer server loaded HBM WG2.5.1 and accepted all 22 allowlist entries. The
client joined successfully, and the source at dimension 0, X 12842, Y 149, Z -3776
was certified at runtime with WGCore integration active. The user confirmed:

- Show Cape disabled their cape correctly.
- In survival, wilderness eruptions produced dirt/foliage changes and Poison III,
  Mining Fatigue III and brief blindness. At least six eruptions ran without the
  reported ticking-entity crash.
- Setting the two test chunks to safe zone suppressed player effects through
  repeated eruptions; clearing the zones restored those effects without crashes.

The cape toggle and this geyser reproduction/protection test are marked complete.
This does not establish coverage for every natural hazard or every checklist item
above. Multi-output and vehicle Resource Node live testing is deferred; automated
WGCore/WGHangers tests and builds have passed. ItemControl separately reports
HBM enforcement disabled because its version allowlist does not recognize WG2.5.1;
that compatibility warning remains unresolved.

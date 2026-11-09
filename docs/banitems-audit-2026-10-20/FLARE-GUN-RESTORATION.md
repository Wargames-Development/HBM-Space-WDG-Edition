# Flare gun availability — 9 November 2026

Released for server testing as WG2.5.3.

The combat-gun expansion included `hbm:item.gun_flaregun` in the default
registration bans. The artillery barrage flare (`hbm:item.ammo_standard@95`)
was retained, but its launcher was unavailable with the default profile enabled.

The flare gun is now enabled by default. Its existing removal hook is retained
as an optional registration so older saved configurations still load. The
artillery flare keeps metadata 95 and remains accepted by the flare gun.
Supply and weapon airdrop flares (metadata 53 and 54) retain their existing bans.

Existing configurations preserve their explicit selections. Remove
`hbm:item.gun_flaregun` from `wdg_banitems.disabledRegistrations` in `config/hbm.cfg`
on the server and clients, then restart to restore the gun. The Release Testing
instance still contained this entry when inspected; this source change does not
edit the installed pack or its saved configuration.

Release Testing also has an existing CraftTweaker rule removing the flare gun's
crafting recipe in `scripts/MineTweakerRecipeMaker.zs`. That recipe restriction
is separate from registration and is preserved here.

Validation passed: 25 policy assertions and disposable Forge lifecycle checks
with the profile enabled (9,790 assertions), disabled (659), and optional
registrations selected (9,790). These checks confirm actual flare gun
registration and artillery flare availability; they do not exercise firing in
the full modpack. Exported loot defaults remain identical across profile modes.

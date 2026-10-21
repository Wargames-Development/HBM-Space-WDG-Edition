# Optional placed bottle-cap block migration

Prepared, tested on synthetic worlds, NOT run on user saves. The installed mod does not delete placed blocks. Approval review rejected automatic in-place deletion because saving could permanently lose world data; the separately authorized migration instead writes a new world copy and keeps the original as the rollback backup.

Tool: tools/migrate-bottlecap-blocks.py (standard Python 3, no packages). Resolves the block ID from the selected world's Forge FML ItemData, never from a hardcoded numeric ID. Targets hbm:tile.block_cap metadata 1..5 only. Metadata 0 stays. Bypass Round inventories are already filtered by the installed availability profile.

Close the game/server before either command. Replace the placeholder paths with the chosen world and an absent independent destination:

```sh
python3 tools/migrate-bottlecap-blocks.py '/absolute/path/to/source-world'
python3 tools/migrate-bottlecap-blocks.py '/absolute/path/to/source-world' --output '/absolute/path/to/new-world-copy' --confirm-game-closed
```

The first command is read-only and lists counts/coordinates. The second copies the entire world into staging, patches affected chunks, verifies no target blocks remain in rewritten regions, checks source region/registry hashes, and promotes the copy only after success. Existing destinations, nested paths and symlinks are refused. No source save is written. Keep the original world unchanged until the migrated copy has been accepted; rollback means reopening the original.

All other chunk NBT bytes and original region timestamps are preserved; selected Blocks/Data/Add nibbles become air/zero and affected chunks request relighting. Height maps and lighting arrays remain conservative seeds for target-side relighting; inspect lighting in the migrated copy before accepting it. This is a legacy 1.7.10 numeric-section tool and refuses unsupported data. Full-pack visual testing of a migrated real world remains pending.

Automated test: python3 tools/test-bottlecap-migration.py. Covers exact metadata and retained variants, high-ID Add nibbles, unknown NBT preservation, three compression methods, source integrity, dry run and output guards. No user worlds were opened by these tests.

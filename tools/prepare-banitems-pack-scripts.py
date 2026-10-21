#!/usr/bin/env python3
"""Stage registry-safe replacements for obsolete pack recipe removals.

Preserves the removal when an item is re-enabled, all markers, other rules and
recipe additions. Does not edit pack files unless --apply is explicitly supplied.
"""
import argparse, hashlib, json, re, shutil, tempfile
from datetime import datetime
from pathlib import Path

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('minecraft', type=Path)
parser.add_argument('--output', type=Path, default=root/'docs/banitems-audit-2026-10-20/pack-script-migration')
parser.add_argument('--apply', action='store_true')
args = parser.parse_args()
if args.apply:
    manifest=args.output/'MIGRATION.json'
    data=json.loads(manifest.read_text());entries=data['entries']
    for entry in entries:
        source=Path(entry['source']);staged=Path(entry['staged'])
        if not source.resolve().is_relative_to((args.minecraft/'scripts').resolve()): raise SystemExit('Migration targets another pack')
        if hashlib.sha256(source.read_bytes()).hexdigest()!=entry['original_sha256']: raise SystemExit('Source changed; refusing overwrite: '+str(source))
        if hashlib.sha256(staged.read_bytes()).hexdigest()!=entry['updated_sha256']: raise SystemExit('Staged script changed; refusing apply: '+str(staged))
    backup=args.minecraft/'.banitems-backups'/datetime.now().strftime('%Y%m%d-%H%M%S');backup.mkdir(parents=True,exist_ok=False)
    for entry in entries:
        source=Path(entry['source']);relative=source.relative_to(args.minecraft/'scripts');saved=backup/relative;saved.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(source,saved)
        with tempfile.NamedTemporaryFile(dir=source.parent,prefix='.banitems-',suffix='.tmp',delete=False) as handle:
            handle.write(Path(entry['staged']).read_bytes());temporary=Path(handle.name)
        temporary.replace(source)
    print('Applied reviewed script migration; backups:',backup)
    raise SystemExit(0)
names = set(json.loads((root/'src/main/resources/assets/hbm/banitems-default.json').read_text())['disabled'])
helper = '''// Registry-safe adapter for the existing removal rules; makeStack takes metadata in this CraftTweaker version.
function efrBanItemsRemoveRegistered(registryName as string, metadata as int) {
    for definition in game.items {
        if (definition.id == registryName) {
            recipes.remove(definition.makeStack(metadata));
        }
    }
}

'''
removal = re.compile(r'^(\s*)recipes\.remove\(<(hbm:[A-Za-z0-9_.]+)(?::(\*|\d+))?>\);(\s*(?://.*)?)$')
reference = re.compile(r'<(hbm:[A-Za-z0-9_.]+)(?::(?:\*|\d+))?>')
sha = lambda data: hashlib.sha256(data).hexdigest()
entries = []
for source in sorted((args.minecraft/'scripts').rglob('*.zs')):
    original = source.read_bytes(); newline = '\r\n' if b'\r\n' in original else '\n'
    text = original.decode('utf-8').replace('\r\n','\n'); lines = text.splitlines(True); edits = []
    for index, line in enumerate(lines):
        if line.lstrip().startswith('//'): continue
        if not any(name in names for name in reference.findall(line)): continue
        match = removal.fullmatch(line.rstrip('\n'))
        if not match or match[2] not in names:
            raise SystemExit('Manual review required; refusing unsupported disabled-item reference at '+str(source)+':'+str(index+1))
        meta = 32767 if match[3] == '*' else int(match[3] or 0)
        lines[index] = match[1]+'efrBanItemsRemoveRegistered("'+match[2]+'", '+str(meta)+');'+match[4]+'\n'
        edits.append({'line':index+1,'registry':match[2],'metadata':meta,'original':line.strip()})
    if not edits: continue
    if 'function efrBanItemsRemoveRegistered' in text: raise SystemExit('Adapter already present; refusing duplicate')
    updated = (helper+''.join(lines)).replace('\n',newline).encode('utf-8')
    assert re.findall(r'^//#MARKER.*$', text, re.M) == re.findall(r'^//#MARKER.*$', updated.decode().replace('\r\n','\n'), re.M)
    original_adds=[line for line in text.splitlines() if re.match(r'\s*recipes\.add',line)]
    assert original_adds == [line for line in updated.decode().replace('\r\n','\n').splitlines() if re.match(r'\s*recipes\.add',line)]
    relative=source.relative_to(args.minecraft/'scripts'); staged=args.output/'scripts'/relative; staged.parent.mkdir(parents=True,exist_ok=True);staged.write_bytes(updated)
    entries.append({'source':str(source),'staged':str(staged),'original_sha256':sha(original),'updated_sha256':sha(updated),'edits':edits})
args.output.mkdir(parents=True,exist_ok=True)
manifest=args.output/'MIGRATION.json';manifest.write_text(json.dumps({'mode':'applied' if args.apply else 'staged only','entries':entries},indent=2)+'\n')
print(('Applied' if args.apply else 'Staged'),sum(len(entry['edits']) for entry in entries),'registry-safe removal rules in',len(entries),'script(s). Other rules and recipe additions unchanged.')

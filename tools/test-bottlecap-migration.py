#!/usr/bin/env python3
"""Synthetic legacy Anvil tests. Never opens a user world."""
import gzip, importlib.util, json, struct, subprocess, sys, tempfile, zlib
from pathlib import Path
p=Path(__file__).with_name('migrate-bottlecap-blocks.py');spec=importlib.util.spec_from_file_location('migration',p);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
def string(v):b=v.encode();return struct.pack('>H',len(b))+b
def tag(t,n,payload):return bytes([t])+string(n)+payload
def compound(*tags):return b''.join(tags)+b'\0'
def root(*tags):return tag(10,'',compound(*tags))
def byte(n,v):return tag(1,n,struct.pack('>b',v))
def array(n,v):return tag(7,n,struct.pack('>i',len(v))+v)
def listing(n,t,values):return tag(9,n,bytes([t])+struct.pack('>i',len(values))+b''.join(values))
def setn(a,i,v):a[i//2]=(a[i//2] & (255^(15<<(4*(i%2))))) | v<<(4*(i%2))
blocks=bytearray(4096);data=bytearray(2048);add=bytearray(2048)
for i in range(6):blocks[i]=3000&255;setn(add,i,3000>>8);setn(data,i,i)
blocks[6]=2;setn(data,6,3)
section=compound(byte('Y',23),array('Blocks',blocks),array('Data',data),array('Add',add),array('SkyLight',bytes([255])*2048),array('BlockLight',bytes(2048)))
chunk=root(tag(10,'Level',compound(listing('Sections',10,[section]),byte('LightPopulated',1),tag(8,'Untouched',string('keep unknown NBT exactly')))))
patched,coords=m.patch_chunk(chunk,3000);assert len(coords)==5 and all(y==368 for x,y,z in coords);assert m.patch_chunk(patched,3000)[1]==[]
level=m.NBT(patched).root()['Level'][1];sec=level['Sections'][1][0][1];bo=sec['Blocks'][1][0];mo=sec['Data'][1][0];ao=sec['Add'][1][0]
assert patched[bo]==(3000&255) and m.nibble(patched,ao,0)==3000>>8
assert all(patched[bo+i]==m.nibble(patched,mo,i)==m.nibble(patched,ao,i)==0 for i in range(1,6))
assert patched[bo+6]==2 and m.nibble(patched,mo,6)==3
assert level['Untouched'][1]=='keep unknown NBT exactly' and level['LightPopulated'][1]==0
assert m.patch_chunk(chunk,2999)==(chunk,[])
def region(kind):
 encoded={1:gzip.compress,2:zlib.compress,3:lambda x:x}[kind](chunk);record=struct.pack('>I',len(encoded)+1)+bytes([kind])+encoded;n=(len(record)+4095)//4096
 header=bytearray(8192);header[:4]=((2<<8)|n).to_bytes(4,'big');header[4096:4100]=b'\x01\x02\x03\x04'
 return bytes(header)+record+bytes(n*4096-len(record))
for kind in (1,2,3):
 original=region(kind);updated,coords=m.patch_region(original,3000);assert len(coords)==5 and m.patch_region(updated,3000)[1]==[];assert updated[4096:8192]==original[4096:8192]
 assert m.patch_region(original,2999)==(original,[])
with tempfile.TemporaryDirectory(prefix='bottlecap-migration-test-') as directory:
 work=Path(directory);source=work/'source';(source/'region').mkdir(parents=True)
 entry=compound(tag(8,'K',string('\x01hbm:tile.block_cap')),tag(3,'V',struct.pack('>i',3000)))
 (source/'level.dat').write_bytes(gzip.compress(root(tag(10,'FML',compound(listing('ItemData',10,[entry]))))))
 (source/'region/r.0.0.mca').write_bytes(region(2));(source/'other.dat').write_bytes(b'unchanged')
 before={str(f.relative_to(source)):f.read_bytes() for f in source.rglob('*') if f.is_file()}
 def run(*args,success=True):
  result=subprocess.run([sys.executable,str(p),str(source),*map(str,args)],capture_output=True,text=True);assert (result.returncode==0)==success,result.stderr;return result
 report=json.loads(run().stdout);assert report['removed_block_count']==5 and not (work/'output').exists()
 run('--output',work/'output',success=False)
 run('--output',work/'output','--confirm-game-closed');assert json.loads((work/'output/BOTTLECAP-MIGRATION.json').read_text())['removed_block_count']==5
 assert (work/'output/other.dat').read_bytes()==b'unchanged'
 assert before=={str(f.relative_to(source)):f.read_bytes() for f in source.rglob('*') if f.is_file()}
 run('--output',work/'output','--confirm-game-closed',success=False)
 run('--output',source/'nested','--confirm-game-closed',success=False)
print('PASS: copy-only migration; exact metadata, high block-ID nibbles, retained variants, all 3 compression types, relight flag, preserved unknown NBT/timestamps, untouched source, dry run and overwrite guards')

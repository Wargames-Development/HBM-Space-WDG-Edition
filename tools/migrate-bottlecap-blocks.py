#!/usr/bin/env python3
"""Remove only bottle-cap block metadata 1..5 into a NEW world copy.

The source is the rollback backup and is never written. No mod-side deletion.
Default is read-only. --output and --confirm-game-closed together create a copy.
Pure Python; preserves all NBT bytes except selected block/data and relight flag.
"""
import argparse, gzip, hashlib, json, os, shutil, struct, tempfile, zlib
from pathlib import Path

class NBT:
    def __init__(self, data): self.data=data; self.pos=0
    def take(self, n):
        if n<0 or self.pos+n>len(self.data): raise ValueError('Truncated NBT')
        start=self.pos; self.pos+=n; return self.data[start:self.pos]
    def number(self, fmt): return struct.unpack(fmt,self.take(struct.calcsize(fmt)))[0]
    def string(self): return self.take(self.number('>H')).decode('utf-8')
    def payload(self,t):
        start=self.pos
        if t in (1,2,3,4,5,6): value=self.number({1:'>b',2:'>h',3:'>i',4:'>q',5:'>f',6:'>d'}[t])
        elif t==8:value=self.string()
        elif t in (7,11,12):
            n=self.number('>i'); offset=self.pos; self.take(n*{7:1,11:4,12:8}[t]); value=(offset,n)
        elif t==9:
            subtype=self.number('>B'); n=self.number('>i')
            if n<0: raise ValueError('Negative list length')
            value=[self.payload(subtype) for _ in range(n)]
        elif t==10:
            value={}
            while True:
                subtype=self.number('>B')
                if subtype==0:break
                name=self.string()
                if name in value:raise ValueError('Duplicate NBT key')
                value[name]=self.payload(subtype)
        else:raise ValueError('Unsupported NBT tag '+str(t))
        return (t,value,start,self.pos)
    def root(self):
        if self.number('>B')!=10:raise ValueError('Expected root compound')
        self.string(); node=self.payload(10)
        if self.pos!=len(self.data):raise ValueError('Trailing NBT data')
        return node[1]

def block_id(world):
    root=NBT(gzip.decompress((world/'level.dat').read_bytes())).root()
    entries=root['FML'][1]['ItemData'][1];found=[]
    for entry in entries:
        v=entry[1]
        if v['K'][1]=='\x01hbm:tile.block_cap':found.append(v['V'][1])
    if len(found)!=1 or not 0<found[0]<4096:raise ValueError('Expected one valid Forge block_cap BLOCK registry ID')
    return found[0]

def nibble(data,offset,i):return (data[offset+i//2] >> (4*(i%2))) & 15

def patch_chunk(raw, target):
    root=NBT(raw).root();level=root['Level'][1];sections=level['Sections'][1];changed=[];out=bytearray(raw)
    for node in sections:
        sec=node[1];blocks,n=sec['Blocks'][1];meta,m=sec['Data'][1];add=sec.get('Add');high=add[1][0] if add else None
        if n!=4096 or m!=2048 or (add and add[1][1]!=2048):raise ValueError('Malformed legacy section arrays')
        y=sec['Y'][1]
        for i in range(4096):
            bid=raw[blocks+i] | ((nibble(raw,high,i) if high is not None else 0)<<8)
            if bid!=target or nibble(raw,meta,i) not in range(1,6):continue
            changed.append((i&15,y*16+(i>>8),(i>>4)&15));out[blocks+i]=0
            mask=255 ^ (15 << (4*(i%2)));out[meta+i//2]&=mask
            if high is not None:out[high+i//2]&=mask
    if changed:
        # Preserve height map and light arrays as conservative seeds; request target-side relight.
        light=level.get('LightPopulated')
        if light is None or light[0]!=1:raise ValueError('Missing byte LightPopulated; refusing unsafe relight migration')
        out[light[2]]=0
    return bytes(out),changed

def patch_region(raw,target):
    if len(raw)<8192 or len(raw)%4096:raise ValueError('Invalid region size')
    result=bytearray(raw);locations=[]
    for index in range(1024):
        entry=int.from_bytes(raw[index*4:index*4+4],'big');sector=entry>>8;count=entry&255
        if not sector:
            if count:raise ValueError('Invalid region header')
            continue
        start=sector*4096
        if sector<2 or count==0 or start+count*4096>len(raw):raise ValueError('Invalid chunk sector range')
        length=int.from_bytes(raw[start:start+4],'big');kind=raw[start+4]
        if length<1 or length+4>count*4096:raise ValueError('Invalid compressed length')
        encoded=raw[start+5:start+4+length]
        chunk={1:gzip.decompress,2:zlib.decompress,3:lambda x:x}[kind](encoded)
        patched,coords=patch_chunk(chunk,target)
        if not coords:continue
        # Append replacements; retain original sector records/timestamps for every other chunk.
        payload=zlib.compress(patched);record=struct.pack('>I',len(payload)+1)+b'\x02'+payload;n=(len(record)+4095)//4096
        off=len(result)//4096
        if n>255 or off>0xffffff:raise ValueError('Anvil size limit')
        result[index*4:index*4+4]=((off<<8)|n).to_bytes(4,'big');result.extend(record);result.extend(bytes(n*4096-len(record)))
        locations.extend((index,*p) for p in coords)
    return bytes(result),locations

def sha(data):return hashlib.sha256(data).hexdigest()

def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('world',type=Path);parser.add_argument('--output',type=Path);parser.add_argument('--confirm-game-closed',action='store_true');args=parser.parse_args()
    source=args.world.resolve();target=block_id(source)
    if any(p.is_symlink() for p in source.rglob('*')):raise SystemExit('Symlinks not supported')
    output=args.output.resolve() if args.output else None
    if output and (not args.confirm_game_closed or output.exists() or output.is_relative_to(source) or source.is_relative_to(output)):raise SystemExit('Close the game, confirm it, and choose an absent independent output folder')
    regions=sorted(source.rglob('region/r.*.*.mca'));report={'source':str(source),'output':str(output) if output else None,'block_id':target,'metadata':[1,2,3,4,5],'source_modified':False,'relight_required':True,'regions':[]};hashes={};levelhash=sha((source/'level.dat').read_bytes())
    for p in regions:
        raw=p.read_bytes();hashes[p]=sha(raw);_,coords=patch_region(raw,target)
        if coords:
            rx,rz=map(int,p.stem.split('.')[1:]);report['regions'].append({'region':str(p.relative_to(source)),'source_sha256':hashes[p],'blocks':[[rx*512+(idx%32)*16+x,y,rz*512+(idx//32)*16+z] for idx,x,y,z in coords]})
    report['removed_block_count']=sum(len(e['blocks']) for e in report['regions'])
    if output:
        if shutil.disk_usage(output.parent).free < sum(p.stat().st_size for p in source.rglob('*') if p.is_file())*1.1:raise SystemExit('Insufficient space for full rollback-preserving copy')
        with tempfile.TemporaryDirectory(prefix='.bottlecap-migration-',dir=output.parent) as temporary:
            staged=Path(temporary)/'world';shutil.copytree(source,staged)
            for entry in report['regions']:
                p=staged/entry['region'];raw=p.read_bytes()
                if sha(raw)!=entry['source_sha256']:raise ValueError('World changed while copying')
                patched,coords=patch_region(raw,target);p.write_bytes(patched)
                if patch_region(p.read_bytes(),target)[1]:raise ValueError('Round-trip migration verification failed')
            if sha((source/'level.dat').read_bytes())!=levelhash or any(sha(p.read_bytes())!=h for p,h in hashes.items()):raise ValueError('Source changed during migration; output not promoted')
            (staged/'BOTTLECAP-MIGRATION.json').write_text(json.dumps(report,indent=2)+'\n');os.rename(staged,output)
    print(json.dumps(report,indent=2))

if __name__=='__main__':main()

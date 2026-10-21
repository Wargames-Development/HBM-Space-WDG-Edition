"""Build and run NTM in disposable Forge servers with the profile on and off.
Requires cached ForgeGradle dependencies and Java 8. Never opens pack saves.
"""
from pathlib import Path
import os,subprocess,tempfile,shutil,time,json
r=Path(__file__).resolve().parents[1]
java=Path(os.environ['JAVA_HOME'])/'bin'
with tempfile.TemporaryDirectory(prefix='hbm-banitems-lifecycle-') as temporary:
 work=Path(temporary);cpfile=work/'classpath.txt';init=work/'classpath.gradle'
 init.write_text("allprojects { afterEvaluate { task banItemsClasspath { doLast { new File("+repr(str(cpfile))+").text = sourceSets.main.runtimeClasspath.asPath } } } }")
 subprocess.run([str(r/'gradlew'),'classes','banItemsClasspath','--offline','--no-daemon','--init-script',str(init)],cwd=r,check=True)
 # ForgeGradle 1.2 appends the cached launcher during runServer execution, not in its declared classpath.
 start=Path.home()/'.gradle/caches/minecraft/net/minecraftforge/forge/1.7.10-10.13.4.1614-1.7.10/start'
 if not (start/'GradleStartServer.class').exists(): raise RuntimeError('Run the development server task once to generate GradleStartServer')
 cp=cpfile.read_text()+os.pathsep+str(start)
 classes=work/'test-classes';classes.mkdir()
 subprocess.run([str(java/'javac'),'-cp',cp,'-d',str(classes),str(r/'tools/tests/BanItemsLifecycleChecks.java')],check=True)
 jar=work/'banitems-checks.jar';subprocess.run([str(java/'jar'),'cf',str(jar),'-C',str(classes),'.'],check=True)
 results=[]
 for enabled in (True,False):
  world=work/('enabled' if enabled else 'disabled');world.mkdir();(world/'mods').mkdir();shutil.copy2(jar,world/'mods'/jar.name)
  (world/'eula.txt').write_text('eula=true\n')
  (world/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\nlevel-type=FLAT\ngenerate-structures=false\nspawn-monsters=false\nspawn-animals=false\nspawn-npcs=false\nview-distance=2\n')
  for name in ['banned-players.json','banned-ips.json','ops.json','whitelist.json']:(world/name).write_text('[]')
  if not enabled:
   (world/'config').mkdir();(world/'config/hbm.cfg').write_text('wdg_banitems {\n B:enableItemProfile=false\n}\n')
  audit=r/'docs/banitems-audit-2026-10-20'
  shutil.copy2(audit/'unselected-variants-KEEP.csv',world/'keep-variants.csv');shutil.copy2(audit/'loot-pool-summary.csv',world/'pool-baseline.csv')
  log=world/'console.log'
  with log.open('w') as output:
   process=subprocess.Popen([str(java/'java'),'-Xmx2G','-cp',cp,'GradleStartServer','nogui'],cwd=world,stdin=subprocess.PIPE,stdout=output,stderr=subprocess.STDOUT)
   try: code=process.wait(timeout=150)
   except subprocess.TimeoutExpired:
    process.stdin.write(b'stop\n');process.stdin.flush()
    try:process.wait(timeout=20)
    except subprocess.TimeoutExpired:process.terminate();process.wait(timeout=15)
    raise RuntimeError('Lifecycle check timed out; log tail:\n'+log.read_text()[-14000:])
  result=world/'banitems-check-result.txt';result_text=result.read_text() if result.exists() else 'FAIL: no lifecycle marker'
  archive=r/'build/banitems-tests';archive.mkdir(exist_ok=True)
  shutil.copy2(log,archive/('enabled.log' if enabled else 'disabled.log'))
  if (world/'loot-audit.json').exists():shutil.copy2(world/'loot-audit.json',archive/('loot-audit-enabled.json' if enabled else 'loot-audit-disabled.json'))
  if code!=0 or not result_text.startswith('PASS:'):raise RuntimeError(result_text+'; exit='+str(code)+'\n'+log.read_text()[-16000:])
  if 'Error while writing recipes' in log.read_text() or 'A broken ore dictionary registration' in log.read_text():raise RuntimeError('Recipe or ore registration errors; see '+str(archive))
  for folder in ['hbmRecipes','hbmConfig']:
   target=archive/('enabled' if enabled else 'disabled')/folder
   if target.exists():shutil.rmtree(target)
   shutil.copytree(world/'config'/folder,target)
  print(result_text,flush=True);results.append(result_text)
 (archive/'results.json').write_text(json.dumps(results,indent=2)+'\n')

 # Compare exported defaults without deciding replacement ingredients or changing loot weights.
 baseline=archive/'disabled/hbmRecipes';current=archive/'enabled/hbmRecipes';removed=[]
 for source in sorted(baseline.glob('*.json')):
  old=json.loads(source.read_text()).get('recipes',[])
  new_file=current/source.name
  new=json.loads(new_file.read_text()).get('recipes',[]) if new_file.exists() else []
  import collections
  remaining=collections.Counter(json.dumps(entry,sort_keys=True) for entry in new)
  for entry in old:
   key=json.dumps(entry,sort_keys=True)
   if remaining[key]:remaining[key]-=1
   else:removed.append({'file':source.name,'recipe':entry})
 old_pools=json.loads((archive/'disabled/hbmConfig/_hbmItemPools.json').read_text())['pools']
 new_pools=json.loads((archive/'enabled/hbmConfig/_hbmItemPools.json').read_text())['pools']
 if old_pools!=new_pools:raise RuntimeError('Exported loot defaults changed unexpectedly')
 (archive/'recipe-impact.json').write_text(json.dumps({'removed_machine_recipes':removed,'loot_defaults_identical':True},indent=2)+'\n')
 print('Recorded',len(removed),'removed machine recipes; exported loot defaults are identical.')

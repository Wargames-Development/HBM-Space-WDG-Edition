#!/usr/bin/env python3
"""Exercise compiled environmental effects and cape overrides without starting a game.

Uses a recording WGCore facade to verify coordinates, denials, and client isolation.
Requires Java 8 and the cached Gradle dependencies. Live multiplayer QA is separate.
"""
from pathlib import Path
import os
import shutil
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
java = Path(os.environ['JAVA_HOME']) / 'bin' if 'JAVA_HOME' in os.environ else Path(shutil.which('javac')).parent
with tempfile.TemporaryDirectory(prefix='hbm-environment-') as directory:
    work = Path(directory)
    classpath = work / 'classpath.txt'
    init = work / 'test.init.gradle'
    init.write_text('allprojects { afterEvaluate { task writeEnvironmentalClasspath { doLast { new File(' + repr(str(classpath)) + ').text = sourceSets.main.runtimeClasspath.asPath } } } }')
    subprocess.run([str(root / 'gradlew'), 'compileJava', 'writeEnvironmentalClasspath', '--offline', '--no-daemon', '--init-script', str(init)], cwd=root, check=True)
    cp = classpath.read_text()
    stub = work / 'api/hbm/wgc/Integrations.java'
    stub.parent.mkdir(parents=True)
    stub.write_text('''package api.hbm.wgc;
import java.util.*;
import net.minecraft.world.World;
public class Integrations {
 public static boolean allow = true;
 public static boolean naturalAllow = true;
 public static boolean canNaturalHazardAffectWGC(World world,int sx,int sz,int tx,int tz,String effect) {
  if(world.isRemote) throw new AssertionError("client natural-hazard lookup");
  return naturalAllow;
 }
 public static Set<String> checked = new HashSet<>();
 public static boolean canExplodeBlockWGC(UUID owner, World world, int x, int z) { return false; }
 public static void reset() { allow = true; checked.clear(); }
 public static boolean canContaminateBlockWGC(UUID owner, World world, int x, int z) {
  if(world.isRemote) throw new AssertionError("client territory lookup");
  checked.add(x+","+z); return allow;
 }
 public static boolean canContaminateBlockWGC(UUID owner, World world, double x, double z) {
  return canContaminateBlockWGC(owner, world, (int)Math.floor(x), (int)Math.floor(z));
 }
}
''')
    subprocess.run([str(java / 'javac'), '-cp', cp, '-d', str(work), str(stub), str(root / 'tools/tests/EnvironmentalChecks.java')], check=True)
    (work / 'logs').mkdir()
    subprocess.run([str(java / 'java'), '-cp', str(work) + os.pathsep + cp, 'EnvironmentalChecks', str(root / 'docs/examples/earth40-natural-sources.json')], cwd=work, check=True)

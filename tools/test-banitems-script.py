"""Compile and execute the script adapter with the pack's actual CraftTweaker compiler and mocked registries."""
from pathlib import Path
import os,subprocess,tempfile,sys,json
root=Path(__file__).resolve().parents[1];java=Path(os.environ['JAVA_HOME'])/'bin'
jar=Path(sys.argv[1]);staged=root/'docs/banitems-audit-2026-10-20/pack-script-migration/scripts/MineTweakerRecipeMaker.zs'
with tempfile.TemporaryDirectory(prefix='hbm-banitems-script-') as temporary:
 work=Path(temporary);cpfile=work/'classpath.txt';init=work/'classpath.gradle'
 init.write_text("allprojects { afterEvaluate { task banItemsScriptClasspath { doLast { new File("+repr(str(cpfile))+").text = sourceSets.main.runtimeClasspath.asPath } } } }")
 subprocess.run([str(root/'gradlew'),'banItemsScriptClasspath','--offline','--no-daemon','--init-script',str(init)],cwd=root,check=True)
 cp=str(jar)+os.pathsep+cpfile.read_text()
 subprocess.run([str(java/'javac'),'-cp',cp,'-d',str(work),str(root/'tools/tests/BanItemsScriptChecks.java')],check=True)
 subprocess.run([str(java/'java'),'-cp',str(work)+os.pathsep+cp,'BanItemsScriptChecks',str(staged)],cwd=work,check=True)

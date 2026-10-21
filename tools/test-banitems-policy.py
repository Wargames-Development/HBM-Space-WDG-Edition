#!/usr/bin/env python3
"""Run BanItems policy regressions against compiled HBM and Minecraft classes.

Requires Java 8 via JAVA_HOME (or PATH), Python 3, and cached Gradle dependencies.
No game/server is launched. Forge lifecycle/world behavior still needs live QA.
"""
from pathlib import Path
import os
import shutil
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
java = Path(os.environ["JAVA_HOME"]) / "bin" if "JAVA_HOME" in os.environ else Path(shutil.which("javac") or "javac").parent
with tempfile.TemporaryDirectory(prefix="hbm-integration-") as directory:
    work = Path(directory)
    classpath = work / "classpath.txt"
    init = work / "test.init.gradle"
    init.write_text("allprojects { afterEvaluate { task writeCompatibilityClasspath { doLast { new File(" + repr(str(classpath)) + ").text = sourceSets.main.runtimeClasspath.asPath } } } }")
    subprocess.run([str(root / "gradlew"), "compileJava", "writeCompatibilityClasspath", "--offline", "--no-daemon", "--init-script", str(init)], cwd=root, check=True)
    cp = classpath.read_text()
    subprocess.run([str(java / "javac"), "-cp", cp, "-d", str(work), str(root / "tools/tests/BanItemsChecks.java")], check=True)
    (work / "logs").mkdir()
    subprocess.run([str(java / "java"), "-cp", str(work) + os.pathsep + cp, "BanItemsChecks"], cwd=work, check=True)

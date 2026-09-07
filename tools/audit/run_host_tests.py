#!/usr/bin/env python3
"""Run real pure-JVM Kotlin sources; no Android stubs. Does not replace the Android build."""
import argparse,glob,os,pathlib,subprocess,tempfile
p=argparse.ArgumentParser();p.add_argument('--gradle-lib',required=True);p.add_argument('--test-libs',required=True);a=p.parse_args()
root=pathlib.Path(__file__).resolve().parents[2];os.chdir(root)
cp=os.pathsep.join(glob.glob(a.gradle_lib+'/*.jar')+glob.glob(a.test_libs+'/*.jar'))
sources=['app/src/main/java/com/cryonum/calculator/CalculatorEngine.kt','app/src/main/java/com/cryonum/math/PermutationUtils.kt','app/src/main/java/com/cryonum/pdf/PdfRenderBudget.kt','app/src/test/java/com/cryonum/CalculatorEngineTests.kt','app/src/test/java/com/cryonum/AuditDomainTests.kt']
with tempfile.TemporaryDirectory() as t:
 out=t+'/tests.jar'
 subprocess.run(['java','-cp',cp,'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-classpath',cp,'-d',out]+sources,check=True)
 subprocess.run(['java','-cp',out+os.pathsep+cp,'org.junit.runner.JUnitCore','com.cryonum.CalculatorEngineTests','com.cryonum.AuditDomainTests'],check=True)

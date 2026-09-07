#!/usr/bin/env python3
"""Extract and execute the original normalizer/evaluator, with only Android logging removed."""
import subprocess,pathlib,glob,os,tempfile,argparse
p=argparse.ArgumentParser();p.add_argument('--gradle-lib',required=True);p.add_argument('--test-libs',required=True);a=p.parse_args()
root=pathlib.Path(__file__).resolve().parents[2];os.chdir(root)
sha='3f8220cce6b167a6dc1013e7c544ac13bc14edae'
s=subprocess.check_output(['git','show',sha+':app/src/main/java/com/cryonum/activity/ActivityCalculator.kt'],text=True)
s=s[s.index('    object CalculatorEngine {'):].rsplit('}',1)[0]
s='\n'.join(line for line in s.splitlines() if 'BuildConfig.DEBUG' not in line)
imports='''import org.mariuszgromada.math.mxparser.*
import java.util.*
import java.util.regex.Pattern
import java.text.*
'''
main='''fun main() {
 License.iConfirmNonCommercialUse("CRYONUM audit")
 for ((s,r) in listOf("|-3|" to true, "2@3" to true, "200+10%" to true, "sin(30)" to false, "asin(0.5)" to false, "log(10)" to true, "-2^2" to true, "1e-20" to true, "1/3" to true)) {
  try { println("$s, radians=$r => ${CalculatorEngine.evaluate(s,r)}") } catch(e:Exception) { println("$s, radians=$r => ERROR: ${e.message}") }
 }
}'''
cp=os.pathsep.join(glob.glob(a.gradle_lib+'/*.jar')+glob.glob(a.test_libs+'/*.jar'))
with tempfile.TemporaryDirectory() as t:
 src=pathlib.Path(t)/'Baseline.kt';src.write_text(imports+s+main);out=t+'/baseline.jar'
 subprocess.run(['java','-cp',cp,'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-classpath',cp,'-d',out,str(src)],check=True)
 subprocess.run(['java','-cp',out+os.pathsep+cp,'BaselineKt'],check=True)

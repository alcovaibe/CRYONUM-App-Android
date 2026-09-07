#!/usr/bin/env python3
"""Run the current signature regression suite against the baseline verifier (expect two failures)."""
import pathlib,subprocess,glob,os,zipfile,tempfile,argparse
p=argparse.ArgumentParser();p.add_argument('--gradle-lib',required=True);p.add_argument('--test-libs',required=True);a=p.parse_args()
root=pathlib.Path(__file__).resolve().parents[2];os.chdir(root)
base='3f8220cce6b167a6dc1013e7c544ac13bc14edae'
src=subprocess.check_output(['git','show',base+':app/src/main/java/com/cryonum/content/SignedContentManifestVerifier.kt'],text=True)
with tempfile.TemporaryDirectory() as t:
 t=pathlib.Path(t);source=t/'SignedContentManifestVerifier.kt';source.write_text(src)
 aar=next((pathlib.Path.home()/'.gradle/caches/modules-2/files-2.1/com.squareup.okhttp3/okhttp-android').rglob('*.aar'))
 with zipfile.ZipFile(aar) as z:(t/'okhttp.jar').write_bytes(z.read('classes.jar'))
 cp=os.pathsep.join(glob.glob(a.gradle_lib+'/*.jar')+glob.glob(a.test_libs+'/*.jar')+[str(t/'okhttp.jar')]+glob.glob(str(pathlib.Path.home()/'.gradle/caches/modules-2/files-2.1/com.squareup.okio/okio-jvm/*/*/*.jar')))
 sources=[str(source),'app/src/main/java/com/cryonum/content/ContentManifestModels.kt','app/src/main/java/com/cryonum/content/ApprovedContentUrlPolicy.kt','app/src/test/java/com/cryonum/content/SignedContentManifestVerifierTest.kt']
 subprocess.run(['java','-cp',cp,'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-Xskip-metadata-version-check','-no-stdlib','-no-reflect','-classpath',cp,'-d',str(t/'tests.jar')]+sources,check=True)
 r=subprocess.run(['java','-cp',str(t/'tests.jar')+os.pathsep+cp,'org.junit.runner.JUnitCore','com.cryonum.content.SignedContentManifestVerifierTest'],capture_output=True,text=True)
 print(r.stdout,r.stderr)
 assert r.returncode!=0 and 'Tests run: 14,  Failures: 2' in r.stdout,'Expected precisely the two overflow regressions to fail on baseline'
 print('Baseline reproduction confirmed: two new overflow regressions fail; the other twelve pass.')

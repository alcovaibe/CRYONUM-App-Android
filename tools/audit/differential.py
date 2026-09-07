#!/usr/bin/env python3
"""Independent deterministic Fraction/cmath oracle, exercised via a JVM subprocess."""
import argparse,pathlib,random,fractions,cmath,math,subprocess,glob,os,tempfile
p=argparse.ArgumentParser();p.add_argument('--gradle-lib',required=True);p.add_argument('--test-libs',required=True);a=p.parse_args()
root=pathlib.Path(__file__).resolve().parents[2];os.chdir(root)
cp=os.pathsep.join(glob.glob(a.gradle_lib+'/*.jar')+glob.glob(a.test_libs+'/*.jar'))
r=random.Random(20260907);cases=[]
for _ in range(1500):
 x,y,z,w=[r.randrange(1,1000) for _ in range(4)];op=r.choice(['+','-','*','/'])
 f=fractions.Fraction(x,y);g=fractions.Fraction(z,w);v={'+':lambda:f+g,'-':lambda:f-g,'*':lambda:f*g,'/':lambda:f/g}[op]()
 cases.append((f'({x}/{y}){op}({z}/{w})',str(v),None))
for _ in range(500):
 x,y=[r.uniform(-2,2) for _ in range(2)];name=r.choice(['sin','cos','tan','ln','exp','sqrt','asin','acos','atan'])
 v=getattr(cmath,'log' if name=='ln' else name)(complex(x,y));cases.append((f'{name}({x}+({y})*i)',v.real,v.imag))
with tempfile.TemporaryDirectory() as t:
 source=pathlib.Path(t)/'OracleProbe.kt';out=t+'/oracle.jar'
 source.write_text('''import com.cryonum.calculator.*
fun main(){generateSequence(::readLine).forEach { s -> try { val v=CalculatorEngine.evaluate(s,complex=true); if(v.exact) println("E\\t"+v.display()) else {val z=v.complex();println("A\\t${z.real}\\t${z.imaginary}")} } catch(e:Exception){println("ERROR\\t${e.message}")} }}''')
 subprocess.run(['java','-cp',cp,'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-classpath',cp,'-d',out,'app/src/main/java/com/cryonum/calculator/CalculatorEngine.kt',str(source)],check=True)
 run=subprocess.run(['java','-cp',out+os.pathsep+cp,'OracleProbeKt'],input='\n'.join(x[0] for x in cases),capture_output=True,text=True,check=True)
 lines=run.stdout.splitlines();assert len(lines)==len(cases),(len(lines),run.stderr)
 failures=[]
 for case,line in zip(cases,lines):
  expr,re,im=case;parts=line.split('\t')
  if im is None: ok=parts==['E',re]
  else:ok=parts[0]=='A' and math.isclose(float(parts[1]),re,rel_tol=1e-11,abs_tol=1e-12) and math.isclose(float(parts[2]),im,rel_tol=1e-11,abs_tol=1e-12)
  if not ok:failures.append((case,line))
 print('seed=20260907; exact Fraction cases=1500; complex cmath cases=500; failures=',len(failures))
 for f in failures[:20]:print(f)
 assert not failures

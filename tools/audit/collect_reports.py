#!/usr/bin/env python3
"""Collect existing build evidence; never substitutes missing reports with success."""
from pathlib import Path
import json,hashlib,shutil,xml.etree.ElementTree as ET,collections
root=Path(__file__).resolve().parents[2];out=root/'docs/audit/reports';out.mkdir(exist_ok=True)
summary={};suites=[]
for p in sorted((root/'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml')):
 r=ET.parse(p).getroot();suites.append({k:r.get(k) for k in ('name','tests','failures','errors','skipped','time')});shutil.copy2(p,out/p.name)
summary['junit']=suites
for variant in ['debug','release']:
 p=root/f'app/build/reports/lint-results-{variant}.xml'
 r=ET.parse(p).getroot();issues=r.findall('issue');summary['lint_'+variant]={'severity':dict(collections.Counter(x.get('severity') for x in issues)),'ids':dict(collections.Counter(x.get('id') for x in issues))}
 for suffix in ['xml','html','txt']:
  source=p.with_suffix('.'+suffix);shutil.copy2(source,out/source.name)
 p=root/f'app/build/intermediates/merged_manifest/{variant}/process{variant.title()}MainManifest/AndroidManifest.xml'
 shutil.copy2(p,out/f'merged-manifest-{variant}.xml')
 r=ET.parse(p).getroot();a='{http://schemas.android.com/apk/res/android}';app=r.find('application')
 summary['manifest_'+variant]={'package':r.get('package'),'permissions':[x.get(a+'name') for x in r.findall('uses-permission')],'exported':[{'type':x.tag,'name':x.get(a+'name'),'permission':x.get(a+'permission')} for x in app if x.get(a+'exported')=='true']}
artifacts=[]
for p in sorted((root/'app/build/outputs/apk').rglob('*.apk')):
 artifacts.append({'path':str(p.relative_to(root)),'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})
summary['apks']=artifacts
p=root/'app/build/outputs/mapping/release/mapping.txt';summary['r8_mapping']={'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'location':str(p.relative_to(root))}
(root/'docs/audit/logs/artifacts.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n')
print('JUnit',sum(int(s['tests']) for s in suites),'failures',sum(int(s['failures'])+int(s['errors']) for s in suites))
for v in ['debug','release']:print(v,summary['lint_'+v],summary['manifest_'+v])
print('APKs',artifacts)

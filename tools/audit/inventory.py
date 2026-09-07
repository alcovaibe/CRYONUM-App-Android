#!/usr/bin/env python3
"""Reproducible inventory of every tracked baseline path; static evidence, not line coverage."""
import subprocess,pathlib,csv,hashlib,xml.etree.ElementTree as ET,json,re,gzip,zipfile
from PIL import Image
root=pathlib.Path(__file__).resolve().parents[2]
base='3f8220cce6b167a6dc1013e7c544ac13bc14edae'
baseline=set(subprocess.check_output(['git','ls-tree','-r','--name-only',base],cwd=root,text=True).splitlines())
paths=sorted(baseline | set(subprocess.check_output(['git','ls-files'],cwd=root,text=True).splitlines()))
rows=[]
for name in paths:
 p=root/name;raw=p.read_bytes();method='';result='';findings=''
 if name=='docs/audit/coverage.csv':
  rows.append([name,'generated','self','Сформирован этим скриптом','Хеш самоссылочного реестра неприменим','']);continue
 if p.suffix=='.gz':
  gzip.decompress(raw);method='Gzip decompression, SHA-256';result='Архив читается'
 elif p.suffix in ['.png','.webp']:
  with Image.open(p) as im:im.verify()
  method='Декодирование Pillow, SHA-256';result='Структура изображения читается; визуальный QA не выполнен'
 elif p.suffix=='.xml':
  ET.fromstring(raw);method='XML parser, поиск атрибутов и ссылок';result='XML корректен; внешний вид/TalkBack не проверены'
 elif p.suffix=='.jar':
  method='SHA-256, исполнение Gradle Wrapper';result='Работает; происхождение wrapper отдельно не удостоверено'
 elif p.suffix=='.kt':
  method='Статический поиск, компиляция/тесты согласно logs';result='Не означает построчное ревью или сквозной тест'
  if '/calculator/' in name or 'Calculator' in name:findings='CALC-01–06'
  elif any(s in name for s in ['Security','AppLock','MyApplication']):findings='SEC-01–03'
  elif '/content/' in name:findings='CONTENT-01–05'
  elif 'Analytics' in name:findings='PRIV-01–03'
  elif '/pdf/' in name:findings='PERF-01'
  elif any(s in name for s in ['Permutation','Substitution','InputFilter','ImagePicker']):findings='MATH-01; OCR-01'
  elif 'History' in name:findings='DATA-01; DATA-02'
 elif p.suffix=='.py':
  compile(raw,name,'exec');method='Чтение сценария, Python compile; tests по logs';result='Синтаксис корректен; R2 --apply не выполнялся'
 else:
  raw.decode('utf-8');method='Текстовый просмотр/поиск, SHA-256';result='Область проверки указана в отчёте'
 rows.append([name,len(raw),hashlib.sha256(raw).hexdigest(),method,result,findings])
out=root/'docs/audit/coverage.csv'
with out.open('w',newline='',encoding='utf-8') as f:
 w=csv.writer(f);w.writerow(['path','current_bytes','current_sha256','method','limits_and_result','findings']);w.writerows(rows)
print('baseline tracked paths:',len(baseline),'current indexed inventory:',len(rows),'not a statement of full semantic coverage')
# Resource key consistency is not translation quality.
sets={}
for folder in ['values','values-en','values-de','values-fr']:
 files=list((root/'app/src/main/res'/folder).glob('*string*.xml'));entries=[]
 for p in files:entries += [x.get('name') for x in ET.parse(p).getroot() if x.tag=='string']
 sets[folder]=set(entries)
 print(folder,'strings',len(entries),'duplicates',len(entries)-len(set(entries)),'missing',sorted(sets.get('values',set())-set(entries)))

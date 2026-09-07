import sys,io,json,time,uuid,pathlib,urllib.request,urllib.error
from integration import request,check,expect_error,results,BASE,ROOT
def no_evidence():
 s=request('/sessions',method='POST')['id']
 r=urllib.request.urlopen(urllib.request.Request(BASE+'/chat',data=json.dumps({'session_id':s,'question':'xenonfrobozzquux'}).encode(),headers={'Content-Type':'application/json'}))
 text=r.read().decode();assert '没有找到足够依据' in text and 'event:done' in text.replace(' ','');return '无相关资料时明确拒绝编造'
def cancellation():
 s=request('/sessions',method='POST')['id'];r=urllib.request.urlopen(urllib.request.Request(BASE+'/chat',data=json.dumps({'session_id':s,'question':'请详细解释数据库、SQL、范式和事务，逐项介绍。'},ensure_ascii=False).encode(),headers={'Content-Type':'application/json'}))
 event='';run=None
 for line in r:
  line=line.decode().strip()
  if line.startswith('event:'):event=line[6:].strip()
  if line.startswith('data:') and event=='meta':run=json.loads(line[5:])['run_id'];break
 assert run;request('/chat/'+run+'/cancel',method='POST');r.read();r.close()
 history=request('/sessions/'+s+'/messages');assert any(m['status']=='cancelled' for m in history);return '生成取消且状态持久化'
def bad_pdf():
 boundary=uuid.uuid4().hex;body=(f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="bad.pdf"\r\nContent-Type: application/pdf\r\n\r\nnot a pdf\r\n--{boundary}--\r\n').encode()
 return expect_error(lambda:urllib.request.urlopen(urllib.request.Request(BASE+'/documents',data=body,headers={'Content-Type':'multipart/form-data; boundary='+boundary})),400)
def cross_origin():
 return expect_error(lambda:urllib.request.urlopen(urllib.request.Request(BASE+'/sessions',data=b'',headers={'Origin':'https://example.org'})),403)
check('无关问题不编造答案',no_evidence)
check('取消生成并保存取消状态',cancellation)
check('损坏PDF被拒绝',bad_pdf)
check('跨站写入请求被拒绝',cross_origin)
check('不存在的计划返回404',lambda:expect_error(lambda:request('/plans/'+str(uuid.uuid4())),404))
check('非法打卡类型被拒绝',lambda:expect_error(lambda:request('/tasks/'+str(uuid.uuid4()),{'done':'true'},'PATCH'),400))
(ROOT/'tests'/'edge-results.json').write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf-8')
print('RESULT',sum(r['passed'] for r in results),'/',len(results))

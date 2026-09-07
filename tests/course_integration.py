"""真实服务课程隔离验收；只创建和清理本次 UUID 标记的测试课程。不会修改旧课程。"""
import json, pathlib, time, urllib.request, urllib.error, uuid
ROOT=pathlib.Path(__file__).resolve().parents[1]
BASE='http://127.0.0.1:18080/api'
results=[]
def call(path,body=None,method=None):
    req=urllib.request.Request(BASE+path,data=None if body is None else json.dumps(body).encode(),method=method,headers={'Content-Type':'application/json'})
    with urllib.request.urlopen(req,timeout=300) as r:return json.load(r)
def denied(fn):
    try:fn()
    except urllib.error.HTTPError as e:
        assert e.code==404,(e.code,e.read());return
    raise AssertionError('Cross-course access was accepted')
def upload(course,path):
    boundary='Course'+uuid.uuid4().hex
    body=(f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="sample.pdf"\r\nContent-Type: application/pdf\r\n\r\n'.encode()+path.read_bytes()+f'\r\n--{boundary}--\r\n'.encode())
    req=urllib.request.Request(BASE+'/documents?courseId='+course,data=body,headers={'Content-Type':'multipart/form-data; boundary='+boundary})
    with urllib.request.urlopen(req,timeout=90) as r:return json.load(r)
def chat(course,session,use_refs):
    body={'courseId':course,'session_id':session,'question':'请用两句话解释数据库事务。','useReferences':use_refs}
    req=urllib.request.Request(BASE+'/chat',data=json.dumps(body).encode(),headers={'Content-Type':'application/json'})
    with urllib.request.urlopen(req,timeout=300) as r:raw=r.read().decode()
    assert 'event:done' in raw.replace('event: ','event:') and 'event:error' not in raw.replace('event: ','event:'),raw
    messages=call('/sessions/'+session+'/messages?courseId='+course)
    assert any(m['role']=='assistant' and m['content'].strip() for m in messages),messages
    return raw

def run(name,fn):
    begin=time.time()
    try:fn();results.append({'test':name,'passed':True,'seconds':round(time.time()-begin,2)})
    except Exception as e:results.append({'test':name,'passed':False,'error':str(e)})
    print(results[-1],flush=True)

def main():
    created=[]
    original={c['id'] for c in call('/courses')}
    try:
        for n in ('A','B'):created.append(call('/courses',{'name':'隔离测试'+n+'-'+uuid.uuid4().hex[:8]})['id'])
        a,b=created
        path=next((ROOT/'samples').glob('*.pdf'))
        da=upload(a,path);db=upload(b,path)
        def separate_docs():
            assert {d['id'] for d in call('/documents?courseId='+a)}=={da['id']}
            assert {d['id'] for d in call('/documents?courseId='+b)}=={db['id']}
        run('same_pdf_allowed_in_different_courses_and_lists_isolated',separate_docs)
        run('cross_course_file_read_blocked',lambda:denied(lambda:call('/documents/'+da['id']+'/file?courseId='+b)))
        run('cross_course_delete_blocked',lambda:denied(lambda:call('/documents/'+da['id']+'?courseId='+b,method='DELETE')))
        session=call('/sessions?courseId='+a,{},'POST')['id']
        run('cross_course_messages_blocked',lambda:denied(lambda:call('/sessions/'+session+'/messages?courseId='+b)))
        run('cross_course_chat_blocked',lambda:denied(lambda:call('/chat',{'courseId':b,'session_id':session,'question':'数据库是什么','useReferences':False})))
        run('general_chat_without_references',lambda:chat(a,session,False))
        call('/documents/'+db['id']+'?courseId='+b,method='DELETE')
        empty_session=call('/sessions?courseId='+b,{},'POST')['id']
        run('empty_course_automatically_answers',lambda:chat(b,empty_session,True))
        def references():
            chat(a,session,True)
            for message in call('/sessions/'+session+'/messages?courseId='+a):
                assert all(s.get('document_id')==da['id'] for s in message.get('sources',[]))
        run('reference_citations_stay_in_course',references)
    finally:
        for course in created:
            try:call('/courses/'+course,method='DELETE')
            except Exception as e:results.append({'test':'cleanup_own_course','passed':False,'error':str(e),'course':course})
        assert original.issubset({c['id'] for c in call('/courses')}),'Original courses were removed'
        (ROOT/'tests'/'course-integration-results.json').write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf-8')
    if any(not r['passed'] for r in results):raise SystemExit(1)
if __name__=='__main__':main()

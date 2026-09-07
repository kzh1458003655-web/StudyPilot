"""Run against the real local services. No mock language-model responses."""
import urllib.request,urllib.error,json,pathlib,time,uuid,datetime
ROOT=pathlib.Path(__file__).resolve().parents[1]
BASE='http://127.0.0.1:18080/api'
results=[]
def request(path,body=None,method=None,timeout=300):
    data=json.dumps(body,ensure_ascii=False).encode() if body is not None else None
    r=urllib.request.urlopen(urllib.request.Request(BASE+path,data=data,method=method,headers={'Content-Type':'application/json'}),timeout=timeout)
    return json.load(r)
def check(name,fn):
    start=time.perf_counter()
    try: detail=fn();results.append({'test':name,'passed':True,'seconds':round(time.perf_counter()-start,2),'detail':detail});print('PASS',name,flush=True);return detail
    except Exception as e:results.append({'test':name,'passed':False,'seconds':round(time.perf_counter()-start,2),'detail':str(e)});print('FAIL',name,str(e),flush=True)
def expect_error(fn,code):
    try:fn()
    except urllib.error.HTTPError as e:
        assert e.code==code,(e.code,e.read().decode());return f'HTTP {code}'
    raise AssertionError('Expected HTTP error')
def sample():
    from reportlab.pdfgen import canvas
    from reportlab.pdfbase import pdfmetrics
    from reportlab.pdfbase.cidfonts import UnicodeCIDFont
    pdfmetrics.registerFont(UnicodeCIDFont('STSong-Light'))
    dest=ROOT/'samples'/'数据库课程示例讲义.pdf';dest.parent.mkdir(exist_ok=True)
    pages=[('第一章 关系数据库与第三范式',[
        '本讲义为项目功能测试编写的示例资料，不是学校正式课件。',
        '关系数据库用二维表组织数据，主键唯一标识每一条记录。',
        '外键建立表之间的引用关系，参照完整性防止出现无效引用。',
        '第一范式要求属性值不可再分；第二范式消除非主属性对候选键的部分函数依赖。',
        '第三范式要求对每个非平凡函数依赖X→A，X为超键或A为主属性。',
        '直观上，第三范式需要避免非主属性通过其他非主属性依赖候选键。',
        '例如选课记录中反复存储课程名称会产生冗余，课程名称应放入课程表。',
        '分解关系时还需要考虑无损连接和函数依赖保持，不能只增加表的数量。']),
      ('第二章 SQL查询与索引',[
        'SELECT用于查询，WHERE筛选行，GROUP BY按属性分组，HAVING筛选分组。',
        'INNER JOIN返回符合连接条件的记录，LEFT JOIN保留左表全部记录。',
        'COUNT统计数量，SUM求和，AVG计算平均值。',
        '索引能够提高特定查询速度，但会增加存储开销和写入维护成本。',
        '复合索引的字段顺序应结合查询条件和排序需求设计。',
        '使用EXPLAIN观察执行计划，不能认为每一个字段都应该建立索引。']),
      ('第三章 事务与数据一致性',[
        '事务的ACID特性分别是原子性、一致性、隔离性和持久性。',
        '原子性保证事务中的操作全部成功或全部回滚。',
        '一致性要求事务执行前后满足数据库约束。',
        '隔离性描述并发事务之间的相互影响程度。持久性保证提交后的修改得到保存。',
        '创建复习计划及全部子任务适合放入一个事务，防止只保存了部分任务。',
        '版本号可以检测并发修改，发现版本变化后应拒绝覆盖旧结果并提示重新操作。'])]
    c=canvas.Canvas(str(dest),invariant=1);c.setTitle('数据库课程示例讲义')
    for title,lines in pages:
        c.setFont('STSong-Light',18);c.drawString(55,790,title);c.setFont('STSong-Light',12)
        for i,line in enumerate(lines):
            for j in range(0,len(line),36):c.drawString(55,745-i*55-(j//36)*20,line[j:j+36])
        c.showPage()
    c.save();return dest
def upload(path):
    boundary='Study'+uuid.uuid4().hex
    body=(f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{path.name}"\r\nContent-Type: application/pdf\r\n\r\n'.encode()+path.read_bytes()+f'\r\n--{boundary}--\r\n'.encode())
    r=urllib.request.urlopen(urllib.request.Request(BASE+'/documents',data=body,headers={'Content-Type':'multipart/form-data; boundary='+boundary}),timeout=90)
    return json.load(r)
def chat():
    session=request('/sessions',method='POST')['id'];start=time.perf_counter()
    r=urllib.request.urlopen(urllib.request.Request(BASE+'/chat',data=json.dumps({'session_id':session,'question':'请解释事务的ACID特性，并说明创建复习计划时为什么需要事务。'},ensure_ascii=False).encode(),headers={'Content-Type':'application/json'}),timeout=240)
    event='';answer='';sources=[];first=None;finished=None
    for line in r:
        line=line.decode().strip()
        if line.startswith('event:'):event=line[6:].strip()
        if line.startswith('data:'):
            d=json.loads(line[5:])
            if event=='sources':sources=d
            if event=='delta':
                if first is None:first=time.perf_counter()-start
                answer+=d['text']
            if event=='error':raise AssertionError(d)
            if event=='done':finished=d['status']
    assert finished=='complete' and len(answer)>40 and sources
    assert any(s['page']==3 for s in sources)
    history=request('/sessions/'+session+'/messages');assert len(history)==2
    return {'session_id':session,'answer':answer,'sources':sources,'first_text_seconds':round(first,3),'total_seconds':round(time.perf_counter()-start,3)}
if __name__=='__main__':
    check('服务与模型健康检查',lambda:request('/health'))
    path=sample();existing=request('/documents')
    for d in existing:
        if d['name']==path.name:request('/documents/'+d['id'],method='DELETE')
    doc=check('上传并索引文字PDF',lambda:upload(path))
    check('重复资料被拒绝',lambda:expect_error(lambda:upload(path),400))
    check('真实模型问答及引用保存',chat)
    form={'goal':'复习关系数据库的范式、SQL与事务','weak_points':'第三范式与事务','start_date':datetime.date.today().isoformat(),'days':3,'daily_minutes':60}
    plan=check('真实Agent生成计划',lambda:request('/plans',form))
    if plan:
        task=plan['tasks'][0]
        check('完成任务打卡',lambda:request('/tasks/'+task['id'],{'done':True},'PATCH'))
        form['instruction']='保留已经完成的任务，剩余任务更加侧重事务。'
        revised=check('真实Agent调整计划',lambda:request('/plans/'+plan['id']+'/revise',form))
        if revised:
            def preserved():
                found=next(t for t in revised['tasks'] if t['id']==task['id']);assert found['done'];return found
            check('调整后完成记录被保留',preserved)
    check('非法时间条件被拒绝',lambda:expect_error(lambda:request('/plans',{**form,'days':0}),400))
    out=ROOT/'tests'/'integration-results.json';out.write_text(json.dumps({'timestamp':datetime.datetime.now().isoformat(),'results':results},ensure_ascii=False,indent=2),encoding='utf-8')
    print('RESULT',sum(r['passed'] for r in results),'/',len(results),flush=True)

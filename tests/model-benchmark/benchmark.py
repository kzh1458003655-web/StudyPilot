"""可复跑的本地小模型Benchmark。

比较目标：Qwen3.5-4B、Qwen3-4B、Phi-4-mini-instruct 的 Q4_K_M GGUF。
每个模型由本脚本独占启动一个 llama.cpp 服务。脚本记录原始请求、SSE、
首Token时间、生成速度、整卡显存采样及自动评分结果，便于报告复核。
"""
import argparse, csv, hashlib, json, pathlib, re, statistics, subprocess, threading, time
import requests

ROOT = pathlib.Path(__file__).resolve().parent
SERVER = pathlib.Path(r"D:\StudyPilot-runtime\llama\llama-server.exe")
PORT = 18090

# id, 学科, 题干, A, B, C, D, 标准答案。题目均为单选、答案固定的基础知识题。
MCQ = [
 ("db01","数据库","事务的原子性表示什么？","事务可部分完成","事务操作要么全成要么全败","事务必须并行","事务永不回滚","B"),
 ("db02","数据库","关系模型中，能够唯一标识一个元组的属性集合称为？","外码","候选码","非主属性","视图","B"),
 ("db03","数据库","SQL中用于从表中查询数据的语句是？","SELECT","INSERT","UPDATE","DELETE","A"),
 ("db04","数据库","若R(A,B)中A函数决定B，正确记法是？","B→A","A→B","A=B","A∈B","B"),
 ("db05","数据库","第三范式主要消除的是？","部分函数依赖","传递函数依赖","所有主键","所有外键","B"),
 ("db06","数据库","B+树索引的叶子结点通常具有什么特点？","按键值有序并可链式访问","不保存键值","只保存根结点","不能范围查询","A"),
 ("db07","数据库","两个事务都在等待对方释放锁时，系统发生？","活锁","死锁","饥饿","回滚","B"),
 ("db08","数据库","SQL的WHERE子句主要用于？","分组","筛选行","排序","创建表","B"),
 ("db09","数据库","内连接的结果是？","两个表所有行","满足连接条件的行","左表所有行","右表所有行","B"),
 ("db10","数据库","数据库恢复中，日志的主要作用是？","加快显示","保证故障后可恢复","替代备份","删除重复数据","B"),
 ("db11","数据库","事务隔离性主要避免？","数据永久丢失","并发事务相互干扰","磁盘损坏","表结构改变","B"),
 ("db12","数据库","COUNT(*)的作用是？","计算列平均值","统计结果行数","删除重复行","创建索引","B"),
 ("db13","数据库","外码通常用于？","保证实体完整性","表示表之间的参照关系","加密数据","排序数据","B"),
 ("db14","数据库","创建索引的主要目的通常是？","减少表中数据量","加快特定查询","替代主键","避免事务","B"),
 ("db15","数据库","下列哪项属于DCL？","GRANT","SELECT","CREATE","INSERT","A"),
 ("os01","操作系统","进程是？","程序的一次执行过程","一个源代码文件","CPU中的寄存器","硬盘分区","A"),
 ("os02","操作系统","线程与同进程其他线程通常共享？","程序计数器","栈","地址空间","寄存器","C"),
 ("os03","操作系统","操作系统进行CPU调度的基本单位通常是？","文件","线程","磁盘块","进程映像","B"),
 ("os04","操作系统","银行家算法用于避免？","缺页","死锁","中断","文件损坏","B"),
 ("os05","操作系统","页面置换发生在？","CPU时间片结束","内存页不足需调入新页","创建文件","网络连接","B"),
 ("os06","操作系统","虚拟内存主要解决？","CPU太慢","内存容量有限","显示器分辨率低","网络拥塞","B"),
 ("os07","操作系统","信号量P操作通常表示？","释放资源","申请或等待资源","创建进程","关闭中断","B"),
 ("os08","操作系统","时间片轮转调度适合？","分时交互系统","只运行一个任务","磁盘格式化","编译程序","A"),
 ("os09","操作系统","发生缺页中断时，系统首先应？","直接终止进程","将所需页调入内存","清空磁盘","删除所有缓存","B"),
 ("os10","操作系统","互斥锁最主要的作用是？","加速网络","保护临界区","扩大内存","建立文件","B"),
 ("net01","计算机网络","TCP属于哪一层协议？","应用层","传输层","网络层","数据链路层","B"),
 ("net02","计算机网络","IP地址主要用于？","标识网络层主机接口","加密网页","替代端口号","显示网页","A"),
 ("net03","计算机网络","DNS的主要作用是？","域名解析为IP地址","传输文件","分配MAC地址","加密数据包","A"),
 ("net04","计算机网络","HTTP默认使用的传输层协议是？","TCP","UDP","ICMP","ARP","A"),
 ("net05","计算机网络","MAC地址主要工作在？","物理层","数据链路层","网络层","应用层","B"),
 ("net06","计算机网络","路由器主要依据什么转发IP分组？","MAC地址表","路由表","文件目录","端口号","B"),
 ("net07","计算机网络","TCP三次握手主要用于？","建立可靠连接","关闭连接","解析域名","分配IP","A"),
 ("net08","计算机网络","ARP协议用于查询？","域名对应IP","IP对应MAC地址","端口对应进程","网址对应文件","B"),
 ("net09","计算机网络","UDP的特点是？","面向连接且可靠","无连接且不保证可靠","必须三次握手","只能传文件","B"),
 ("net10","计算机网络","子网掩码的主要用途是？","判断网络号和主机号","加密IP","增加端口","压缩报文","A"),
 ("alg01","数据结构算法","栈的操作特点是？","先进先出","后进先出","随机访问","按优先级出队","B"),
 ("alg02","数据结构算法","队列通常遵循？","后进先出","先进先出","随机访问","二分查找","B"),
 ("alg03","数据结构算法","二分查找的前提是？","数据有序","数据无重复","使用链表","数据量为偶数","A"),
 ("alg04","数据结构算法","快速排序的平均时间复杂度是？","O(1)","O(log n)","O(n log n)","O(n²)","C"),
 ("alg05","数据结构算法","链表相对数组的主要优点是？","随机访问快","插入删除无需移动大量元素","占用内存一定更少","天然有序","B"),
 ("alg06","数据结构算法","广度优先遍历通常使用？","栈","队列","二叉树","哈希表","B"),
 ("alg07","数据结构算法","深度优先遍历通常使用？","队列","栈或递归","优先队列","数组排序","B"),
 ("alg08","数据结构算法","哈希表平均查找时间复杂度通常为？","O(1)","O(log n)","O(n)","O(n²)","A"),
 ("alg09","数据结构算法","完全二叉树中，若结点下标从1开始，结点i的左孩子下标是？","i+1","2i","2i+1","i/2","B"),
 ("alg10","数据结构算法","Dijkstra算法适用于？","有负权回路图","非负权有向图或无向图","只能无权图","只有树结构","B"),
 ("java01","Java软件工程","Java中实现接口使用的关键字是？","extends","implements","import","package","B"),
 ("java02","Java软件工程","JVM的作用是？","执行Java字节码","替代数据库","编译HTML","管理Git仓库","A"),
 ("java03","Java软件工程","try-catch主要用于？","循环控制","异常处理","创建线程","声明包名","B"),
 ("java04","Java软件工程","REST接口中GET通常用于？","读取资源","删除资源","创建资源","编译资源","A"),
 ("java05","Java软件工程","单元测试主要验证？","单个函数或类的行为","整个机房网络","显示器色彩","操作系统安装","A"),
 ("logic01","计算逻辑","45分钟连续学习8天，共计多少小时？","5小时","6小时","7小时","8小时","B"),
 ("logic02","计算逻辑","若x+3=10，则x等于？","5","6","7","8","C"),
 ("logic03","计算逻辑","一个班有40人，其中25人通过考试，通过率是？","40%","50%","62.5%","75%","C"),
 ("logic04","计算逻辑","二进制1010转换为十进制是？","8","9","10","12","C"),
 ("logic05","计算逻辑","3天共90分钟且每天时长相同，每天多少分钟？","20","30","40","45","B"),
 ("logic06","计算逻辑","若所有A都是B，且某对象是A，则该对象？","一定是B","一定不是B","可能不是B","无法判断","A"),
 ("logic07","计算逻辑","一个数组含100个元素，顺序查找最坏情况下比较次数是？","1","10","50","100","D"),
 ("logic08","计算逻辑","5个实验中完成3个，完成率是？","40%","50%","60%","80%","C"),
 ("logic09","计算逻辑","若今天是周一，3天后是？","周二","周三","周四","周五","C"),
 ("logic10","计算逻辑","一个5分题有三个评分点，得分2、2、1，覆盖前两个评分点应得？","2分","3分","4分","5分","C"),
]

RAG = [
 ("rag01", "[数据库讲义，第3页] 课程成绩由平时作业30%、实验20%、期末考试50%组成。", "仅依据资料回答并输出JSON：{\"answer\":\"数字\",\"page\":页码,\"answerable\":true}。问题：实验成绩占多少？", "20%", 3, True),
 ("rag02", "[算法讲义，第5页] 顺序查找时间复杂度O(n)，不要求有序。 [算法讲义，第6页] 折半查找时间复杂度O(log n)，要求有序。", "仅依据资料回答并输出JSON：{\"answer\":\"简短比较\",\"page\":[页码],\"answerable\":true}。问题：折半查找需要什么前提？", "有序", 6, True),
 ("rag03", "[课程说明，第2页] 本课程共32学时。", "仅依据资料输出JSON：{\"answerable\":false} 或 {\"answer\":\"...\",\"page\":页码,\"answerable\":true}。问题：期末考试日期是哪天？", None, None, False),
 ("rag04", "[实验安排，第4页] 第一阶段完成2个实验，第二阶段完成3个实验。", "仅依据资料回答并输出JSON：{\"answer\":\"数字\",\"page\":页码,\"answerable\":true}。问题：合计多少个实验？", "5", 4, True),
 ("rag05", "[网络讲义，第8页] TCP面向连接，提供可靠字节流服务。UDP无连接，不保证可靠传输。", "仅依据资料回答并输出JSON：{\"answer\":\"简短回答\",\"page\":页码,\"answerable\":true}。问题：哪个协议提供可靠传输？", "TCP", 8, True),
 ("rag06", "[数据库讲义，第9页] 外码用于表示关系之间的参照关系。", "仅依据资料回答并输出JSON：{\"answer\":\"简短回答\",\"page\":页码,\"answerable\":true}。问题：外码有什么作用？", "参照", 9, True),
]

GENERATION = [
 ("gen01","事务",3,"choice"), ("gen02","SQL查询",3,"choice"), ("gen03","TCP可靠传输",2,"choice"),
 ("gen04","二分查找",2,"choice"), ("gen05","进程与线程",3,"choice"), ("gen06","B+树索引",2,"choice"),
]

GRADING = [
 ("grade01","解释事务原子性",5,"事务中操作要么全部成功，要么全部失败，不能只完成其中一部分。",["整体性","全成全败","不能部分完成"],5,["P1","P2","P3"]),
 ("grade02","解释事务原子性",5,"操作要么全部成功，要么全部失败。",["整体性","全成全败","不能部分完成"],3,["P2","P3"]),
 ("grade03","解释事务原子性",5,"事务保证数据不会丢失。",["整体性","全成全败","不能部分完成"],0,[]),
 ("grade04","说明二分查找前提",4,"待查找数组必须有序。",["数组有序","比较中间元素"],2,["P1"]),
 ("grade05","说明二分查找前提",4,"数据有序，每次比较中间元素并缩小一半范围。",["数组有序","比较中间元素"],4,["P1","P2"]),
 ("grade06","说明TCP特点",4,"TCP无连接，不能保证数据到达。",["面向连接","可靠传输"],0,[]),
 ("grade07","说明TCP特点",4,"TCP先建立连接，并通过确认和重传等机制保证可靠传输。",["面向连接","可靠传输"],4,["P1","P2"]),
 ("grade08","说明索引代价",4,"索引会占用额外存储，写入和更新时需要维护索引。",["额外存储","写入维护开销"],4,["P1","P2"]),
]

PERF = "请用约180个中文字符解释数据库事务的隔离性，并举一个并发读写场景。"

def json_from(text):
    text = text.strip()
    candidates = [text]
    candidates.extend(re.findall(r"```(?:json)?\s*(\{.*?\})\s*```", text, re.S))
    candidates.extend(re.findall(r"(\{.*\})", text, re.S))
    for item in candidates:
        try: return json.loads(item)
        except (ValueError, TypeError): pass
    return None

def start_server(model, out):
    # 18090是本Benchmark专用端口。清除上次异常退出遗留的监听者，防止本轮
    # 错把上一款模型当成已就绪服务，从而造成“文件名和实际模型不一致”的假结果。
    listing=subprocess.run(["netstat","-ano","-p","tcp"],capture_output=True,text=True,creationflags=0x08000000).stdout
    for line in listing.splitlines():
        if f"127.0.0.1:{PORT}" in line and "LISTENING" in line:
            pid=line.split()[-1]
            subprocess.run(["taskkill","/PID",pid,"/F"],capture_output=True,creationflags=0x08000000)
    time.sleep(.5)
    log = (out / "server.log").open("w", encoding="utf-8")
    command = [str(SERVER), "-m", str(model), "--host", "127.0.0.1", "--port", str(PORT), "-ngl", "99", "-c", "4096", "-np", "1", "--jinja", "--reasoning-budget", "0"]
    proc = subprocess.Popen(command, stdout=log, stderr=log, creationflags=0x08000000)
    session = requests.Session(); session.trust_env = False
    for _ in range(120):
        if proc.poll() is not None: raise RuntimeError("llama.cpp启动失败，请查看server.log")
        try:
            if session.get(f"http://127.0.0.1:{PORT}/health", timeout=2).ok: return proc, log, session, command
        except requests.RequestException: pass
        time.sleep(1)
    proc.terminate(); log.close(); raise RuntimeError("模型120秒内未就绪")

def ask(session, ident, category, prompt, max_tokens=128):
    body = {"model":"local", "messages":[
        {"role":"system","content":"你是中文计算机课程助手。严格服从用户指定的输出JSON格式；不确定时不要编造。"},
        {"role":"user","content":prompt}], "temperature":0, "seed":42, "max_tokens":max_tokens,
        "stream":True, "stream_options":{"include_usage":True}, "cache_prompt":False,
        "chat_template_kwargs":{"enable_thinking":False}}
    start = time.perf_counter(); first=None; parts=[]; events=[]; usage={}; finish=None; error=None
    try:
        with session.post(f"http://127.0.0.1:{PORT}/v1/chat/completions", json=body, stream=True, timeout=(5,180)) as response:
            response.raise_for_status(); response.encoding="utf-8"
            for line in response.iter_lines(chunk_size=1, decode_unicode=True):
                if not line or not line.startswith("data: "): continue
                data=line[6:]
                if data=="[DONE]": break
                event=json.loads(data); events.append(event)
                if event.get("usage"): usage=event["usage"]
                for choice in event.get("choices",[]):
                    content=choice.get("delta",{}).get("content") or ""
                    if content:
                        if first is None: first=time.perf_counter()-start
                        parts.append(content)
                    if choice.get("finish_reason"): finish=choice["finish_reason"]
    except Exception as exc: error=str(exc)
    elapsed=time.perf_counter()-start; tokens=usage.get("completion_tokens",0)
    speed=(tokens-1)/(elapsed-first) if first and tokens>1 and elapsed>first else None
    return {"id":ident,"category":category,"prompt":prompt,"response":"".join(parts),"ttft_seconds":first,
            "elapsed_seconds":elapsed,"usage":usage,"tokens_per_second":speed,"finish_reason":finish,"error":error,"events":events}

def score(row, expected):
    parsed=json_from(row["response"]); row["parsed"]=parsed
    if expected[0]=="mcq":
        row["format_ok"]=bool(isinstance(parsed,dict) and parsed.get("answer") in ("A","B","C","D") and len(parsed)==1)
        row["correct"]=bool(row["format_ok"] and parsed["answer"]==expected[1])
    elif expected[0]=="rag":
        answer,page,answerable=expected[1:]; row["format_ok"]=isinstance(parsed,dict)
        if not answerable: row["correct"]=bool(row["format_ok"] and parsed.get("answerable") is False)
        else:
            # 非JSON或缺字段属于可复核的格式失败，不能让整轮模型测试中断。
            got=str(parsed.get("answer", "")) if isinstance(parsed,dict) else ""
            got_page=parsed.get("page") if isinstance(parsed,dict) else None
            pages=got_page if isinstance(got_page,list) else [got_page]
            row["correct"]=bool(row["format_ok"] and parsed.get("answerable") is True and answer.lower() in got.lower() and page in pages)
    elif expected[0]=="gen":
        count=expected[1]; qs=parsed.get("questions",[]) if isinstance(parsed,dict) else []
        valid=[]
        for q in qs:
            valid.append(bool(isinstance(q,dict) and q.get("type")=="choice" and isinstance(q.get("options"),list) and len(q["options"])==4 and q.get("answer") in ["A","B","C","D"] and q.get("knowledgePoint") and q.get("explanation")))
        row["format_ok"]=isinstance(parsed,dict)
        row["correct"]=bool(len(qs)==count and len(valid)==count and all(valid))
        row["structure_rate"]=sum(valid)/count if count else 0
    elif expected[0]=="grade":
        target,point_ids=expected[1:]; row["format_ok"]=isinstance(parsed,dict) and isinstance(parsed.get("score"),int) and isinstance(parsed.get("coveredPoints"),list)
        got=parsed.get("score") if isinstance(parsed,dict) else None
        row["absolute_error"]=abs(got-target) if isinstance(got,int) else target
        row["correct"]=bool(row["format_ok"] and got==target and set(parsed.get("coveredPoints",[]))==set(point_ids))

def main():
    ap=argparse.ArgumentParser(); ap.add_argument("--model",required=True); ap.add_argument("--name",required=True); ap.add_argument("--out",default=str(ROOT/"results")); ap.add_argument("--external-server",action="store_true",help="连接已单独启动的18090模型服务，不负责启动或停止该服务"); args=ap.parse_args()
    model=pathlib.Path(args.model); out=pathlib.Path(args.out)/args.name; out.mkdir(parents=True,exist_ok=True)
    sha=hashlib.sha256()
    with model.open("rb") as f:
        for block in iter(lambda:f.read(8*1024*1024),b""): sha.update(block)
    proc=log=session=command=None; gpu=[]; done=threading.Event()
    def sample_gpu():
        while not done.is_set():
            p=subprocess.run(["nvidia-smi","--query-gpu=timestamp,name,memory.used,memory.total,utilization.gpu","--format=csv,noheader,nounits"],capture_output=True,text=True,creationflags=0x08000000)
            gpu.append({"time":time.time(),"csv":p.stdout.strip()}); done.wait(.5)
    rows=[]
    try:
        if args.external_server:
            session=requests.Session(); session.trust_env=False
            health=session.get(f"http://127.0.0.1:{PORT}/health",timeout=10)
            health.raise_for_status(); command=["external-server",f"127.0.0.1:{PORT}"]
        else:
            proc,log,session,command=start_server(model,out)
        threading.Thread(target=sample_gpu,daemon=True).start()
        # 预热不计入正式数据。
        ask(session,"warmup","warmup","只回复你好。",16)
        for ident,subject,q,a,b,c,d,answer in MCQ:
            prompt=f"{q}\nA. {a}\nB. {b}\nC. {c}\nD. {d}\n只输出合法JSON：{{\"answer\":\"A\"}}"
            row=ask(session,ident,"mcq",prompt,16); score(row,("mcq",answer)); rows.append(row)
        for ident,context,prompt,answer,page,answerable in RAG:
            row=ask(session,ident,"rag",context+"\n"+prompt,96); score(row,("rag",answer,page,answerable)); rows.append(row)
        for ident,point,count,kind in GENERATION:
            prompt=(f"围绕“{point}”生成{count}道四选一模拟题。只输出合法JSON："
                    '{"questions":[{"type":"choice","knowledgePoint":"知识点","question":"题干","options":["A...","B...","C...","D..."],"answer":"A","explanation":"解析"}]}')
            row=ask(session,ident,"generation",prompt,256); score(row,("gen",count)); rows.append(row)
        for ident,question,total,student,points,target,ids in GRADING:
            rubric="；".join(f"P{i+1}:{p}" for i,p in enumerate(points))
            prompt=(f"题目：{question}。满分{total}分。评分点：{rubric}。学生答案：{student}。"
                    f"只输出合法JSON：{{\"score\":整数,\"coveredPoints\":[\"P1\"]}}")
            row=ask(session,ident,"grading",prompt,128); score(row,("grade",target,ids)); rows.append(row)
        for repeat in range(3): rows.append(ask(session,f"perf{repeat+1}","performance",PERF,256))
    finally:
        done.set()
        if proc: proc.terminate(); proc.wait(timeout=30)
        if log: log.close()
        (out/"results.json").write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding="utf-8")
        (out/"gpu-samples.json").write_text(json.dumps(gpu,ensure_ascii=False,indent=2),encoding="utf-8")
        (out/"metadata.json").write_text(json.dumps({"model":str(model),"bytes":model.stat().st_size,"sha256":sha.hexdigest(),"command":command,"mcq":len(MCQ),"rag":len(RAG),"generation":len(GENERATION),"grading":len(GRADING),"performance_repeats":3,"date":time.strftime("%Y-%m-%d %H:%M:%S")},ensure_ascii=False,indent=2),encoding="utf-8")

if __name__=="__main__": main()

"""Build evidence summary from measured JSON and explicit manual scoring."""
import json,pathlib,statistics,csv,html
P=pathlib.Path(__file__).resolve().parent
def load(p):return json.loads(p.read_text(encoding='utf-8'))
reviews=load(P/'quality-review.json')
summary=[];details=[]
for name in ['Qwen3.5-4B','Qwen3-4B']:
    rows=load(P/name/'results.json');meta=load(P/name/'metadata.json');gpu=load(P/name/'gpu-samples.json')
    valid=[r for r in rows if r['error'] is None and r['finish_reason']=='stop']
    memory=[int(x['csv'].split(',')[-3].strip()) for x in gpu if x['csv']]
    score=sum(reviews[name][r['id']]['score'] for r in rows)
    js=[]
    for r in rows:
        if r['id'].startswith('plan-') and r['id']!='plan-zero':
            try:json.loads(r['response']);js.append(True)
            except ValueError:js.append(False)
        details.append({'model':name,'case':r['id'],'category':r['category'],'score':reviews[name][r['id']]['score'],'note':reviews[name][r['id']]['note'],'ttft_seconds':r['ttft_seconds'],'elapsed_seconds':r['elapsed_seconds'],'tokens_per_second':r['generation_tokens_per_second'],'completion_tokens':r['usage'].get('completion_tokens'),'response':r['response']})
    summary.append({'model':name,'requests_ok':len(valid),'total':len(rows),'quality_score':score,'quality_max':30,'json_parse':f'{sum(js)}/{len(js)}','mean_ttft_s':round(statistics.mean(r['ttft_seconds'] for r in valid),3),'mean_total_s':round(statistics.mean(r['elapsed_seconds'] for r in valid),3),'mean_tokens_s':round(statistics.mean(r['generation_tokens_per_second'] for r in valid),2),'peak_device_memory_mib':max(memory),'model_gib':round(meta['bytes']/1024**3,3),'sha256':meta['sha256']})
(P/'summary.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2),encoding='utf-8')
with (P/'results.csv').open('w',encoding='utf-8-sig',newline='') as f:
    w=csv.DictWriter(f,fieldnames=list(details[0]));w.writeheader();w.writerows(details)
esc=lambda x:html.escape(str(x))
cards=''.join(f'<article><h2>{esc(s["model"])}</h2><div class="score">{s["quality_score"]}<small> / 30</small></div><p>请求完成 {s["requests_ok"]}/15 · JSON {s["json_parse"]}</p><p>平均首 token <b>{s["mean_ttft_s"]} s</b></p><p>平均生成 <b>{s["mean_tokens_s"]} token/s</b></p><p>设备显存峰值 <b>{s["peak_device_memory_mib"]} MiB</b></p></article>' for s in summary)
trs=''.join(f'<tr><td>{esc(d["model"])}</td><td>{esc(d["case"])}</td><td>{d["score"]}/2</td><td>{esc(d["note"])}</td><td>{d["ttft_seconds"]:.3f}s</td></tr>' for d in details)
responses=''.join(f'<section><h3>{esc(d["model"])} · {esc(d["case"])}</h3><p>{esc(d["note"])}</p><pre>{esc(d["response"])}</pre></section>' for d in details)
(P/'evidence.html').write_text('''<!doctype html><html lang="zh-CN"><meta charset="utf-8"><title>知序 · 模型选型实测</title><style>body{font:16px/1.6 "Microsoft YaHei",sans-serif;background:#f5f6f8;color:#243248;margin:0;padding:42px}main{max-width:1160px;margin:auto}h1{font-size:34px;margin-bottom:8px}.muted{color:#65738a}.cards{display:flex;gap:24px;margin:24px 0}article{background:white;border:1px solid #dce4ee;border-radius:16px;padding:22px;flex:1}.score{font-size:44px;color:#246b65}small{font-size:18px;color:#7a8799}table{border-collapse:collapse;background:white;width:100%;font-size:14px}td,th{padding:10px;text-align:left;border-bottom:1px solid #e2e8ef}section{background:white;padding:20px;margin:18px 0;border-radius:12px}pre{white-space:pre-wrap;overflow-wrap:anywhere;font:14px/1.7 "Microsoft YaHei"}.notice{padding:14px;background:#fff8e5;border-left:4px solid #e2b64a}</style><main><p class="muted">STUDYPILOT / 2026-09-07 / 本地实测证据</p><h1>两款 4B 量化模型，15 道相同中文题</h1><p class="muted">RTX 4060 Laptop 8GB · llama.cpp b10819 · Q4_K_M · 4096 上下文 · 单并发 · 关闭思考 · temperature=0</p>'''+f'<div class="cards">{cards}</div><p class="notice">仅反映本机15题单次实测。质量分为人工按题核查；token口径随分词器不同，不能单独代表回答速度。显存为整个GPU用量，非模型独占。</p><h2>逐题核查</h2><table><tr><th>模型</th><th>题号</th><th>得分</th><th>核查说明</th><th>首token</th></tr>{trs}</table><h2>原始回答</h2>{responses}</main></html>',encoding='utf-8')
print(json.dumps(summary,ensure_ascii=False,indent=2))

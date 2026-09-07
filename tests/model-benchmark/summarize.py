"""汇总三个模型的自动评分和性能数据，生成可放入结题报告的结果文件。"""
import csv,json,pathlib,statistics,html
ROOT=pathlib.Path(__file__).resolve().parent; RESULTS=ROOT/'results'
def load(p): return json.loads(p.read_text(encoding='utf-8'))
def mean(xs): return round(statistics.mean(xs),3) if xs else None
def pct(rows): return round(100*sum(bool(r.get('correct')) for r in rows)/len(rows),2) if rows else 0
summary=[]
for folder in sorted(RESULTS.iterdir()):
 if not folder.is_dir(): continue
 rows=load(folder/'results.json'); meta=load(folder/'metadata.json'); gpu=load(folder/'gpu-samples.json')
 groups={k:[r for r in rows if r['category']==k] for k in ('mcq','rag','generation','grading','performance')}
 memory=[int(x['csv'].split(',')[-3].strip()) for x in gpu if x.get('csv')]
 grade_mae=mean([r.get('absolute_error',0) for r in groups['grading']]) or 0
 grading_score=max(0,100-grade_mae/5*100)
 valid=[r for r in rows if r['category']!='warmup' and not r.get('error') and r.get('finish_reason')=='stop']
 perf=groups['performance']; ttft=mean([r['ttft_seconds'] for r in perf if r.get('ttft_seconds')]); speed=mean([r['tokens_per_second'] for r in perf if r.get('tokens_per_second')])
 summary.append({'model':folder.name,'mcq_accuracy':pct(groups['mcq']),'mcq_format':round(100*sum(bool(r.get('format_ok')) for r in groups['mcq'])/len(groups['mcq']),2),'rag_accuracy':pct(groups['rag']),'generation_structure':round(100*sum(r.get('structure_rate',0) for r in groups['generation'])/len(groups['generation']),2),'grading_mae':grade_mae,'grading_score':round(grading_score,2),'ttft_s':ttft,'tokens_s':speed,'peak_gpu_mib':max(memory) if memory else None,'success_rate':round(100*len(valid)/(len(rows)-1),2),'model_gib':round(meta['bytes']/1024**3,3)})
fastest=min(x['ttft_s'] for x in summary if x['ttft_s'] is not None); fastest_mem=min(x['peak_gpu_mib'] for x in summary if x['peak_gpu_mib'] is not None); fastest_speed=max(x['tokens_s'] for x in summary if x['tokens_s'] is not None)
for s in summary:
 s['performance_score']=round((fastest/s['ttft_s']*40+s['tokens_s']/fastest_speed*40+fastest_mem/s['peak_gpu_mib']*20),2)
 s['overall_score']=round(s['mcq_accuracy']*.45+s['rag_accuracy']*.15+s['generation_structure']*.15+s['grading_score']*.15+s['performance_score']*.08+s['success_rate']*.02,2)
summary.sort(key=lambda x:x['overall_score'],reverse=True)
(ROOT/'benchmark-summary.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2),encoding='utf-8')
with (ROOT/'benchmark-summary.csv').open('w',encoding='utf-8-sig',newline='') as f:
 w=csv.DictWriter(f,fieldnames=list(summary[0]));w.writeheader();w.writerows(summary)
rows=''.join('<tr>'+''.join(f'<td>{html.escape(str(v))}</td>' for v in s.values())+'</tr>' for s in summary); heads=''.join(f'<th>{html.escape(k)}</th>' for k in summary[0])
(ROOT/'benchmark-evidence.html').write_text(f'''<!doctype html><meta charset="utf-8"><title>StudyPilot 模型Benchmark</title><style>body{{font:16px/1.6 Microsoft YaHei,sans-serif;background:#f4f6f8;color:#1e293b;margin:0;padding:42px}}main{{max-width:1200px;margin:auto}}table{{width:100%;border-collapse:collapse;background:#fff}}th,td{{padding:10px;border-bottom:1px solid #dce3eb;text-align:center}}th{{background:#143a5b;color:#fff}}.note{{background:#fff5df;padding:14px;border-left:4px solid #c88618}}</style><main><h1>本地小模型量化 Benchmark</h1><p>Qwen3.5-4B、Qwen3-4B、Phi-4-mini-instruct · Q4_K_M · RTX 4060 Laptop 8GB · llama.cpp · 4096上下文 · 单并发</p><table><thead><tr>{heads}</tr></thead><tbody>{rows}</tbody></table><p class="note">综合分权重：选择题45%，资料依据15%，出题结构15%，评分准确性15%，性能8%，成功率2%。选择题、资料题、JSON结构和评分误差均由程序自动计算；性能分在三款模型的实测结果中归一化。</p></main>''',encoding='utf-8')
print(json.dumps(summary,ensure_ascii=False,indent=2))

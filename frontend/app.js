// 知序前端的状态入口：data 保存页面状态，computed 负责派生显示数据，methods 处理交互。
// 业务校验不放在浏览器中，浏览器只做体验性限制；数据库一致性由 Java 后端保证。
const {createApp,nextTick}=Vue;
createApp({
 data(){return {tab:'chat',courses:[],courseId:null,courseName:'',courseModal:false,loadingCourse:false,courseEpoch:0,useReferences:true,quote:'',health:{},documents:[],sessions:[],sessionId:null,sessionRequest:0,messages:[],question:'',generating:false,run:null,uploading:false,preview:null,deleteTarget:null,notice:null,planList:[],selectedPlan:null,planning:false,toggling:null,trace:[],form:{goal:'掌握数据库课程核心知识',weak_points:'',start_date:thisDate(),days:3,daily_minutes:60,instruction:''}}},
 computed:{
  activeCourse(){return this.courses.find(c=>c.id===this.courseId)},
  groupedTasks(){const groups={};for(const t of this.selectedPlan?.tasks||[])(groups[t.task_date]??=[]).push(t);return groups},
  doneCount(){return this.selectedPlan?.tasks.filter(t=>t.done).length||0},
  progress(){return this.selectedPlan?.tasks.length?100*this.doneCount/this.selectedPlan.tasks.length:0}
 },
 async mounted(){this.pickQuote();await Promise.allSettled([this.checkHealth(),this.initCourses()]);},
 methods:{
  formatText(value){return String(value).replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll('"','&quot;').replaceAll("'",'&#39;').replace(/\*\*([^*\n]+)\*\*/g,'<strong>$1</strong>').replace(/^\*\s+/gm,'• ')},
  async api(path,options={}){const r=await fetch('/api'+path,options);if(!r.ok){let e;try{e=await r.json()}catch{}throw new Error(e?.message||'请求失败，请稍后重试')}return r.json()},
  json(method,body){return {method,headers:{'Content-Type':'application/json'},body:JSON.stringify(body)}},
  notify(text,type='error'){this.notice={text,type};clearTimeout(this.toastTimer);this.toastTimer=setTimeout(()=>this.notice=null,6500)},
  async checkHealth(){try{this.health=await this.api('/health')}catch{this.health={};this.notify('业务服务未连接，请运行启动脚本')}},
  // 每次切课先清空页面，并递增版本号。旧请求即使稍后返回，也不能写入新课程。
  pickQuote(){const quotes=['慢慢来，把今天的一个问题弄懂就很好。','学习不必一口气走很远，每一步都算数。','暂时不会，是理解开始的地方。','把复杂的问题拆小，答案就会慢慢清晰。','今天多理解一点，明天就多一份从容。','认真走过的每一步，都会成为你的底气。'];const choices=quotes.filter(q=>q!==this.quote);this.quote=choices[Math.floor(Math.random()*choices.length)]},
  scoped(path,id=this.courseId){return path+(path.includes('?')?'&':'?')+'courseId='+encodeURIComponent(id)},
  async initCourses(){try{this.courses=await this.api('/courses');if(this.courses.length){let saved;try{saved=localStorage.getItem('studypilot.course')}catch{}await this.switchCourse(this.courses.some(c=>c.id===saved)?saved:this.courses[0].id)}}catch(e){this.notify(e.message)}},
  async createCourse(){const name=this.courseName.trim();if(!name)return;try{const c=await this.api('/courses',this.json('POST',{name}));this.courses=await this.api('/courses');this.courseModal=false;this.courseName='';await this.switchCourse(c.id)}catch(e){this.notify(e.message)}},
  async switchCourse(id){if(id===this.courseId)return;this.stop();this.courseEpoch++;this.courseId=id;this.sessionId=null;this.messages=[];this.documents=[];this.sessions=[];this.planList=[];this.selectedPlan=null;this.preview=null;this.deleteTarget=null;this.question='';this.trace=[];this.planning=false;this.uploading=false;this.tab='chat';this.form={goal:'',weak_points:'',start_date:thisDate(),days:3,daily_minutes:60,instruction:''};this.pickQuote();try{localStorage.setItem('studypilot.course',id)}catch{}const epoch=this.courseEpoch;this.loadingCourse=true;await Promise.allSettled([this.refreshDocs(),this.refreshSessions(),this.refreshPlans()]);if(epoch===this.courseEpoch)this.loadingCourse=false},
  async refreshDocs(){const epoch=this.courseEpoch;try{const rows=await this.api(this.scoped('/documents'));if(epoch===this.courseEpoch)this.documents=rows}catch(e){if(epoch===this.courseEpoch)this.notify(e.message)}},
  async refreshSessions(){const epoch=this.courseEpoch;try{const rows=await this.api(this.scoped('/sessions'));if(epoch===this.courseEpoch)this.sessions=rows}catch(e){if(epoch===this.courseEpoch)this.notify(e.message)}},
  async refreshPlans(){const epoch=this.courseEpoch;try{const rows=await this.api(this.scoped('/plans'));if(epoch===this.courseEpoch)this.planList=rows}catch(e){if(epoch===this.courseEpoch)this.notify(e.message)}},
  async upload(event){const file=event.target.files?.[0];if(!file)return;event.target.value='';if(!/\.pdf$/i.test(file.name))return this.notify('目前仅支持文字型 PDF');if(file.size>20*1024*1024)return this.notify('单份PDF不能超过20MB');this.uploading=true;const epoch=this.courseEpoch;
   try{const data=new FormData();data.append('file',file);await this.api(this.scoped('/documents'),{method:'POST',body:data});if(epoch!==this.courseEpoch)return;await this.refreshDocs();this.notify('资料已添加，可以开始提问','success')}catch(e){this.notify(e.message)}finally{if(epoch===this.courseEpoch)this.uploading=false}},
  async removeDocument(){if(!this.deleteTarget)return;try{await this.api(this.scoped('/documents/'+this.deleteTarget.id),{method:'DELETE'});this.deleteTarget=null;await this.refreshDocs();this.notify('资料已删除','success')}catch(e){this.notify(e.message)}},
  async newChat(){if(this.generating||!this.courseId)return;const epoch=this.courseEpoch;try{const s=await this.api(this.scoped('/sessions'),{method:'POST'});if(epoch!==this.courseEpoch)return;this.sessionRequest++;this.sessionId=s.id;this.messages=[];this.tab='chat';await this.refreshSessions()}catch(e){this.notify(e.message)}},
  async openSession(id){if(this.generating)return;const epoch=this.courseEpoch;try{const request=++this.sessionRequest;const rows=await this.api(this.scoped('/sessions/'+id+'/messages'));if(epoch!==this.courseEpoch||request!==this.sessionRequest)return;this.messages=rows;this.sessionId=id;this.tab='chat';this.scroll()}catch(e){this.notify(e.message)}},
  scroll(){nextTick(()=>{const b=this.$refs.messageBox;if(b)b.scrollTop=b.scrollHeight})},
  // 流式问答：先创建空助手气泡，再按 SSE 事件将 token、来源和最终状态写回同一条消息。
  // buffer 必须保留不完整事件，因为网络传输可能在任意字符处分块。
  async send(){if(this.generating||!this.question.trim()||!this.courseId||this.loadingCourse)return;const startingCourse=this.courseEpoch;if(!this.sessionId)await this.newChat();if(!this.sessionId||startingCourse!==this.courseEpoch||!this.question.trim())return;
   const epoch=this.courseEpoch;const controller=new AbortController();this.chatController=controller;const question=this.question.trim();this.question='';this.generating=true;this.run=null;this.messages.push({role:'user',content:question});this.messages.push({role:'assistant',content:'',sources:[],status:'generating'});const index=this.messages.length-1;this.scroll();
   try{const response=await fetch('/api/chat',{...this.json('POST',{question,session_id:this.sessionId,courseId:this.courseId,useReferences:this.useReferences}),signal:controller.signal});if(!response.ok){const e=await response.json();throw new Error(e.message||'请求失败')}
    const reader=response.body.getReader(),decoder=new TextDecoder();let buffer='';
    while(true){const {done,value}=await reader.read();if(epoch!==this.courseEpoch||this.chatController!==controller){await reader.cancel();return;}buffer+=decoder.decode(value||new Uint8Array(),{stream:!done});let split;
     while((split=buffer.indexOf('\n\n'))>=0){const block=buffer.slice(0,split);buffer=buffer.slice(split+2);let event='message',data='';for(const line of block.split('\n')){if(line.startsWith('event:'))event=line.slice(6).trim();if(line.startsWith('data:'))data+=line.slice(5).trim()}if(!data)continue;const payload=JSON.parse(data);
      if(event==='meta')this.run=payload.run_id;
      if(event==='delta')this.messages[index].content+=payload.text;
      if(event==='sources')this.messages[index].sources=payload;
      if(event==='done')this.messages[index].status=payload.status;
      if(event==='error'){this.notify(payload.message);this.messages[index].status='failed'}this.scroll();
     }if(done)break;
    }
   }catch(e){if(epoch===this.courseEpoch&&this.chatController===controller){this.messages[index].status=e.name==='AbortError'?'cancelled':'failed';if(e.name!=='AbortError')this.notify(e.message)}}finally{if(epoch===this.courseEpoch&&this.chatController===controller){this.generating=false;this.run=null;this.chatController=null;if(this.messages[index].status==='generating')this.messages[index].status='failed';await this.refreshSessions()}}},
  async stop(){const run=this.run;if(this.chatController)this.chatController.abort();this.chatController=null;const last=this.messages[this.messages.length-1];if(last?.status==='generating')last.status='cancelled';this.generating=false;this.run=null;if(run)try{await this.api(this.scoped('/chat/'+run+'/cancel'),{method:'POST'})}catch{/* 浏览器已停止接收；服务端仍由超时机制回收。 */}},
  resetPlan(){this.selectedPlan=null;this.trace=[];this.form.instruction=''},
  async loadPlan(id){const epoch=this.courseEpoch;try{const result=await this.api(this.scoped('/plans/'+id));if(epoch!==this.courseEpoch)return;this.selectedPlan=result;const p=this.selectedPlan;this.form={goal:p.goal,weak_points:p.weak_points,start_date:p.start_date,days:p.days,daily_minutes:p.daily_minutes,instruction:''};this.trace=[]}catch(e){this.notify(e.message)}},
  // 有 selectedPlan 时调用 revise。服务端事务会保留已完成任务，只替换未完成任务。
  async generatePlan(){if(this.planning)return;this.planning=true;const epoch=this.courseEpoch;try{const url=this.selectedPlan?'/plans/'+this.selectedPlan.id+'/revise':'/plans';const p=await this.api(url,this.json('POST',{...this.form,courseId:this.courseId}));if(epoch!==this.courseEpoch)return;this.selectedPlan=p;await this.refreshPlans();if(p.run_id){const trace=await this.api(this.scoped('/agent/'+p.run_id));if(epoch!==this.courseEpoch)return;this.trace=trace;}if(epoch!==this.courseEpoch)return;this.notify('计划已校验并保存','success')}catch(e){this.notify(e.message)}finally{if(epoch===this.courseEpoch)this.planning=false}},
  async toggle(task,event){this.toggling=task.id;const desired=event.target.checked;try{await this.api(this.scoped('/tasks/'+task.id),this.json('PATCH',{done:desired}));this.selectedPlan=await this.api(this.scoped('/plans/'+this.selectedPlan.id))}catch(e){event.target.checked=!!task.done;this.notify(e.message)}finally{this.toggling=null}},
  toolLabel(tool){return {get_learning_context:'已读取课程资料与完成进度',submit_plan:'已提交复习任务并执行时间校验'}[tool]||tool}
 }
}).mount('#app');
// 使用本地日期而非 UTC 日期，避免中国时区凌晨出现“开始日期少一天”。
function thisDate(){const d=new Date();return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`}

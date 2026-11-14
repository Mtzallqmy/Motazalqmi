import {useState} from 'react';
import {api} from '../lib/api';
import {Copy,Send,ShieldCheck,Link2} from 'lucide-react';
export function TelegramAdmin(){
 const [open,setOpen]=useState(false),[busy,setBusy]=useState(false),[state,setState]=useState(null),[message,setMessage]=useState(''),[code,setCode]=useState('');
 async function action(type){
  setBusy(true);setMessage('');
  try{
   const result=await api('/api/v1/telegram-pair',{method:'POST',body:JSON.stringify({action:type})});
   if(type==='issue')setCode(result.command);
   if(type==='configure')setMessage(result.message||'تم تفعيل البوت.');
   if(type==='revoke'){setCode('');setMessage('أُبطلت صلاحية حسابات تيليجرام المربوطة.');}
   setState(old=>({...old,...result}));
  }catch(e){setMessage(e.message||'تعذّر الاتصال.');}
  finally{setBusy(false)}
 }
 return <section className="mt-5 rounded-3xl border border-gold/30 bg-white shadow-soft p-5 sm:p-7">
  <button type="button" onClick={()=>{setOpen(x=>!x);if(!open)action('status');}} className="flex items-center gap-2 text-teal font-bold text-lg">
   <Send size={20}/> إدارة بوت تيليجرام <span className="text-xs text-ink/50">إظهار / إخفاء</span>
  </button>
  {open&&<div className="space-y-4 mt-4">
   <p className="text-sm text-ink/70">اربط حسابك الخاص بالبوت برمز لمرة واحدة، ثم انشر المقالات وأدرها دون مشاركة كلمة المرور.</p>
   <p className="text-sm">{state?.configured?'✅ إعدادات اتصال البوت متوفرة.':'⚠️ أضف TELEGRAM_BOT_TOKEN في متغيرات Vercel للإنتاج ثم أعد النشر.'} {state?.linkedAccounts!=null?'· الحسابات المربوطة: '+state.linkedAccounts:''}</p>
   <div className="flex flex-wrap gap-2">
    <button disabled={busy} className="inline-flex gap-2 items-center rounded-xl bg-teal px-4 py-2 text-white text-sm font-bold" onClick={()=>action('configure')}><ShieldCheck size={16}/> تفعيل Webhook</button>
    <button disabled={busy} className="inline-flex gap-2 items-center rounded-xl bg-gold/30 px-4 py-2 text-sm font-bold" onClick={()=>action('issue')}><Link2 size={16}/> إنشاء رمز ربط 10 دقائق</button>
   </div>
   {code&&<div className="rounded-xl bg-cream p-4 border border-gold/30">
    <p className="text-sm font-bold mb-2">أرسل الأمر التالي داخل محادثتك الخاصة بالبوت:</p>
    <code dir="ltr" className="block text-sm break-all select-all">{code}</code>
    <button className="mt-3 inline-flex gap-1 items-center text-sm text-teal underline" onClick={()=>navigator.clipboard.writeText(code)}><Copy size={15}/> نسخ أمر الربط</button>
   </div>}
   {message&&<p role="status" className="text-sm text-rust">{message}</p>}
   <div className="border-t border-gold/20 pt-3"><button disabled={busy} type="button" className="text-red-700 text-sm font-bold underline" onClick={()=>{if(window.confirm('إلغاء جميع صلاحيات حسابات تيليجرام المربوطة؟'))action('revoke');}}>إبطال ربط كل حسابات تيليجرام</button></div>
  </div>}
 </section>;
}

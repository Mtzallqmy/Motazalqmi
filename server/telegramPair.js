import {randomBytes,createHash} from 'node:crypto';
import {database} from './db.js';
import {getAdmin,checkOrigin,noCache} from './auth.js';
import {telegramConfig} from './telegramBot.js';
const webhookUrl='https://motazalqmi.vercel.app/api/v1/telegram';
export function createTelegramPairHandler({sqlFactory=database,send=globalThis.fetch,env=process.env,authorize=getAdmin,originAllowed=checkOrigin}={}){
 return async(req,res)=>{
  noCache(res);
  if(req.method!=='POST'){res.setHeader('Allow','POST');return res.status(405).json({error:'POST required'});}
  if(!authorize(req)||!originAllowed(req))return res.status(401).json({error:'سجّل دخول الإدارة أولًا.'});
  const action=req.body?.action;
  if(!['status','issue','configure','verify','revoke'].includes(action))return res.status(400).json({error:'طلب غير معروف.'});
  try{
   const cfg=telegramConfig(env),sql=sqlFactory();
   const linked=async()=>{const r=await sql.query("SELECT data FROM blog_public_metadata WHERE id='telegram_admin' LIMIT 1",[]);return r[0]?.data?.ids?.length||0;};
   const call=async(method,body={})=>{
    const response=await send('https://api.telegram.org/bot'+cfg.token+'/'+method,{
     method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body),signal:AbortSignal.timeout(9000)});
    const json=await response.json();if(!response.ok||!json.ok)throw Error('Telegram request failed');return json.result;
   };
   if(action==='status')return res.status(200).json({configured:cfg.ready,linkedAccounts:await linked()});
   if(action==='revoke'){
    if(req.body.confirm!==true)return res.status(400).json({error:'تأكيد إبطال الربط مطلوب.'});
    await sql.query("DELETE FROM blog_public_metadata WHERE id IN ('telegram_admin','telegram_pair')",[]);
    return res.status(200).json({ok:true,linkedAccounts:0});
   }
   if(!cfg.ready)return res.status(503).json({error:'احفظ رمز البوت المجدّد في TELEGRAM_BOT_TOKEN على Vercel ثم أعد النشر.'});
   if(action==='issue'){
    const code=randomBytes(20).toString('hex'),hash=createHash('sha256').update(code).digest('hex'),expiresAt=Date.now()+10*60*1000;
    await sql.query("INSERT INTO blog_public_metadata(id,data) VALUES ('telegram_pair',$1::jsonb) ON CONFLICT(id) DO UPDATE SET data=EXCLUDED.data,updated_at=NOW()",[JSON.stringify({hash,expiresAt})]);
    return res.status(200).json({command:'/link '+code,expiresAt});
   }
   const bot=await call('getMe');if(bot?.is_bot!==true||!Number.isSafeInteger(bot.id))throw Error('Invalid bot identity');
   if(action==='configure')await call('setWebhook',{url:webhookUrl,secret_token:cfg.secret,allowed_updates:['message','callback_query'],drop_pending_updates:false});
   const webhook=await call('getWebhookInfo');
   const active=webhook.url===webhookUrl;
   if(action==='configure'&&!active)return res.status(502).json({error:'لم تؤكد Telegram عنوان استقبال الأوامر الصحيح.'});
   return res.status(200).json({ok:true,configured:true,bot:{id:bot.id,username:bot.username||''},linkedAccounts:await linked(),
    webhook:{active,pendingUpdates:webhook.pending_update_count||0,hasRecentError:Boolean(webhook.last_error_date&&Date.now()/1000-webhook.last_error_date<600)},
    message:action==='configure'?'تأكدت هوية البوت وعنوان Webhook. أكمل ربط حساب المدير واختبار الأوامر.':'تم التحقق من هوية البوت وحالة Webhook.'});
  }catch{return res.status(503).json({error:'تعذّر التحقق من Telegram. تحقق من الرمز المجدّد والاتصال ثم أعد المحاولة.'});}
 };
}
export default createTelegramPairHandler();

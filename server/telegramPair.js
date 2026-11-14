import {randomBytes,createHash} from 'node:crypto';
import {database} from './db.js';
import {getAdmin,checkOrigin,noCache} from './auth.js';
import {telegramConfig} from './telegramBot.js';

export function createTelegramPairHandler({sqlFactory=database,send=globalThis.fetch,env=process.env}={}){
 return async(req,res)=>{
  noCache(res);
  if(req.method!=='POST'){res.setHeader('Allow','POST');return res.status(405).json({error:'POST required'});}
  if(!getAdmin(req)||!checkOrigin(req))return res.status(401).json({error:'سجّل دخول الإدارة أولًا.'});
  const action=req.body?.action;
  if(!['status','issue','configure','revoke'].includes(action))return res.status(400).json({error:'طلب غير معروف.'});
  try{
   const cfg=telegramConfig(env),sql=sqlFactory();
   if(action==='status'){
    const r=await sql.query("SELECT data FROM blog_public_metadata WHERE id='telegram_admin' LIMIT 1",[]);
    return res.status(200).json({configured:cfg.ready,linkedAccounts:r[0]?.data?.ids?.length||0});
   }
   if(action==='issue'){
    if(!cfg.ready)return res.status(503).json({error:'هيّئ رمز البوت في متغير TELEGRAM_BOT_TOKEN على Vercel أولًا.'});
    const code=randomBytes(20).toString('hex'),hash=createHash('sha256').update(code).digest('hex'),expiresAt=Date.now()+10*60*1000;
    await sql.query("INSERT INTO blog_public_metadata(id,data) VALUES ('telegram_pair',$1::jsonb) ON CONFLICT(id) DO UPDATE SET data=EXCLUDED.data,updated_at=NOW()",[JSON.stringify({hash,expiresAt})]);
    return res.status(200).json({command:'/link '+code,expiresAt});
   }
   if(action==='revoke'){
    await sql.query("DELETE FROM blog_public_metadata WHERE id IN ('telegram_admin','telegram_pair')",[]);
    return res.status(200).json({ok:true,linkedAccounts:0});
   }
   if(!cfg.ready)return res.status(503).json({error:'أضف TELEGRAM_BOT_TOKEN إلى Vercel وأعد النشر.'});
   const response=await send('https://api.telegram.org/bot'+cfg.token+'/setWebhook',{
    method:'POST',headers:{'Content-Type':'application/json'},signal:AbortSignal.timeout(10000),
    body:JSON.stringify({url:'https://motazalqmi.vercel.app/api/v1/telegram',
     secret_token:cfg.secret,allowed_updates:['message','callback_query'],drop_pending_updates:false})
   });
   const json=await response.json();
   if(!response.ok||!json.ok)return res.status(502).json({error:'رفضت Telegram ربط Webhook؛ تحقق من رمز البوت.'});
   return res.status(200).json({ok:true,configured:true,message:'تم تفعيل استقبال أوامر البوت.'});
  }catch{return res.status(503).json({error:'تعذّر تنفيذ الربط. حاول مجددًا.'});}
 };
}
export default createTelegramPairHandler();

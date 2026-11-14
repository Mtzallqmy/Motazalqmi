// Dedicated Android API: login returns a short-lived bearer token. Browser admin remains on /api/session.
import {adminReady,fingerprint,noCache,issueMobileAdminToken,validateLogin} from '../../server/auth.js';
import {database} from '../../server/db.js';
export default async function handler(req,res){
 noCache(res);
 res.setHeader('Allow','POST');
 if(req.method!=='POST')return res.status(405).json({code:'METHOD_NOT_ALLOWED',error:'POST مطلوب.'});
 if(!/application\/json/i.test(req.headers?.['content-type']||''))return res.status(415).json({error:'JSON مطلوب.'});
 if(!adminReady()||!process.env.DATABASE_URL)return res.status(503).json({error:'لم تُهيأ لوحة الكاتب بعد.'});
 if(typeof req.body?.email!=='string'||typeof req.body?.password!=='string'||JSON.stringify(req.body).length>2048)return res.status(400).json({error:'بيانات غير صالحة.'});
 try{
  const sql=database(),ip=fingerprint(req);
  const rows=await sql`SELECT attempts FROM blog_login_attempts WHERE ip_hash=${ip} AND last_attempt>NOW()-INTERVAL '15 minutes'`;
  if(rows[0]?.attempts>=10)return res.status(429).json({error:'تجاوزت عدد المحاولات، حاول لاحقًا.'});
  if(!validateLogin(req.body.email,req.body.password)){
    await sql`INSERT INTO blog_login_attempts(ip_hash,attempts,last_attempt) VALUES (${ip},1,NOW())
     ON CONFLICT(ip_hash) DO UPDATE SET attempts=CASE WHEN blog_login_attempts.last_attempt<NOW()-INTERVAL '15 minutes' THEN 1 ELSE blog_login_attempts.attempts+1 END,last_attempt=NOW()`;
    return res.status(401).json({error:'البريد أو كلمة المرور غير صحيحة.'});
  }
  await sql`DELETE FROM blog_login_attempts WHERE ip_hash=${ip}`;
  return res.status(200).json({user:{email:process.env.ADMIN_EMAIL},...issueMobileAdminToken(process.env.ADMIN_EMAIL),tokenType:'Bearer'});
 }catch(e){console.error('Auth error',e.code||e.name);return res.status(503).json({error:'تعذر الاتصال بخدمة الدخول.'});}
}

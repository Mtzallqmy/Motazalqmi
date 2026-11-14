import {adminReady,checkOrigin,expiredCookie,getAdmin,fingerprint,noCache,sessionCookie,validateLogin} from '../server/auth.js';
import {database} from '../server/db.js';
export default async function handler(req,res){
 noCache(res);
 if(req.method==='GET')return res.status(200).json({user:getAdmin(req)});
 if(!['POST','DELETE'].includes(req.method)){res.setHeader('Allow','GET, POST, DELETE');return res.status(405).end();}
 if(!checkOrigin(req))return res.status(403).json({error:'طلب من مصدر غير مسموح.'});
 if(req.method==='DELETE'){res.setHeader('Set-Cookie',expiredCookie());return res.status(200).json({ok:true});}
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
  res.setHeader('Set-Cookie',sessionCookie(process.env.ADMIN_EMAIL));
  return res.status(200).json({user:{email:process.env.ADMIN_EMAIL}});
 }catch(e){console.error('Auth error',e.code||e.name);return res.status(503).json({error:'تعذر الاتصال بخدمة الدخول.'});}
}

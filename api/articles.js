import { editorial } from '../server/editorial.js';
import { randomUUID } from 'node:crypto';
import { checkOrigin,getAdmin,noCache } from '../server/auth.js';
import { database,articleRow,normalizeArticle } from '../server/db.js';
export default async function handler(req,res){
 noCache(res);
 if(!['GET','POST'].includes(req.method)){res.setHeader('Allow','GET, POST');return res.status(405).end();}
 const admin=getAdmin(req);
 if(req.method==='POST'&&(!admin||!checkOrigin(req)))return res.status(403).json({error:'غير مصرح بإجراء التعديل.'});
 if(req.method==='GET'&&req.query.scope==='admin'&&!admin)return res.status(401).json({error:'تسجيل الدخول مطلوب.'});
 try{
  const sql=database();
  if(req.method==='GET'){
   const list=req.query.scope==='admin'
      ?await sql`SELECT id,slug,status,publish_date::text,data FROM blog_articles ORDER BY publish_date DESC,updated_at DESC LIMIT 500`
      :await sql`SELECT id,slug,status,publish_date::text,data FROM blog_articles WHERE status='published' ORDER BY publish_date DESC,updated_at DESC LIMIT 500`;
   return res.status(200).json({articles:list.map(articleRow),editorial:await editorial()});
  }
  if(JSON.stringify(req.body||'').length>240000)return res.status(413).json({error:'المقالة تتجاوز الحد المسموح.'});
  if(req.body.action==='save'){
   const a=normalizeArticle(req.body.article),id=a.id||'art-'+randomUUID(),data=JSON.stringify({...a,id});
   await sql`INSERT INTO blog_articles(id,slug,status,publish_date,data) VALUES (${id},${a.slug},${a.status},${a.publishDate}::date,${data}::jsonb)
   ON CONFLICT (id) DO UPDATE SET slug=EXCLUDED.slug,status=EXCLUDED.status,publish_date=EXCLUDED.publish_date,data=EXCLUDED.data,updated_at=NOW()`;
   return res.status(200).json({id});
  }
  if(req.body.action==='delete'){
   if(typeof req.body.id!=='string'||req.body.id.length>150)return res.status(400).json({error:'المعرف غير صالح.'});
   await sql`DELETE FROM blog_articles WHERE id=${req.body.id}`;
   return res.status(200).json({ok:true});
  }
  if(req.body.action==='import'){
   const count=await sql`SELECT COUNT(*)::int AS n FROM blog_articles`;
   if(count[0].n>0)return res.status(409).json({error:'قاعدة البيانات تحتوي على مقالات.'});
   if(!Array.isArray(req.body.articles)||req.body.articles.length>7)return res.status(400).json({error:'بيانات استيراد غير صالحة.'});
   for(const source of req.body.articles){
    const a=normalizeArticle(source);
    if(!a.id)throw new Error('معرف الاستيراد مفقود.');
    const data=JSON.stringify(a);
    await sql`INSERT INTO blog_articles(id,slug,status,publish_date,data) VALUES (${a.id},${a.slug},${a.status},${a.publishDate}::date,${data}::jsonb) ON CONFLICT(id) DO NOTHING`;
   }
   return res.status(200).json({imported:req.body.articles.length});
  }
  return res.status(400).json({error:'طلب غير معروف.'});
 }catch(e){
  if(e?.code==='23505')return res.status(409).json({error:'الرابط الوصفي مستخدم.'});
  if(/غير صالحة|مطلوبة|يجب|حقل/.test(e?.message||''))return res.status(400).json({error:e.message});
  console.error('Database error',e?.code||e?.name);
  return res.status(503).json({error:'تعذر الوصول إلى قاعدة البيانات.'});
 }
}

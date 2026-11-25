import { normalizeTicker, tickerForAudience } from '../shared/announcementTicker.js';
import { editorial, publicEditorial } from '../server/editorial.js';
import { createHash, randomUUID } from 'node:crypto';
import {shortCode,shortArticleUrl} from '../server/articleLinks.js';
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
   const articles = req.query.scope === 'admin'
      ?sql`SELECT id,slug,status,publish_date::text,updated_at::text,data FROM blog_articles ORDER BY publish_date DESC,updated_at DESC LIMIT 500`
      :sql`SELECT id,slug,status,publish_date::text,updated_at::text,data FROM blog_published_articles WHERE status='published' ORDER BY publish_date DESC,updated_at DESC LIMIT 500`;
   const [list, metadata] = await Promise.all([
    articles, sql`SELECT id,data FROM blog_public_metadata WHERE id IN ('editorial','announcement_ticker')`
   ]);
   const record = id => metadata.find(entry => entry.id === id)?.data;
   const payload={articles:list.map(articleRow),editorial:publicEditorial(record('editorial')),
    ticker:tickerForAudience(record('announcement_ticker'), req.query.scope==='admin')};
   if(req.query.scope==='admin')return res.status(200).json(payload);
   const body=JSON.stringify(payload);
   const etag='"'+createHash('sha256').update(body).digest('base64url')+'"';
   res.setHeader('ETag',etag);
   res.setHeader('Cache-Control','public, max-age=0, must-revalidate');
   res.setHeader('Content-Type','application/json; charset=utf-8');
   if(req.headers['if-none-match']===etag)return res.status(304).end();
   return res.status(200).send(body);
  }
  if(JSON.stringify(req.body||'').length>240000)return res.status(413).json({error:'المقالة تتجاوز الحد المسموح.'});
  if (req.body.action === 'announcement-ticker') {
   let ticker;
   try { ticker = normalizeTicker(req.body.ticker); }
   catch (e) { return res.status(400).json({error:e.message}); }
   await sql`INSERT INTO blog_public_metadata(id,data) VALUES ('announcement_ticker',${JSON.stringify(ticker)}::jsonb)
    ON CONFLICT (id) DO UPDATE SET data=EXCLUDED.data,updated_at=NOW()`;
   return res.status(200).json({ticker});
  }
  if(req.body.action==='editorial'){
   const input=req.body.editorial;
   if(!input||typeof input!=='object'||Array.isArray(input))return res.status(400).json({error:'إعدادات غير صالحة.'});
   const current=await editorial(sql);
   const tagline=input.tagline,about=input.about;
   if(typeof tagline!=='string'||tagline.trim().length<3||tagline.length>240||
      typeof about!=='string'||about.trim().length<10||about.length>12000)
    return res.status(400).json({error:'وصف المدونة أو نبذتها غير صالح.'});
   if(!Array.isArray(input.quotes)||input.quotes.length>40||
      !input.quotes.every(q=>q&&typeof q.text==='string'&&q.text.trim()&&q.text.length<=1200&&
       typeof q.author==='string'&&q.author.length<=160&&typeof q.source==='string'&&q.source.length<=160))
    return res.status(400).json({error:'صيغة الاقتباسات غير صالحة.'});
   const value=JSON.stringify({...current,site:{...current.site,tagline:tagline.trim(),about:about.trim()},
    quotes:input.quotes.map(q=>({text:q.text.trim(),author:q.author.trim(),source:q.source.trim()}))});
   await sql`INSERT INTO blog_public_metadata(id,data) VALUES ('editorial',${value}::jsonb)
     ON CONFLICT (id) DO UPDATE SET data=EXCLUDED.data,updated_at=NOW()`;
   return res.status(200).json({ok:true});
  }
  if(req.body.action==='save'){
   const a=normalizeArticle(req.body.article),id=a.id||'art-'+randomUUID(),data=JSON.stringify({...a,id});
   if (req.body.expectedVersion && a.id) {
    const version=req.body.expectedVersion;
    if(typeof version!=='string'||Number.isNaN(Date.parse(version)))return res.status(400).json({error:'نسخة المقالة غير صالحة.'});
    const changed=await sql`UPDATE blog_articles SET slug=${a.slug},status=${a.status},publish_date=${a.publishDate}::date,
      data=${data}::jsonb || CASE WHEN ${a.status}='published' AND NOT (${data}::jsonb ? 'scheduledAt') AND data ? 'scheduledAt' THEN jsonb_build_object('scheduledAt',data->'scheduledAt') ELSE '{}'::jsonb END,updated_at=NOW() WHERE id=${id} AND updated_at=${version}::timestamptz RETURNING id`;
    if(!changed.length)return res.status(409).json({error:'تغيّرت نسخة الخادم. نسخة عملك محفوظة على جهازك. راجع النسختين قبل النشر.'});
    return res.status(200).json({id,shortCode:shortCode(id),shortUrl:shortArticleUrl(id)});
   }
   await sql`INSERT INTO blog_articles(id,slug,status,publish_date,data) VALUES (${id},${a.slug},${a.status},${a.publishDate}::date,${data}::jsonb)
   ON CONFLICT (id) DO UPDATE SET slug=EXCLUDED.slug,status=EXCLUDED.status,publish_date=EXCLUDED.publish_date,data=EXCLUDED.data || CASE WHEN EXCLUDED.status='published' AND NOT (EXCLUDED.data ? 'scheduledAt') AND blog_articles.data ? 'scheduledAt' THEN jsonb_build_object('scheduledAt',blog_articles.data->'scheduledAt') ELSE '{}'::jsonb END,updated_at=NOW()`;
   return res.status(200).json({id,shortCode:shortCode(id),shortUrl:shortArticleUrl(id)});
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
  if(e?.code==='23514')return res.status(409).json({error:'أحد الملفات محجوز للحذف. اختر ملفًا آخر أو ارفع نسخة جديدة.'});
  if(e?.code==='23505')return res.status(409).json({error:'الرابط الوصفي مستخدم.'});
  if(/غير صالحة|مطلوبة|يجب|حقل/.test(e?.message||''))return res.status(400).json({error:e.message});
  console.error('Database error',e?.code||e?.name);
  return res.status(503).json({error:'تعذر الوصول إلى قاعدة البيانات.'});
 }
}

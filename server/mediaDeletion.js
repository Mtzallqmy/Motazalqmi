import {list,del} from '@vercel/blob';
import {database} from './db.js';
import {getAdmin,checkOrigin,noCache} from './auth.js';

export function managedMediaUrl(value) {
 try {
  if(typeof value!=='string'||value.length>2048)return null;
  const url=new URL(value);
  return url.protocol==='https:'&&/^[a-z0-9]+\.public\.blob\.vercel-storage\.com$/.test(url.hostname)&&
   !url.username&&!url.password&&!url.port&&!url.search&&!url.hash?url:null;
 }catch{return null;}
}
export async function mediaReferences(sql,url) {
 const result=await sql`SELECT id,data->>'title' AS title,'article' AS kind FROM blog_articles WHERE strpos(data::text,${url})>0
  UNION ALL SELECT id,coalesce(data->>'name',id),'profile' FROM blog_public_metadata WHERE strpos(data::text,${url})>0
  UNION ALL SELECT article_id,'نسخة سابقة','revision' FROM blog_article_revisions WHERE strpos(data::text,${url})>0 LIMIT 20`;
 return result;
}
export function createMediaDeleteHandler({sqlFactory=database,listObjects=list,deleteObject=del,authorize=getAdmin,originAllowed=checkOrigin}={}) {
 return async function(req,res) {
  noCache(res);
  if(req.method!=='POST')return res.status(405).json({error:'POST مطلوب.'});
  if(!authorize(req)||!originAllowed(req))return res.status(401).json({error:'تسجيل الدخول مطلوب.'});
  const url=managedMediaUrl(req.body?.url);
  if(!url||req.body?.confirm!==true)return res.status(400).json({error:'تأكيد الحذف ورابط ملف من مكتبة المدونة مطلوبان.'});
  try {
   const sql=sqlFactory(),address=url.href;
   const existing=await sql`SELECT state FROM blog_deleted_media WHERE url=${address}`;
   if(!existing.length) {
    const page=await listObjects({prefix:decodeURIComponent(url.pathname.slice(1)),limit:1000});
    if(!page.blobs.some(blob=>blob.url===address))return res.status(404).json({error:'الملف غير موجود في مخزن المدونة.'});
   }
   // A separate statement acquires the lock before the reference query obtains its MVCC snapshot.
   const [,marked]=await sql.transaction([
    sql`SELECT pg_advisory_xact_lock(6921330)`,
    sql`INSERT INTO blog_deleted_media(url,state)
     SELECT ${address},'pending' WHERE
       NOT EXISTS(SELECT 1 FROM blog_articles WHERE strpos(data::text,${address})>0) AND
       NOT EXISTS(SELECT 1 FROM blog_public_metadata WHERE strpos(data::text,${address})>0) AND
       NOT EXISTS(SELECT 1 FROM blog_article_revisions WHERE strpos(data::text,${address})>0)
     ON CONFLICT(url) DO UPDATE SET state=CASE WHEN blog_deleted_media.state='deleted' THEN 'deleted' ELSE 'pending' END
     RETURNING url,state`
   ]);
   if(!marked.length)return res.status(409).json({error:'الملف مستخدم في المحتوى أو مراجعاته.',references:await mediaReferences(sql,address)});
   if(marked[0].state!=='deleted') {
    await deleteObject(address);
    await sql`UPDATE blog_deleted_media SET state='deleted',deleted_at=now() WHERE url=${address}`;
   }
   return res.status(200).json({ok:true,url:address});
  }catch{
   return res.status(503).json({error:'تعذّر إكمال الحذف. قد يبقى الملف محجوزًا للحذف. أعد المحاولة من المكتبة.'});
  }
 };
}
export default createMediaDeleteHandler();

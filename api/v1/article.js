import { database, articleRow } from '../../server/db.js';
import { publicGet, sendJson, publicError, param } from '../../server/publicApi.js';
export default async function handler(req,res){
 if(!publicGet(req,res))return;
 const slug=param(req,'slug',150);const id=param(req,'id',150);
 if(!slug&&!id)return res.status(400).json({error:'slug or id is required'});
 try{
  const sql=database();
  const rows=await sql`SELECT id,slug,status,publish_date::text,data FROM blog_published_articles WHERE status='published' AND (${slug}<>'' AND slug=${slug} OR ${id}<>'' AND id=${id}) LIMIT 1`;
  if(!rows.length)return res.status(404).json({error:'المقالة غير موجودة.'});
  return sendJson(req,res,{apiVersion:1,article:articleRow(rows[0])});
 }catch(e){return publicError(res,e)}
}

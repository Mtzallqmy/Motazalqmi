import { createHash } from 'node:crypto';
import { database, articleRow } from './db.js';

export function publicGet(req, res) {
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Allow', 'GET, HEAD');
  if (!['GET','HEAD'].includes(req.method)) {
    res.setHeader('Cache-Control', 'no-store');
    res.status(405).json({error:'Method not allowed'});
    return false;
  }
  return true;
}
export function sendJson(req,res,payload,{seconds=60}={}) {
  const text=JSON.stringify(payload);
  const etag=`\"${createHash('sha256').update(text).digest('base64url')}\"`;
  res.setHeader('ETag',etag);
  res.setHeader('Cache-Control',`public, max-age=${seconds}, s-maxage=${seconds}, stale-while-revalidate=300`);
  res.setHeader('Content-Type','application/json; charset=utf-8');
  if(req.headers?.['if-none-match']===etag) return res.status(304).end();
  if(req.method==='HEAD') return res.status(200).end();
  return res.status(200).send(text);
}
export function publicError(res,err) {
  console.error('Public blog API failed',err?.code || err?.name || 'unexpected');
  res.setHeader('Cache-Control','no-store');
  return res.status(503).json({error:'تعذر تحميل المحتوى حاليًا.'});
}
export function param(req,key,max=120){
  const x=req.query?.[key];
  return typeof x==='string'&&x.length<=max ? x.trim() : '';
}
export function boundedInt(value,fallback,min,max){
  if(value===undefined)return fallback;
  if(!/^\d{1,7}$/.test(String(value)))return fallback;
  const n=Number(value);return Math.min(Math.max(n,min),max);
}
export async function listPublished({limit=100,offset=0,category='',q='',featured=false,full=false}={}) {
  const sql=database();
  const search=`%${q.replace(/[\\%_]/g,'\\$&')}%`;
  // Filtering and pagination remain parameterized: no SQL text is built from client input.
  const where=category||q||featured;
  let rows,count;
  if(where){
    [rows,count]=await Promise.all([
      sql`SELECT id,slug,status,publish_date::text,data FROM blog_articles
        WHERE status='published'
        AND (${category}='' OR data->>'category'=${category})
        AND (${q}='' OR data->>'title' ILIKE ${search} ESCAPE '\\' OR data->>'excerpt' ILIKE ${search} ESCAPE '\\' OR data->>'content' ILIKE ${search} ESCAPE '\\')
        AND (${featured}=false OR data->>'featured'='true')
        ORDER BY publish_date DESC,updated_at DESC LIMIT ${limit} OFFSET ${offset}`,
      sql`SELECT COUNT(*)::int AS total FROM blog_articles
        WHERE status='published'
        AND (${category}='' OR data->>'category'=${category})
        AND (${q}='' OR data->>'title' ILIKE ${search} ESCAPE '\\' OR data->>'excerpt' ILIKE ${search} ESCAPE '\\' OR data->>'content' ILIKE ${search} ESCAPE '\\')
        AND (${featured}=false OR data->>'featured'='true')`
    ]);
  } else {
    [rows,count]=await Promise.all([
      sql`SELECT id,slug,status,publish_date::text,data FROM blog_articles WHERE status='published' ORDER BY publish_date DESC,updated_at DESC LIMIT ${limit} OFFSET ${offset}`,
      sql`SELECT COUNT(*)::int AS total FROM blog_articles WHERE status='published'`
    ]);
  }
  const articles=rows.map(r=>{const a=articleRow(r);if(!full)delete a.content;return a;});
  return {articles,total:count[0]?.total||0,offset,limit,hasMore:offset+articles.length<(count[0]?.total||0)};
}

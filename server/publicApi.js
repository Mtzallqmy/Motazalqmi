import { createHash } from 'node:crypto';
import { database, articleRow } from './db.js';

export function publicGet(req, res) {
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Allow', 'GET, HEAD');
  if (!['GET','HEAD'].includes(req.method)) {
    res.setHeader('Cache-Control', 'no-store');
    res.status(405).json({apiVersion:1,error:'Method not allowed',code:'METHOD_NOT_ALLOWED'});
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
  return res.status(503).json({apiVersion:1,error:'تعذر تحميل المحتوى حاليًا.',code:'CONTENT_UNAVAILABLE'});
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
export async function listPublished(options={}) {
  const { publishedSnapshot, pageSnapshot } = await import('./contentSnapshot.js');
  return pageSnapshot(await publishedSnapshot(),options);
}

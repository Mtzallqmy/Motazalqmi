import {publicGet,sendJson,publicError} from './publicApi.js';
import {publishedSnapshot} from './snapshot.js';

// A read-only inventory. Remote HTTPS media must be fetched by the client; this
// endpoint never claims to embed original image bytes in a JSON response.
export function imageInventory(snapshot){
 const items=[], seen=new Set();
 function add(link,kind,articleId,slug){
  if(typeof link!=='string'||link.length>2048||!link.startsWith('https://')||seen.has(link))return;
  let u;try{u=new URL(link)}catch{return}
  if(u.username||u.password||u.port||u.protocol!=='https:')return;
  seen.add(link);items.push({url:link,kind,articleId,slug,cacheUrl:link.startsWith('https://images.unsplash.com/')?'https://motazalqmi.vercel.app/api/v1/image?src='+encodeURIComponent(link):link});
 }
 for(const a of snapshot.articles||[]){
  if(a.status!=='published')continue;
  add(a.coverImage,'cover',a.id,a.slug);
  const pattern=/!\[[^\]]{0,300}\]\(\s*<?(https:\/\/[^)\s>]+)>?(?:\s+["'][^"']*["'])?\s*\)/g;
  for(const match of String(a.content||'').matchAll(pattern))add(match[1],'article',a.id,a.slug);
 }
 return items;
}
export function createHandler(load=publishedSnapshot){
 return async function handler(req,res){
  if(!publicGet(req,res))return;
  try{
   const snapshot=await load();
   const items=imageInventory(snapshot);
   return sendJson(req,res,{apiVersion:1,media:items,total:items.length,downloadRequired:true},{seconds:60});
  }catch(e){return publicError(res,e)}
 };
}
export default createHandler();


/** Bounded first-party read-through proxy for images already referenced by published articles.
 * A trusted-host check and exact match in the published catalogue prevent arbitrary SSRF. */
export async function imageHandler(req,res){
 if(!publicGet(req,res))return;
 const url=typeof req.query?.src==='string'&&req.query.src.length<=2048?req.query.src:'';
 if(!url.startsWith('https://images.unsplash.com/'))return res.status(400).json({apiVersion:1,error:'Unsupported media URL'});
 try{
  const parsed=new URL(url);
  if(parsed.hostname!=='images.unsplash.com'||parsed.protocol!=='https:'||parsed.port||parsed.username||parsed.password)
   return res.status(400).json({apiVersion:1,error:'Invalid media host'});
  const images=imageInventory(await publishedSnapshot());
  if(!images.some(x=>x.url===url))return res.status(404).json({apiVersion:1,error:'Image not published'});
  // Android 9 cannot decode AVIF natively. Request JPEG explicitly from Unsplash.
  const sourceUrl=new URL(url);
  sourceUrl.searchParams.delete('auto');
  sourceUrl.searchParams.set('fm','jpg');
  const upstream=await fetch(sourceUrl.toString(),{method:'GET',redirect:'error',signal:AbortSignal.timeout(9000),headers:{Accept:'image/jpeg'}});
  if(!upstream.ok)return res.status(502).json({apiVersion:1,error:'Image origin unavailable'});
  const kind=(upstream.headers.get('content-type')||'').split(';')[0].trim().toLowerCase();
  if(!['image/jpeg','image/webp','image/png','image/avif'].includes(kind))
   return res.status(502).json({apiVersion:1,error:'Unsupported image MIME type'});
  const declared=Number(upstream.headers.get('content-length')||0);
  if(declared>4*1024*1024)return res.status(413).json({apiVersion:1,error:'Image too large'});
  if(req.method==='HEAD'){
   res.setHeader('Content-Type',kind);
   res.setHeader('Cache-Control','public, max-age=3600, s-maxage=86400');
   return res.status(200).end();
  }
  const bytes=Buffer.from(await upstream.arrayBuffer());
  if(bytes.length>4*1024*1024)return res.status(413).json({apiVersion:1,error:'Image too large'});
  res.setHeader('Content-Type',kind);
  res.setHeader('Cache-Control','public, max-age=3600, s-maxage=86400, stale-while-revalidate=604800');
  res.setHeader('X-Content-Type-Options','nosniff');
  return res.status(200).send(bytes);
 }catch(e){console.error('Image fetch failed',e?.name||'Error');return res.status(502).json({apiVersion:1,error:'Image unavailable'})}
}

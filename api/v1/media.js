import {publicGet,sendJson,publicError} from '../../server/publicApi.js';
import {publishedSnapshot} from '../../server/snapshot.js';

// A read-only inventory. Remote HTTPS media must be fetched by the client; this
// endpoint never claims to embed original image bytes in a JSON response.
export function imageInventory(snapshot){
 const items=[], seen=new Set();
 function add(link,kind,articleId,slug){
  if(typeof link!=='string'||link.length>2048||!link.startsWith('https://')||seen.has(link))return;
  let u;try{u=new URL(link)}catch{return}
  if(u.username||u.password||u.port||u.protocol!=='https:')return;
  seen.add(link);items.push({url:link,kind,articleId,slug});
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

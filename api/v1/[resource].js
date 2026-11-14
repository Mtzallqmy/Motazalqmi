import mobileAdminHandler from '../../server/mobileAdmin.js';
import {createHandler as createMediaHandler} from '../../server/mediaInventory.js';
const mediaHandler=createMediaHandler();
import { publicGet, sendJson, publicError, listPublished, param, boundedInt } from '../../server/publicApi.js';
import { pageSnapshot } from '../../server/contentSnapshot.js';
import { publishedSnapshot } from '../../server/snapshot.js';

export function createHandler({snapshot=publishedSnapshot,list=listPublished}={}) {
 return async function handler(req,res) {
  const resource=param(req,'resource',30);
  // Consolidated into this dynamic route to stay within Vercel Hobby's 12-function quota.
  if(resource==='mobile-admin') return mobileAdminHandler(req,res);
  if(resource==='media') return mediaHandler(req,res);
  if (!publicGet(req,res)) return;
  if(resource==='search'&&!param(req,'q',120))return res.status(400).json({apiVersion:1,error:'Invalid query',code:'INVALID_QUERY'});
  try {
   switch(resource) {
    case 'sync': {
     // since is accepted for forwards compatibility; this version always sends an authoritative snapshot.
     const since=param(req,'since',64);
     if(since && !(/^[a-f0-9]{64}$/.test(since)) && Number.isNaN(Date.parse(since)))return res.status(400).json({error:'Invalid since timestamp',code:'INVALID_INPUT',apiVersion:1});
     const value=await snapshot();
     return sendJson(req,res,{...value,authoritative:true,ids:value.articles.map(a=>a.id)},{seconds:0});
    }
    case 'search':
    case 'featured': {
     const page=await list({q:resource==='search'?param(req,'q',120):'',featured:resource==='featured',
      category:param(req,'category',60),limit:boundedInt(req.query?.limit,30,1,100),offset:boundedInt(req.query?.offset,0,0,100000)});
     return sendJson(req,res,{apiVersion:1,...page});
    }
    case 'config': return sendJson(req,res,{apiVersion:1,syncMode:'snapshot',syncPath:'/api/v1/sync',maxSnapshotArticles:2000,
      officialUrl:'https://motazalqmi.vercel.app',privacy:'Reader data stays on your device. HTTPS requests may be logged by the hosting provider.'});
    case 'versions': return sendJson(req,res,{apiVersion:1,versions:[{version:'2.0.0',versionCode:20000,minSdk:26,status:'development',downloadUrl:null}]});
    default: return res.status(404).json({apiVersion:1,code:'NOT_FOUND',error:'Unknown resource'});
   }
  } catch(e) { return publicError(res,e); }
 };
}
export function createResourceHandler(load) {
 return createHandler({snapshot:async()=>{const s=await load();return {apiVersion:1,mode:'snapshot',complete:true,...s,total:s.articles.length}},list:async(options)=>pageSnapshot(await load(),options)});
}
export default createHandler();

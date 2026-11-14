import { publicGet, sendJson, publicError, param, boundedInt } from '../../server/publicApi.js';
import { publishedSnapshot, pageSnapshot, syncManifest } from '../../server/contentSnapshot.js';
export function createResourceHandler(load=publishedSnapshot) {
 return async function handler(req,res) {
  if(!publicGet(req,res))return;
  const resource=param(req,'resource',30);
  if(resource==='config')return sendJson(req,res,{apiVersion:1,syncMode:'verified-snapshot',maxPageSize:100,minimumSyncMinutes:15,privacy:'https://motazalqmi.vercel.app/about',readerOnly:true});
  if(resource==='versions')return sendJson(req,res,{apiVersion:1,version:'2.0.0',versionCode:20000,minSdk:26,appId:'com.moataz.motazalqmi',releaseUrl:'https://github.com/Mtzallqmy/Motazalqmi/releases',download:null});
  if(!['sync','search','featured'].includes(resource))return res.status(404).json({apiVersion:1,error:'المسار غير موجود.',code:'NOT_FOUND'});
  if(resource==='search'&&(!param(req,'q',120)||Array.isArray(req.query.q)))return res.status(400).json({apiVersion:1,error:'اكتب عبارة بحث لا تتجاوز 120 حرفًا.',code:'INVALID_QUERY'});
  try {
   const snapshot=await load();
   if(resource==='sync') {
    const manifest=syncManifest(snapshot);
    return sendJson(req,res,{...manifest,unchanged:param(req,'since',64)===snapshot.revision},{seconds:0});
   }
   const page=pageSnapshot(snapshot,{q:param(req,'q',120),featured:resource==='featured',limit:boundedInt(req.query.limit,30,1,100),offset:boundedInt(req.query.offset,0,0,100000)});
   return sendJson(req,res,{apiVersion:1,...page});
  }catch(e){return publicError(res,e);}
 };
}
export default createResourceHandler();

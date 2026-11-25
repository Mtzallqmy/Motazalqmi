import mediaDeleteHandler from '../../server/mediaDeletion.js';
import {releaseCatalogue} from '../../server/releases.js';
import { studio } from '../../server/editorialStudio.js';
import { discovery } from '../../server/articleDiscovery.js';
import mobileAdminHandler from '../../server/mobileAdmin.js';
import blobUploadHandler from '../../server/blobUpload.js';
import { generateClientTokenFromReadWriteToken } from '@vercel/blob/client';
import mediaDirectHandler, {createTicketHandler} from '../../server/mediaDirect.js';
import telegramHandler from '../../server/telegramBot.js';
import telegramPairHandler from '../../server/telegramPair.js';
import {createHandler as createMediaHandler,imageHandler} from '../../server/mediaInventory.js';
const mediaHandler=createMediaHandler();
const ticketHandler=createTicketHandler(generateClientTokenFromReadWriteToken);
import { publicGet, sendJson, publicError, listPublished, param, boundedInt } from '../../server/publicApi.js';
import { pageSnapshot } from '../../server/contentSnapshot.js';
import { publishedSnapshot } from '../../server/snapshot.js';

export function createHandler({snapshot=publishedSnapshot,list=listPublished,releases=releaseCatalogue}={}) {
 return async function handler(req,res) {
  const resource=param(req,'resource',30);
  // Consolidated into this dynamic route to stay within Vercel Hobby's 12-function quota.
  if(['authors','revisions','assets'].includes(resource)) return studio(req,res,resource);
  if(['article-page','sitemap','short-link'].includes(resource)) return discovery(req,res,resource);
  if(resource==='mobile-admin') return mobileAdminHandler(req,res);
  if(resource==='media-delete') return mediaDeleteHandler(req,res);
  if(resource==='media-upload') return blobUploadHandler(req,res);
  if(resource==='media-ticket') return ticketHandler(req,res);
  if(resource==='media-direct') return mediaDirectHandler(req,res);
  if(resource==='telegram') return telegramHandler(req,res);
  if(resource==='telegram-pair') return telegramPairHandler(req,res);
  if(resource==='media') return mediaHandler(req,res);
  if(resource==='image') return imageHandler(req,res);
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
    case 'versions': return sendJson(req,res,{apiVersion:1,...await releases()});
    default: return res.status(404).json({apiVersion:1,code:'NOT_FOUND',error:'Unknown resource'});
   }
  } catch(e) { return publicError(res,e); }
 };
}
export function createResourceHandler(load,releases=releaseCatalogue) {
 return createHandler({releases,snapshot:async()=>{const s=await load();return {apiVersion:1,mode:'snapshot',complete:true,...s,total:s.articles.length}},list:async(options)=>pageSnapshot(await load(),options)});
}
export default createHandler();

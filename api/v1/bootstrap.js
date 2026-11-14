import { SITE, SECTIONS, QUOTES } from '../../shared/publicContent.js';
import { publicGet, sendJson, publicError, listPublished } from '../../server/publicApi.js';
export default async function handler(req,res){
  if(!publicGet(req,res))return;
  try{
    // The first 100 complete articles are cached offline by the Android app.
    // Apps with a larger library retrieve all remaining pages from /api/v1/articles?full=1.
    const page=await listPublished({limit:100,full:true});
    return sendJson(req,res,{apiVersion:1,site:SITE,sections:SECTIONS,quotes:QUOTES,...page});
  }catch(e){return publicError(res,e)}
}

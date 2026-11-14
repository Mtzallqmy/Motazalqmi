import { editorial } from '../../server/editorial.js';
import { publicGet, sendJson, publicError, listPublished } from '../../server/publicApi.js';
export default async function handler(req,res){
  if(!publicGet(req,res))return;
  try{
    // The first 100 complete articles are cached offline by the Android app.
    // Apps with a larger library retrieve all remaining pages from /api/v1/articles?full=1.
    const [page,metadata]=await Promise.all([listPublished({limit:100,full:true}),editorial()]);
    return sendJson(req,res,{apiVersion:1,site:metadata.site,sections:metadata.sections,quotes:metadata.quotes,...page});
  }catch(e){return publicError(res,e)}
}

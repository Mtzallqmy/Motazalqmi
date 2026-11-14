import { publicGet, sendJson, publicError, listPublished, param, boundedInt } from '../../server/publicApi.js';
export default async function handler(req,res){
 if(!publicGet(req,res))return;
 try{
  const limit=boundedInt(req.query.limit,30,1,100);
  const offset=boundedInt(req.query.offset,0,0,100000);
  const category=param(req,'category',60);
  const q=param(req,'q',120);
  const featured=req.query.featured==='1';
  const full=req.query.full==='1';
  const page=await listPublished({limit,offset,category,q,featured,full});
  return sendJson(req,res,{apiVersion:1,...page});
 }catch(e){return publicError(res,e)}
}

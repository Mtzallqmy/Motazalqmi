import {mediaExtensions,contentTypesForPath} from '../shared/mediaTypes.js';
import {handleUpload} from '@vercel/blob/client';
import {getAdmin,checkOrigin,noCache} from './auth.js';
export const allowedMediaTypes=Object.keys(mediaExtensions);
export const mediaMaxBytes=100*1024*1024;
export function validMediaPath(path){
 return typeof path==='string'&&path.length<=240&&/^(media|authors|covers)\/[a-zA-Z0-9][a-zA-Z0-9._-]{0,180}$/.test(path)&&!path.includes('..');
}
export function createUploadHandler({handle=handleUpload}={}){
 return async function uploadHandler(req,res){
  noCache(res);
  if(req.method!=='POST'){res.setHeader('Allow','POST');return res.status(405).json({error:'POST مطلوب.'});}
  if(!process.env.BLOB_READ_WRITE_TOKEN)return res.status(503).json({error:'مخزن الوسائط غير مهيأ.'});
  if(typeof req.body!=='object'||!req.body||JSON.stringify(req.body).length>10000)return res.status(400).json({error:'طلب رفع غير صالح.'});
  try{
   const result=await handle({
    request:req,body:req.body,
    onBeforeGenerateToken:async pathname=>{
     if(!getAdmin(req)||!checkOrigin(req))throw Error('AUTH_REQUIRED');
     if(!validMediaPath(pathname)||!contentTypesForPath(pathname).length)throw Error('INVALID_MEDIA_PATH');
     return {allowedContentTypes:contentTypesForPath(pathname),maximumSizeInBytes:mediaMaxBytes,
      addRandomSuffix:true,validUntil:Date.now()+10*60*1000};
    },
    onUploadCompleted:async ({blob})=>{
     if(!blob?.url?.startsWith('https://'))throw Error('Invalid uploaded URL');
    }
   });
   return res.status(200).json(result);
  }catch(e){
   const message=e.message==='AUTH_REQUIRED'?'تسجيل دخول الكاتب مطلوب.':
    e.message==='INVALID_MEDIA_PATH'?'اسم الملف غير صالح.':'تعذر رفع الملف. تحقق من نوعه وحجمه.';
   return res.status(e.message==='AUTH_REQUIRED'?401:400).json({error:message});
  }
 };
}
export default createUploadHandler();

import {put} from '@vercel/blob';
import {randomUUID} from 'node:crypto';
import {getAdmin,checkOrigin,noCache} from './auth.js';

const allowed={
 'image/jpeg':'jpg','image/png':'png','image/webp':'webp','image/gif':'gif',
 'video/mp4':'mp4','video/webm':'webm','audio/mpeg':'mp3','audio/mp4':'m4a',
 'audio/ogg':'ogg','application/pdf':'pdf'
};
export const nativeMaxBytes=1800000;
export function mediaMagic(bytes,mime){
 if(!Buffer.isBuffer(bytes)||bytes.length<12)return false;
 if(mime==='image/jpeg')return bytes[0]===255&&bytes[1]===216&&bytes[2]===255;
 if(mime==='image/png')return bytes.subarray(0,8).equals(Buffer.from([137,80,78,71,13,10,26,10]));
 if(mime==='image/gif')return ['GIF89a','GIF87a'].includes(bytes.toString('ascii',0,6));
 if(mime==='image/webp')return bytes.toString('ascii',0,4)==='RIFF'&&bytes.toString('ascii',8,12)==='WEBP';
 if(mime==='video/mp4'||mime==='audio/mp4')return bytes.toString('ascii',4,8)==='ftyp';
 if(mime==='video/webm')return bytes.subarray(0,4).equals(Buffer.from([26,69,223,163]));
 if(mime==='audio/mpeg')return bytes.toString('ascii',0,3)==='ID3'||(bytes[0]===255&&(bytes[1]&224)===224);
 if(mime==='audio/ogg')return bytes.toString('ascii',0,4)==='OggS';
 if(mime==='application/pdf')return bytes.toString('ascii',0,5)==='%PDF-';
 return false;
}
export function createMediaDirectHandler({putBlob=put}={}){
 return async (req,res)=>{
  noCache(res);
  if(req.method!=='POST'){res.setHeader('Allow','POST');return res.status(405).json({error:'POST required'});}
  if(!getAdmin(req)||!checkOrigin(req))return res.status(401).json({error:'سجّل دخول الإدارة أولًا.'});
  if(!process.env.BLOB_READ_WRITE_TOKEN)return res.status(503).json({error:'تخزين الملفات غير مهيأ.'});
  const {mime,base64,purpose}=req.body||{};
  if(typeof mime!=='string'||!allowed[mime]||!['media','author','cover'].includes(purpose)||
    (purpose!=='media'&&!mime.startsWith('image/'))||typeof base64!=='string'||
    base64.length>Math.ceil(nativeMaxBytes*4/3)+4||!/^[A-Za-z0-9+/]+={0,2}$/.test(base64))
   return res.status(400).json({error:'صيغة الملف أو حجمه غير مدعوم.'});
  const bytes=Buffer.from(base64,'base64');
  if(bytes.length<12||bytes.length>nativeMaxBytes||!mediaMagic(bytes,mime))
   return res.status(415).json({error:'نوع الملف لا يطابق محتواه أو حجمه كبير.'});
  const folder=purpose==='author'?'authors':purpose==='cover'?'covers':'media';
  try{
   const uploaded=await putBlob(folder+'/'+randomUUID()+'.'+allowed[mime],bytes,{access:'public',contentType:mime,addRandomSuffix:false});
   return res.status(200).json({url:uploaded.url,size:bytes.length,mime});
  }catch{return res.status(503).json({error:'تعذر إرسال الملف إلى مخزن الوسائط.'});}
 };
}
export default createMediaDirectHandler();

/** Scope a native writer upload to one new object, one declared MIME and a byte ceiling. */
export function createTicketHandler(issue) {
 return async function(req,res) {
  noCache(res);
  if(req.method!=='POST')return res.status(405).end();
  if(!getAdmin(req)||!checkOrigin(req))return res.status(401).json({error:'تسجيل الدخول مطلوب.'});
  const {mime,size,purpose,prefix}=req.body || {};
  if(typeof mime!=='string'||!Object.hasOwn(allowed,mime)||!['cover','author','media'].includes(purpose)||
     (purpose!=='media'&&!mime.startsWith('image/'))||!Number.isSafeInteger(size)||size<12||size>100*1024*1024||
     typeof prefix!=='string'||prefix.length>64||!mediaMagic(Buffer.from(prefix,'base64'),mime))
    return res.status(400).json({error:'صيغة الملف أو حجمه غير مدعوم.'});
  const folder={cover:'covers',author:'authors',media:'media'}[purpose];
  const pathname=folder+'/'+randomUUID()+'.'+allowed[mime];
  try {
   const token=await issue({pathname,allowedContentTypes:[mime],maximumSizeInBytes:size,validUntil:Date.now()+600000,
    addRandomSuffix:false,allowOverwrite:false});
   return res.status(200).json({pathname,token});
  }catch{return res.status(503).json({error:'تعذّر تجهيز الرفع المباشر.'});}
 };
}

import {useRef,useState} from 'react';
import {upload} from '@vercel/blob/client';
import {UploadCloud} from 'lucide-react';
import {mediaMarkdown} from '../lib/media.js';
const types=['image/jpeg','image/png','image/webp','image/gif','video/mp4','video/webm','audio/mpeg','audio/mp4','audio/ogg','application/pdf'];
const PHOTO='image/jpeg,image/png,image/webp,image/gif';
const ALL=PHOTO+',video/mp4,video/webm,audio/mpeg,audio/mp4,audio/ogg,application/pdf';
const SIZE=100*1024*1024;
/** Browser -> Vercel Blob: only a signed short-lived upload token crosses the app API. */
export function MediaUploader({purpose='body',onUploaded,disabled=false}){
 const input=useRef(null),[pending,setPending]=useState(false),[progress,setProgress]=useState(0),[error,setError]=useState('');
 const imageOnly=purpose!=='body';
 async function select(event){
  const file=event.target.files?.[0];
  if(!file)return;
  event.target.value='';
  setError('');
  if(!types.includes(file.type)||(imageOnly&&!file.type.startsWith('image/'))){setError('نوع الملف غير مدعوم.');return}
  if(file.size>SIZE||file.size===0){setError('الحد الأقصى 100 ميجابايت للملف.');return}
  const folder=purpose==='author'?'authors':purpose==='cover'?'covers':'media';
  const extension=file.name.split('.').pop()?.toLowerCase()||'bin';
  const filename=(Date.now()+'-'+Math.random().toString(36).slice(2,10)+'.'+extension);
  setPending(true);setProgress(0);
  try{
   const result=await upload(folder+'/'+filename,file,{access:'public',
    handleUploadUrl:'/api/v1/media-upload',
    multipart:file.size>8*1024*1024,
    onUploadProgress:p=>setProgress(Math.round(p.percentage??0))
   });
   if(!result.url?.startsWith('https://'))throw new Error('لم يرجع الخادم رابطًا صالحًا.');
   onUploaded?.(result.url,file,mediaMarkdown(result.url,file.name));
  }catch(e){setError(e.message||'فشل رفع الملف. تحقق من الاتصال.');}
  finally{setPending(false)}
 }
 const labels={author:'صورة الكاتب',cover:'صورة الغلاف',body:'إضافة صورة أو فيديو أو صوت أو PDF'};
 return <div className="rounded-xl border border-gold/30 bg-cream/60 p-3">
  <input ref={input} type="file" accept={imageOnly?PHOTO:ALL} hidden onChange={select}/>
  <button type="button" disabled={pending||disabled} onClick={()=>input.current?.click()}
   className="inline-flex items-center gap-2 rounded-xl bg-teal px-4 py-2.5 text-white text-sm font-bold">
   <UploadCloud size={17}/>{pending?'جار رفع الملف…':labels[purpose]}
  </button>
  {pending&&<div className="mt-2" role="status"><div className="h-2 rounded-full bg-gold/20 overflow-hidden"><div className="h-full bg-teal" style={{width:progress+'%'}}/></div><p className="text-xs mt-1">{progress}%</p></div>}
  {error&&<p role="alert" className="text-red-700 text-sm mt-2">{error}</p>}
  <p className="text-xs text-ink/60 mt-2">{imageOnly?'JPG، PNG، WebP، GIF':'صور، MP4، WebM، ملفات صوتية وPDF'} · حتى 100 ميجابايت · ملفات عامة</p>
 </div>;
}

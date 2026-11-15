import {useEffect,useRef,useState} from 'react';
import {upload} from '@vercel/blob/client';
import {UploadCloud} from 'lucide-react';
import {mediaMarkdown} from '../lib/media';
import {mediaExtensions} from '../../shared/mediaTypes';

const limit=100*1024*1024;
export function MediaUploader({purpose='body',onUploaded,disabled=false}){
 const input=useRef(null),abort=useRef(null),callback=useRef(onUploaded);callback.current=onUploaded;
 const [pending,setPending]=useState(false),[progress,setProgress]=useState(0),[error,setError]=useState('');
 const [fileName,setFileName]=useState('');
 useEffect(()=>()=>abort.current?.abort(),[]);
 const imageOnly=purpose!=='body';
 const accepted=Object.keys(mediaExtensions).filter(t=>!imageOnly||t.startsWith('image/'));
 async function sendFiles(selection){
  const files=Array.from(selection);
  if(!files.length||disabled||abort.current)return;
  if(imageOnly&&files.length>1){setError('اختر صورة واحدة.');return;}
  const controller=new AbortController();abort.current=controller;setPending(true);setError('');
  try {
   for(const file of files){
    if(!accepted.includes(file.type)||!file.size||file.size>limit)throw Error('نوع الملف أو حجمه غير مدعوم.');
    const folder=purpose==='author'?'authors':purpose==='cover'?'covers':'media';
    setFileName(file.name);setProgress(0);
    const pathname=folder+'/'+crypto.randomUUID()+'.'+mediaExtensions[file.type];
    const result=await upload(pathname,file,{access:'public',handleUploadUrl:'/api/v1/media-upload',
     multipart:file.size>8*1024*1024,abortSignal:controller.signal,onUploadProgress:p=>setProgress(Math.round(p.percentage))});
    if(controller.signal.aborted)break;
    callback.current?.(result.url,file,mediaMarkdown(result.url,file.name));
   }
  }catch{setError(controller.signal.aborted?'أُلغي الرفع.':'تعذّر رفع الملف. تحقق من النوع والحجم والاتصال ثم أعد اختياره.');}
  finally{abort.current=null;setPending(false);}
 }
 return <div className="rounded-xl border border-gold/30 bg-cream/60 p-3" onDragOver={e=>e.preventDefault()} onDrop={e=>{e.preventDefault();sendFiles(e.dataTransfer.files);}}>
  <input ref={input} type="file" multiple={!imageOnly} accept={accepted.join(',')} hidden onChange={e=>{sendFiles(e.target.files);e.target.value='';}}/>
  <button type="button" disabled={pending||disabled} onClick={()=>input.current.click()} className="inline-flex items-center gap-2 rounded-xl bg-teal px-4 py-2.5 text-white">
   <UploadCloud size={17}/>{purpose==='author'?'رفع صورة الكاتب':purpose==='cover'?'رفع صورة الغلاف':'إضافة ملفات إلى المقالة'}</button>
  {pending&&<div role="status">{fileName} · {progress}% <button onClick={()=>abort.current?.abort()}>إلغاء الرفع</button></div>}
  {error&&<p role="alert">{error}</p>}<p className="text-xs mt-2">اسحب {imageOnly?'الصورة':'الملفات'} هنا أو اختر من الجهاز · حتى 100 ميجابايت للملف</p>
 </div>;
}

import {put} from '@vercel/blob';
import {randomUUID} from 'node:crypto';
import {mediaMagic,nativeMaxBytes} from './mediaDirect.js';
import {mediaExtensions} from '../shared/mediaTypes.js';
export function telegramAttachment(message) {
 const photo=message.photo?.at(-1);
 const item=photo||message.video||message.audio||message.document;
 if(!item)return null;
 const mime=photo?'image/jpeg':item.mime_type;
 if(!Object.hasOwn(mediaExtensions,mime)||typeof item.file_id!=='string'||item.file_id.length>300||
  !Number.isSafeInteger(item.file_size)||item.file_size<12||item.file_size>nativeMaxBytes)
  throw Error('الملف غير مدعوم أو أكبر من 1.8 ميجابايت. استخدم مكتبة الموقع للملفات الكبيرة.');
 return {mime,fileId:item.file_id,size:item.file_size};
}
export async function storeTelegramMedia({attachment,token,getFile,download=fetch,upload=put}) {
 const file=await getFile(attachment.fileId),path=file?.file_path;
 if(typeof path!=='string'||path.includes('..')||!/^[-a-zA-Z0-9_/\.]{1,400}$/.test(path))throw Error('Unsafe Telegram path');
 const response=await download('https://api.telegram.org/file/bot'+token+'/'+path,
  {redirect:'error',signal:AbortSignal.timeout(7000)});
 if(!response.ok||Number(response.headers.get('content-length')||0)>nativeMaxBytes)throw Error('Telegram file unavailable');
 const reader=response.body?.getReader();if(!reader)throw Error('Empty Telegram file');
 const chunks=[];let total=0;
 try {
  while(true){const {done,value}=await reader.read();if(done)break;total+=value.length;
   if(total>nativeMaxBytes)throw Error('Telegram file too large');chunks.push(Buffer.from(value));}
 }finally{await reader.cancel().catch(()=>{});}
 const bytes=Buffer.concat(chunks);
 if(bytes.length!==attachment.size||!mediaMagic(bytes,attachment.mime))throw Error('Invalid Telegram media');
 const blob=await upload('media/telegram-'+randomUUID()+'.'+mediaExtensions[attachment.mime],bytes,
  {access:'public',contentType:attachment.mime,addRandomSuffix:false,abortSignal:AbortSignal.timeout(7000)});
 return blob.url;
}

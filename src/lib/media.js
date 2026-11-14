export const mediaKind=url=>{
 try{
  const u=new URL(url);
  if(u.protocol!=='https:')return 'invalid';
  const p=u.pathname.toLowerCase();
  if(/\.(mp4|webm)$/.test(p))return 'video';
  if(/\.(mp3|m4a|ogg)$/.test(p))return 'audio';
  if(/\.pdf$/.test(p))return 'document';
  return 'image';
 }catch{return 'invalid'}
};
export const mediaMarkdown=(url,label='ملف مرفق')=>{
 const name=String(label).replace(/[\]\r\n]/g,'').trim().slice(0,120)||'ملف مرفق';
 if(mediaKind(url)==='invalid')throw Error('HTTPS required');
 return mediaKind(url)==='document'?'[📎 '+name+']('+url+')':'!['+name+']('+url+')';
};

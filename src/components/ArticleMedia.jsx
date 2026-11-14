import {mediaKind} from '../lib/media.js';
export function ArticleMedia({src,alt='',...props}){
 const kind=mediaKind(src);
 if(kind==='video')return <figure className="my-7"><video controls playsInline preload="metadata"
  className="w-full max-h-[510px] rounded-2xl bg-ink shadow-md" src={src} aria-label={alt||'فيديو من المقالة'}>
  متصفحك لا يدعم تشغيل هذا الفيديو.
 </video>{alt&&<figcaption className="mt-2 text-center text-sm text-ink/60">{alt}</figcaption>}</figure>;
 if(kind==='audio')return <figure className="my-6"><audio controls preload="none" className="w-full" src={src} aria-label={alt||'مقطع صوتي'}/>{alt&&<figcaption className="text-center text-sm text-ink/60">{alt}</figcaption>}</figure>;
 if(kind==='document')return <a href={src} target="_blank" rel="noopener noreferrer" className="inline-flex p-3 rounded-xl border border-teal/40 text-teal">📎 {alt||'فتح المرفق PDF'}</a>;
 if(kind!=='image')return null;
 return <figure className="my-7"><img loading="lazy" src={src} alt={alt} className="w-full h-auto rounded-2xl shadow-sm" {...props}/>{alt&&<figcaption className="mt-2 text-center text-sm text-ink/60">{alt}</figcaption>}</figure>;
}
export function AuthorBadge({name,image,compact=false}){
 const label=String(name||'كاتب المقالة');
 return <div className="flex items-center gap-3 min-w-0">
  <div className={(compact?'w-9 h-9':'w-12 h-12')+' rounded-full shrink-0 overflow-hidden border-2 border-gold bg-teal/10 flex items-center justify-center text-teal font-bold'} aria-hidden="true">
   {image?<img src={image} alt="" loading="lazy" className="w-full h-full object-cover"/>:<span>{label.trim().charAt(0)||'ك'}</span>}
  </div>
  <div className="min-w-0"><p className="text-[11px] text-ink/50">بقلم</p><p className={(compact?'text-sm':'text-lg')+' font-bold text-ink truncate'}>{label}</p></div>
 </div>;
}

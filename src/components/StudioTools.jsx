import {useEffect, useState} from 'react';
import {api} from '../lib/api';
import {mediaKind, mediaMarkdown} from '../lib/media';
import {MediaUploader} from './MediaUploader';

export function StudioTools({form, update, editingId, insert, restore}) {
  const [authors, setAuthors] = useState([]);
  const [profile, setProfile] = useState(null);
  const [assets, setAssets] = useState([]);
  const [pendingDeletes,setPendingDeletes]=useState([]);
  const [cursor,setCursor] = useState(null);
  const [search,setSearch] = useState('');
  const [revisions,setRevisions] = useState([]);
  const [message,setMessage] = useState('');
  const [pending,setPending] = useState(false);
  useEffect(() => {let active=true;api('/api/v1/authors').then(r=>active && setAuthors(r.authors)).catch(e=>active&&setMessage(e.message));return()=>{active=false};},[]);
  useEffect(() => {setRevisions([]);},[editingId]);
  function choose(a) {
    update({authorId:a.id, author:a.name, authorImage:a.image});
  }
  async function saveProfile() {
    setPending(true);
    try {
      const {author} = await api('/api/v1/authors',{method:'POST',body:JSON.stringify(profile)});
      setAuthors(old=>[...old.filter(a=>a.id!==author.id),author]);choose(author);setProfile(null);setMessage('حُفظ ملف الكاتب.');
    } catch(e) {setMessage(e.message);}
    finally {setPending(false);}
  }
  async function loadAssets(next) {
    setPending(true);
    try {
      const page=await api('/api/v1/assets'+(next?'?cursor='+encodeURIComponent(next):''));
      setAssets(old=>next?[...old,...page.items]:page.items);setCursor(page.cursor);setPendingDeletes(page.pendingDeletions||[]);
      if(!page.items.length)setMessage('مكتبة الوسائط فارغة.');
    } catch(e){setMessage(e.message);} finally{setPending(false);}
  }
  async function deleteAsset(url) {
    if(!window.confirm('حذف هذا الملف من المخزن؟ سيُرفض الحذف إذا كان مستخدمًا في المقالات أو ملفات الكتّاب أو النسخ السابقة.'))return;
    setPending(true);setMessage('');
    try {
      await api('/api/v1/media-delete',{method:'POST',body:JSON.stringify({url,confirm:true})});
      setAssets(old=>old.filter(a=>a.url!==url));setPendingDeletes(old=>old.filter(a=>a.url!==url));setMessage('حُذف الملف من المكتبة.');
    } catch(e) {setMessage(e.message+(e.details?.references?.length?' '+e.details.references.map(r=>r.title||r.id).join('، '):''));}
    finally {setPending(false);}
  }
  return <details className="studio-tools"><summary>الكتّاب ومكتبة الوسائط وسجل النسخ</summary>
    <label>ملف الكاتب <select value={form.authorId || ''} onChange={e=>{const a=authors.find(a=>a.id===e.target.value);if(a)choose(a);else update({authorId:''});}}>
      <option value="">اسم مكتوب في المقالة</option>{authors.map(a=><option key={a.id} value={a.id}>{a.name}</option>)}</select></label>
    <button onClick={()=>setProfile({name:'',bio:'',image:'',link:''})}>كاتب جديد</button>
    {authors.some(a=>a.id===form.authorId)&&<button onClick={()=>setProfile(authors.find(a=>a.id===form.authorId))}>تعديل ملف الكاتب</button>}
    {profile && <fieldset><legend>بيانات الكاتب</legend>{[['name','الاسم'],['bio','النبذة'],['link','الرابط الشخصي'],['image','رابط الصورة']].map(([key,label])=><label key={key}>{label}
      <input value={profile[key]||''} onChange={e=>setProfile({...profile,[key]:e.target.value})}/></label>)}
      <MediaUploader purpose="author" onUploaded={url=>setProfile(p=>({...p,image:url}))}/>
      <button disabled={pending} onClick={saveProfile}>حفظ الملف</button><button onClick={()=>setProfile(null)}>إلغاء</button></fieldset>}
    <button disabled={pending} onClick={()=>loadAssets(null)}>عرض مكتبة الوسائط</button>
    {assets.length>0&&<><label>بحث في الملفات المعروضة<input value={search} onChange={e=>setSearch(e.target.value)}/></label>
      <ul className="asset-list">{assets.filter(x=>x.pathname.toLowerCase().includes(search.toLowerCase())).map(a=><li key={a.url}>
        {mediaKind(a.url)==='image'&&<img src={a.url} loading="lazy" alt="" width="64" height="48"/>}
        <a href={a.url} target="_blank" rel="noreferrer">{a.pathname.split('/').pop()}</a><small>{Math.ceil(a.size/1024)} KB</small>
        <button onClick={()=>insert(mediaMarkdown(a.url,a.pathname.split('/').pop()))}>إدراج في النص</button>
        {mediaKind(a.url)==='image'&&<button onClick={()=>update({coverImage:a.url})}>اختيار للغلاف</button>}<button disabled={pending} onClick={()=>deleteAsset(a.url)}>حذف من المخزن</button></li>)}</ul></>}
    {pendingDeletes.map(item=><div key={item.url} className="my-2"><span>عملية حذف تحتاج إعادة المحاولة · {item.url.split('/').pop()}</span><button disabled={pending} onClick={()=>deleteAsset(item.url)}>إكمال الحذف</button></div>)}
    {cursor&&<button disabled={pending} onClick={()=>loadAssets(cursor)}>المزيد من الملفات</button>}
    {editingId&&<button onClick={async()=>{try{const r=await api('/api/v1/revisions?id='+encodeURIComponent(editingId));setRevisions(r.revisions);setMessage(r.revisions.length?'':'لا توجد نسخ أقدم.');}catch(e){setMessage(e.message);}}}>عرض نسخ المقالة السابقة</button>}
    {revisions.map(r=><button key={r.id} onClick={()=>restore(r.data)}>استعادة إلى المحرر · {new Date(r.savedAt).toLocaleString('ar')}</button>)}
    {message&&<p role="status">{message}</p>}
  </details>;
}

import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { BookOpen, Edit3, Eye, Feather, PlusCircle, Quote, Save, Send, Settings, Trash2, Upload, LogOut, Link2, Copy, List, Italic, Image, Heading2 } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useArticles } from '../hooks/useArticles';
import { ARTICLE_CATEGORIES, getCategory } from '../data/categories';
import { AUTHOR, DEFAULT_IMAGE, slugify, articlePath } from '../lib/format';

function blankArticle() {
  return { title:'', slug:'', category:'general', excerpt:'', content:'', coverImage:DEFAULT_IMAGE,
    readTime:'5 دقائق', featured:false, tags:'الأدب العربي, ثقافة', fontFamily:'amiri', fontSize:22 };
}
function formFrom(article) {
  return { title: article.title || '', slug:article.slug || '', category:article.category || 'general',
    excerpt:article.excerpt || '', content:article.content || '', coverImage:article.coverImage || DEFAULT_IMAGE,
    readTime:article.readTime || '5 دقائق', featured:Boolean(article.featured), tags:(article.tags || []).join(', '),
    fontFamily:article.fontFamily || 'amiri', fontSize:article.fontSize || 22 };
}
function localeToday() {
  // local date, avoiding a UTC day rollover when editing in Yemen
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-${String(now.getDate()).padStart(2,'0')}`;
}

export function DashboardPage() {
  const { articles, save, remove, isDemo, importExamples, site, quotes, saveEditorial } = useArticles();
  const { logout, user } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(blankArticle);
  const [filter, setFilter] = useState('all');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [confirmDeleteId, setConfirmDeleteId] = useState(null);
  const [preview, setPreview] = useState(false);
  const [editorialOpen,setEditorialOpen] = useState(false);
  const [editorialForm,setEditorialForm] = useState(null);
  const openEditorial=()=>{setEditorialForm({tagline:site.tagline||'',about:site.about||'',
    quotes:quotes.map(q=>({text:q.text||'',author:q.author||'',source:q.source||''}))});setEditorialOpen(true);};
  const changeQuote=(i,key,value)=>setEditorialForm(f=>({...f,quotes:f.quotes.map((q,j)=>j===i?{...q,[key]:value}:q)}));
  const commitEditorial=async()=>{
   setBusy(true);setError('');setSuccess('');
   try{await saveEditorial(editorialForm.tagline,editorialForm.about,editorialForm.quotes);
    setEditorialOpen(false);setSuccess('حُفظت هوية المدونة واقتباساتها في Neon.');}
   catch(e){setError(e.message||'تعذّر حفظ إعدادات المدونة.');}finally{setBusy(false);}
  };
  const editorRef = useRef(null);

  useEffect(() => { document.title = 'لوحة الكاتب | مدونة معتز العلقمي'; }, []);
  useEffect(() => {
    const id = searchParams.get('edit');
    if (!id) return;
    const article = articles.find(a => a.id === id);
    if (article) { setEditingId(id); setForm(formFrom(article)); setError(''); }
  }, [searchParams, articles]);

  const filtered = useMemo(() => articles.filter(a => filter === 'all' || (filter === 'draft' ? a.status === 'draft' : a.status !== 'draft')), [articles, filter]);
  const change = (name, value) => setForm(f => ({...f, [name]:value}));
  const clear = () => { setEditingId(null); setForm(blankArticle()); setError(''); setSuccess(''); if (searchParams.has('edit')) setSearchParams({}, {replace:true}); };
  const edit = article => { setEditingId(article.id); setForm(formFrom(article)); setError(''); setSuccess(''); window.scrollTo({top:0,behavior:'smooth'}); };
  const format = (before, after='') => {
    const target = editorRef.current;
    if (!target) return;
    const start = target.selectionStart, end = target.selectionEnd;
    const selected = form.content.slice(start,end) || 'النص المختار';
    change('content', form.content.slice(0,start)+before+selected+after+form.content.slice(end));
    requestAnimationFrame(() => { target.focus(); target.setSelectionRange(start+before.length, start+before.length+selected.length); });
  };
  async function submit(status) {
    setError(''); setSuccess('');
    const title = form.title.trim(), content = form.content.trim();
    if (!title || !content) { setError('عنوان المقالة ومتنها مطلوبان.'); return; }
    if (title.length > 220) { setError('عنوان المقالة طويل جدًا.'); return; }
    const slug = slugify(form.slug || title);
    if (articles.some(a => a.slug === slug && a.id !== editingId)) { setError('الرابط الوصفي مستخدم في مقالة أخرى؛ غيّره قبل الحفظ.'); return; }
    const old = articles.find(a => a.id === editingId);
    const item = { ...(old || {}), ...(editingId ? { id: editingId } : {}), title, slug,
      category: form.category, excerpt: form.excerpt.trim() || content.replace(/[#>*_]/g,'').slice(0,160)+'…',
      content, coverImage: form.coverImage.trim() || DEFAULT_IMAGE, author: old?.author || AUTHOR,
      publishDate: old?.publishDate || localeToday(), readTime:form.readTime.trim() || '5 دقائق',
      featured:form.featured, fontFamily:form.fontFamily, fontSize:Number(form.fontSize), status, tags:form.tags.split(/[,،]/).map(t=>t.trim()).filter(Boolean),
      views:old?.views || 0, likes:old?.likes || 0 };
    setBusy(true);
    try { await save(item); clear(); setSuccess(status === 'draft' ? 'حُفظت المسودة في قاعدة البيانات.' : 'نُشرت المقالة بنجاح.'); }
    catch (e) { setError(e.message || 'تعذر حفظ المقالة.'); }
    finally { setBusy(false); }
  }
  async function handleDelete(id) {
    setBusy(true); setError(''); setSuccess('');
    try { await remove(id); if (editingId === id) clear(); setConfirmDeleteId(null); setSuccess('حُذفت المقالة.'); }
    catch(e) { setError(e.message || 'فشل الحذف.'); }
    finally { setBusy(false); }
  }
  async function handleImport() {
    setBusy(true); setError(''); setSuccess('');
    try { await importExamples(); setSuccess('تم استيراد المقالات السبعة إلى Neon.'); }
    catch(e) { setError(e.message || 'تعذر استيراد المقالات.'); }
    finally { setBusy(false); }
  }

  return <div className="max-w-7xl mx-auto px-4 sm:px-6 py-8">
    <header className="rounded-3xl bg-gradient-to-l from-ink via-[#2C2523] to-teal text-white p-7 sm:p-9 shadow-xl flex flex-wrap items-center justify-between gap-4">
      <div><p className="text-gold text-xs font-bold flex gap-2 items-center"><Settings size={17}/> نظام إدارة المحتوى الآمن</p><h1 className="font-literature text-3xl sm:text-4xl font-bold mt-2">لوحة الكاتب</h1><p className="text-white/70 text-sm mt-2">مرحبًا، {user?.email || 'أيها الكاتب'} — تحرير المقالات والمسودات ونشرها</p></div>
      <div className="flex flex-wrap gap-2"><button onClick={clear} className="bg-gold text-ink px-4 py-3 rounded-xl font-bold text-sm flex items-center gap-2"><PlusCircle size={17}/> مقالة جديدة</button><Link to="/" className="bg-white/15 px-4 py-3 rounded-xl font-bold text-sm flex items-center gap-2"><Eye size={17}/> معاينة</Link><button onClick={openEditorial} className="bg-white/15 px-4 py-3 rounded-xl font-bold text-sm flex items-center gap-2"><Settings size={17}/> إدارة المدونة</button><button onClick={logout} className="border border-white/40 px-4 py-3 rounded-xl font-bold text-sm flex items-center gap-2"><LogOut size={17}/> خروج</button></div>
    </header>
    {isDemo && <div className="mt-6 flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-[#F4ECE1] border border-gold/40 p-5"><p className="text-sm">المقالات الحالية أمثلة تجريبية. استوردها إلى Neon إن رغبت في إدارتها، أو ابدأ بكتابة مقالة جديدة.</p><button disabled={busy} onClick={handleImport} className="bg-teal text-white rounded-xl px-5 py-2.5 font-bold text-sm inline-flex items-center gap-2"><Upload size={17}/> استيراد الأمثلة السبعة</button></div>}
    {error && <p role="alert" className="mt-5 rounded-xl bg-red-50 text-red-800 border border-red-200 p-4">{error}</p>}
    {success && <p role="status" className="mt-5 rounded-xl bg-cream text-teal border border-gold/30 p-4">{success}</p>}
    {editorialOpen && editorialForm && <section className="mt-6 rounded-3xl bg-white border border-gold/40 p-6 sm:p-8 shadow-soft">
      <div className="flex justify-between items-center gap-3"><h2 className="font-literature text-2xl font-bold text-teal">إدارة هوية المدونة والاقتباسات</h2><button type="button" className="text-rust underline text-sm" onClick={()=>setEditorialOpen(false)}>إغلاق</button></div>
      <div className="grid gap-4 mt-5">
        <label className="text-sm font-bold">العبارة التعريفية للمدونة
          <input value={editorialForm.tagline} maxLength={240} onChange={e=>setEditorialForm(f=>({...f,tagline:e.target.value}))} className="w-full block mt-2 p-3 rounded-xl bg-cream border border-gold/40"/>
        </label>
        <label className="text-sm font-bold">نبذة «حول المدونة»
          <textarea rows={5} value={editorialForm.about} maxLength={12000} onChange={e=>setEditorialForm(f=>({...f,about:e.target.value}))} className="w-full block mt-2 p-3 rounded-xl bg-cream border border-gold/40 leading-loose"/>
        </label>
        <div className="flex items-center justify-between"><h3 className="font-bold text-teal">الاقتباسات ({editorialForm.quotes.length})</h3><button type="button" className="bg-teal text-white px-3 py-2 rounded-xl text-sm" onClick={()=>setEditorialForm(f=>({...f,quotes:[...f.quotes,{text:'',author:'',source:''}]}))}>إضافة اقتباس</button></div>
        <div className="grid md:grid-cols-2 gap-4">{editorialForm.quotes.map((q,i)=><div key={i} className="rounded-xl bg-cream p-4 border border-gold/30 space-y-3">
          <label className="block text-xs font-bold">نص الاقتباس<textarea rows={3} maxLength={1200} value={q.text} onChange={e=>changeQuote(i,'text',e.target.value)} className="w-full mt-1 p-2 rounded-lg border bg-white"/></label>
          <label className="block text-xs font-bold">القائل<input maxLength={160} value={q.author} onChange={e=>changeQuote(i,'author',e.target.value)} className="w-full mt-1 p-2 rounded-lg border bg-white"/></label>
          <label className="block text-xs font-bold">المصدر<input maxLength={160} value={q.source} onChange={e=>changeQuote(i,'source',e.target.value)} className="w-full mt-1 p-2 rounded-lg border bg-white"/></label>
          <button type="button" className="text-red-700 text-xs font-bold" onClick={()=>setEditorialForm(f=>({...f,quotes:f.quotes.filter((_,j)=>j!==i)}))}>إزالة الاقتباس من القائمة</button>
        </div>)}</div>
        <div><button disabled={busy} onClick={commitEditorial} className="inline-flex items-center gap-2 bg-teal text-white px-6 py-3 rounded-xl font-bold"><Save size={17}/> حفظ إعدادات المدونة</button></div>
      </div>
    </section>}
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 mt-8 items-start">
      <section className="lg:col-span-7 rounded-3xl bg-white p-6 sm:p-8 border border-gold/40 shadow-soft" aria-labelledby="editor-title">
        <h2 id="editor-title" className="font-literature text-2xl font-bold text-teal flex items-center gap-2 mb-6"><Edit3 size={21} className="text-gold"/>{editingId ? 'تعديل المقالة' : 'كتابة مقالة جديدة'}</h2>
        <div className="space-y-5">
          <label className="block text-sm font-bold">عنوان المقالة *<input value={form.title} onChange={e=>change('title',e.target.value)} maxLength={220} placeholder="مثال: جماليات الاستعارة في شعر أبي تمام" className="block mt-2 w-full border border-gold/40 rounded-xl p-3 bg-cream"/></label>
          <div className="grid sm:grid-cols-3 gap-4">
            <label className="text-sm font-bold">القسم الثقافي<select value={form.category} onChange={e=>change('category',e.target.value)} className="block mt-2 w-full border border-gold/40 rounded-xl p-3 bg-cream">{ARTICLE_CATEGORIES.map(c=><option key={c.id} value={c.id}>{c.title}</option>)}</select></label>
            <label className="text-sm font-bold">زمن القراءة<input value={form.readTime} onChange={e=>change('readTime',e.target.value)} className="block mt-2 w-full border border-gold/40 rounded-xl p-3 bg-cream"/></label>
            <label className="text-sm font-bold">الرابط الوصفي<input dir="ltr" value={form.slug} onChange={e=>change('slug',e.target.value)} placeholder="my-article" className="block mt-2 w-full border border-gold/40 rounded-xl p-3 bg-cream text-left"/></label>
          </div>
          <label className="block text-sm font-bold">رابط صورة الغلاف<input type="url" dir="ltr" value={form.coverImage} onChange={e=>change('coverImage',e.target.value)} className="block mt-2 w-full border border-gold/40 rounded-xl p-3 bg-cream text-left text-sm"/></label>
          {form.coverImage && <img loading="lazy" src={form.coverImage} alt="معاينة غلاف المقالة" className="h-36 w-full object-cover rounded-xl border border-gold/30"/>}
          <label className="block text-sm font-bold">المقتطف التعريفي<textarea rows={3} value={form.excerpt} onChange={e=>change('excerpt',e.target.value)} placeholder="نبذة مختصرة عن المقالة" className="block mt-2 w-full border border-gold/40 rounded-xl p-3 bg-cream"/></label>
          <div className="rounded-2xl border border-gold/30 bg-cream/70 p-4 space-y-3">
            <div className="flex flex-wrap gap-4 items-center justify-between">
              <div><p className="font-bold text-teal">استوديو تحرير النصوص العربية</p><p className="text-xs text-ink/60">معاينة فورية للخط والتنسيق كما سيظهر للقارئ</p></div>
              <button type="button" onClick={()=>setPreview(v=>!v)} className="rounded-xl bg-teal text-white px-4 py-2 text-sm font-bold flex gap-2 items-center"><Eye size={16}/>{preview?'العودة للكتابة':'معاينة المقالة'}</button>
            </div>
            <div className="grid sm:grid-cols-2 gap-4">
              <label className="text-sm font-bold">نوع الخط
                <select value={form.fontFamily} onChange={e=>change('fontFamily',e.target.value)} className="block w-full mt-1.5 border border-gold/40 bg-white rounded-xl p-2.5">
                  <option value="amiri">أميري — أدبي كلاسيكي</option><option value="naskh">نسخ — رسمي وقابل للقراءة</option><option value="tajawal">تجوال — حديث ومتزن</option>
                </select>
              </label>
              <label className="text-sm font-bold">حجم خط المقالة: {form.fontSize} بكسل
                <input type="range" min="16" max="32" step="1" value={form.fontSize} onChange={e=>change('fontSize',Number(e.target.value))} className="block w-full mt-3 accent-[#0D5C63]" />
              </label>
            </div>
            <p className="text-xs text-ink/60">الكلمات: {form.content.trim()?form.content.trim().split(/\s+/).length:0} · الحروف: {form.content.length}</p>
          </div>
          <div><div className="flex flex-wrap items-center justify-between gap-2 mb-2"><label htmlFor="article-content" className="text-sm font-bold">متن المقالة الكامل *</label><div className="flex gap-1 flex-wrap"><button onClick={()=>format('\n\n### ','\n\n')} type="button" className="bg-cream border border-gold/30 rounded-lg px-2 py-1 text-xs font-bold text-teal">عنوان فرعي</button><button onClick={()=>format('\n\n> ','\n\n')} type="button" className="bg-cream border border-gold/30 rounded-lg px-2 py-1 text-xs font-bold text-rust flex items-center gap-1"><Quote size={13}/> اقتباس / شعر</button><button onClick={()=>format('**','**')} type="button" className="bg-cream border border-gold/30 rounded-lg px-2 py-1 text-xs font-bold">عريض</button><button onClick={()=>format('«','»')} type="button" className="bg-cream border border-gold/30 rounded-lg px-2 py-1 text-xs font-bold">« »</button>
            <button onClick={()=>format('## ')} type="button" className="bg-cream border border-gold/30 rounded-lg px-2 py-1 text-xs font-bold flex gap-1 items-center"><Heading2 size={13}/> عنوان كبير</button>
            <button onClick={()=>format('*','*')} type="button" className="bg-cream border border-gold/30 rounded-lg px-2 py-1 text-xs font-bold flex gap-1 items-center"><Italic size={13}/> مائل</button>
            <button onClick={()=>format('\n- ')} type="button" className="bg-cream border border-gold/30 rounded-lg px-2 py-1 text-xs font-bold flex gap-1 items-center"><List size={13}/> قائمة</button>
            <button onClick={()=>format('[','](https://example.com)')} type="button" className="bg-cream border border-gold/30 rounded-lg px-2 py-1 text-xs font-bold flex gap-1 items-center"><Link2 size={13}/> رابط</button>
            <button onClick={()=>format('![وصف الصورة](',')')} type="button" className="bg-cream border border-gold/30 rounded-lg px-2 py-1 text-xs font-bold flex gap-1 items-center"><Image size={13}/> صورة</button></div></div><textarea id="article-content" ref={editorRef} rows={15} value={form.content} onChange={e=>change('content',e.target.value)} placeholder="اكتب المقالة هنا. يدعم المحرر Markdown للعناوين والاقتباسات والنصوص المنسقة…" className={`w-full border border-gold/40 rounded-2xl p-4 bg-cream leading-loose ${preview?'hidden':''}`}/>{preview && <div aria-label="معاينة المقال" className="article-prose max-h-[640px] overflow-y-auto rounded-2xl border border-gold/30 bg-white p-6" style={{fontFamily:form.fontFamily==='tajawal'?"'Tajawal',sans-serif":form.fontFamily==='naskh'?"'Noto Naskh Arabic','Amiri',serif":"'Amiri',serif",fontSize:`${form.fontSize}px`}}><ReactMarkdown remarkPlugins={[remarkGfm]} skipHtml>{form.content || 'اكتب نصًا لمعاينته هنا.'}</ReactMarkdown></div>}</div>
          <div className="grid sm:grid-cols-2 gap-4 items-center"><label className="text-sm font-bold">الوسوم (افصل بفاصلة)<input value={form.tags} onChange={e=>change('tags',e.target.value)} className="block mt-2 w-full border border-gold/40 rounded-xl p-3 bg-cream"/></label><label className="flex gap-2 items-center text-sm font-bold"><input type="checkbox" className="accent-[#0D5C63] h-4 w-4" checked={form.featured} onChange={e=>change('featured',e.target.checked)}/> إظهار في المقالات المميزة</label></div>
          <div className="flex flex-wrap gap-3 border-t border-gold/20 pt-5"><button disabled={busy} onClick={()=>submit('published')} className="inline-flex items-center gap-2 bg-teal text-white px-6 py-3 rounded-xl text-sm font-bold shadow-sm"><Send size={17}/> نشر المقالة</button><button disabled={busy} onClick={()=>submit('draft')} className="inline-flex items-center gap-2 bg-gold/20 text-rust px-5 py-3 rounded-xl text-sm font-bold"><Save size={17}/> حفظ كمسودة</button>{editingId && <button onClick={clear} className="text-ink/65 underline text-sm">إلغاء التعديل</button>}</div>
        </div>
      </section>
      <section className="lg:col-span-5 bg-white rounded-3xl border border-gold/40 p-6 shadow-soft" aria-labelledby="manage-title"><div className="flex flex-wrap justify-between gap-2 items-center mb-5"><h2 id="manage-title" className="font-literature text-2xl font-bold">إدارة المنشورات ({articles.length})</h2><div className="flex gap-1 bg-cream p-1 rounded-xl">{[['all','الكل'],['published','منشور'],['draft','مسودات']].map(([id,title])=><button key={id} onClick={()=>setFilter(id)} className={`text-xs font-bold rounded-lg px-2.5 py-1.5 ${filter===id?'bg-teal text-white':''}`}>{title}</button>)}</div></div><div className="space-y-3 max-h-[750px] overflow-y-auto">{filtered.map(article=><div key={article.id} className={`rounded-2xl p-4 border ${editingId===article.id?'border-teal bg-teal/5':'border-gold/30 bg-cream/50'}`}><div className="flex items-start justify-between gap-2"><div className="min-w-0"><p className="text-xs text-rust font-bold">{getCategory(article.category).title} · {article.status === 'draft' ? 'مسودة' : 'منشور'}{article.featured?' · مميزة':''}</p><h3 className="font-literature font-bold text-lg line-clamp-2 mt-1">{article.title}</h3><p className="text-xs text-ink/55 mt-1">{article.publishDate} · {article.readTime}</p></div><div className="flex gap-1 shrink-0"><Link to={articlePath(article)} title="معاينة" className="p-2 text-teal rounded-lg hover:bg-teal/10"><Eye size={17}/></Link><button type="button" onClick={async()=>{const url=window.location.origin+articlePath(article);try{await navigator.clipboard.writeText(url);setSuccess('نُسخ رابط المقالة: '+url);}catch{setError('تعذر نسخ الرابط.');}}} title="نسخ الرابط المباشر" className="p-2 text-teal rounded-lg hover:bg-teal/10"><Copy size={17}/></button><button onClick={()=>edit(article)} title="تعديل" className="p-2 text-rust rounded-lg hover:bg-gold/20"><Edit3 size={17}/></button><button onClick={()=>setConfirmDeleteId(article.id)} title="حذف" className="p-2 text-red-700 rounded-lg hover:bg-red-50"><Trash2 size={17}/></button></div></div>{confirmDeleteId===article.id&&<div className="mt-3 pt-3 border-t border-red-200 bg-red-50 rounded-xl p-3 text-sm"><p className="text-red-800 font-bold">حذف هذه المقالة نهائيًا؟</p><div className="flex gap-2 mt-2"><button disabled={busy} onClick={()=>handleDelete(article.id)} className="bg-red-700 text-white rounded-lg px-3 py-1.5 text-xs font-bold">نعم، حذف</button><button onClick={()=>setConfirmDeleteId(null)} className="bg-white border rounded-lg px-3 py-1.5 text-xs">إلغاء</button></div></div>}</div>)}{!filtered.length&&<p className="text-center p-8 text-ink/60"><BookOpen className="mx-auto mb-3 text-gold"/>لا توجد منشورات في هذا التصنيف.</p>}</div></section>
    </div>
  </div>;
}

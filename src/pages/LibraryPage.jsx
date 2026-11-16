import {useMemo,useState} from 'react';
import {Link} from 'react-router-dom';
import {Bookmark,ArrowLeft} from 'lucide-react';
import {useArticles} from '../hooks/useArticles';
import {ArticleCard} from '../components/ArticleCard';
import {normalizeArabic} from '../../shared/arabicSearch';

export function LibraryPage(interactions) {
 const {published,loading}=useArticles();
 const [query,setQuery]=useState('');
 const saved=useMemo(()=>published.filter(a=>interactions.bookmarks.includes(a.id)),[published,interactions.bookmarks]);
 const filtered=saved.filter(a=>normalizeArabic(a.title+' '+a.author).includes(normalizeArabic(query)));
 const missing=interactions.bookmarks.filter(id=>!published.some(a=>a.id===id));
 return <section className="edition personal-library">
  <header><Bookmark size={28}/><h1>مكتبتك</h1><p>الكتابات التي اخترت العودة إليها على هذا الجهاز.</p></header>
  <label className="library-search">بحث في المحفوظات<input value={query} onChange={e=>setQuery(e.target.value)} placeholder="عنوان أو اسم كاتب"/></label>
  {loading?<p role="status">جار فتح المكتبة…</p>:<div className="grid md:grid-cols-2 lg:grid-cols-3 gap-7">{filtered.map(a=><ArticleCard key={a.id} article={a} {...interactions}/>)}</div>}
  {!loading&&!filtered.length&&<div className="library-empty"><Bookmark size={36}/><h2>{query?'لا توجد نتائج مطابقة':'ابدأ باختيار نصّ قريب منك'}</h2>
   <p>اضغط علامة الحفظ على المقالة لتجدها هنا.</p><Link to="/">تصفّح الكتابات <ArrowLeft size={16}/></Link></div>}
  {!loading&&missing.length>0&&<p className="my-5 text-sm" role="status">توجد {missing.length} من الإشارات المحفوظة لمقالات لم تعد متاحة للنشر.</p>}
 </section>;
}

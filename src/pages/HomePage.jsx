import {AnnouncementTicker} from '../components/AnnouncementTicker';
import { useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { ArrowLeft, BookOpen, RotateCcw } from 'lucide-react';
import { ARTICLE_CATEGORIES, PRIMARY_TOPICS, getCategory } from '../data/categories';
import { useArticles } from '../hooks/useArticles';
import { ArticleCard } from '../components/ArticleCard';
import { EmptyState } from '../components/Ornaments';
import { articlePath, formatDate } from '../lib/format';
import { normalizeArabic } from '../../shared/arabicSearch';

export function HomePage(interactions) {
  const { published, quotes, loading, error, site, ticker } = useArticles();
  const [params] = useSearchParams();
  const term = params.get('q')?.trim() || '';
  const [category, setCategory] = useState('');
  const [order, setOrder] = useState('recent');
  const [quoteIndex, setQuoteIndex] = useState(0);
  const quote = quotes.length ? quotes[quoteIndex % quotes.length] : null;
  const lead = published.find(a => a.featured) || published[0];
  const articles = useMemo(() => {
    const query = normalizeArabic(term);
    return published.filter(a => (!category || a.category === category) &&
      normalizeArabic([a.title, a.author, a.content, a.excerpt, getCategory(a.category).title, ...(a.tags || [])].join(' ')).includes(query))
      .sort((a, b) => order === 'title' ? a.title.localeCompare(b.title, 'ar') :
        order === 'oldest' ? a.publishDate.localeCompare(b.publishDate) : b.publishDate.localeCompare(a.publishDate));
  }, [published, category, term, order]);
  return <div className="edition">
    {!term && <>
      <header className="masthead"><span>{site?.tagline}</span><h1>مدونة معتز العلقمي</h1><p>أدب وإنسان وثقافة ودين ولغة عربية</p></header>
      <AnnouncementTicker ticker={ticker}/>
      <nav className="edition-topics" aria-label="الأبواب الرئيسية">{PRIMARY_TOPICS.map(({id,title,icon:Icon}) =>
        <Link key={id} to={'/category/'+id}><Icon size={21} aria-hidden="true"/><span>{title}</span></Link>)}</nav>
      {lead && <section className="lead-story">
        <div className="lead-text"><Link className="section-name" to={'/category/' + lead.category}>{getCategory(lead.category).title}</Link>
          <h2><Link to={articlePath(lead)}>{lead.title}</Link></h2>
          <p>{lead.excerpt}</p><div className="byline">{[lead.author, formatDate(lead.publishDate)].filter(Boolean).join('  /  ')}</div>
          <Link className="read-link" to={articlePath(lead)}>قراءة المقالة <ArrowLeft size={18}/></Link>
        </div>{lead.coverImage && <Link className="lead-image" to={articlePath(lead)} aria-label={lead.title}>
          <img alt="" fetchPriority="high" src={lead.coverImage}/></Link>}
      </section>}
    </>}
    <section id="latest" className="catalog">
      <div className="catalog-heading"><h2>{term ? 'نتائج البحث عن «' + term + '»' : 'أحدث الكتابات'}</h2>
        <label>ترتيب <select value={order} onChange={e => setOrder(e.target.value)}>
          <option value="recent">الأحدث أولًا</option><option value="oldest">الأقدم أولًا</option><option value="title">حسب العنوان</option>
        </select></label></div>
      <div className="filters" role="group" aria-label="تصفية حسب القسم">
        {[{id:'', title:'الكل'}, ...ARTICLE_CATEGORIES].map(c => <button key={c.id} aria-pressed={c.id === category} onClick={() => setCategory(c.id)}>{c.title}</button>)}
      </div>
      {error && <p role="alert">{error}</p>}
      {loading ? <p role="status">جار تحميل الكتابات…</p> : articles.length ?
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-7">{articles.map(a => <ArticleCard key={a.id} article={a} {...interactions}/>)}</div> :
        <EmptyState title="لا توجد مقالات مطابقة" description="جرّب اسم الكاتب أو كلمة من النص"/>}
    </section>
    {!term && !category && <section className="reading-shelves">
      {['books','arabic_lit'].map(id=>{
        const chosen=published.filter(a=>a.category===id).slice(0,3);
        return chosen.length>0&&<div className="reading-shelf" key={id}><h2>{getCategory(id).title}</h2>
          {chosen.map(a=><Link key={a.id} to={articlePath(a)}><span>{a.author}</span><h3>{a.title}</h3><ArrowLeft size={18}/></Link>)}
          <Link className="shelf-more" to={'/category/'+id}>جميع الكتابات</Link></div>;
      })}
    </section>}
    {!term && quote && <aside className="quote-panel"><BookOpen aria-hidden="true"/><blockquote>«{quote.text}»</blockquote>
      <p>{[quote.author, quote.source].filter(Boolean).join(' · ')}</p>
      {quotes.length > 1 && <button onClick={() => setQuoteIndex(i => i + 1)}><RotateCcw size={16}/> اقتباس آخر</button>}</aside>}
  </div>;
}

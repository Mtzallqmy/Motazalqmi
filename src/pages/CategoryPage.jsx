import { Link, useParams } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import { CATEGORIES, ARTICLE_CATEGORIES } from '../data/categories';
import { useArticles } from '../hooks/useArticles';
import { ArticleCard } from '../components/ArticleCard';
import { EmptyState } from '../components/Ornaments';
import { HealthDesk } from '../components/HealthDesk';
import { HEALTH_SECTION_ID } from '../../shared/healthDesk.js';
export function CategoryPage(interactions) {
  const { categoryId } = useParams();
  const category = CATEGORIES.find(c => c.id === categoryId && c.id !== 'about');
  const { published, loading, error } = useArticles();
  if (!category) return <div className="max-w-5xl mx-auto p-10"><EmptyState title="القسم غير موجود" description="اختر أحد أقسام المدونة"/><Link className="text-teal" to="/">العودة للرئيسية</Link></div>;
  const Icon = category.icon;
  const articles = published.filter(a => a.category === categoryId);
  if (categoryId === HEALTH_SECTION_ID) return <HealthDesk articles={articles} loading={loading} error={error} interactions={interactions}/>;
  return <div className="max-w-7xl mx-auto px-4 sm:px-6 py-7"><section className={`relative overflow-hidden rounded-3xl bg-gradient-to-l ${category.color} p-8 sm:p-12 text-white shadow-xl`}><div className="absolute inset-0 ornament opacity-50"/><div className="relative"><Link to="/" className="inline-flex gap-2 items-center text-xs rounded-xl bg-white/15 px-3 py-2"><ArrowRight size={15}/> الرئيسية</Link><div className="flex items-center gap-4 mt-5"><span className="rounded-2xl p-4 bg-white/15"><Icon size={28}/></span><div><h1 className="font-literature text-4xl sm:text-5xl font-bold">{category.title}</h1><p className="mt-2 text-white/80">{category.subtitle}</p></div></div><p className="mt-5 text-sm">{articles.length} مادة منشورة</p></div></section><nav aria-label="الأقسام الأخرى" className="flex gap-2 overflow-x-auto py-6">{ARTICLE_CATEGORIES.map(c => <Link key={c.id} to={`/category/${c.id}`} className={`whitespace-nowrap px-4 py-2 rounded-xl text-xs font-bold ${categoryId === c.id ? 'bg-teal text-white' : 'border border-gold/30 bg-white'}`}>{c.title}</Link>)}</nav>{loading ? <p className="p-10 text-center">جار التحميل…</p> : articles.length ? <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-7">{articles.map(article => <ArticleCard key={article.id} article={article} {...interactions}/>)}</div> : <EmptyState title="لم تُنشر مقالات في هذا القسم بعد" description="ستظهر النصوص هنا فور نشرها"/>}</div>;
}

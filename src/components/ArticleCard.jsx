import { Link } from 'react-router-dom';
import { Bookmark, CalendarDays, ChevronLeft, Clock3, Heart, Share2 } from 'lucide-react';
import { getCategory } from '../data/categories';
import { articlePath, formatDate } from '../lib/format';
export function ArticleCard({ article, bookmarks, onBookmark, onLike, liked, onShare, large = false }) {
  return <article className="group flex flex-col overflow-hidden rounded-3xl border border-gold/30 bg-white shadow-sm hover:shadow-xl hover:-translate-y-1 transition-all duration-300">
    <div className={`relative overflow-hidden ${large ? 'h-72 sm:h-80' : 'h-56'}`}>
      <Link to={articlePath(article)} aria-label={`قراءة ${article.title}`} className="block h-full"><img loading="lazy" src={article.coverImage} alt={`غلاف: ${article.title}`} className="h-full w-full object-cover group-hover:scale-105 transition-transform duration-700" onError={e => { e.currentTarget.style.objectFit = 'contain'; }}/></Link>
      <span className="absolute top-4 right-4 rounded-xl bg-cream/95 px-3 py-1 text-xs font-bold text-teal">{getCategory(article.category).title}</span>
      {onBookmark && <button onClick={() => onBookmark(article.id)} aria-label="حفظ المقالة" className={`absolute top-4 left-4 rounded-xl p-2 ${bookmarks.includes(article.id) ? 'bg-gold text-ink' : 'bg-black/40 text-white'}`}><Bookmark size={17}/></button>}
    </div>
    <div className="flex flex-1 flex-col justify-between p-6">
      <div><div className="flex gap-3 text-xs text-ink/60 mb-3"><span className="flex gap-1 items-center"><CalendarDays size={14} className="text-gold"/>{formatDate(article.publishDate)}</span><span className="flex gap-1 items-center"><Clock3 size={14} className="text-gold"/>{article.readTime}</span></div><Link to={articlePath(article)}><h3 className={`font-literature font-bold leading-snug group-hover:text-teal ${large ? 'text-3xl' : 'text-2xl'}`}>{article.title}</h3></Link><p className="mt-3 text-ink/70 leading-relaxed line-clamp-3">{article.excerpt}</p></div>
      <div className="mt-6 border-t border-gold/20 pt-4 flex items-center justify-between gap-3"><div className="flex items-center gap-2"><button onClick={() => onLike?.(article.id)} aria-label="إعجاب" className={`flex items-center gap-1 text-sm ${liked ? 'text-rust font-bold' : 'text-ink/60'}`}><Heart size={17} fill={liked ? 'currentColor' : 'none'}/>{(article.likes || 0) + (liked ? 1 : 0)}</button><button aria-label="مشاركة" onClick={() => onShare?.(article)} className="p-1.5 text-ink/60 hover:text-teal"><Share2 size={16}/></button></div><Link to={articlePath(article)} className="flex gap-1 items-center text-xs font-bold text-teal">مطالعة النص <ChevronLeft size={17}/></Link></div>
    </div>
  </article>;
}

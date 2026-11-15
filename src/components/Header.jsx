import { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { Feather, Menu, Search, X } from 'lucide-react';
import { ARTICLE_CATEGORIES } from '../data/categories';
import { useAuth } from '../context/AuthContext';
import {useArticles} from '../hooks/useArticles';
export function Header() {
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [term, setTerm] = useState('');
  const { isAdmin } = useAuth();
  const {site}=useArticles();
  const itemClass = ({ isActive }) => `rounded-xl px-3 py-2 text-sm font-bold transition-colors ${isActive ? 'bg-teal text-white' : 'hover:bg-gold/20 text-ink'}`;
  const links = <><NavLink to="/" end className={itemClass} onClick={() => setOpen(false)}>الرئيسية</NavLink>{ARTICLE_CATEGORIES.map(c => <NavLink key={c.id} to={`/category/${c.id}`} className={itemClass} onClick={() => setOpen(false)}>{c.title}</NavLink>)}<NavLink to="/about" className={itemClass} onClick={() => setOpen(false)}>حول المدونة</NavLink></>;
  function submit(e) { e.preventDefault(); navigate(`/?q=${encodeURIComponent(term)}`); setSearchOpen(false); setOpen(false); }
  return <header className="sticky top-0 z-40 bg-cream/95 backdrop-blur-xl shadow-sm border-b border-gold/25"><div className="max-w-7xl mx-auto px-4 sm:px-6 py-3 flex items-center justify-between gap-3">
    <Link to="/" className="flex items-center gap-3 shrink-0"><span className="grid place-items-center w-12 h-12 rounded-2xl border-2 border-gold bg-white text-teal font-calligraphy text-3xl font-bold">م</span><span><strong className="block font-calligraphy text-2xl sm:text-3xl">معتز العلقمي</strong><small className="hidden sm:block font-literature text-rust">{site?.tagline||'حيث تلتقي الكلمة بالفكر، ويزهر الأدب'}</small></span></Link>
    <nav aria-label="التنقل الرئيسي" className="hidden xl:flex gap-1 items-center flex-wrap justify-center">{links}</nav>
    <div className="flex gap-2 items-center shrink-0"><button onClick={() => setSearchOpen(v => !v)} className="p-2.5 border border-gold/40 rounded-xl bg-white text-teal" aria-label="فتح البحث"><Search size={21}/></button><Link aria-label="لوحة الكاتب" className="flex items-center gap-2 rounded-xl bg-gradient-to-l from-gold to-[#B3863E] px-3 py-2.5 text-xs sm:text-sm font-bold" to={isAdmin ? '/admin' : '/admin/login'}><Feather size={17}/><span className="hidden sm:inline">لوحة الكاتب</span></Link><button onClick={() => setOpen(v => !v)} aria-expanded={open} aria-label="القائمة" className="xl:hidden p-2.5 border border-gold/40 rounded-xl bg-white">{open ? <X size={21}/> : <Menu size={21}/>}</button></div>
  </div>
  {searchOpen && <form onSubmit={submit} className="max-w-7xl mx-auto px-4 pb-4 flex gap-2"><input aria-label="ابحث في المدونة" autoFocus value={term} onChange={e => setTerm(e.target.value)} placeholder="ابحث في المقالات، الشعر، الكتب أو الوسوم…" className="w-full rounded-xl border border-gold/40 px-4 py-3 bg-white"/><button type="submit" className="bg-teal text-white rounded-xl px-5 font-bold">بحث</button></form>}
  {open && <nav aria-label="قائمة الهاتف" className="xl:hidden max-w-7xl mx-auto px-4 pb-4 grid grid-cols-2 gap-1 bg-cream">{links}</nav>}
  </header>;
}

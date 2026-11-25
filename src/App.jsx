import {publicationUrl} from './lib/format';
import {LibraryPage} from './pages/LibraryPage';
import {AuthorPage} from './pages/AuthorPage';
import { lazy, Suspense, useEffect, useState } from 'react';
import { BrowserRouter, Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ArticlesProvider } from './hooks/useArticles';
import { useBookmarks } from './hooks/useBookmarks';
import { Header } from './components/Header';
import { Footer } from './components/Footer';
import { HomePage } from './pages/HomePage';
import { CategoryPage } from './pages/CategoryPage';
const ArticlePage = lazy(() => import('./pages/ArticlePage').then(module => ({ default: module.ArticlePage })));
import { AboutPage } from './pages/AboutPage';
import { LoginPage } from './pages/LoginPage';
const DashboardPage = lazy(() => import('./pages/DashboardPage').then(module => ({ default: module.DashboardPage })));
import { NotFoundPage } from './pages/NotFoundPage';
function Protected({ children }) { const { isAdmin, loading } = useAuth(); if (loading) return <p className="text-center p-16">جار التحقق من الصلاحيات…</p>; return isAdmin ? children : <Navigate to="/admin/login" replace/>; }
function Layout() {
  const loc = useLocation();
  const bookmarks = useBookmarks();
  const [likes, setLikes] = useState(() => { try { return JSON.parse(localStorage.getItem('moataz-blog-likes-v1') || '[]'); } catch { return []; } });
  const [notice, setNotice] = useState('');
  useEffect(() => { window.scrollTo({top: 0}); setNotice(''); }, [loc.pathname]);
  useEffect(() => { try { localStorage.setItem('moataz-blog-likes-v1', JSON.stringify(likes)); } catch { /* ignore */ } }, [likes]);
  const onLike = id => { if (likes.includes(id)) { setNotice('سبق أن أبديت إعجابك بهذا النص'); return; } setLikes(old => [...old, id]); setNotice('شكراً لإعجابك بالمقالة'); };
  const onBookmark = id => { bookmarks.toggle(id); setNotice(bookmarks.ids.includes(id) ? 'تمت إزالة المقالة من محفوظاتك' : 'حُفظت إشارة المقالة على هذا الجهاز'); };
  const onShare = async article => { const url = `${publicationUrl(article)}`; try { if (navigator.share) await navigator.share({title: article.title, url}); else { await navigator.clipboard.writeText(url); setNotice('تم نسخ رابط المقالة'); } } catch { setNotice('تعذر مشاركة الرابط'); } };
  const interactions = {bookmarks: bookmarks.ids, onBookmark, likes, onLike, onShare};
  return <div dir="rtl" className="min-h-screen flex flex-col font-body bg-cream text-ink"><Header/><main key={loc.pathname} className="paper flex-1"><Suspense fallback={<p role="status" className="p-12 text-center">جار فتح الصفحة…</p>}><Routes><Route path="/" element={<HomePage {...interactions}/>} /><Route path="/category/:categoryId" element={<CategoryPage {...interactions}/>} /><Route path="/articles/:slug" element={<ArticlePage {...interactions}/>} /><Route path="/authors/:key" element={<AuthorPage/>}/><Route path="/library" element={<LibraryPage {...interactions}/>}/><Route path="/about" element={<AboutPage/>}/><Route path="/admin/login" element={<LoginPage/>}/><Route path="/admin" element={<Protected><DashboardPage/></Protected>}/><Route path="*" element={<NotFoundPage/>}/></Routes></Suspense></main><Footer/>{notice && <div role="status" className="fixed bottom-5 left-5 z-50 rounded-2xl bg-ink text-cream border border-gold/50 shadow-2xl p-4">{notice}</div>}</div>;
}
export default function App() { return <BrowserRouter><AuthProvider><ArticlesProvider><Layout/></ArticlesProvider></AuthProvider></BrowserRouter>; }

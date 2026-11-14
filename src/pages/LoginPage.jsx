import { useEffect, useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { Feather, LockKeyhole, LogIn, Mail } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export function LoginPage() {
  const { login, user, isAdmin, loading, logout } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [remember, setRemember] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  useEffect(() => { document.title = 'دخول الكاتب | مدونة معتز العلقمي'; }, []);
  if (isAdmin) return <Navigate to="/admin" replace />;

  async function submit(event) {
    event.preventDefault();
    setBusy(true); setError('');
    try { await login(email.trim(), password, remember); }
    catch (e) { setError(e.message || 'تعذر تسجيل الدخول.'); }
    finally { setBusy(false); }
  }

  return <section className="max-w-lg mx-auto px-4 sm:px-6 py-14">
    <div className="rounded-3xl overflow-hidden bg-white shadow-xl border border-gold/40">
      <div className="p-8 bg-gradient-to-l from-[#093A3E] via-teal to-ink text-white relative">
        <div className="absolute inset-0 ornament opacity-40" />
        <div className="relative"><Feather className="text-gold mb-4" size={34}/><h1 className="font-literature font-bold text-4xl">دخول الكاتب</h1><p className="mt-2 text-white/75 text-sm">هذه اللوحة مخصّصة لمالك المدونة والمحررين المخوّلين فقط.</p></div>
      </div>
      <form onSubmit={submit} className="p-7 sm:p-9 space-y-5">
        <label className="block text-sm font-bold"><span className="flex gap-2 items-center mb-2"><Mail size={16} className="text-gold"/> البريد الإلكتروني</span><input type="email" required autoComplete="username" dir="ltr" value={email} onChange={e=>setEmail(e.target.value)} placeholder="author@example.com" className="w-full rounded-xl border border-gold/40 px-4 py-3 outline-offset-2 bg-cream text-left"/></label>
        <label className="block text-sm font-bold"><span className="flex gap-2 items-center mb-2"><LockKeyhole size={16} className="text-gold"/> كلمة المرور</span><input type="password" required autoComplete="current-password" value={password} onChange={e=>setPassword(e.target.value)} className="w-full rounded-xl border border-gold/40 px-4 py-3 bg-cream text-left" dir="ltr"/></label>
        <label className="flex items-center gap-3 text-sm text-ink font-bold"><input type="checkbox" checked={remember} onChange={e=>setRemember(e.target.checked)} className="w-5 h-5 accent-[#0D5C63]"/> تذكّر دخولي على هذا الجهاز لمدة 30 يومًا</label>
        {error && <p role="alert" className="rounded-xl border border-red-300 bg-red-50 text-red-800 p-3 text-sm">{error}</p>}
        <button type="submit" disabled={busy || loading} className="w-full rounded-xl bg-teal text-white px-5 py-3 font-bold flex justify-center gap-2 items-center"><LogIn size={18}/>{busy || loading ? 'جار التحقق…' : 'دخول لوحة الكاتب'}</button>
        <Link to="/" className="block text-center text-sm font-bold text-teal underline">العودة إلى المدونة</Link>
      </form>
    </div>
  </section>;
}

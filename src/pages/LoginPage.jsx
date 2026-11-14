import { useEffect, useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { Feather, LockKeyhole, LogIn, Mail } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { isCloudConfigured } from '../lib/firebase';

export function LoginPage() {
  const { login, user, isAdmin, loading, logout } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  useEffect(() => { document.title = 'دخول الكاتب | مدونة معتز العلقمي'; }, []);
  if (isAdmin) return <Navigate to="/admin" replace />;

  async function submit(event) {
    event.preventDefault();
    setBusy(true); setError('');
    try { await login(email.trim(), password); navigate('/admin', { replace: true }); }
    catch { setError('تعذر تسجيل الدخول. تحقق من بيانات الحساب وتفعيل Firebase وصلاحية الإدارة.'); }
    finally { setBusy(false); }
  }

  return <section className="max-w-lg mx-auto px-4 sm:px-6 py-14">
    <div className="rounded-3xl overflow-hidden bg-white shadow-xl border border-gold/40">
      <div className="p-8 bg-gradient-to-l from-[#093A3E] via-teal to-ink text-white relative">
        <div className="absolute inset-0 ornament opacity-40" />
        <div className="relative"><Feather className="text-gold mb-4" size={34}/><h1 className="font-literature font-bold text-4xl">دخول الكاتب</h1><p className="mt-2 text-white/75 text-sm">هذه اللوحة مخصّصة لمالك المدونة والمحررين المخوّلين فقط.</p></div>
      </div>
      <form onSubmit={submit} className="p-7 sm:p-9 space-y-5">
        {!isCloudConfigured && <p role="alert" className="text-rust text-sm rounded-xl bg-rust/10 p-3">لم تُضبط إعدادات Firebase. يمكنك تصفح المدونة التجريبية، لكن النشر يتطلب ربط قاعدة البيانات.</p>}
        {user && !loading && !isAdmin && <div className="text-sm p-4 rounded-xl border border-gold/40 bg-cream"><p>تم تسجيل دخولك، لكن الحساب لا يحمل صلاحية مدير. أضف الوثيقة <code dir="ltr">admins/{user.uid}</code> بقيمة <code dir="ltr">enabled: true</code> من Firebase Console.</p><button className="underline text-teal font-bold mt-2" type="button" onClick={logout}>تسجيل الخروج وتجربة حساب آخر</button></div>}
        <label className="block text-sm font-bold"><span className="flex gap-2 items-center mb-2"><Mail size={16} className="text-gold"/> البريد الإلكتروني</span><input type="email" required autoComplete="username" dir="ltr" value={email} onChange={e=>setEmail(e.target.value)} placeholder="author@example.com" className="w-full rounded-xl border border-gold/40 px-4 py-3 outline-offset-2 bg-cream text-left"/></label>
        <label className="block text-sm font-bold"><span className="flex gap-2 items-center mb-2"><LockKeyhole size={16} className="text-gold"/> كلمة المرور</span><input type="password" required autoComplete="current-password" value={password} onChange={e=>setPassword(e.target.value)} className="w-full rounded-xl border border-gold/40 px-4 py-3 bg-cream text-left" dir="ltr"/></label>
        {error && <p role="alert" className="rounded-xl border border-red-300 bg-red-50 text-red-800 p-3 text-sm">{error}</p>}
        <button type="submit" disabled={busy || loading || !isCloudConfigured} className="w-full rounded-xl bg-teal text-white px-5 py-3 font-bold flex justify-center gap-2 items-center"><LogIn size={18}/>{busy || loading ? 'جار التحقق…' : 'دخول لوحة الكاتب'}</button>
        <Link to="/" className="block text-center text-sm font-bold text-teal underline">العودة إلى المدونة</Link>
      </form>
    </div>
  </section>;
}

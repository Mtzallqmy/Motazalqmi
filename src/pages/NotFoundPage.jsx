import { Link } from 'react-router-dom';
export function NotFoundPage() { return <section className="max-w-xl mx-auto text-center p-16"><h1 className="font-literature text-5xl font-bold text-teal">الصفحة غير موجودة</h1><p className="my-5">ربما تغير عنوان الصفحة أو تمت إزالتها.</p><Link to="/" className="text-rust underline font-bold">العودة إلى الرئيسية</Link></section>; }

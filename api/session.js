import { adminReady, checkOrigin, expiredCookie, getAdmin, getFingerprint, methodNotAllowed, privateResponse, sessionCookie, validateLogin } from '../server/auth.js';
import { database } from '../server/db.js';

export default async function handler(req, res) {
  privateResponse(res);
  if (req.method === 'GET') return res.status(200).json({ user: getAdmin(req) });
  if (req.method !== 'POST' && req.method !== 'DELETE') return methodNotAllowed(res, 'GET, POST, DELETE');
  if (!checkOrigin(req)) return res.status(403).json({ error: 'طلب من مصدر غير مسموح.' });
  if (req.method === 'DELETE') {
    res.setHeader('Set-Cookie', expiredCookie());
    return res.status(200).json({ ok: true });
  }
  if (!adminReady() || !process.env.DATABASE_URL) return res.status(503).json({ error: 'لوحة الكاتب غير مهيأة بعد.' });
  const email = req.body?.email, password = req.body?.password;
  if (typeof email !== 'string' || typeof password !== 'string' || JSON.stringify(req.body).length > 2048) return res.status(400).json({ error: 'بيانات الدخول غير صالحة.' });
  try {
    const sql = database();
    const ipHash = getFingerprint(req);
    const blocked = await sql`SELECT attempts, last_attempt FROM blog_login_attempts WHERE ip_hash=${ipHash} AND last_attempt > NOW() - INTERVAL '15 minutes'`;
    if (blocked[0]?.attempts >= 10) return res.status(429).json({ error: 'تجاوزت عدد المحاولات، حاول لاحقًا.' });
    if (!validateLogin(email, password)) {
      await sql`INSERT INTO blog_login_attempts (ip_hash, attempts, last_attempt) VALUES (${ipHash}, 1, NOW())
       ON CONFLICT (ip_hash) DO UPDATE SET attempts=CASE WHEN blog_login_attempts.last_attempt < NOW() - INTERVAL '15 minutes' THEN 1 ELSE blog_login_attempts.attempts + 1 END, last_attempt=NOW()`;
      return res.status(401).json({ error: 'البريد الإلكتروني أو كلمة المرور غير صحيحة.' });
    }
    await sql`DELETE FROM blog_login_attempts WHERE ip_hash=${ipHash}`;
    res.setHeader('Set-Cookie', sessionCookie(process.env.ADMIN_EMAIL));
    return res.status(200).json({ user: {email: process.env.ADMIN_EMAIL} });
  } catch (error) {
    console.error('Login service failed', error?.code || error?.name || 'unexpected');
    return res.status(503).json({ error: 'تعذر الاتصال بخدمة الدخول.' });
  }
}

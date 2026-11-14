import { createHmac, randomBytes, scryptSync, timingSafeEqual } from 'node:crypto';

const MAX_AGE = 7 * 24 * 60 * 60;
const secure = process.env.NODE_ENV === 'production';
const cookieName = secure ? '__Host-blog_session' : 'blog_session_dev';
const key = () => process.env.SESSION_SECRET || '';
const sign = value => createHmac('sha256', key()).update(value).digest('base64url');

export function adminReady() {
  return Boolean(process.env.ADMIN_EMAIL && /^[a-f0-9]{32}:[a-f0-9]{128}$/i.test(process.env.ADMIN_PASSWORD_HASH || '') && key().length >= 32);
}
export function validateLogin(email, password) {
  if (!adminReady() || typeof email !== 'string' || typeof password !== 'string' || password.length > 512) return false;
  const [salt, expected] = process.env.ADMIN_PASSWORD_HASH.split(':');
  const candidate = scryptSync(password, Buffer.from(salt, 'hex'), 64);
  const correct = Buffer.from(expected, 'hex');
  const validPassword = candidate.length === correct.length && timingSafeEqual(candidate, correct);
  const current = Buffer.from(email.trim().toLowerCase());
  const accepted = Buffer.from(process.env.ADMIN_EMAIL.trim().toLowerCase());
  const validEmail = current.length === accepted.length && timingSafeEqual(current, accepted);
  return validEmail && validPassword;
}
function cookie(req) {
  const raw = req.headers.cookie || '';
  return raw.split(';').map(v => v.trim()).find(v => v.startsWith(cookieName + '='))?.slice(cookieName.length + 1);
}
export function getAdmin(req) {
  if (!adminReady()) return null;
  const token = cookie(req);
  if (!token) return null;
  const i = token.lastIndexOf('.');
  if (i < 0) return null;
  const value = token.slice(0, i);
  const actual = Buffer.from(token.slice(i + 1));
  const expected = Buffer.from(sign(value));
  if (actual.length !== expected.length || !timingSafeEqual(actual, expected)) return null;
  try {
    const parsed = JSON.parse(Buffer.from(value, 'base64url').toString('utf8'));
    if (parsed.email !== process.env.ADMIN_EMAIL || !Number.isFinite(parsed.exp) || parsed.exp < Date.now()) return null;
    return { email: parsed.email };
  } catch { return null; }
}
export function sessionCookie(email) {
  const value = Buffer.from(JSON.stringify({ email, exp: Date.now() + MAX_AGE * 1000 })).toString('base64url');
  return `${cookieName}=${value}.${sign(value)}; Path=/; Max-Age=${MAX_AGE}; HttpOnly; SameSite=Strict${secure ? '; Secure' : ''}`;
}
export function expiredCookie() {
  return `${cookieName}=; Path=/; Max-Age=0; HttpOnly; SameSite=Strict${secure ? '; Secure' : ''}`;
}
export function checkOrigin(req) {
  const origin = req.headers.origin;
  if (typeof origin !== 'string' || !req.headers.host) return false;
  try { const url = new URL(origin); return url.host === req.headers.host && (url.protocol === 'https:' || (!secure && url.protocol === 'http:')); } catch { return false; }
}
export function getFingerprint(req) {
  const ip = (req.headers['x-forwarded-for'] || req.socket?.remoteAddress || 'unknown').split(',')[0].trim();
  return createHmac('sha256', key() || 'blog-fallback-key').update(ip).digest('hex');
}
export function methodNotAllowed(res, allowed) {
  res.setHeader('Allow', allowed);
  res.status(405).json({ error: 'طريقة الطلب غير مسموحة.' });
}
export function privateResponse(res) {
  res.setHeader('Cache-Control', 'private, no-store');
  res.setHeader('X-Content-Type-Options', 'nosniff');
}

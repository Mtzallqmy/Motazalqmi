import { createHmac, scryptSync, timingSafeEqual } from 'node:crypto';
const secure=process.env.NODE_ENV==='production';
const cookieName=secure?'__Host-blog_session':'blog_session_dev';
const secret=()=>process.env.SESSION_SECRET||'';
const signature=value=>createHmac('sha256',secret()).update(value).digest('base64url');
export const adminReady=()=>Boolean(process.env.ADMIN_EMAIL && /^[0-9a-f]{32}:[0-9a-f]{128}$/i.test(process.env.ADMIN_PASSWORD_HASH||'') && secret().length>=32);
export function validateLogin(email,password) {
  if(!adminReady()||typeof email!=='string'||typeof password!=='string'||password.length>512)return false;
  const [salt,expected]=process.env.ADMIN_PASSWORD_HASH.split(':');
  const actual=scryptSync(password,Buffer.from(salt,'hex'),64);
  const good=Buffer.from(expected,'hex');
  const one=Buffer.from(email.trim().toLowerCase()),two=Buffer.from(process.env.ADMIN_EMAIL.trim().toLowerCase());
  return actual.length===good.length&&timingSafeEqual(actual,good)&&one.length===two.length&&timingSafeEqual(one,two);
}
export function getAdmin(req) {
 if(!adminReady())return null;
 const v=(req.headers.cookie||'').split(';').map(x=>x.trim()).find(x=>x.startsWith(cookieName+'='));
 if(!v)return null;
 const tok=v.slice(cookieName.length+1),i=tok.lastIndexOf('.');
 if(i<0)return null;
 const body=tok.slice(0,i),a=Buffer.from(tok.slice(i+1)),b=Buffer.from(signature(body));
 if(a.length!==b.length||!timingSafeEqual(a,b))return null;
 try{
  const decoded=JSON.parse(Buffer.from(body,'base64url').toString('utf8'));
  return decoded.email===process.env.ADMIN_EMAIL&&decoded.exp>Date.now()?{email:decoded.email}:null;
 }catch{return null;}
}
export function sessionCookie(email){
 const body=Buffer.from(JSON.stringify({email,exp:Date.now()+604800000})).toString('base64url');
 return cookieName+'='+body+'.'+signature(body)+'; Path=/; HttpOnly; SameSite=Strict; Max-Age=604800'+(secure?'; Secure':'');
}
export const expiredCookie=()=>cookieName+'=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0'+(secure?'; Secure':'');
export function checkOrigin(req){
 if(!req.headers.origin||!req.headers.host)return false;
 try{const url=new URL(req.headers.origin);return url.host===req.headers.host&&(url.protocol==='https:'||(!secure&&url.protocol==='http:'));}catch{return false;}
}
export const fingerprint=req=>createHmac('sha256',secret()||'fallback').update(String(req.headers['x-forwarded-for']||req.socket?.remoteAddress||'unknown').split(',')[0]).digest('hex');
export function noCache(res){res.setHeader('Cache-Control','private, no-store');res.setHeader('X-Content-Type-Options','nosniff');}

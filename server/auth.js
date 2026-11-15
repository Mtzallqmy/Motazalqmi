import { createHmac, randomBytes, scryptSync, timingSafeEqual } from 'node:crypto';
const secure=process.env.NODE_ENV==='production';
const cookieName=secure?'__Host-blog_session':'blog_session_dev';
const secret=()=>process.env.SESSION_SECRET||'';
const signature=value=>createHmac('sha256',secret()).update(value).digest('base64url');
// Password rotation revokes previously issued cookies and mobile tokens.
const credentialVersion=()=>createHmac('sha256',secret()).update(process.env.ADMIN_PASSWORD_HASH||'').digest('hex').slice(0,24);
export const adminReady=()=>Boolean(process.env.ADMIN_EMAIL && /^[0-9a-f]{32}:[0-9a-f]{128}$/i.test(process.env.ADMIN_PASSWORD_HASH||'') && secret().length>=32);
export function validateLogin(email,password) {
  if(!adminReady()||typeof email!=='string'||typeof password!=='string'||password.length>512)return false;
  const [salt,expected]=process.env.ADMIN_PASSWORD_HASH.split(':');
  const actual=scryptSync(password,Buffer.from(salt,'hex'),64);
  const good=Buffer.from(expected,'hex');
  const one=Buffer.from(email.trim().toLowerCase()),two=Buffer.from(process.env.ADMIN_EMAIL.trim().toLowerCase());
  return actual.length===good.length&&timingSafeEqual(actual,good)&&one.length===two.length&&timingSafeEqual(one,two);
}

/** Two-hour, purpose-bound native writer credential. Passwords stay server-side. */
export function issueMobileAdminToken(email) {
 if (!adminReady() || email !== process.env.ADMIN_EMAIL) throw new Error('Admin unavailable');
 const expiresAt=Date.now()+2*60*60*1000;
 const body=Buffer.from(JSON.stringify({aud:'motazalqmi-mobile-admin',email,exp:expiresAt,pwd:credentialVersion(),jti:randomBytes(16).toString('hex')})).toString('base64url');
 return {accessToken:body+'.'+signature(body),expiresAt};
}
export function getMobileAdmin(req) {
 if(!adminReady())return null;
 const header=req.headers?.authorization;
 if(typeof header!=='string'||!/^Bearer [A-Za-z0-9_.-]{40,2048}$/.test(header))return null;
 const token=header.slice(7),i=token.lastIndexOf('.');
 if(i<1)return null;
 const body=token.slice(0,i),actual=Buffer.from(token.slice(i+1)),expected=Buffer.from(signature(body));
 if(actual.length!==expected.length||!timingSafeEqual(actual,expected))return null;
 try{
  const d=JSON.parse(Buffer.from(body,'base64url').toString('utf8'));
  return d.aud==='motazalqmi-mobile-admin'&&d.email===process.env.ADMIN_EMAIL&&d.pwd===credentialVersion()&&
   Number.isSafeInteger(d.exp)&&d.exp>Date.now()&&d.exp<=Date.now()+2*60*60*1000&&
   typeof d.jti==='string'&&/^[a-f0-9]{32}$/.test(d.jti)?{email:d.email}:null;
 }catch{return null}
}

export function getAdmin(req) {
 if(!adminReady())return null;
 const mobile=getMobileAdmin(req);
 if(mobile)return mobile;
 const v=(req.headers.cookie||'').split(';').map(x=>x.trim()).find(x=>x.startsWith(cookieName+'='));
 if(!v)return null;
 const tok=v.slice(cookieName.length+1),i=tok.lastIndexOf('.');
 if(i<0)return null;
 const body=tok.slice(0,i),a=Buffer.from(tok.slice(i+1)),b=Buffer.from(signature(body));
 if(a.length!==b.length||!timingSafeEqual(a,b))return null;
 try{
  const decoded=JSON.parse(Buffer.from(body,'base64url').toString('utf8'));
  return !decoded.aud&&decoded.email===process.env.ADMIN_EMAIL&&decoded.pwd===credentialVersion()&&decoded.exp>Date.now()?{email:decoded.email}:null;
 }catch{return null;}
}
export function sessionCookie(email,remember=true){
 const seconds=remember?30*24*60*60:12*60*60;
 const body=Buffer.from(JSON.stringify({email,exp:Date.now()+seconds*1000,pwd:credentialVersion()})).toString('base64url');
 return cookieName+'='+body+'.'+signature(body)+'; Path=/; HttpOnly; SameSite=Strict'+(remember?'; Max-Age='+seconds:'')+(secure?'; Secure':'');
}
export function issueMobileRefreshToken(email){
 if(!adminReady() || email!==process.env.ADMIN_EMAIL)throw new Error('Admin unavailable');
 const exp=Date.now()+30*24*60*60*1000;
 const body=Buffer.from(JSON.stringify({aud:'motazalqmi-mobile-refresh',email,exp,pwd:credentialVersion(),jti:randomBytes(16).toString('hex')})).toString('base64url');
 return {refreshToken:body+'.'+signature(body),refreshExpiresAt:exp};
}
export function validateMobileRefreshToken(token){
 if(!adminReady()||typeof token!=='string'||token.length>2048)return null;
 const dot=token.lastIndexOf('.');if(dot<1)return null;
 const body=token.slice(0,dot),a=Buffer.from(token.slice(dot+1)),b=Buffer.from(signature(body));
 if(a.length!==b.length||!timingSafeEqual(a,b))return null;
 try{
  const t=JSON.parse(Buffer.from(body,'base64url').toString('utf8'));
  if(t.aud!=='motazalqmi-mobile-refresh'||t.email!==process.env.ADMIN_EMAIL||
   t.pwd!==credentialVersion()||!Number.isSafeInteger(t.exp)||t.exp<=Date.now()||
   t.exp>Date.now()+30*24*60*60*1000||!/^[a-f0-9]{32}$/.test(t.jti))return null;
  return {email:t.email,expiresAt:t.exp};
 }catch{return null}
}
export const expiredCookie=()=>cookieName+'=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0'+(secure?'; Secure':'');
export function checkOrigin(req){
 if(getMobileAdmin(req))return true; // Bearer auth is not ambient, unlike a browser cookie.
 if(!req.headers.origin||!req.headers.host)return false;
 try{const url=new URL(req.headers.origin);return url.host===req.headers.host&&(url.protocol==='https:'||(!secure&&url.protocol==='http:'));}catch{return false;}
}
export const fingerprint=req=>createHmac('sha256',secret()||'fallback').update(String(req.headers['x-forwarded-for']||req.socket?.remoteAddress||'unknown').split(',')[0]).digest('hex');
export function noCache(res){res.setHeader('Cache-Control','private, no-store');res.setHeader('X-Content-Type-Options','nosniff');}

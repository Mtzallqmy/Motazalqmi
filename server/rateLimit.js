import { createHash } from 'node:crypto';
// Bounded burst protection per warm serverless instance. Vercel's network controls
// remain the perimeter; this is deliberately not described as a global quota.
export function createBurstLimiter({limit=120,windowMs=60000,maxKeys=10000}={}) {
 const entries=new Map();
 return function allowed(identity,now=Date.now()) {
  const key=createHash('sha256').update(String(identity)).digest('hex');
  const old=entries.get(key);
  if(!old||now-old.started>=windowMs) {
   if(entries.size>=maxKeys)entries.delete(entries.keys().next().value);
   entries.set(key,{started:now,count:1});return true;
  }
  old.count++;return old.count<=limit;
 };
}
export const allowBurst=createBurstLimiter();

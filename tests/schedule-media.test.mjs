import test from 'node:test';
import assert from 'node:assert/strict';
import {publicationTime,isScheduled,publicationLabel} from '../shared/publication.js';
import {normalizeArticle} from '../server/db.js';
import {createMediaDeleteHandler,managedMediaUrl} from '../server/mediaDeletion.js';
const article={id:'a',title:'عنوان',content:'نص',slug:'title',status:'published'};
const address='https://example123.public.blob.vercel-storage.com/media/a.png';
const response=()=>({statusCode:0,setHeader(){},status(code){this.statusCode=code;return this},json(body){this.body=body;return this}});
test('publication time has a timezone and scheduling survives normalization',()=>{
 const time=publicationTime('2027-01-20T20:30+03:00');assert.equal(time,'2027-01-20T17:30:00.000Z');
 assert.equal(normalizeArticle({...article,scheduledAt:time}).scheduledAt,time);
 assert.equal(normalizeArticle({...article,status:'draft',scheduledAt:time}).scheduledAt,'');
 assert.equal(Object.hasOwn(normalizeArticle(article),'scheduledAt'),false);
 assert.throws(()=>publicationTime('2027-01-20T20:30'));
 assert.equal(isScheduled({...article,scheduledAt:time},Date.parse('2027-01-01')),true);
 assert.equal(publicationLabel({...article,scheduledAt:time},Date.parse('2027-02-01')),'منشور');
});
test('managed deletion URLs reject foreign origins, credentials and query variants',()=>{
 assert(managedMediaUrl(address));for(const url of ['https://example.org/a.png','http://example123.public.blob.vercel-storage.com/a.png',address+'?x=1',address+'#x','https://user@example123.public.blob.vercel-storage.com/a.png'])assert.equal(managedMediaUrl(url),null);
});
function handler(marked,options={}) {
 const sql=()=>Promise.resolve([]);
 sql.transaction=async queries=>[[],marked];
 return createMediaDeleteHandler({sqlFactory:()=>sql,authorize:()=>({email:'writer'}),originAllowed:()=>true,
  listObjects:async()=>({blobs:[{url:address}]}),deleteObject:async()=>{},...options});
}
test('deletion requires authorization and explicit confirmation before storage access',async()=>{
 let res=response();await createMediaDeleteHandler({authorize:()=>null})({method:'POST',headers:{},body:{url:address,confirm:true}},res);assert.equal(res.statusCode,401);
 res=response();await handler([])({method:'POST',body:{url:address}},res);assert.equal(res.statusCode,400);
});
test('referenced media cannot reach the storage delete operation',async()=>{
 let deleted=false,res=response();await handler([],{deleteObject:async()=>{deleted=true}})({method:'POST',body:{url:address,confirm:true}},res);
 assert.equal(res.statusCode,409);assert.equal(deleted,false);
});
test('a reserved unreferenced object is deleted and retries remain idempotent',async()=>{
 let count=0,res=response();await handler([{url:address,state:'pending'}],{deleteObject:async()=>{count++}})({method:'POST',body:{url:address,confirm:true}},res);
 assert.equal(res.statusCode,200);assert.equal(count,1);
 res=response();await handler([{url:address,state:'deleted'}],{deleteObject:async()=>{count++}})({method:'POST',body:{url:address,confirm:true}},res);
 assert.equal(res.statusCode,200);assert.equal(count,1);
});

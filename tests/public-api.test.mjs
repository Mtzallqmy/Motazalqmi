import { test } from 'node:test';
import { strict as assert } from 'node:assert';
import { SITE, SECTIONS, QUOTES } from '../shared/publicContent.js';
import { boundedInt, param, publicGet, sendJson } from '../server/publicApi.js';
import { metadataHandler } from '../server/editorial.js';
const load=async()=>({site:SITE,sections:SECTIONS,quotes:QUOTES});
const categories=metadataHandler('sections',load);
const about=metadataHandler('site',load);
const quotes=metadataHandler('quotes',load);

function fake(req={}){
 const headers={};const response={statusCode:200,body:null,setHeader(k,v){headers[k.toLowerCase()]=v;return this},status(n){this.statusCode=n;return this},send(x){this.body=x;return this},json(x){this.body=JSON.stringify(x);return this},end(){this.body='';return this}};
 return {req:{method:'GET',headers:{},query:{},...req},res:response,headers};
}
test('API v1 metadata is complete and stable',async()=>{
 assert.equal(SITE.id,'motazalqmi');
 assert.equal(SECTIONS.length,8);
 assert.equal(new Set(SECTIONS.map(c=>c.id)).size,SECTIONS.length);
 assert.ok(QUOTES.length>=4);
 for(const handler of [categories,about,quotes]){const x=fake();await handler(x.req,x.res);assert.equal(x.res.statusCode,200);assert.equal(JSON.parse(x.res.body).apiVersion,1);assert.match(x.headers.etag,/^"/)}
});
test('ETag support prevents unnecessary syncs',()=>{
 const a=fake();sendJson(a.req,a.res,{a:1});const b=fake({headers:{'if-none-match':a.headers.etag}});sendJson(b.req,b.res,{a:1});assert.equal(b.res.statusCode,304);
});
test('read-only API refuses mutations and bounds parameters',()=>{
 const a=fake({method:'POST'});assert.equal(publicGet(a.req,a.res),false);assert.equal(a.res.statusCode,405);
 assert.equal(boundedInt('10000000',30,1,100),30);
 assert.equal(boundedInt('999',30,1,100),100);
 assert.equal(param({query:{q:' x '}},'q'),'x');
});

import {normalizeArabic,pageSnapshot,publishedSnapshot,syncManifest} from '../server/contentSnapshot.js';
import {createResourceHandler} from '../api/v1/[resource].js';
const snapshot={revision:'a'.repeat(64),articles:[{id:'a',slug:'alif',title:'إِبْدَاع',content:'نص عربي',author:'معتز',category:'books',tags:['بلاغة'],featured:true},{id:'b',title:'آخر',content:'نص',author:'كاتب',tags:[]}]};
test('Arabic search includes author, tags, content and category',()=>{
 assert.equal(normalizeArabic('إِبْدَاع'),'ابداع');
 for(const q of ['ابداع','معتز','بلاغة','عربي','بطون'])assert.equal(pageSnapshot(snapshot,{q}).articles[0].id,'a');
 assert.equal(pageSnapshot(snapshot,{featured:true}).total,1);
 assert.equal(pageSnapshot(snapshot,{limit:1}).hasMore,true);
 assert.equal(pageSnapshot(snapshot).articles[0].content,undefined);
});
test('snapshot is published only, atomic and changes revision on deletion',async()=>{
 let statement='';const sql=async (parts)=>{statement=parts.join('?');return [{id:'a',slug:'s',status:'published',publish_date:'2026-11-14',updated_at:'today',data:{title:'t',content:'c'}}]};
 const a=await publishedSnapshot(sql),b=await publishedSnapshot(async()=>[]);
 assert.match(statement,/WHERE status='published'/);assert.notEqual(a.revision,b.revision);
 assert.deepEqual(syncManifest(b).ids,[]);assert.equal(syncManifest(a).authoritative,true);
});
test('new API resources report snapshots, validation, ETags and failures',async()=>{
 const handler=createResourceHandler(async()=>snapshot);
 for(const resource of ['sync','search','featured','config','versions']) {
  const x=fake({query:{resource,q:'ابداع',since:snapshot.revision}});await handler(x.req,x.res);
  assert.equal(x.res.statusCode,200,resource);assert.equal(JSON.parse(x.res.body).apiVersion,1);
  const y=fake({query:{resource,q:'ابداع',since:snapshot.revision},headers:{'if-none-match':x.headers.etag}});await handler(y.req,y.res);assert.equal(y.res.statusCode,304);
 }
 const a=fake({query:{resource:'search',q:'x'.repeat(121)}});await handler(a.req,a.res);assert.equal(a.res.statusCode,400);
 const b=fake({query:{resource:'sync'}});await createResourceHandler(async()=>{throw Error('db secret')})(b.req,b.res);assert.equal(b.res.statusCode,503);assert.ok(!b.res.body.includes('secret'));
 const c=fake({method:'POST',query:{resource:'sync'}});await handler(c.req,c.res);assert.equal(c.res.statusCode,405);
});

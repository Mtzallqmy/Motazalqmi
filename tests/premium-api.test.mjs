import { test } from 'node:test';
import { strict as assert } from 'node:assert';
import { createHandler } from '../api/v1/[resource].js';
import { SITE,SECTIONS,QUOTES } from '../shared/publicContent.js';
import { metadataHandler } from '../server/editorial.js';
const snapshot={apiVersion:1,mode:'snapshot',complete:true,articles:[],total:0,site:SITE,sections:SECTIONS,quotes:QUOTES};
function fake(query={},method='GET',headers={}) {
 const h={}; const res={statusCode:200,body:null,setHeader(k,v){h[k]=v},status(s){this.statusCode=s;return this},json(b){this.body=b;return this},send(b){this.body=JSON.parse(b);return this},end(){return this}};
 return {req:{method,query,headers},res,h};
}
const handler=createHandler({snapshot:async()=>snapshot,list:async args=>({articles:[],total:0,...args})});
test('sync returns complete empty snapshot so all removed articles can be retired',async()=>{
 const x=fake({resource:'sync'});await handler(x.req,x.res);assert.equal(x.res.statusCode,200);assert.equal(x.res.body.complete,true);assert.equal(x.res.body.total,0);
});
test('sync never sends partial or fabricated success after a database failure',async()=>{
 const x=fake({resource:'sync'});await createHandler({snapshot:async()=>{throw new Error('offline')}})(x.req,x.res);assert.equal(x.res.statusCode,503);assert.equal(x.h['Cache-Control'],'no-store');assert.equal(x.res.body.complete,undefined);
});
test('sync validates since and public routes refuse writes',async()=>{
 const x=fake({resource:'sync',since:'bad'});await handler(x.req,x.res);assert.equal(x.res.statusCode,400);
 for (const resource of ['sync','search','featured','config','versions']) {const y=fake({resource},'DELETE');await handler(y.req,y.res);assert.equal(y.res.statusCode,405)}
});
test('search is bounded and featured filtering is passed to parameterized queries',async()=>{
 const x=fake({resource:'search',q:'الشِّعر',limit:'999'});await handler(x.req,x.res);assert.equal(x.res.body.limit,100);assert.equal(x.res.body.q,'الشِّعر');
 const y=fake({resource:'featured'});await handler(y.req,y.res);assert.equal(y.res.body.featured,true);
});
test('configuration and versions disclose no fabricated released APK',async()=>{
 const x=fake({resource:'config'});await handler(x.req,x.res);assert.equal(x.res.body.syncMode,'snapshot');
 const y=fake({resource:'versions'});await handler(y.req,y.res);assert.equal(y.res.body.versions[0].downloadUrl,null);
 const z=fake({resource:'unknown'});await handler(z.req,z.res);assert.equal(z.res.statusCode,404);
});
test('editorial metadata is read from the same supplied storage source',async()=>{
 for(const key of ['site','sections','quotes']) { const x=fake();await metadataHandler(key,async()=>snapshot)(x.req,x.res);assert.deepEqual(x.res.body[key],snapshot[key]) }
});

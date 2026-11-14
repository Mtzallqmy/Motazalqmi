import { test } from 'node:test';
import { strict as assert } from 'node:assert';
import { SITE, SECTIONS, QUOTES } from '../shared/publicContent.js';
import { boundedInt, param, publicGet, sendJson } from '../server/publicApi.js';
import categories from '../api/v1/categories.js';
import about from '../api/v1/about.js';
import quotes from '../api/v1/quotes.js';

function fake(req={}){
 const headers={};const response={statusCode:200,body:null,setHeader(k,v){headers[k.toLowerCase()]=v;return this},status(n){this.statusCode=n;return this},send(x){this.body=x;return this},json(x){this.body=JSON.stringify(x);return this},end(){this.body='';return this}};
 return {req:{method:'GET',headers:{},query:{},...req},res:response,headers};
}
test('API v1 metadata is complete and stable',()=>{
 assert.equal(SITE.id,'motazalqmi');
 assert.equal(SECTIONS.length,8);
 assert.equal(new Set(SECTIONS.map(c=>c.id)).size,SECTIONS.length);
 assert.ok(QUOTES.length>=4);
 for(const handler of [categories,about,quotes]){const x=fake();handler(x.req,x.res);assert.equal(x.res.statusCode,200);assert.equal(JSON.parse(x.res.body).apiVersion,1);assert.match(x.headers.etag,/^"/)}
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

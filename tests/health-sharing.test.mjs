import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {articleRow} from '../server/db.js';
import {shortCode,shortArticleUrl} from '../server/articleLinks.js';
import {discovery} from '../server/articleDiscovery.js';
import {SECTIONS,categoryId} from '../shared/publicContent.js';
import {HEALTH_TOPICS} from '../shared/healthDesk.js';

test('public link is stable when a slug changes and legacy categories remain visible',()=>{
 const one=articleRow({id:'seed-1',slug:'original',data:{category:'الرئيسي'}});
 const two=articleRow({id:'seed-1',slug:'renamed',data:{category:'general',shortCode:'old'}});
 assert.equal(one.shortCode,two.shortCode);
 assert.equal(one.shortCode,shortCode('seed-1'));
 assert.match(shortArticleUrl('seed-1'),/\/r\/[0-9a-f]{16}$/);
 assert.equal(categoryId('الصحة والدواء'),'health');
 assert.equal(one.category,'general');
});
test('short URLs resolve a published record only',async()=>{
 function response(){return {headers:{},code:0,setHeader(k,v){this.headers[k]=v},status(n){this.code=n;return this},send(){return this},end(){return this}}}
 const code=shortCode('id');
 let res=response();
 const sql=async (query,value)=>{
  assert.match(query.join('?'),/blog_published_articles/);
  assert.equal(value, code);
  return [{slug:'العنوان-الحالي'}];
 };
 const req={method:'GET',query:{code},headers:{},socket:{remoteAddress:'short-test'}};
 await discovery(req,res,'short-link',()=>sql);
 assert.equal(res.code,302);
 assert.equal(res.headers.Location,'https://motazalqmi.vercel.app/articles/'+encodeURIComponent('العنوان-الحالي'));
 res=response();
 await discovery(req,res,'short-link',()=>async()=>[]);
 assert.equal(res.code,404);
 assert.equal(res.headers.Location,undefined);
});
test('medical launch and offline bootstrap agree on all topics',()=>{
 const launch=JSON.parse(readFileSync(new URL('../db/content/health-launch.json',import.meta.url)));
 const boot=JSON.parse(readFileSync(new URL('../android/app/src/main/assets/bootstrap.json',import.meta.url)));
 assert.ok(SECTIONS.some(s=>s.id==='health'));
 assert.ok(boot.sections.some(s=>s.id==='health'));
 for(const topic of HEALTH_TOPICS){
  const article=launch.find(a=>a.tags.includes(topic.title));
  assert.ok(article);
  assert.ok(article.content.includes('## المصادر'));
  assert.ok(boot.articles.some(a=>a.id===article.id&&a.content===article.content));
 }
});

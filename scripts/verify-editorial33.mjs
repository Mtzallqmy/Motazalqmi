// Runs real server handlers against an isolated Neon branch and the configured Blob store.
// Uses only test-owned IDs and deletes its uploaded file and temporary records.
import {randomBytes,scryptSync} from 'node:crypto';
import assert from 'node:assert/strict';
if(process.env.MOTAZ_VERIFY_ISOLATED!=='true'||!process.env.DATABASE_URL||!process.env.BLOB_READ_WRITE_TOKEN)
 throw Error('Provide an isolated verification DATABASE_URL, Blob credentials and MOTAZ_VERIFY_ISOLATED=true.');
const password=randomBytes(32).toString('hex'),salt=randomBytes(16);
Object.assign(process.env,{NODE_ENV:'test',SESSION_SECRET:randomBytes(40).toString('hex'),ADMIN_EMAIL:'verification@example.invalid',ADMIN_PASSWORD_HASH:salt.toString('hex')+':'+scryptSync(password,salt,64).toString('hex')});
const {default:session}=await import('../api/session.js');
const {default:articles}=await import('../api/articles.js');
const {default:resource}=await import('../api/v1/[resource].js');
const {default:article}=await import('../api/v1/article.js');
const {database}=await import('../server/db.js');
const sql=database();let cookie='',articleId,author,uploaded='';
const evidence=[];
async function call(handler,method,query={},body={}) {
 const res={code:0,headers:{},setHeader(k,v){this.headers[k]=v},status(n){this.code=n;return this},json(v){this.value=v;return this},send(v){this.value=v;return this},end(){return this}};
 await handler({method,query,body,headers:{'content-type':'application/json',origin:'http://localhost',host:'localhost',cookie}},res);
 return res;
}
const studio=(name,body)=>call(resource,body?'POST':'GET',{resource:name},body||{});
try {
 const login=await call(session,'POST',{}, {email:process.env.ADMIN_EMAIL,password});assert.equal(login.code,200);cookie=login.headers['Set-Cookie'].split(';')[0];
 let r=await studio('authors',{name:'كاتب فحص 3.3',bio:'ملف تحقق مؤقت'});assert.equal(r.code,200);author=r.value.author;
 r=await studio('authors',{...author,name:'كاتب مختلف بعد التعديل'});assert.equal(r.code,200);assert.equal(r.value.author.id,author.id);evidence.push('Author identity survives profile editing');
 const draft={title:'اختبار جدولة 3.3',slug:'verification33-'+randomBytes(12).toString('hex'),content:'نص تحقق مؤقت على قاعدة المعاينة المنفصلة.',status:'published',author:author.name,authorId:author.id,scheduledAt:'2099-01-01T00:00:00Z'};
 r=await call(articles,'POST',{}, {action:'save',article:draft});assert.equal(r.code,200);articleId=r.value.id;
 let sync=await studio('sync');assert.equal(sync.code,200);assert(!JSON.parse(sync.value).articles.some(a=>a.id===articleId));
 const hidden=await call(article,'GET',{slug:draft.slug});assert.equal(hidden.code,404);
 const listing=await call(articles,'GET',{scope:'admin'}),saved=listing.value.articles.find(a=>a.id===articleId);assert(saved);
 r=await call(articles,'POST',{}, {action:'save',article:{...saved,scheduledAt:'2020-01-01T00:00:00Z'},expectedVersion:saved.updatedAt});assert.equal(r.code,200);
 sync=await studio('sync');assert(JSON.parse(sync.value).articles.some(a=>a.id===articleId));evidence.push('Future content is private; due content reaches the real Android snapshot');
 r=await call(articles,'POST',{}, {action:'save',article:saved,expectedVersion:saved.updatedAt});assert.equal(r.code,409);evidence.push('Concurrent writer changes are protected');
 r=await studio('media-direct',{mime:'image/png',purpose:'author',base64:'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAFgAI/ScLbtAAAAABJRU5ErkJggg=='});assert.equal(r.code,200);uploaded=r.value.url;
 r=await studio('authors',{...author,image:uploaded});assert.equal(r.code,200);
 r=await studio('media-delete',{url:uploaded,confirm:true});assert.equal(r.code,409);assert(r.value.references.some(ref=>ref.kind==='profile'));
 r=await studio('authors',{...author,image:''});assert.equal(r.code,200);
 r=await studio('media-delete',{url:uploaded,confirm:true});assert.equal(r.code,200);
 r=await studio('media-delete',{url:uploaded,confirm:true});assert.equal(r.code,200);
 r=await studio('authors',{...author,image:uploaded});assert.equal(r.code,409);
 evidence.push('Real Blob upload, used-file protection, deletion, retry and database tombstone guard');
 console.log(evidence.map(e=>'PASS '+e).join('\n'));
} finally {
 if(author)await studio('authors',{...author,image:''});
 if(uploaded){const removed=await studio('media-delete',{url:uploaded,confirm:true});assert.equal(removed.code,200);}
 if(articleId){const removed=await call(articles,'POST',{}, {action:'delete',id:articleId});assert.equal(removed.code,200);}
 if(author)await sql`DELETE FROM blog_public_metadata WHERE id=${'author:'+author.id}`;
 console.log('PASS test-owned cloud records and media cleanup');
}

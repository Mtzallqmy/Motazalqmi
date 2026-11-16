import {randomBytes,scryptSync} from 'node:crypto';
import assert from 'node:assert/strict';

if(!process.env.DATABASE_URL)throw Error('Set DATABASE_URL to the isolated verification database.');
const password=randomBytes(32).toString('hex'),salt=randomBytes(16);
Object.assign(process.env,{NODE_ENV:'test',SESSION_SECRET:randomBytes(40).toString('hex'),
 ADMIN_EMAIL:'verification@example.invalid',ADMIN_PASSWORD_HASH:salt.toString('hex')+':'+scryptSync(password,salt,64).toString('hex')});
const {default:session}=await import('../api/session.js');
const {default:articles}=await import('../api/articles.js');
const {default:resource}=await import('../api/v1/[resource].js');
let cookie='',articleId;
async function call(handler,method,query={},body={}) {
 const response={code:0,headers:{},setHeader(k,v){this.headers[k]=v},status(n){this.code=n;return this},json(v){this.value=v},send(v){this.value=v},end(){}};
 await handler({method,query,body,headers:{origin:'http://localhost',host:'localhost',cookie}},response);
 return response;
}
try {
 const login=await call(session,'POST',{}, {email:process.env.ADMIN_EMAIL,password});assert.equal(login.code,200);
 cookie=login.headers['Set-Cookie'].split(';')[0];
 const slug='verification-'+randomBytes(10).toString('hex');
 const draft={title:'اختبار دورة التحرير',slug,content:'نص مؤقت للتحقق من الحفظ والنشر والمزامنة.',status:'draft',author:'كاتب التحقق',category:'general'};
 const created=await call(articles,'POST',{}, {action:'save',article:draft});assert.equal(created.code,200);articleId=created.value.id;
 const list=await call(articles,'GET',{scope:'admin'});const stored=list.value.articles.find(a=>a.id===articleId);assert(stored.updatedAt);
 const publish=await call(articles,'POST',{}, {action:'save',article:{...stored,slug:slug+'-published',status:'published'},expectedVersion:stored.updatedAt});assert.equal(publish.code,200);
 const stale=await call(articles,'POST',{}, {action:'save',article:stored,expectedVersion:stored.updatedAt});assert.equal(stale.code,409);
 const redirect=await call(resource,'GET',{resource:'article-page',slug});assert.equal(redirect.code,308);
 const html=await call(resource,'GET',{resource:'article-page',slug:slug+'-published'});assert.equal(html.code,200);assert(html.value.includes('application/ld+json'));
 const revisions=await call(resource,'GET',{resource:'revisions',id:articleId});assert.equal(revisions.value.revisions[0].data.status,'draft');
 const sync=await call(resource,'GET',{resource:'sync'});assert(JSON.parse(sync.value).articles.some(a=>a.id===articleId));
 console.log('PASS Neon draft persistence, publish, conflict rejection, old-link redirect, revision history, SEO document and Android sync');
} finally {
 if(articleId){const removed=await call(articles,'POST',{}, {action:'delete',id:articleId});assert.equal(removed.code,200);console.log('PASS isolated verification article cleanup');}
}

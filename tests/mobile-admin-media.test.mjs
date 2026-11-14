import {test} from 'node:test';
import {strict as assert} from 'node:assert';
import {randomBytes,scryptSync} from 'node:crypto';
import {issueMobileAdminToken,getAdmin,getMobileAdmin,checkOrigin} from '../server/auth.js';
import {imageInventory,createHandler} from '../api/v1/media.js';

test('signed native bearer is purpose-bound and cannot be forged',()=>{
 const salt=randomBytes(16).toString('hex'),password='test-password';
 process.env.ADMIN_EMAIL='admin@example.com';
 process.env.ADMIN_PASSWORD_HASH=salt+':'+scryptSync(password,Buffer.from(salt,'hex'),64).toString('hex');
 process.env.SESSION_SECRET=randomBytes(48).toString('base64url');
 const token=issueMobileAdminToken('admin@example.com').accessToken;
 const req={headers:{authorization:'Bearer '+token,host:'motazalqmi.vercel.app'}};
 assert.deepEqual(getAdmin(req),{email:'admin@example.com'});
 assert.equal(checkOrigin(req),true);
 const bad=token.slice(0,-1)+(token.endsWith('A')?'B':'A');
 assert.equal(getAdmin({headers:{authorization:'Bearer '+bad}}),null);
 assert.equal(getMobileAdmin({headers:{authorization:'Bearer random'}}),null);
 assert.equal(checkOrigin({headers:{origin:'https://evil.test',host:'motazalqmi.vercel.app'}}),false);
});
test('media inventory includes published covers and inline images only, without duplicates',()=>{
 const cover='https://images.example.test/a.png',inline='https://images.example.test/b.jpg';
 const items=imageInventory({articles:[
  {id:'1',slug:'good',status:'published',coverImage:cover,content:'![صورة]('+inline+') ![مكررة]('+inline+')'},
  {id:'2',slug:'draft',status:'draft',coverImage:'https://secret.test/image.png',content:''}
 ]});
 assert.equal(items.length,2);
 assert.equal(items[0].kind,'cover');
 assert.equal(items[1].kind,'article');
 assert.equal(items.some(item=>item.slug==='draft'),false);
});
test('media API is read only',async()=>{
 let status=200;
 const res={setHeader(){},status(n){status=n;return this},json(){return this}};
 await createHandler(async()=>{throw Error('MUST_NOT_QUERY')})({method:'POST',headers:{}},res);
 assert.equal(status,405);
});

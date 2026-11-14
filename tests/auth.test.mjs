import {test} from 'node:test';
import {strict as assert} from 'node:assert';
import {randomBytes,scryptSync} from 'node:crypto';
import {adminReady,validateLogin,sessionCookie,getAdmin} from '../server/auth.js';
import {normalizeArticle} from '../server/db.js';
test('signed session and wrong password',()=>{
 const salt=randomBytes(16).toString('hex'),p='secure-password-test';
 process.env.ADMIN_EMAIL='writer@example.com';
 process.env.ADMIN_PASSWORD_HASH=salt+':'+scryptSync(p,Buffer.from(salt,'hex'),64).toString('hex');
 process.env.SESSION_SECRET=randomBytes(48).toString('base64url');
 assert.equal(adminReady(),true);
 assert.equal(validateLogin('writer@example.com',p),true);
 assert.equal(validateLogin('writer@example.com','bad'),false);
 const cookie=sessionCookie('writer@example.com').split(';')[0];
 assert.equal(getAdmin({headers:{cookie}})?.email,'writer@example.com');
 assert.equal(getAdmin({headers:{cookie:cookie+'x'}}),null);
});
test('server rejects unsafe article image',()=>{
 assert.throws(()=>normalizeArticle({title:'x',slug:'x',content:'y',status:'published',coverImage:'javascript:abc'}));
 assert.equal(normalizeArticle({title:'نص',slug:'good',content:'تمام',status:'published'}).title,'نص');
});

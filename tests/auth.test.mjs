import { strict as assert } from 'node:assert';
import { test } from 'node:test';
import { randomBytes, scryptSync } from 'node:crypto';
import { adminReady, validateLogin, sessionCookie, getAdmin } from '../server/auth.js';
import { normalizeArticle } from '../server/db.js';
test('session signature rejects modified and expired values', () => {
 const salt=randomBytes(16).toString('hex');const password='unique-password-test';
 process.env.ADMIN_EMAIL='writer@example.com';process.env.ADMIN_PASSWORD_HASH=`${salt}:${scryptSync(password,Buffer.from(salt,'hex'),64).toString('hex')}`;
 process.env.SESSION_SECRET=randomBytes(48).toString('base64url');
 assert.equal(adminReady(),true);assert.equal(validateLogin('writer@example.com',password),true);assert.equal(validateLogin('writer@example.com','incorrect'),false);
 const token=sessionCookie('writer@example.com').split(';')[0];
 assert.equal(getAdmin({headers:{cookie:token}})?.email,'writer@example.com');
 assert.equal(getAdmin({headers:{cookie:token+'fake'}}),null);
});
test('bad article urls are rejected', () => {
 assert.throws(()=>normalizeArticle({title:'abc',content:'abc',slug:'test',status:'published',coverImage:'javascript:alert(1)'}));
 assert.equal(normalizeArticle({title:'نص',content:'محتوى',slug:'test',status:'published'}).title,'نص');
});

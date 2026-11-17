import {test} from 'node:test';
import {strict as assert} from 'node:assert';
import {telegramConfig} from '../server/telegramBot.js';
import {createTelegramPairHandler} from '../server/telegramPair.js';
test('Telegram configuration can use the existing session secret as webhook secret',()=>{
 const t=telegramConfig({TELEGRAM_BOT_TOKEN:'123456789:ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghi',SESSION_SECRET:'x'.repeat(48)});
 assert.equal(t.ready,true);assert.equal(t.secret.length,64);assert.equal(t.admins.size,0);
});
test('Pair management cannot be called without authenticated writer',async()=>{
 const handler=createTelegramPairHandler({sqlFactory:()=>{throw Error('DB must not be reached')}});
 const res={setHeader(){},status(n){this.code=n;return this},json(x){this.value=x;return this}};
 await handler({method:'POST',body:{action:'issue'},headers:{}},res);
 assert.equal(res.code,401);
});
const environment={TELEGRAM_BOT_TOKEN:'123456789:ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghi',SESSION_SECRET:'x'.repeat(48)};
function result(){return {setHeader(){},status(n){this.code=n;return this},json(data){this.data=data;return this}};}
test('Authenticated configuration verifies bot identity, then webhook without exposing secrets',async()=>{
 const calls=[];
 const handler=createTelegramPairHandler({env:environment,authorize:()=>true,originAllowed:()=>true,
  sqlFactory:()=>({query:async()=>[{data:{ids:['111']}}]}),send:async(url,init)=>{
   calls.push({method:url.split('/').at(-1),body:JSON.parse(init.body)});
   const method=calls.at(-1).method;
   return {ok:true,json:async()=>({ok:true,result:method==='getMe'?{id:123456789,is_bot:true,username:'test_bot'}:method==='getWebhookInfo'?{url:'https://motazalqmi.vercel.app/api/v1/telegram',pending_update_count:0}:true})};
  }});
 const res=result();await handler({method:'POST',body:{action:'configure'}},res);
 assert.equal(res.code,200);assert.equal(res.data.webhook.active,true);assert.equal(res.data.linkedAccounts,1);
 assert.deepEqual(calls.map(c=>c.method),['getMe','setWebhook','getWebhookInfo']);
 assert.ok(calls[1].body.secret_token);assert.equal(calls[1].body.drop_pending_updates,false);
 assert.equal(JSON.stringify(res.data).includes(environment.TELEGRAM_BOT_TOKEN),false);
 assert.equal(JSON.stringify(res.data).includes(calls[1].body.secret_token),false);
});
test('Revocation requires explicit confirmation and never calls Telegram',async()=>{
 let writes=0;
 const handler=createTelegramPairHandler({env:environment,authorize:()=>true,originAllowed:()=>true,
  sqlFactory:()=>({query:async()=>{writes++;return[]}}),send:async()=>{throw Error('unexpected')}});
 const denied=result();await handler({method:'POST',body:{action:'revoke'}},denied);assert.equal(denied.code,400);assert.equal(writes,0);
 const confirmed=result();await handler({method:'POST',body:{action:'revoke',confirm:true}},confirmed);assert.equal(confirmed.code,200);assert.equal(writes,1);
});
test('Configuration rejects a webhook mismatch and sanitizes remote failures',async()=>{
 const handler=createTelegramPairHandler({env:environment,authorize:()=>true,originAllowed:()=>true,
  sqlFactory:()=>({query:async()=>[]}),send:async url=>({ok:true,json:async()=>({ok:true,result:url.endsWith('getMe')?{id:123,is_bot:true}:url.endsWith('getWebhookInfo')?{url:'https://invalid.example'}:true})})});
 const res=result();await handler({method:'POST',body:{action:'configure'}},res);assert.equal(res.code,502);
 const broken=createTelegramPairHandler({env:environment,authorize:()=>true,originAllowed:()=>true,sqlFactory:()=>({}),send:async()=>{throw Error(environment.TELEGRAM_BOT_TOKEN)}});
 const failed=result();await broken({method:'POST',body:{action:'verify'}},failed);assert.equal(failed.code,503);assert.ok(!JSON.stringify(failed.data).includes(environment.TELEGRAM_BOT_TOKEN));
});

import {test} from 'node:test';
import {strict as assert} from 'node:assert';
import {telegramCommand,telegramConfig,articleLink,createTelegramHandler} from '../server/telegramBot.js';
const env={TELEGRAM_BOT_TOKEN:'123456789:ABCDEF_abcdefghijklmnopqrstuvwxy',TELEGRAM_WEBHOOK_SECRET:'random-webhook-secret-of-32-chars',TELEGRAM_ADMIN_IDS:'111,222'};
function response(){return {code:0,setHeader(){},status(n){this.code=n;return this},json(v){this.data=v;return this}};}
test('Telegram allowlist and webhook secret required',()=>{
 assert.equal(telegramConfig({}).ready,false);assert.equal(telegramConfig(env).ready,true);
 assert.deepEqual([...telegramConfig(env).admins],['111','222']);
});
test('Telegram parses multiline Arabic and creates stable links',()=>{
 assert.deepEqual(telegramCommand('/new عنوان | محتوى\nسطر'),{name:'new',args:'عنوان | محتوى\nسطر'});
 assert.deepEqual(telegramCommand('/articles@MyBot'),{name:'articles',args:''});
 assert.equal(telegramCommand('not a command'),null);
 assert.equal(articleLink('نص عربي'), 'https://motazalqmi.vercel.app/articles/'+encodeURIComponent('نص عربي'));
});
test('Unauthorized Telegram messages never reach DB',async()=>{
 const h=createTelegramHandler({env,sqlFactory:()=>{throw Error('Unexpected DB access')}});
 const get=response();await h({method:'GET',headers:{}},get);assert.equal(get.code,405);
 const bad=response();await h({method:'POST',headers:{'x-telegram-bot-api-secret-token':'bad'},body:{}},bad);assert.equal(bad.code,403);
 const unauth=response();await h({method:'POST',headers:{'x-telegram-bot-api-secret-token':env.TELEGRAM_WEBHOOK_SECRET},
  body:{message:{from:{id:999},chat:{id:999,type:'private'},text:'/stats'}}},unauth);
 assert.equal(unauth.code,200);
});

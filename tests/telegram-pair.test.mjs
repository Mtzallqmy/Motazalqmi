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

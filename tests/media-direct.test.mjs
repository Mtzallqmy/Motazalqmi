import {test} from 'node:test';
import {strict as assert} from 'node:assert';
import {mediaMagic,nativeMaxBytes,createMediaDirectHandler} from '../server/mediaDirect.js';
test('signature and upload cap protect native attachments',()=>{
 const jpeg=Buffer.from([255,216,255,1,2,3,4,5,6,7,8,9]);
 assert.equal(mediaMagic(jpeg,'image/jpeg'),true);
 assert.equal(mediaMagic(jpeg,'video/mp4'),false);
 assert(nativeMaxBytes<2000000);
});
test('native binary endpoint must be authenticated',async()=>{
 const handler=createMediaDirectHandler({putBlob:async()=>{throw Error('Unexpected blob write')}});
 const r={setHeader(){},status(n){this.code=n;return this},json(x){this.data=x;return this}};
 await handler({method:'POST',headers:{},body:{}},r);
 assert.equal(r.code,401);
});

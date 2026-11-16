import test from 'node:test';
import assert from 'node:assert/strict';
import {telegramAttachment,storeTelegramMedia} from '../server/telegramMedia.js';
const bytes=Buffer.from([255,216,255,224,0,16,74,70,73,70,0,1,0,0,0,0]);
test('Telegram attachments enforce supported MIME and bounded sizes',()=>{
 assert.equal(telegramAttachment({photo:[{file_id:'photo',file_size:16}]}).mime,'image/jpeg');
 assert.throws(()=>telegramAttachment({document:{file_id:'file',file_size:2500000,mime_type:'image/png'}}));
 assert.equal(telegramAttachment({}),null);
});
test('Telegram attachment fetch cannot escape its fixed file origin',async()=>{
 let fetched=false;
 await assert.rejects(()=>storeTelegramMedia({attachment:{fileId:'f'},token:'ephemeral-test',
  getFile:async()=>({file_path:'../unsafe'}),download:async()=>{fetched=true}}));
 assert.equal(fetched,false);
});
test('validated Telegram bytes reach Blob with an allowed filename and MIME',async()=>{
 let saved=false;
 const result=await storeTelegramMedia({attachment:{fileId:'f',mime:'image/jpeg',size:bytes.length},token:'ephemeral-test',
  getFile:async()=>({file_path:'photos/file.jpg'}),
  download:async()=>new Response(bytes,{headers:{'content-length':String(bytes.length)}}),
  upload:async(path,value,options)=>{assert.match(path,/^media\/telegram-.*\.jpg$/);assert.deepEqual(value,bytes);assert.equal(options.contentType,'image/jpeg');saved=true;return {url:'https://media.example/image.jpg'}}});
 assert.equal(saved,true);assert.equal(result,'https://media.example/image.jpg');
});

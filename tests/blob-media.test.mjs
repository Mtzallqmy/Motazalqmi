import {test} from 'node:test';
import {strict as assert} from 'node:assert';
import {validMediaPath,allowedMediaTypes,mediaMaxBytes,createUploadHandler} from '../server/blobUpload.js';
test('only safe media paths can receive Blob upload tokens',()=>{
 assert.equal(validMediaPath('media/picture-123.png'),true);
 assert.equal(validMediaPath('authors/photo.webp'),true);
 assert.equal(validMediaPath('covers/cover.jpg'),true);
 for(const bad of ['media/../password','anything/file.mp4','media/a/b.mp4','media//a.jpg','media/a?.jpg',''])assert.equal(validMediaPath(bad),false);
 assert(allowedMediaTypes.includes('video/mp4'));assert(allowedMediaTypes.includes('application/pdf'));assert(mediaMaxBytes<=100*1024*1024);
});
test('media endpoint requires POST and available storage',async()=>{
 const r={setHeader(){},status(code){this.code=code;return this},json(body){this.body=body;return this}};
 const h=createUploadHandler({handle:async()=>{throw Error('Must not be called')}});
 await h({method:'GET',headers:{}},r);assert.equal(r.code,405);
});

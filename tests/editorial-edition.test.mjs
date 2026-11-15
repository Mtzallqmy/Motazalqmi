import test from 'node:test';
import assert from 'node:assert/strict';
import {articleHtml} from '../server/articleDiscovery.js';
import {contentTypesForPath} from '../shared/mediaTypes.js';
import {normalizeArabic} from '../shared/arabicSearch.js';
import {createTicketHandler} from '../server/mediaDirect.js';
test('reader and server fold the same Arabic characters',()=>assert.equal(normalizeArabic('أَدَبٌ عَـرَبيّ'),'ادب عربي'));
test('document has canonical author and escapes text in JSON and HTML',()=>{
 const html=articleHtml({title:'<عنوان>',slug:'مقالة',author:'كاتبة',excerpt:'<مقتطف>',coverImage:'https://example.org/cover.jpg',publishDate:'2026-11-15'});
 assert(html.includes(encodeURIComponent('مقالة')));assert(html.includes('&lt;عنوان&gt;'));assert(!html.includes('<مقتطف>'));
 const json=JSON.parse(html.match(/application\/ld\+json">(.*?)<\/script>/s)[1]);assert.equal(json[0].author.name,'كاتبة');
});
test('upload permissions bind folder and suffix to a media type',()=>{
 assert.deepEqual(contentTypesForPath('authors/a.mp4'),[]);assert.deepEqual(contentTypesForPath('media/a.pdf'),['application/pdf']);
 assert.deepEqual(contentTypesForPath('covers/a.jpeg'),['image/jpeg']);assert.deepEqual(contentTypesForPath('media/a.html'),[]);
});
test('native upload ticket requires an authenticated writer',async()=>{
 const response={setHeader(){},status(n){this.code=n;return this},json(){}};
 await createTicketHandler(()=>assert.fail())({method:'POST',headers:{}},response);assert.equal(response.code,401);
});

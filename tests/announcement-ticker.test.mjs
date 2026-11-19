import {test} from 'node:test';
import {strict as assert} from 'node:assert';
import {DEFAULT_TICKER, normalizeTicker, safeTickerLink, tickerForAudience} from '../shared/announcementTicker.js';
import {editorialSections, SECTIONS} from '../shared/publicContent.js';

test('ticker validates links, scheduling speed, and plain messages', () => {
  for (const url of ['//elsewhere.test', '/\\elsewhere.test','javascript:alert(1)','http://elsewhere.test','https://user:pw@example.com','/\n/path']) {
    assert.equal(safeTickerLink(url), null);
  }
  const ticker = normalizeTicker({enabled:true, intervalSeconds:5, items:[{kind:'announcement',text:' إشعار  جديد ',link:'/articles/new'}]});
  assert.deepEqual(ticker.items,[{kind:'announcement',text:'إشعار جديد',link:'/articles/new'}]);
  assert.throws(() => normalizeTicker({...ticker,intervalSeconds:1}));
  assert.throws(() => normalizeTicker({...ticker,items:[]}));
});

test('disabled announcements stay private and legacy sections keep their labels', () => {
  const disabled = {...DEFAULT_TICKER,enabled:false};
  assert.deepEqual(tickerForAudience(disabled).items, []);
  assert.deepEqual(tickerForAudience(disabled,true), disabled);
  const sections = editorialSections([{id:'general',title:'عنوان محفوظ'}]);
  assert.equal(sections.find(section=>section.id==='general').title,'عنوان محفوظ');
  assert.deepEqual(sections.map(section=>section.id),SECTIONS.map(section=>section.id));
});

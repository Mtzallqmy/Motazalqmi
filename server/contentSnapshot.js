import { createHash } from 'node:crypto';
import { database, articleRow } from './db.js';
import { SITE, SECTIONS, QUOTES } from '../shared/publicContent.js';

export function normalizeArabic(value='') {
  return String(value).normalize('NFKD').replace(/[\u064b-\u065f\u0670\u0640]/g,'').replace(/[أإآٱ]/g,'ا').replace(/ى/g,'ي').toLocaleLowerCase('ar');
}
export async function publishedSnapshot(sql=database()) {
  // A single PostgreSQL statement sees one MVCC snapshot, including all article bodies.
  const rows=await sql`SELECT id,slug,status,publish_date::text,updated_at::text,data
    FROM blog_articles WHERE status='published'
    ORDER BY publish_date DESC,id ASC LIMIT 10001`;
  if(rows.length>10000)throw new Error('Published catalogue exceeds sync limit');
  const articles=rows.map(row=>({...articleRow(row),updatedAt:row.updated_at}));
  const revision=createHash('sha256').update(JSON.stringify({articles,site:SITE,sections:SECTIONS,quotes:QUOTES})).digest('hex');
  return {articles,revision};
}
export function pageSnapshot(snapshot,{limit=30,offset=0,category='',q='',featured=false,full=false}={}) {
  const needle=normalizeArabic(q);
  const found=snapshot.articles.filter(a=>(!category||a.category===category)&&(!featured||a.featured)&&(!needle||normalizeArabic([a.title,a.excerpt,a.content,a.author,...(a.tags||[]),SECTIONS.find(s=>s.id===a.category)?.title].join(' ')).includes(needle)));
  return {articles:found.slice(offset,offset+limit).map(a=>{const copy={...a};if(!full)delete copy.content;return copy;}),total:found.length,offset,limit,hasMore:offset+limit<found.length,revision:snapshot.revision};
}
export const syncManifest=snapshot=>({apiVersion:1,revision:snapshot.revision,authoritative:true,ids:snapshot.articles.map(a=>a.id),total:snapshot.articles.length});

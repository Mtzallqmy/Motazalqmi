import { createHash } from 'node:crypto';
import { database, articleRow } from './db.js';
import { SITE, SECTIONS, QUOTES } from '../shared/publicContent.js';

// One PostgreSQL statement gives all rows and metadata a single MVCC snapshot.
// Fail closed on size limits: a partial page must NEVER authorize local deletion.
export async function publishedSnapshot(sql = database()) {
 const [result] = await sql`WITH published AS (
   SELECT id,slug,status,publish_date::text,data,updated_at FROM blog_published_articles
   WHERE status='published' ORDER BY publish_date DESC,id LIMIT 2001
  ) SELECT COALESCE(jsonb_agg(to_jsonb(published) ORDER BY publish_date DESC,id),'[]'::jsonb) AS articles,
  (SELECT data FROM blog_public_metadata WHERE id='editorial') AS metadata,
  (SELECT COALESCE(jsonb_agg(jsonb_build_object(
     'id',data->>'id','name',data->>'name','bio',coalesce(data->>'bio',''),
     'image',coalesce(data->>'image',''),'link',coalesce(data->>'link','')) ORDER BY id),'[]'::jsonb)
   FROM blog_public_metadata WHERE id LIKE 'author:%' AND data->>'id' IS NOT NULL AND data->>'name' IS NOT NULL) AS authors
  FROM published`;
 const rows = result.articles;
 if (!Array.isArray(rows) || rows.length > 2000) throw new Error('Snapshot limit exceeded');
 const metadata = result.metadata || {site:SITE,sections:SECTIONS,quotes:QUOTES};
 const payload = {apiVersion:1,mode:'snapshot',complete:true,
  site:metadata.site, sections:metadata.sections, quotes:metadata.quotes, authors:result.authors || [],
  articles:rows.map(r=>({...articleRow(r),updatedAt:r.updated_at})),total:rows.length};
 if (Buffer.byteLength(JSON.stringify(payload)) > 3 * 1024 * 1024) throw new Error('Snapshot response too large');
 payload.revision=createHash('sha256').update(JSON.stringify(payload)).digest('hex');
 return payload;
}

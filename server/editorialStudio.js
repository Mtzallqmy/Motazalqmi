import { randomUUID } from 'node:crypto';
import { list } from '@vercel/blob';
import { getAdmin, checkOrigin, noCache } from './auth.js';
import { database } from './db.js';

const idPattern = /^[a-f0-9-]{36}$/i;
function publicUrl(value) {
  if (!value) return '';
  if (typeof value !== 'string' || value.length > 2048) throw Error('INPUT');
  const u = new URL(value);
  if (u.protocol !== 'https:' || u.username || u.password) throw Error('INPUT');
  return u.href;
}
export async function studio(req, res, resource) {
  noCache(res);
  const admin = getAdmin(req);
  const write = req.method === 'POST';
  if (!['GET','POST'].includes(req.method)) return res.status(405).end();
  if ((resource !== 'authors' || write) && !admin) return res.status(401).json({error:'تسجيل الدخول مطلوب.'});
  if (write && !checkOrigin(req)) return res.status(403).end();
  try {
    const sql = database();
    if (resource === 'revisions' && !write) {
      const rows = await sql`SELECT revision_id::text AS id,saved_at AS "savedAt",data FROM blog_article_revisions
        WHERE article_id=${String(req.query.id || '').slice(0,150)} ORDER BY revision_id DESC LIMIT 12`;
      return res.status(200).json({revisions:rows});
    }
    if (resource === 'assets' && !write) {
      const cursor = req.query.cursor;
      if (cursor && (typeof cursor !== 'string' || cursor.length > 2048)) return res.status(400).end();
      const page = await list({limit:40, cursor});
      return res.status(200).json({items:page.blobs.map(({url,pathname,size,uploadedAt}) => ({url,pathname,size,uploadedAt})),cursor:page.cursor || null});
    }
    if (resource !== 'authors') return res.status(405).end();
    if (!write) {
      const rows = await sql`SELECT data FROM blog_public_metadata WHERE id LIKE 'author:%' ORDER BY data->>'name'`;
      return res.status(200).json({authors:rows.map(r => r.data)});
    }
    const {id=randomUUID(), name, bio='', image='', link=''} = req.body || {};
    if (!idPattern.test(id) || typeof name !== 'string' || !name.trim() || name.length>150 || typeof bio!=='string' || bio.length>3000) throw Error('INPUT');
    const author = {id, name:name.trim(), bio:bio.trim(), image:publicUrl(image), link:publicUrl(link)};
    await sql`INSERT INTO blog_public_metadata (id,data) VALUES (${'author:'+id},${JSON.stringify(author)}::jsonb)
      ON CONFLICT (id) DO UPDATE SET data=EXCLUDED.data,updated_at=NOW()`;
    return res.status(200).json({author});
  } catch (e) {
    if (e.message==='INPUT'||e.code==='ERR_INVALID_URL') return res.status(400).json({error:'تحقق من بيانات الكاتب وروابط HTTPS.'});
    return res.status(503).json({error:'تعذر تحميل بيانات الاستوديو.'});
  }
}

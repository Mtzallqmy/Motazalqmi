import { getDb } from './_lib/db.js';
import { sendJson, sendError } from './_lib/http.js';

export default async function handler(req, res) {
  const since = req.query?.since ? new Date(String(req.query.since)) : null;
  if (since && Number.isNaN(since.getTime())) {
    return sendError(req, res, 400, 'INVALID_SINCE', 'since must be a valid ISO-8601 timestamp');
  }

  try {
    const sql = getDb();
    const sinceIso = since ? since.toISOString() : '1970-01-01T00:00:00.000Z';

    const [articles, deletedArticles, quotes, deletedQuotes, categories] = await Promise.all([
      sql`SELECT id, slug, title, excerpt, content_markdown, cover_image_url, author_name,
                 category_id, category_name, tags, reading_time_minutes, published_at, updated_at, is_featured
          FROM articles
          WHERE status = 'published' AND deleted_at IS NULL AND updated_at > ${sinceIso}
          ORDER BY updated_at ASC`,
      sql`SELECT id FROM articles WHERE (deleted_at IS NOT NULL OR status <> 'published') AND updated_at > ${sinceIso}`,
      sql`SELECT id, text, author, source, category_id, updated_at FROM quotes
          WHERE status = 'published' AND deleted_at IS NULL AND updated_at > ${sinceIso}`,
      sql`SELECT id FROM quotes WHERE (deleted_at IS NOT NULL OR status <> 'published') AND updated_at > ${sinceIso}`,
      sql`SELECT id, slug, name_ar, description, sort_order FROM categories ORDER BY sort_order ASC`
    ]);

    return sendJson(req, res, {
      data: {
        articles,
        deletedArticleIds: deletedArticles.map((r) => r.id),
        quotes,
        deletedQuoteIds: deletedQuotes.map((r) => r.id),
        categories,
        serverTime: new Date().toISOString()
      },
      cacheSeconds: 0
    });
  } catch (err) {
    return sendError(req, res, 500, 'SYNC_ERROR', 'Incremental sync failed');
  }
}

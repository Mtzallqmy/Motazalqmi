import { getDb } from './_lib/db.js';
import { sendJson, sendError } from './_lib/http.js';

export default async function handler(req, res) {
  try {
    const sql = getDb();
    await sql`SELECT 1 AS ok`;
    return sendJson(req, res, { data: { status: 'healthy', database: 'connected', project: 'motazalqmi-blog' }, cacheSeconds: 15 });
  } catch (err) {
    return sendError(req, res, 503, 'DB_UNAVAILABLE', 'Database health check failed');
  }
}

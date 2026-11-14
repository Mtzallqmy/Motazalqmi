import crypto from 'node:crypto';

export const API_VERSION = '1.0.0';

export function sendJson(req, res, { status = 200, data = null, meta = null, error = null, cacheSeconds = 60 } = {}) {
  const payload = {
    ok: !error,
    version: API_VERSION,
    data,
    meta: { ...(meta || {}), serverTime: new Date().toISOString() },
    error
  };

  const body = JSON.stringify(payload);
  const etag = '"' + crypto.createHash('sha1').update(body).digest('hex') + '"';

  res.setHeader('Content-Type', 'application/json; charset=utf-8');
  res.setHeader('X-API-Version', API_VERSION);
  res.setHeader('ETag', etag);
  res.setHeader('Cache-Control', cacheSeconds > 0
    ? `public, s-maxage=${cacheSeconds}, stale-while-revalidate=${cacheSeconds * 2}`
    : 'no-store');

  if (req.headers['if-none-match'] === etag && status === 200) {
    return res.status(304).end();
  }
  return res.status(status).send(body);
}

export function sendError(req, res, status, code, message) {
  return sendJson(req, res, { status, error: { code, message }, cacheSeconds: 0 });
}

export function parsePagination(query) {
  const page = Math.max(1, parseInt(query?.page || '1', 10) || 1);
  const limit = Math.min(50, Math.max(1, parseInt(query?.limit || '20', 10) || 20));
  const offset = (page - 1) * limit;
  return { page, limit, offset };
}

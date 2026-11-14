import { sendJson } from './_lib/http.js';

export default function handler(req, res) {
  return sendJson(req, res, {
    data: {
      appNameAr: 'مدونة معتز العلقمي',
      appNameEn: 'Motaz Alalqami',
      siteUrl: 'https://motazalqmi.vercel.app',
      copyright: 'برمجة وتطوير: معتز العلقمي — تعز — اليمن — 2026م'
    },
    cacheSeconds: 600
  });
}

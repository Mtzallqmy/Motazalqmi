import { database, articleRow } from './db.js';
import { publicGet, publicError } from './publicApi.js';

const origin = 'https://motazalqmi.vercel.app';
const escape = v => String(v ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const articleUrl = slug => origin + '/articles/' + encodeURIComponent(slug);
export function articleHtml(article) {
  const title = escape(article.title), summary = escape(article.excerpt);
  const url = articleUrl(article.slug), image = /^https:\/\//i.test(article.coverImage || '') ? article.coverImage : '';
  const structured = [
    {'@context':'https://schema.org','@type':'BlogPosting',headline:article.title,description:article.excerpt,
      inLanguage:'ar',mainEntityOfPage:url,datePublished:article.publishDate,dateModified:article.updatedAt,
      ...(image && {image}),...(article.author && {author:{'@type':'Person',name:article.author}})},
    {'@context':'https://schema.org','@type':'BreadcrumbList',itemListElement:[
      {'@type':'ListItem',position:1,name:'مدونة معتز العلقمي',item:origin+'/'},
      {'@type':'ListItem',position:2,name:article.title,item:url}]}
  ];
  return `<!doctype html><html lang="ar" dir="rtl"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>${title} | مدونة معتز العلقمي</title><meta name="description" content="${summary}"><link rel="canonical" href="${url}">
<meta property="og:locale" content="ar_YE"><meta property="og:type" content="article"><meta property="og:site_name" content="مدونة معتز العلقمي">
<meta property="og:title" content="${title}"><meta property="og:description" content="${summary}"><meta property="og:url" content="${url}">
${image ? '<meta property="og:image" content="'+escape(image)+'">' : ''}<meta name="twitter:card" content="${image?'summary_large_image':'summary'}">
<link rel="icon" href="/favicon.svg"><script type="application/ld+json">${JSON.stringify(structured).replace(/</g,'\\u003c')}</script>
<script type="module" src="/article-entry.js"></script></head><body><div id="root"><article><h1>${title}</h1>
<p>${escape(article.author)}</p><p>${summary}</p></article></div></body></html>`;
}
export async function discovery(req,res,resource) {
  if (!publicGet(req,res)) return;
  try {
    const sql = database();
    if (resource === 'sitemap') {
      const rows = await sql`SELECT slug,updated_at::text FROM blog_articles WHERE status='published' ORDER BY id`;
      res.setHeader('Content-Type','application/xml; charset=utf-8');
      res.setHeader('Cache-Control','public,max-age=0,s-maxage=60');
      const urls = ['<url><loc>'+origin+'/</loc></url>', ...rows.map(r => '<url><loc>'+escape(articleUrl(r.slug))+'</loc><lastmod>'+new Date(r.updated_at).toISOString()+'</lastmod></url>')];
      return res.status(200).send(req.method==='HEAD'?'':'<?xml version="1.0" encoding="UTF-8"?><urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">'+urls.join('')+'</urlset>');
    }
    const slug = req.query?.slug;
    if (typeof slug !== 'string' || !slug.length || slug.length > 150) return res.status(404).end();
    const [row] = await sql`SELECT a.id,a.slug,a.status,a.publish_date::text,a.updated_at::text,a.data
      FROM blog_articles a WHERE a.status='published' AND (a.slug=${slug} OR a.id=${slug} OR a.id=(
      SELECT article_id FROM blog_article_slugs WHERE slug=${slug})) ORDER BY (a.slug=${slug}) DESC LIMIT 1`;
    if (!row) {res.setHeader('Cache-Control','no-store');return res.status(404).send('المقالة غير متاحة');}
    if (row.slug !== slug) {res.setHeader('Location',articleUrl(row.slug));return res.status(308).end();}
    res.setHeader('Cache-Control','public,max-age=0,must-revalidate');
    res.setHeader('Content-Type','text/html; charset=utf-8');
    return res.status(200).send(req.method==='HEAD'?'':articleHtml(articleRow(row)));
  } catch (error) { return publicError(res,error); }
}

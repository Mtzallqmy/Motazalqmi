export const AUTHOR = 'معتز العلقمي';
export const DEFAULT_IMAGE = 'https://images.unsplash.com/photo-1455390582262-044cdead277a?auto=format&fit=crop&w=1200&q=85';
export const formatDate = date => date ? new Intl.DateTimeFormat('ar', { dateStyle: 'medium' }).format(new Date(`${date}T12:00:00`)) : '';
export const slugify = text => (text || '').trim().toLowerCase().replace(/[^\p{L}\p{N}\s-]/gu, '').replace(/\s+/g, '-').replace(/-+/g, '-').slice(0, 120) || `article-${Date.now()}`;
export const articlePath = article => `/articles/${encodeURIComponent(article.slug || article.id)}`;
export const nextId = () => `art-${globalThis.crypto?.randomUUID?.() || Date.now()}`;

export const publicationUrl = article => window.location.origin + (/^[0-9a-f]{16}$/.test(article.shortCode || '') ? '/r/'+article.shortCode : articlePath(article));

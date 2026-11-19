export const TICKER_LIMIT = 8;
export const TICKER_INTERVALS = [5, 8, 12, 16];
export const TICKER_LABELS = { announcement:'إعلان', update:'جديد المدونة', phrase:'عبارة مختارة' };
export const DEFAULT_TICKER = Object.freeze({
  enabled: true,
  intervalSeconds: 8,
  items: [{
    kind: 'update',
    text: 'أبواب تجمع الأدب والتجارب الإنسانية والثقافة والدين واللغة العربية. تصفّح أحدث الكتابات.',
    link: '/#latest'
  }]
});

export function safeTickerLink(value) {
  if (typeof value !== 'string' || value.length > 2048 || /[\\\u0000-\u001f\u007f]/.test(value)) return null;
  const link = value.trim();
  if (!link) return '';
  if (link.startsWith('/') && !link.startsWith('//')) return link;
  try {
    const url = new URL(link);
    return url.protocol === 'https:' && !url.username && !url.password ? url.href : null;
  } catch {
    return null;
  }
}

export function normalizeTicker(value) {
  if (typeof value?.enabled !== 'boolean' || !TICKER_INTERVALS.includes(value.intervalSeconds) ||
      !Array.isArray(value.items) || value.items.length > TICKER_LIMIT || (value.enabled && !value.items.length)) {
    throw new Error('إعدادات شريط الإعلانات غير صالحة. أضف عبارة قبل إظهاره.');
  }
  const items = value.items.map(item => {
    const link = safeTickerLink(item?.link ?? '');
    if (!Object.hasOwn(TICKER_LABELS, item?.kind) || typeof item?.text !== 'string' ||
        !item.text.trim() || item.text.length > 200 || link === null) {
      throw new Error('حقل الإعلان غير صالح. اكتب عبارة حتى 200 حرف، واستخدم رابط HTTPS أو رابطًا داخليًا.');
    }
    return {kind:item.kind, text:item.text.trim().replace(/\s+/g, ' '), link};
  });
  return {enabled:value.enabled, intervalSeconds:value.intervalSeconds, items};
}

export function tickerForAudience(stored, admin = false) {
  let ticker;
  try { ticker = stored == null ? DEFAULT_TICKER : normalizeTicker(stored); }
  catch { return {enabled:false, intervalSeconds:8, items:[]}; }
  return ticker.enabled || admin ? ticker : {...ticker, items:[]};
}

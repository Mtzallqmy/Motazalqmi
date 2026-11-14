import { useEffect, useState } from 'react';
const KEY = 'moataz-blog-bookmarks-v1';
function read() { try { const value = JSON.parse(localStorage.getItem(KEY) || '[]'); return Array.isArray(value) ? value : []; } catch { return []; } }
export function useBookmarks() {
  const [ids, setIds] = useState(read);
  useEffect(() => { try { localStorage.setItem(KEY, JSON.stringify(ids)); } catch { /* private mode */ } }, [ids]);
  const toggle = id => setIds(prev => prev.includes(id) ? prev.filter(v => v !== id) : [...prev, id]);
  return { ids, toggle };
}

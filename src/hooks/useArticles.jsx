import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { INITIAL_ARTICLES } from '../data/articles';
import { useAuth } from '../context/AuthContext';
import { api } from '../lib/api';
const Context = createContext(null);
export function ArticlesProvider({ children }) {
  const { isAdmin, loading: authLoading } = useAuth();
  const [articles, setArticles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [isDemo, setIsDemo] = useState(false);
  const load = async (admin) => {
    setLoading(true);
    try {
      const { articles: list } = await api(admin ? '/api/articles?scope=admin' : '/api/articles');
      setArticles(list.length ? list : INITIAL_ARTICLES);
      setIsDemo(list.length === 0);
      setError('');
    } catch(e) {
      setArticles(INITIAL_ARTICLES);
      setIsDemo(true);
      setError(`تعذّر تحميل المقالات من Neon: ${e.message}`);
    } finally { setLoading(false); }
  };
  useEffect(() => { if (!authLoading) load(isAdmin); }, [isAdmin, authLoading]);
  const save = async item => {
    if (!isAdmin) throw new Error('يتطلب الحفظ صلاحية مدير.');
    const result = await api('/api/articles', { method:'POST', body:JSON.stringify({action:'save',article:item}) });
    await load(true);
    return result.id;
  };
  const remove = async id => {
    if (!isAdmin) throw new Error('ليس لديك صلاحية الحذف.');
    await api('/api/articles', { method:'POST', body:JSON.stringify({action:'delete',id}) });
    await load(true);
  };
  const importExamples = async () => {
    if (!isAdmin || !isDemo) throw new Error('الاستيراد متاح للمدير إذا كانت القاعدة خالية.');
    await api('/api/articles', { method:'POST',body:JSON.stringify({action:'import',articles:INITIAL_ARTICLES}) });
    await load(true);
  };
  const published = useMemo(() => articles.filter(a => a.status === 'published'), [articles]);
  return <Context.Provider value={{ articles, published, loading, error, isDemo, save, remove, importExamples }}>{children}</Context.Provider>;
}
export const useArticles = () => useContext(Context);

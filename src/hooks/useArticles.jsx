import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { collection, deleteDoc, doc, onSnapshot, query, setDoc, where } from 'firebase/firestore';
import { INITIAL_ARTICLES } from '../data/articles';
import { db, isCloudConfigured } from '../lib/firebase';
import { useAuth } from '../context/AuthContext';
import { nextId } from '../lib/format';
const Context = createContext(null);
export function ArticlesProvider({ children }) {
  const { isAdmin, loading: authLoading } = useAuth();
  const [articles, setArticles] = useState(INITIAL_ARTICLES);
  const [loading, setLoading] = useState(isCloudConfigured);
  const [error, setError] = useState('');
  const [isDemo, setIsDemo] = useState(!isCloudConfigured);
  useEffect(() => {
    if (!db || authLoading) { setLoading(false); return; }
    setLoading(true);
    const ref = collection(db, 'articles');
    const collectionQuery = isAdmin ? ref : query(ref, where('status', '==', 'published'));
    return onSnapshot(collectionQuery, snapshot => {
      const results = snapshot.docs.map(d => ({ ...d.data(), id: d.id }));
      setIsDemo(results.length === 0);
      setArticles(results.length ? results.sort((a,b) => (b.publishDate || '').localeCompare(a.publishDate || '')) : INITIAL_ARTICLES);
      setLoading(false); setError('');
    }, err => { console.error(err); setError('تعذر تحميل المقالات من قاعدة البيانات.'); setLoading(false); });
  }, [isAdmin, authLoading]);
  const save = async (item) => {
    if (!isAdmin || !db) throw new Error('يتطلب الحفظ صلاحية المدير واتصال Firebase.');
    const id = item.id || nextId();
    await setDoc(doc(db, 'articles', id), { ...item, id });
    return id;
  };
  const remove = async id => {
    if (!isAdmin || !db) throw new Error('ليس لديك صلاحية الحذف.');
    await deleteDoc(doc(db, 'articles', id));
  };
  const importExamples = async () => {
    if (!isAdmin || !db) throw new Error('الاستيراد متاح للمدير فقط.');
    if (!isDemo) throw new Error('توجد مقالات في قاعدة البيانات.');
    for (const article of INITIAL_ARTICLES) await setDoc(doc(db, 'articles', article.id), article);
  };
  const published = useMemo(() => articles.filter(a => a.status === 'published'), [articles]);
  return <Context.Provider value={{ articles, published, loading, error, isDemo, save, remove, importExamples }}>{children}</Context.Provider>;
}
export const useArticles = () => useContext(Context);

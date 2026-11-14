import { createContext, useContext, useEffect, useState } from 'react';
import { onAuthStateChanged, signInWithEmailAndPassword, signOut } from 'firebase/auth';
import { doc, getDoc } from 'firebase/firestore';
import { auth, db } from '../lib/firebase';
const Context = createContext(null);
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isAdmin, setIsAdmin] = useState(false);
  const [loading, setLoading] = useState(Boolean(auth));
  useEffect(() => {
    if (!auth || !db) return;
    let seq = 0;
    return onAuthStateChanged(auth, async nextUser => {
      const current = ++seq;
      setLoading(true);
      setUser(nextUser);
      setIsAdmin(false);
      try {
        if (nextUser) {
          const record = await getDoc(doc(db, 'admins', nextUser.uid));
          if (current === seq) setIsAdmin(record.exists() && record.data()?.enabled === true);
        }
      } catch (error) { console.warn('Admin role lookup failed', error); }
      if (current === seq) setLoading(false);
    });
  }, []);
  const login = async (email, password) => {
    if (!auth) throw new Error('Firebase غير مُعدّ. أضف إعداداته إلى ملف .env');
    await signInWithEmailAndPassword(auth, email, password);
  };
  const logout = () => auth ? signOut(auth) : Promise.resolve();
  return <Context.Provider value={{ user, isAdmin, loading, login, logout }}>{children}</Context.Provider>;
}
export const useAuth = () => useContext(Context);

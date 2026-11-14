import { createContext, useContext, useEffect, useState } from 'react';
import { api } from '../lib/api';
const Context = createContext(null);
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    let active = true;
    api('/api/session').then(d => { if (active) setUser(d.user || null); }).catch(() => { if (active) setUser(null); }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);
  const login = async (email, password) => {
    const d = await api('/api/session', { method:'POST', body: JSON.stringify({email, password}) });
    setUser(d.user);
  };
  const logout = async () => {
    try { await api('/api/session', { method:'DELETE' }); } finally { setUser(null); }
  };
  return <Context.Provider value={{ user, isAdmin: Boolean(user), loading, login, logout }}>{children}</Context.Provider>;
}
export const useAuth = () => useContext(Context);

import {createContext,useContext,useEffect,useMemo,useState,useCallback} from 'react';
import {INITIAL_ARTICLES} from '../data/articles';
import {useAuth} from '../context/AuthContext';
import {api} from '../lib/api';
import {SITE} from '../../shared/publicContent.js';
const Context=createContext(null);
export function ArticlesProvider({children}){
 const {isAdmin,loading:authLoading}=useAuth();
 const [quotes,setQuotes]=useState([]);
 const [site,setSite]=useState(SITE);
 const [articles,setArticles]=useState([]),[loading,setLoading]=useState(true),[error,setError]=useState(''),[isDemo,setIsDemo]=useState(false);
 const load=useCallback(async (admin,quiet=false)=>{
  if(!quiet)setLoading(true);
  try{
   const r=await api(admin?'/api/articles?scope=admin':'/api/articles');
   setArticles(r.articles);setQuotes(r.editorial?.quotes || []);setSite(r.editorial?.site || SITE);setIsDemo(!r.articles.length);setError('');
  }catch(e){if(!quiet){setArticles([]);setQuotes([]);setIsDemo(true);}setError('تعذّر تحميل المقالات من Neon: '+e.message);}
  finally{if(!quiet)setLoading(false);}
 },[]);
 useEffect(()=>{if(!authLoading)load(isAdmin);},[isAdmin,authLoading,load]);
 // Refresh promptly after edits from Android/Telegram while preserving browser drafts.
 useEffect(()=>{
  if(authLoading)return;
  const refresh=()=>{if(document.visibilityState==='visible')load(isAdmin,true);};
  const timer=window.setInterval(refresh,45_000);
  document.addEventListener('visibilitychange',refresh);
  window.addEventListener('focus',refresh);
  return()=>{window.clearInterval(timer);document.removeEventListener('visibilitychange',refresh);window.removeEventListener('focus',refresh);};
 },[authLoading,isAdmin,load]);
 const save=async (article,expectedVersion)=>{if(!isAdmin)throw new Error('صلاحية المدير مطلوبة.');const r=await api('/api/articles',{method:'POST',body:JSON.stringify({action:'save',article,expectedVersion})});await load(true);return r.id;};
 const remove=async id=>{if(!isAdmin)throw new Error('صلاحية المدير مطلوبة.');await api('/api/articles',{method:'POST',body:JSON.stringify({action:'delete',id})});await load(true);};
 const importExamples=async()=>{if(!isAdmin||!isDemo)throw new Error('الاستيراد غير متاح.');await api('/api/articles',{method:'POST',body:JSON.stringify({action:'import',articles:INITIAL_ARTICLES})});await load(true);};
 const saveEditorial=async (tagline,about,editedQuotes)=>{
  if(!isAdmin)throw new Error('صلاحية المدير مطلوبة.');
  await api('/api/articles',{method:'POST',body:JSON.stringify({action:'editorial',editorial:{tagline,about,quotes:editedQuotes}})});
  await load(true);
 };
 const published=useMemo(()=>articles.filter(a=>a.status==='published'),[articles]);
 return <Context.Provider value={{articles,published,quotes,site,loading,error,isDemo,save,remove,importExamples,saveEditorial}}>{children}</Context.Provider>;
}
export const useArticles=()=>useContext(Context);

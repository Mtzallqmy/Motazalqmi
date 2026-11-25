import {DEFAULT_TICKER} from '../../shared/announcementTicker';
import {isScheduled} from '../../shared/publication';
import {createContext,useContext,useEffect,useMemo,useState,useCallback,useRef} from 'react';
import {INITIAL_ARTICLES} from '../data/articles';
import {useAuth} from '../context/AuthContext';
import {api} from '../lib/api';
import {SITE} from '../../shared/publicContent.js';
const Context=createContext(null);
export function ArticlesProvider({children}){
 const {isAdmin,loading:authLoading}=useAuth();
 const [quotes,setQuotes]=useState([]);
 const [site,setSite]=useState(SITE);
 const [ticker,setTicker]=useState(DEFAULT_TICKER);
 const [articles,setArticles]=useState([]),[loading,setLoading]=useState(true),[error,setError]=useState(''),[isDemo,setIsDemo]=useState(false);
 const scope=isAdmin&&!authLoading;
 const active=useRef(null);
 const currentScope=useRef(scope);
 currentScope.current=scope;
 const [loadedScope,setLoadedScope]=useState(null);
 const load=useCallback(async (admin,quiet=false)=>{
  if(admin!==currentScope.current || (quiet && active.current))return;
  active.current?.abort();
  const controller=new AbortController();
  active.current=controller;
  const current=()=>!controller.signal.aborted && currentScope.current===admin;
  if(!quiet)setLoading(true);
  try{
   const r=await api(admin?'/api/articles?scope=admin':'/api/articles',{signal:controller.signal});
   if(!current())return;
   setArticles(r.articles);setQuotes(r.editorial?.quotes || []);setSite(r.editorial?.site || SITE);setTicker(r.ticker || DEFAULT_TICKER);setLoadedScope(admin);setIsDemo(!r.articles.length);setError('');
  }catch{
   if(!current())return;
   setError('تعذّر تحميل المقالات حاليًا. أعد المحاولة بعد قليل.');
  }
  finally{
   if(active.current===controller){active.current=null;setLoading(false);}
  }
 },[]);
 useEffect(()=>{
  setArticles([]);setLoadedScope(null);setIsDemo(false);setTicker(DEFAULT_TICKER);setQuotes([]);
  load(scope);
  return()=>active.current?.abort();
 },[scope,load]);
 // Refresh promptly after edits from Android/Telegram while preserving browser drafts.
 useEffect(()=>{
  const refresh=()=>{if(document.visibilityState==='visible')load(scope,true);};
  const timer=window.setInterval(refresh,45_000);
  document.addEventListener('visibilitychange',refresh);
  window.addEventListener('focus',refresh);
  return()=>{window.clearInterval(timer);document.removeEventListener('visibilitychange',refresh);window.removeEventListener('focus',refresh);};
 },[scope,load]);
 const save=async (article,expectedVersion)=>{if(!isAdmin)throw new Error('صلاحية المدير مطلوبة.');const r=await api('/api/articles',{method:'POST',body:JSON.stringify({action:'save',article,expectedVersion})});await load(true);return r;};
 const remove=async id=>{if(!isAdmin)throw new Error('صلاحية المدير مطلوبة.');await api('/api/articles',{method:'POST',body:JSON.stringify({action:'delete',id})});await load(true);};
 const importExamples=async()=>{if(!isAdmin||!isDemo)throw new Error('الاستيراد غير متاح.');await api('/api/articles',{method:'POST',body:JSON.stringify({action:'import',articles:INITIAL_ARTICLES})});await load(true);};
 const saveEditorial=async (tagline,about,editedQuotes)=>{
  if(!isAdmin)throw new Error('صلاحية المدير مطلوبة.');
  await api('/api/articles',{method:'POST',body:JSON.stringify({action:'editorial',editorial:{tagline,about,quotes:editedQuotes}})});
  await load(true);
 };
 const saveTicker=async ticker=>{
  if(!isAdmin)throw new Error('صلاحية المدير مطلوبة.');
  const result=await api('/api/articles',{method:'POST',body:JSON.stringify({action:'announcement-ticker',ticker})});
  setTicker(result.ticker);
 };
 const visibleArticles=loadedScope===scope?articles:[];
 const published=useMemo(()=>visibleArticles.filter(a=>a.status==='published'&&!isScheduled(a)),[visibleArticles]);
 return <Context.Provider value={{articles:visibleArticles,published,quotes,site,ticker:loadedScope===scope?ticker:DEFAULT_TICKER,saveTicker,loading,error,isDemo,save,remove,importExamples,saveEditorial}}>{children}</Context.Provider>;
}
export const useArticles=()=>useContext(Context);

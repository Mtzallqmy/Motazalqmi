import {useEffect, useRef, useState} from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import {ArticleMedia} from './ArticleMedia';

const options = {
  family:['editor','amiri','naskh','plex'], size:['editor','18','20','22','24','28','32'],
  line:['1.6','1.9','2.2','2.5'], space:['0.6','1.2','1.8'], width:['34','44','54'],
  align:['start','justify','center'], theme:['paper','night'], focus:['off','on']
};
const defaults = {family:'editor',size:'editor',line:'1.9',space:'1.2',width:'44',align:'start',theme:'paper',focus:'off'};
const labels = {family:'الخط',size:'حجم الخط',line:'تباعد السطور',space:'تباعد الفقرات',width:'عرض النص',align:'المحاذاة',theme:'المظهر',focus:'وضع التركيز'};
const names = {editor:'اختيار الكاتب',amiri:'أميري',naskh:'نسخ',plex:'بلكس',start:'يمين',justify:'ضبط',center:'توسيط',paper:'ورقي',night:'ليلي',on:'تشغيل',off:'إيقاف'};
function readPrefs(){
  let raw;try{raw=JSON.parse(localStorage.getItem('motaz-reader-preferences'));}catch{}
  return Object.fromEntries(Object.keys(options).map(k=>[k,options[k].includes(raw?.[k])?raw[k]:defaults[k]]));
}
export function ReaderText({article}) {
  const [prefs,setPrefs]=useState(readPrefs);
  const [headings,setHeadings]=useState([]);
  const [progress,setProgress]=useState(0);
  const body=useRef(null);
  useEffect(()=>{
    const nodes=Array.from(body.current.querySelectorAll('h1,h2,h3,h4'));
    setHeadings(nodes.map((el,i)=>{if(!el.id)el.id='reading-section-'+(i+1);return {id:el.id,title:el.textContent};}));
  },[article.content]);
  useEffect(()=>{
    const panel=body.current.closest('.reader-surface');
    const measure=()=>{
      const rect=body.current.getBoundingClientRect();
      const fraction=(window.innerHeight-rect.top)/Math.max(1,rect.height);
      setProgress(Math.round(Math.min(1,Math.max(0,fraction))*100));
    };
    const observer=new ResizeObserver(measure);observer.observe(body.current);
    window.addEventListener('scroll',measure,{passive:true});panel.addEventListener('scroll',measure,{passive:true});
    window.addEventListener('resize',measure);measure();
    return ()=>{observer.disconnect();window.removeEventListener('scroll',measure);panel.removeEventListener('scroll',measure);window.removeEventListener('resize',measure);};
  },[]);
  function change(k,v){const next={...prefs,[k]:v};setPrefs(next);try{localStorage.setItem('motaz-reader-preferences',JSON.stringify(next));}catch{}}
  const family=prefs.family==='editor'?article.fontFamily:prefs.family;
  const fonts={amiri:'Amiri, serif',naskh:'"Noto Naskh Arabic", serif',plex:'"Plex Arabic", sans-serif',tajawal:'"Plex Arabic", sans-serif'};
  return <section className={'reader-surface '+prefs.theme+(prefs.focus==='on'?' focused':'')}>
    <details className="reader-controls"><summary>إعدادات القراءة</summary><div>{Object.entries(options).map(([k,values])=><label key={k}>{labels[k]}
      <select value={prefs[k]} onChange={e=>change(k,e.target.value)}>{values.map(v=><option key={v} value={v}>{names[v]||v}</option>)}</select></label>)}</div></details>
    {headings.length>=2&&<details className="reader-index"><summary>فهرس المقالة</summary><ol>{headings.map(h=><li key={h.id}><a href={'#'+h.id}>{h.title}</a></li>)}</ol></details>}
    <progress value={progress} max={100} aria-label="تقدم القراءة" className="reader-progress"/>
    <div ref={body} className="article-prose" style={{fontFamily:fonts[family]||fonts.amiri,fontSize:(prefs.size==='editor'?article.fontSize||22:prefs.size)+'px',lineHeight:prefs.line,maxWidth:prefs.width+'rem',textAlign:prefs.align,'--paragraph-space':prefs.space+'em'}}>
      <ReactMarkdown remarkPlugins={[remarkGfm]} components={{img:ArticleMedia}} skipHtml>{article.content}</ReactMarkdown>
    </div>
  </section>;
}

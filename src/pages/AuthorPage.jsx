import {useEffect,useState} from 'react';
import {useParams,Link} from 'react-router-dom';
import {useArticles} from '../hooks/useArticles';
import {api} from '../lib/api';
import {articlePath} from '../lib/format';
import {AuthorBadge} from '../components/ArticleMedia';

export function AuthorPage(){
 const {key}=useParams();
 const {published,loading}=useArticles();
 const [profiles,setProfiles]=useState(null);
 useEffect(()=>{let active=true;api('/api/v1/authors').then(r=>{if(active)setProfiles(r.authors);}).catch(()=>{if(active)setProfiles([]);});return()=>{active=false};},[]);
 const profile=profiles?.find(p=>p.id===key || p.name===key);
 const name=profile?.name || key;
 const items=published.filter(a=>a.authorId ? a.authorId===(profile?.id||key) : a.author===name);
 if(loading||!profiles)return <p className="p-12 text-center">جار فتح ملف الكاتب…</p>;
 if(!profile&&!items.length)return <p className="p-12 text-center">لم يُعثر على ملف الكاتب.</p>;
 return <section className="max-w-3xl mx-auto p-6 my-8"><AuthorBadge name={name} image={profile?.image||items.find(a=>a.authorImage)?.authorImage}/>
  {profile?.bio&&<p className="font-literature text-2xl my-5">{profile.bio}</p>}{profile?.link&&<a href={profile.link} rel="noreferrer" className="underline">موقع الكاتب</a>}
  <h1 className="my-6 font-literature text-3xl">كتابات {name} ({items.length})</h1><ul>{items.map(a=><li key={a.id} className="border-t py-4"><Link to={articlePath(a)}>{a.title}</Link></li>)}</ul></section>;
}

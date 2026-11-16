const repository='https://github.com/Mtzallqmy/Motazalqmi';
export function publishedReleases(records) {
 return records.filter(r=>!r.draft&&/^v\d+\.\d+\.\d+(?:[-.].*)?$/.test(r.tag_name)).map(r=>{
  const numbers=r.tag_name.match(/^v(\d+)\.(\d+)\.(\d+)/).slice(1).map(Number);
  const asset=r.assets?.find(a=>a.name.endsWith('.apk')&&a.browser_download_url?.startsWith(repository+'/releases/download/'));
  return {tag:r.tag_name,version:numbers.join('.'),versionCode:numbers[0]*10000+numbers[1]*100+numbers[2],
   minSdk:26,architecture:'arm64-v8a',status:'released',prerelease:r.prerelease===true,downloadUrl:asset?.browser_download_url||null,
   releaseUrl:repository+'/releases/tag/'+encodeURIComponent(r.tag_name),publishedAt:r.published_at,
   installation:r.tag_name.includes('standalone')||r.tag_name.includes('independent')?'independent':'production'};
 }).filter(r=>r.downloadUrl).sort((a,b)=>b.versionCode-a.versionCode || String(b.publishedAt).localeCompare(String(a.publishedAt)));
}
export async function releaseCatalogue(fetcher=fetch) {
 try {
  const response=await fetcher('https://api.github.com/repos/Mtzallqmy/Motazalqmi/releases?per_page=30',
   {headers:{Accept:'application/vnd.github+json','User-Agent':'Motaz-Alalqami'},signal:AbortSignal.timeout(5000)});
  if(!response.ok)throw Error('Catalogue unavailable');
  const records=await response.json();if(!Array.isArray(records))throw Error('Invalid catalogue');
  const versions=publishedReleases(records);
  return {versions,latest:versions[0]||null,official:versions.find(r=>!r.prerelease&&r.installation==='production')||null,available:true};
 } catch { return {versions:[],latest:null,official:null,available:false}; }
}

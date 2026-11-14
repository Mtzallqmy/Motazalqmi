import { publicGet, sendJson } from '../../server/publicApi.js';

// Public build catalogue: no GitHub token, database secret or Android signing key is exposed.
export default async function handler(req,res){
 if(!publicGet(req,res))return;
 const workflow=req.query?.workflow==='premium' ? 'premium-release.yml' : 'android.yml';
 const data={apiVersion:1,workflow,appId:'com.moataz.motazalqmi',version:'2.0.0',versionCode:20000,minAndroid:'8.0',minSdk:26,architecture:'arm64-v8a',native:true,
  source:'https://github.com/Mtzallqmy/Motazalqmi/tree/main/android',
  builds:'https://github.com/Mtzallqmy/Motazalqmi/actions/workflows/'+workflow,
  releaseStatus:'Signing key required for official production release',latestBuild:null};
 try {
  const response=await fetch('https://api.github.com/repos/Mtzallqmy/Motazalqmi/actions/workflows/'+workflow+'/runs?branch=main&per_page=3',{headers:{Accept:'application/vnd.github+json','User-Agent':'Motazalqmi-blog-native-app'},signal:AbortSignal.timeout(4500)});
  if(response.ok){const body=await response.json();const run=body.workflow_runs?.[0];if(run)data.latestBuild={id:run.id,status:run.status,conclusion:run.conclusion,url:run.html_url,commit:run.head_sha};}
 }catch{ /* Build info is optional and never blocks reader content. */ }
 return sendJson(req,res,data,{seconds:45});
}

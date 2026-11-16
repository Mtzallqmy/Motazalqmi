import { publicGet,sendJson } from '../../server/publicApi.js';
import {releaseCatalogue} from '../../server/releases.js';
export default async function handler(req,res) {
 if(!publicGet(req,res))return;
 const releases=await releaseCatalogue();
 return sendJson(req,res,{apiVersion:1,appId:'com.moataz.motazalqmi',minAndroid:'8.0',minSdk:26,
  architecture:'arm64-v8a',native:true,source:'https://github.com/Mtzallqmy/Motazalqmi/tree/main/android',
  builds:'https://github.com/Mtzallqmy/Motazalqmi/actions/workflows/edition-release.yml',
  ...releases,version:releases.latest?.version||null,versionCode:releases.latest?.versionCode||null,
  releaseStatus:releases.available?'published-catalogue':'catalogue-unavailable'},{seconds:60});
}

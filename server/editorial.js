import { database } from './db.js';
import { SITE, QUOTES, editorialSections } from '../shared/publicContent.js';
export function publicEditorial(stored) {
 const {tagline, about} = stored?.site || {};
 return {
  site:{...SITE, tagline:typeof tagline === 'string' ? tagline : SITE.tagline, about:typeof about === 'string' ? about : SITE.about},
  sections:editorialSections(stored?.sections).map(({id,title,subtitle})=>({id,title,subtitle})),
  quotes:(Array.isArray(stored?.quotes) ? stored.quotes : QUOTES).map(({text,author,source})=>({text,author,source}))
 };
}

export async function editorial(sql = database()) {
 const rows=await sql`SELECT data FROM blog_public_metadata WHERE id='editorial'`;
 return publicEditorial(rows[0]?.data);
}

export function metadataHandler(key, load=editorial) {
 return async function handler(req,res) {
  if(!publicGet(req,res))return;
  try{return sendJson(req,res,{apiVersion:1,[key]:(await load())[key]},{seconds:60})}catch(e){return publicError(res,e)}
 };
}
import { publicGet,sendJson,publicError } from './publicApi.js';

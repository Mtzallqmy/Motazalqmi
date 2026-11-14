import { database } from './db.js';
import { SITE, SECTIONS, QUOTES } from '../shared/publicContent.js';
export async function editorial() {
 const sql=database();
 const rows=await sql`SELECT data FROM blog_public_metadata WHERE id='editorial'`;
 return rows[0]?.data || {site:SITE,sections:SECTIONS,quotes:QUOTES};
}

export function metadataHandler(key, load=editorial) {
 return async function handler(req,res) {
  if(!publicGet(req,res))return;
  try{return sendJson(req,res,{apiVersion:1,[key]:(await load())[key]},{seconds:60})}catch(e){return publicError(res,e)}
 };
}
import { publicGet,sendJson,publicError } from './publicApi.js';

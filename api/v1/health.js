import { database } from '../../server/db.js';
import { publicGet } from '../../server/publicApi.js';
export default async function handler(req,res){
 if(!publicGet(req,res))return;
 try{const sql=database();const rows=await sql`SELECT 1 AS connected`;
 res.setHeader('Cache-Control','no-store');
 return res.status(200).json({status:rows[0]?.connected===1?'ok':'degraded',apiVersion:1,storage:'neon'});
 }catch{res.setHeader('Cache-Control','no-store');return res.status(503).json({status:'unavailable',apiVersion:1})}
}

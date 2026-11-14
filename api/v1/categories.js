import { SECTIONS } from '../../shared/publicContent.js';
import { publicGet, sendJson } from '../../server/publicApi.js';
export default function handler(req,res){if(!publicGet(req,res))return;return sendJson(req,res,{apiVersion:1,sections:SECTIONS}, {seconds:3600});}

import { QUOTES } from '../../shared/publicContent.js';
import { publicGet, sendJson } from '../../server/publicApi.js';
export default function handler(req,res){if(!publicGet(req,res))return;return sendJson(req,res,{apiVersion:1,quotes:QUOTES}, {seconds:3600});}

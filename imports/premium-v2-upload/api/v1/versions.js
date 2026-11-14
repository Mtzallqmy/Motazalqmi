import { sendJson } from './_lib/http.js';

export default function handler(req, res) {
  return sendJson(req, res, {
    data: {
      apiVersion: '1.0.0',
      androidLatest: '2.0.0',
      androidVersionCode: 200
    },
    cacheSeconds: 300
  });
}

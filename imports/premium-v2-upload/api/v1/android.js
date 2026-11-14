import { sendJson } from './_lib/http.js';

export default function handler(req, res) {
  return sendJson(req, res, {
    data: {
      packageId: 'com.moataz.motazalqmi',
      latestVersionName: '2.0.0',
      latestVersionCode: 200,
      minSupportedVersionCode: 100,
      minAndroidApi: 26,
      abi: 'arm64-v8a',
      releaseUrl: 'https://github.com/Mtzallqmy/Motazalqmi/releases/latest'
    },
    cacheSeconds: 300
  });
}

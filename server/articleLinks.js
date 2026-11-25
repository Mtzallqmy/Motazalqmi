import {createHash} from 'node:crypto';
import {SITE} from '../shared/publicContent.js';

// A public identifier, not a credential. The DB index enforces uniqueness.
export const shortCode = id => createHash('md5').update(id, 'utf8').digest('hex').slice(0, 16);
export const shortArticleUrl = id => `${SITE.officialUrl}/r/${shortCode(id)}`;
export const validShortCode = value => typeof value === 'string' && /^[0-9a-f]{16}$/.test(value);

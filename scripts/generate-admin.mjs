// Run locally only, never commit its output or log it in CI.
import { randomBytes, scryptSync } from 'node:crypto';
const password = randomBytes(18).toString('base64url');
const salt = randomBytes(16).toString('hex');
const hash = scryptSync(password, Buffer.from(salt,'hex'), 64).toString('hex');
console.log('Admin password (store securely, shown once):', password);
console.log('ADMIN_PASSWORD_HASH=' + salt + ':' + hash);
console.log('SESSION_SECRET=' + randomBytes(48).toString('base64url'));

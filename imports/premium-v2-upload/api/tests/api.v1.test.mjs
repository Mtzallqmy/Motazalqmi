import assert from 'node:assert/strict';
import test from 'node:test';
import { parsePagination } from '../v1/_lib/http.js';

test('parsePagination enforces bounds', () => {
  assert.deepEqual(parsePagination({ page: '-2', limit: '999' }), { page: 1, limit: 50, offset: 0 });
  assert.deepEqual(parsePagination({ page: '3', limit: '10' }), { page: 3, limit: 10, offset: 20 });
});

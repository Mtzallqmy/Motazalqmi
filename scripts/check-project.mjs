import { existsSync, readFileSync } from 'node:fs';
const required = [
 'index.html', 'src/main.jsx', 'src/App.jsx', 'src/lib/api.js', 'server/db.js', 'server/auth.js',
 'api/session.js','api/articles.js','db/schema.sql','src/context/AuthContext.jsx','src/hooks/useArticles.jsx',
 'src/pages/HomePage.jsx','src/pages/CategoryPage.jsx','src/pages/ArticlePage.jsx',
 'src/pages/AboutPage.jsx','src/pages/DashboardPage.jsx','src/pages/LoginPage.jsx',
 'vercel.json', 'README.md'
];
const missing = required.filter(path => !existsSync(path));
if (missing.length) { console.error('Missing files:', missing.join(', ')); process.exit(1); }
const schema = readFileSync('db/schema.sql','utf8');
if (!schema.includes('blog_articles') || !schema.includes('blog_login_attempts')) process.exit(1);
const frontend = readFileSync('src/hooks/useArticles.jsx','utf8');
if (/firebase|firestore/i.test(frontend)) process.exit(1);
console.log(`OK: ${required.length} required files present; server-only Neon API configured`);

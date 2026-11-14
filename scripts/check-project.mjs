import {existsSync,readFileSync} from 'node:fs';
const required=['index.html','src/App.jsx','src/lib/api.js','server/db.js','server/auth.js','api/session.js','api/articles.js','db/schema.sql','src/context/AuthContext.jsx','src/hooks/useArticles.jsx','src/pages/HomePage.jsx','src/pages/DashboardPage.jsx','src/pages/LoginPage.jsx','vercel.json','README.md'];
const missing=required.filter(x=>!existsSync(x));
if(missing.length){console.error('Missing:',missing);process.exit(1);}
if(!readFileSync('db/schema.sql','utf8').includes('blog_articles'))process.exit(1);
if(/firebase/i.test(readFileSync('src/hooks/useArticles.jsx','utf8')))process.exit(1);
console.log('OK: Neon server API and frontend checked');

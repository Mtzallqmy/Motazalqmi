import { existsSync, readFileSync } from 'node:fs';
const required = [
  'index.html', 'src/main.jsx', 'src/App.jsx', 'src/lib/firebase.js',
  'src/context/AuthContext.jsx', 'src/hooks/useArticles.jsx',
  'src/pages/HomePage.jsx', 'src/pages/CategoryPage.jsx', 'src/pages/ArticlePage.jsx',
  'src/pages/AboutPage.jsx', 'src/pages/DashboardPage.jsx', 'src/pages/LoginPage.jsx',
  'firestore.rules', 'vercel.json', 'README.md'
];
const missing = required.filter(path => !existsSync(path));
if (missing.length) { console.error('Missing files:', missing.join(', ')); process.exit(1); }
const rules = readFileSync('firestore.rules','utf8');
if (!rules.includes('isAdmin()') || !rules.includes('allow list, create, update, delete: if false')) {
  console.error('Firestore admin security rules are missing'); process.exit(1);
}
console.log(`OK: ${required.length} project files present and Firestore admin rules defined`);

/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: { cream: '#FAF6F0', ink: '#231F20', teal: '#0D5C63', gold: '#C89D54', rust: '#A85A3A' },
      fontFamily: { body: ['Tajawal', 'sans-serif'], literature: ['Amiri', 'serif'], calligraphy: ['Aref Ruqaa', 'Amiri', 'serif'] },
      boxShadow: { soft: '0 18px 44px -28px rgba(35,31,32,.35)' }
    }
  }, plugins: []
};

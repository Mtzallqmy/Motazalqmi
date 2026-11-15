import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const articleEntry = {
  name: 'article-document-entry',
  generateBundle(_, bundle) {
    const entry = Object.values(bundle).find(file => file.type === 'chunk' && file.isEntry);
    const styles = [...entry.viteMetadata.importedCss];
    this.emitFile({ type:'asset', fileName:'article-entry.js', source:
      styles.map(path => `{const link=document.createElement('link');link.rel='stylesheet';link.href=${JSON.stringify('/'+path)};document.head.append(link)}`).join('\n')+
      `\nimport(${JSON.stringify('/'+entry.fileName)});\n` });
  }
};
export default defineConfig({ plugins:[react(), articleEntry], build:{outDir:'dist',sourcemap:false} });

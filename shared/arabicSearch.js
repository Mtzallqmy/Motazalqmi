export function normalizeArabic(value = '') {
  return String(value).normalize('NFKC').replace(/[\u064b-\u065f\u0670\u0640]/g, '')
    .replace(/[أإآٱ]/g, 'ا').replace(/ى/g, 'ي').toLocaleLowerCase('ar');
}

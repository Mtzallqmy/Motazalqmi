import { Feather, Quote, BookOpen, Sparkles, Compass, Scroll, Landmark, UserCheck } from 'lucide-react';
import { SECTIONS } from '../../shared/publicContent.js';

// Titles/descriptions have a single source of truth for website, API, and Android.
const DESIGN = {
  general: { icon: Feather, color: 'from-[#0D5C63] to-[#147A82]' },
  quotes: { icon: Quote, color: 'from-[#C89D54] to-[#B3863E]' },
  books: { icon: BookOpen, color: 'from-[#A85A3A] to-[#8C4629]' },
  pens: { icon: Sparkles, color: 'from-[#16697A] to-[#0D5C63]' },
  news: { icon: Compass, color: 'from-[#7D5A50] to-[#604239]' },
  wisdom: { icon: Scroll, color: 'from-[#B8860B] to-[#996515]' },
  arabic_lit: { icon: Landmark, color: 'from-[#0A484E] to-[#062F33]' },
  about: { icon: UserCheck, color: 'from-[#2C2523] to-[#1A1615]' }
};
export const CATEGORIES = SECTIONS.map(section=>({ ...section,...DESIGN[section.id] }));
export const getCategory = id => CATEGORIES.find(c=>c.id===id) ?? CATEGORIES[0];
export const ARTICLE_CATEGORIES = CATEGORIES.filter(c=>c.id!=='about');

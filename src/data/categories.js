import { Feather, Quote, BookOpen, Sparkles, Compass, Scroll, Landmark, UserCheck } from 'lucide-react';
export const CATEGORIES = [
  { id: 'general', title: 'كتابات عامة', subtitle: 'مقالات وتأملات وموضوعات متنوعة في الفكر والحياة', icon: Feather, color: 'from-[#0D5C63] to-[#147A82]' },
  { id: 'quotes', title: 'اقتباسات', subtitle: 'مختارات من العبارات الأدبية والفكرية الملهمة', icon: Quote, color: 'from-[#C89D54] to-[#B3863E]' },
  { id: 'books', title: 'من بطون الكتب', subtitle: 'مقتطفات وقراءات ومراجعات من كنوز المؤلفات', icon: BookOpen, color: 'from-[#A85A3A] to-[#8C4629]' },
  { id: 'pens', title: 'من وحي الأقلام', subtitle: 'نصوص إبداعية وخواطر ومقالات بأسلوب أدبي رفيع', icon: Sparkles, color: 'from-[#16697A] to-[#0D5C63]' },
  { id: 'news', title: 'أحاديث وأخبار', subtitle: 'موضوعات ثقافية وأخبار أدبية ومحطات تاريخية', icon: Compass, color: 'from-[#7D5A50] to-[#604239]' },
  { id: 'wisdom', title: 'حكم ومواعظ', subtitle: 'درر الحكمة والتجارب الإنسانية العميقة عبر العصور', icon: Scroll, color: 'from-[#B8860B] to-[#996515]' },
  { id: 'arabic_lit', title: 'أدبنا العربي', subtitle: 'الشعر العربي، واللغة، والبلاغة، والأدباء، والتراث', icon: Landmark, color: 'from-[#0A484E] to-[#062F33]' },
  { id: 'about', title: 'حول المدونة', subtitle: 'تعريف برسالة المدونة وصاحبها وأهدافها الثقافية', icon: UserCheck, color: 'from-[#2C2523] to-[#1A1615]' }
];
export const getCategory = (id) => CATEGORIES.find(c => c.id === id) ?? CATEGORIES[0];
export const ARTICLE_CATEGORIES = CATEGORIES.filter(c => c.id !== 'about');

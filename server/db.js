import { neon } from '@neondatabase/serverless';
export function database(){
 const url=process.env.DATABASE_URL;
 if(!url||!/^postgres(ql)?:\/\//.test(url))throw new Error('DATABASE_URL is not configured');
 return neon(url);
}
export const articleRow=row=>({...row.data,id:row.id,slug:row.slug,status:row.status,publishDate:row.publish_date,updatedAt:row.updated_at});
export function normalizeArticle(a){
 if(!a||typeof a!=='object'||Array.isArray(a))throw new Error('بيانات المقالة غير صالحة.');
 const str=(key,max,def='')=>{const value=a[key]??def;if(typeof value!=='string'||value.length>max)throw new Error('حقل غير صالح: '+key);return value.trim();};
 const id=str('id',150),slug=str('slug',150),title=str('title',220),content=str('content',180000);
 if(!title||!content||!slug||!/^[\p{L}\p{N}-]+$/u.test(slug))throw new Error('العنوان والمحتوى والرابط الوصفي مطلوبة.');
 const status=['draft','published'].includes(a.status)?a.status:null;
 if(!status)throw new Error('حالة المقالة غير صالحة.');
 const publishDate=str('publishDate',10,new Date().toISOString().slice(0,10));
 if(!/^\d{4}-\d{2}-\d{2}$/.test(publishDate)||Number.isNaN(Date.parse(publishDate)))throw new Error('تاريخ المقالة غير صالح.');
 const coverImage=str('coverImage',2048),authorImage=str('authorImage',2048);
 if(coverImage&&!/^https:\/\//i.test(coverImage))throw new Error('رابط صورة الغلاف يجب أن يكون HTTPS.');
 if(authorImage&&!/^https:\/\//i.test(authorImage))throw new Error('صورة الكاتب يجب أن تكون HTTPS.');
 return {id,slug,title,content,status,publishDate,coverImage,category:str('category',60,'general'),
 excerpt:str('excerpt',1000),author:str('author',150,'معتز العلقمي'),authorImage,readTime:str('readTime',100,'5 دقائق'),
 fontFamily:['amiri','naskh','tajawal'].includes(a.fontFamily)?a.fontFamily:'amiri',
 fontSize:Number.isInteger(a.fontSize)&&a.fontSize>=16&&a.fontSize<=32?a.fontSize:22,
 featured:a.featured===true,tags:Array.isArray(a.tags)?a.tags.filter(x=>typeof x==='string').slice(0,20).map(x=>x.slice(0,55)):[],
 views:Number.isSafeInteger(a.views)&&a.views>=0?a.views:0,likes:Number.isSafeInteger(a.likes)&&a.likes>=0?a.likes:0};
}

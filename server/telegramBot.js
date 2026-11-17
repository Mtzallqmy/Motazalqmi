import {publicationTime,isScheduled} from '../shared/publication.js';
import {telegramAttachment,storeTelegramMedia} from './telegramMedia.js';
import {randomUUID,timingSafeEqual,createHash,createHmac} from 'node:crypto';
import {database,normalizeArticle} from './db.js';
import {noCache} from './auth.js';

const SITE='https://motazalqmi.vercel.app';
export const articleLink=slug=>SITE+'/articles/'+encodeURIComponent(slug);
const same=(a,b)=>{
 if(typeof a!=='string'||typeof b!=='string')return false;
 const x=Buffer.from(a),y=Buffer.from(b);
 return x.length===y.length&&timingSafeEqual(x,y);
};
export function telegramConfig(env=process.env){
 const token=env.TELEGRAM_BOT_TOKEN||'',secret=env.TELEGRAM_WEBHOOK_SECRET||(env.SESSION_SECRET?.length>=32?createHmac('sha256',env.SESSION_SECRET).update('telegram-webhook-motazalqmi').digest('hex'):'');
 const admins=new Set((env.TELEGRAM_ADMIN_IDS||'').split(/[,\s]+/).filter(x=>/^\d+$/.test(x)));
 return {token,secret,admins,ready:token.length>30&&secret.length>=24};
}
export function telegramCommand(text){
 if(typeof text!=='string')return null;
 const m=text.trim().match(/^\/([a-z]+)(?:@[a-z0-9_]+)?(?:\s+([\s\S]*))?$/i);
 return m?{name:m[1].toLowerCase(),args:(m[2]||'').trim()}:null;
}
const slugify=title=>title.normalize('NFKC').toLowerCase().replace(/[\u064b-\u065f]/g,'').replace(/[^\p{L}\p{N}\s-]/gu,'').trim().replace(/\s+/g,'-').replace(/-+/g,'-').slice(0,100)||'post';
const HELP=[
 'لوحة إدارة المدونة من تيليجرام 🔐',
 '/articles — المقالات المنشورة',
 '/search الكلمات — البحث في عناوين المقالات',
 '/author slug | اسم الكاتب',
 '/scheduled — المقالات المجدولة',
 '/schedule slug | 2027-01-20T20:30+03:00 — جدولة النشر',
 '/attach slug | وصف المرفق — مع صورة أو فيديو أو صوت صغير',
 '/failures — آخر أخطاء العمليات',
 '/drafts — المسودات',
 '/stats — إحصاء المنشورات',
 '/show slug — تفاصيل المقالة',
 '/link slug — رابط المقالة',
 '/new العنوان | المحتوى — إنشاء مسودة',
 '/title slug | العنوان الجديد',
 '/text slug | النص الجديد (Markdown)',
 '/excerpt slug | الملخص',
 '/cover slug | رابط صورة HTTPS',
 '/category slug | القسم',
 '/featured slug — تمييز/إلغاء تمييز',
 '/publish slug — نشر',
 '/unpublish slug — تحويل لمسودة',
 '/delete slug — حذف مع تأكيد',
 '/site — معلومات المدونة',
 '/aboutset النص — تعديل نبذة المدونة',
 '/quotes — عرض الاقتباسات',
 '/quoteadd النص | القائل | المصدر — إضافة اقتباس',
 '/quotedelete رقم الاقتباس — حذف بعد تأكيد',
 '/tagline النص — تغيير وصف المدونة',
 '/help — هذه الأوامر'
].join('\n');
const rowArticle=r=>({...r.data,id:r.id,slug:r.slug,status:r.status,publishDate:r.publish_date,updatedAt:r.updated_at});
const errorText=e=>/^(الصيغة|اكتب|القيمة|رابط|معرف|بيانات|موعد|تغيرت|الملف|لا توجد|هذه المقالة)/.test(e?.message||'')?e.message:'تحقق من محتوى الأمر أو أعد المحاولة.';

export function createTelegramHandler({sqlFactory=database,send=globalThis.fetch,download=globalThis.fetch,uploadMedia,env=process.env}={}){
 return async (req,res)=>{
  noCache(res);
  if(req.method!=='POST'){res.setHeader('Allow','POST');return res.status(405).json({error:'POST only'});}
  const cfg=telegramConfig(env);
  if(!cfg.ready)return res.status(503).json({error:'Telegram integration is not configured'});
  if(!same(req.headers?.['x-telegram-bot-api-secret-token'],cfg.secret))return res.status(403).json({error:'Forbidden'});
  if(!req.body||typeof req.body!=='object'||JSON.stringify(req.body).length>28000)return res.status(400).json({error:'Invalid update'});
  const msg=req.body.message,cb=req.body.callback_query,actor=msg?.from||cb?.from,chat=msg?.chat||cb?.message?.chat;
  if(!actor||!chat||chat.type!=='private'||String(actor.id)!==String(chat.id))
   return res.status(200).json({ok:true});
  const call=async(method,body)=>{
   const response=await send('https://api.telegram.org/bot'+cfg.token+'/'+method,{
    method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body),signal:AbortSignal.timeout(10000)});
   if(!response.ok)throw Error('Telegram unavailable');
   const data=await response.json();if(!data.ok)throw Error('Telegram error');return data;
  };
  const reply=async(text,buttons)=>{
   const body={chat_id:chat.id,text:String(text||'').slice(0,3800),disable_web_page_preview:true};
   if(buttons)body.reply_markup={inline_keyboard:buttons};
   return call('sendMessage',body);
  };
  let authorized=false;
  try{
   const sql=sqlFactory();
   const actorId=String(actor.id);
   const adminRows=await sql.query("SELECT data FROM blog_public_metadata WHERE id='telegram_admin' LIMIT 1",[]);
   const isAdmin=cfg.admins.has(actorId)||Boolean(adminRows[0]?.data?.ids?.includes(actorId));
   if(!isAdmin){
    const pair=telegramCommand(msg?.text);
    if(pair?.name==='link'&&/^[a-f0-9]{40}$/.test(pair.args)){
     const hash=createHash('sha256').update(pair.args).digest('hex');
     const matches=await sql.query("WITH consumed AS (DELETE FROM blog_public_metadata WHERE id='telegram_pair' AND data->>'hash'=$1 AND (data->>'expiresAt')::bigint>$2::bigint RETURNING id) INSERT INTO blog_public_metadata(id,data) SELECT 'telegram_admin',jsonb_build_object('ids',jsonb_build_array($3::text)) FROM consumed ON CONFLICT(id) DO UPDATE SET data=jsonb_build_object('ids',(SELECT jsonb_agg(DISTINCT value) FROM jsonb_array_elements_text(COALESCE(blog_public_metadata.data->'ids','[]'::jsonb)||(EXCLUDED.data->'ids')))) ,updated_at=NOW() RETURNING id",[hash,String(Date.now()),actorId]);
     if(matches.length){
      await reply('✅ تم ربط حسابك بلوحة إدارة مدونة معتز العلقمي. أرسل /help.');
     }else await reply('رمز الربط منتهي أو غير صحيح. أنشئ رمزًا جديدًا من لوحة الكاتب.');
    }
    return res.status(200).json({ok:true});
   }
   authorized=true;
   const lookup=async slug=>{
    if(!slug||slug.length>150)throw Error('اكتب رابطًا وصفيًا صحيحًا.');
    const rows=await sql.query('SELECT id,slug,status,publish_date::text,updated_at::text,data FROM blog_articles WHERE slug=$1 LIMIT 1',[slug]);
    if(!rows.length)throw Error('لا توجد مقالة بهذا الرابط الوصفي.');
    return rowArticle(rows[0]);
   };
   const update=async (old,changes)=>{
    const a=normalizeArticle({...old,...changes});
    const changed=await sql.query('UPDATE blog_articles SET slug=$1,status=$2,publish_date=$3::date,data=$4::jsonb,updated_at=NOW() WHERE id=$5 AND updated_at=$6::timestamptz RETURNING id',
     [a.slug,a.status,a.publishDate,JSON.stringify(a),old.id,old.updatedAt]);
    if(!changed.length)throw Error('تغيرت المقالة. أعد فتحها قبل تنفيذ التعديل.');
    return a;
   };
   const getEditorial=async()=>{
    const r=await sql.query("SELECT data FROM blog_public_metadata WHERE id='editorial' LIMIT 1",[]);
    if(!r.length)throw Error('بيانات المدونة غير موجودة.');
    return r[0].data;
   };
   const setEditorial=async data=>sql.query("UPDATE blog_public_metadata SET data=$1::jsonb,updated_at=NOW() WHERE id='editorial'",[JSON.stringify(data)]);
   const confirmSetting=async(field,value)=>{
    const nonce=randomUUID().replaceAll('-','').slice(0,20);
    await sql.query('INSERT INTO blog_public_metadata(id,data) VALUES($1,$2::jsonb) ON CONFLICT(id) DO UPDATE SET data=EXCLUDED.data,updated_at=NOW()',
     ['telegram_setting:'+actorId,JSON.stringify({nonce,field,value,expiresAt:Date.now()+600000})]);
    await reply('تأكيد تغيير '+(field==='about'?'نبذة المدونة':'عبارة المدونة')+'؟\n'+value.slice(0,400),[[{text:'تأكيد التغيير',callback_data:'setting:'+nonce},{text:'إلغاء',callback_data:'cancel'}]]);
   };
   const done=async a=>reply((isScheduled(a)?'🕒 مجدول: ':a.status==='published'?'✅ منشور: ':'📝 مسودة: ')+a.title+'\n'+articleLink(a.slug));
   if(cb){
    await call('answerCallbackQuery',{callback_query_id:cb.id});
    if(cb.data==='cancel'){await reply('أُلغيت العملية.');return res.status(200).json({ok:true});}
    if(typeof cb.data==='string'&&cb.data.startsWith('setting:')){
     const rows=await sql.query("DELETE FROM blog_public_metadata WHERE id=$1 AND data->>'nonce'=$2 AND (data->>'expiresAt')::bigint>$3::bigint RETURNING data",['telegram_setting:'+actorId,cb.data.slice(8),String(Date.now())]);
     const pending=rows[0]?.data;
     if(!pending||!['about','tagline'].includes(pending.field)||typeof pending.value!=='string'){await reply('انتهى التأكيد أو استُخدم. أعد تنفيذ الأمر.');return res.status(200).json({ok:true});}
     await sql.query("UPDATE blog_public_metadata SET data=jsonb_set(data,ARRAY['site',$1],to_jsonb($2::text),true),updated_at=NOW() WHERE id='editorial'",[pending.field,pending.value]);
     await reply('تم تحديث إعداد المدونة.');return res.status(200).json({ok:true});
    }
    if(typeof cb.data==='string'&&cb.data.startsWith('qdel:')){
     const parts=cb.data.split(':'),index=Number(parts[1]),signature=parts[2];
     const current=await getEditorial(),quotes=[...(current.quotes||[])];
     if(!Number.isInteger(index)||index<0||index>=quotes.length||
        createHash('sha256').update(quotes[index].text).digest('hex').slice(0,12)!==signature){
      await reply('تغيرت القائمة، أعد تنفيذ الأمر.');return res.status(200).json({ok:true});
     }
     quotes.splice(index,1);await setEditorial({...current,quotes});
     await reply('✅ حُذف الاقتباس من المدونة.');
     return res.status(200).json({ok:true});
    }
    const id=typeof cb.data==='string'&&cb.data.startsWith('del:')?cb.data.slice(4):'';
    if(!id||id.length>100)return res.status(200).json({ok:true});
    const rows=await sql.query('DELETE FROM blog_articles WHERE id=$1 RETURNING slug',[id]);
    await reply(rows.length?'حُذفت المقالة: '+rows[0].slug:'المقالة غير موجودة.');
    return res.status(200).json({ok:true});
   }
   const cmd=telegramCommand(msg?.text||msg?.caption);
   if(!cmd)return res.status(200).json({ok:true});
   if(['help','start'].includes(cmd.name))await reply(HELP);
   else if(cmd.name==='stats'){
    const rows=await sql.query("SELECT CASE WHEN status='published' AND NOT blog_is_due(data) THEN 'scheduled' ELSE status END AS status,COUNT(*)::int AS amount FROM blog_articles GROUP BY 1",[]);
    await reply('📊 الإحصاءات\nالمنشور: '+(rows.find(x=>x.status==='published')?.amount||0)+'\nالمسودات: '+(rows.find(x=>x.status==='draft')?.amount||0)+'\nالمجدول: '+(rows.find(x=>x.status==='scheduled')?.amount||0));
   }else if(['articles','drafts','scheduled'].includes(cmd.name)){
    const status=cmd.name==='drafts'?'draft':'published';
    const rows=await sql.query('SELECT slug,data FROM blog_articles WHERE status=$1 AND ($2=\'drafts\' OR ($2=\'scheduled\' AND NOT blog_is_due(data)) OR ($2=\'articles\' AND blog_is_due(data))) ORDER BY updated_at DESC LIMIT 20',[status,cmd.name]);
    await reply((status==='draft'?'📝 المسودات':'📰 المقالات')+'\n'+(rows.length?rows.map((r,i)=>(i+1)+'. '+r.data.title+'\n/show '+r.slug).join('\n\n'):'لا توجد مقالات'));
   }else if(cmd.name==='search'){
    if(!cmd.args||cmd.args.length>150)throw Error('اكتب كلمات البحث بعد /search');
    const rows=await sql.query("SELECT slug,data->>'title' AS title,status FROM blog_articles WHERE strpos(lower(data->>'title'),lower($1))>0 ORDER BY updated_at DESC LIMIT 12",[cmd.args]);
    await reply(rows.map(r=>r.title+'\n/show '+r.slug).join('\n\n')||'لا توجد نتائج.');
   }else if(cmd.name==='new'){
    const pos=cmd.args.indexOf('|');
    if(pos<1)throw Error('الصيغة: /new العنوان | النص الكامل');
    const title=cmd.args.slice(0,pos).trim(),content=cmd.args.slice(pos+1).trim();
    const id=Number.isSafeInteger(msg.message_id)?'art-tg-'+actorId+'-'+msg.message_id:'art-'+randomUUID();
    const date=new Date(Date.now()+3*60*60*1000).toISOString().slice(0,10);
    const a=normalizeArticle({id,title,content,status:'draft',slug:slugify(title)+'-'+(Number.isSafeInteger(msg.message_id)?actorId+'-'+msg.message_id:randomUUID().slice(0,6)),
     category:'general',coverImage:'',author:'معتز العلقمي',publishDate:date});
    await sql.query('INSERT INTO blog_articles(id,slug,status,publish_date,data) VALUES ($1,$2,$3,$4::date,$5::jsonb) ON CONFLICT(id) DO NOTHING',
     [id,a.slug,a.status,a.publishDate,JSON.stringify(a)]);
    await done(a);
   }else if(cmd.name==='schedule'){
    const split=cmd.args.indexOf('|');if(split<1)throw Error('الصيغة: /schedule slug | موعد مع المنطقة الزمنية');
    const a=await lookup(cmd.args.slice(0,split).trim()),scheduledAt=publicationTime(cmd.args.slice(split+1).trim());
    if(!scheduledAt||Date.parse(scheduledAt)<=Date.now())throw Error('موعد الجدولة يجب أن يكون في المستقبل.');
    await done(await update(a,{status:'published',scheduledAt}));
   }else if(cmd.name==='attach'){
    const [slug,...captions]=cmd.args.split('|').map(x=>x.trim()),a=await lookup(slug);
    const attachment=telegramAttachment(msg);if(!attachment)throw Error('الملف مطلوب مع الأمر /attach.');
    if(!Number.isSafeInteger(msg.message_id))throw Error('الملف بلا معرّف رسالة صالح.');
    const key='telegram_attachment:'+actorId+':'+msg.message_id;
    const existing=await sql.query('SELECT data FROM blog_public_metadata WHERE id=$1 LIMIT 1',[key]);
    const cached=existing[0]?.data;
    if(cached?.articleId&&cached.articleId!==a.id)throw Error('الملف مرتبط بمقالة أخرى.');
    const url=cached?.url||await storeTelegramMedia({attachment,token:cfg.token,download,upload:uploadMedia,
      getFile:async id=>(await call('getFile',{file_id:id})).result});
    if(!cached)await sql.query('INSERT INTO blog_public_metadata(id,data) VALUES($1,$2::jsonb) ON CONFLICT(id) DO NOTHING',
      [key,JSON.stringify({url,articleId:a.id})]);
    if(a.content.includes(url)){await done(a);return res.status(200).json({ok:true});}
    const label=captions.join(' | ').replace(/[\[\]\r\n]/g,'').slice(0,180)||'ملف مرفق';
    await done(await update(a,{content:a.content+'\n\n!['+label+']('+url+')\n\n'}));
   }else if(cmd.name==='failures'){
    const rows=await sql.query("SELECT data FROM blog_public_metadata WHERE id LIKE 'telegram_failure:%' ORDER BY updated_at DESC LIMIT 15",[]);
    await reply(rows.length?rows.map(r=>r.data.at+' · /'+r.data.command+' · '+r.data.code).join('\n'):'لا توجد عمليات فاشلة مسجلة.');
   }else if(['show','link'].includes(cmd.name)){
    const a=await lookup(cmd.args);
    await reply((cmd.name==='show'?a.title+'\nالحالة: '+(isScheduled(a)?'مجدول إلى '+a.scheduledAt:a.status)+'\nالقسم: '+a.category+'\n'+a.content.slice(0,1800)+'\n':'')+articleLink(a.slug));
   }else if(['publish','unpublish','featured'].includes(cmd.name)){
    const a=await lookup(cmd.args);
    await done(await update(a,cmd.name==='featured'?{featured:!a.featured}:{status:cmd.name==='publish'?'published':'draft',scheduledAt:''}));
   }else if(['title','text','excerpt','cover','category','author'].includes(cmd.name)){
    const pos=cmd.args.indexOf('|');
    if(pos<1)throw Error('الصيغة: /'+cmd.name+' slug | القيمة الجديدة');
    const slug=cmd.args.slice(0,pos).trim(),value=cmd.args.slice(pos+1).trim();
    if(!value)throw Error('القيمة الجديدة لا يمكن أن تكون فارغة.');
    if(cmd.name==='cover'&&!/^https:\/\//i.test(value))throw Error('رابط الصورة يجب أن يبدأ بـ HTTPS.');
    const a=await lookup(slug);
    const field={title:'title',text:'content',excerpt:'excerpt',cover:'coverImage',category:'category',author:'author'}[cmd.name];
    await done(await update(a,{[field]:value,...(field==='author'?{authorId:'',authorImage:''}:{})}));
   }else if(cmd.name==='delete'){
    const a=await lookup(cmd.args);
    if(a.id.length>55)throw Error('معرف المقالة طويل؛ احذفها من الموقع.');
    await reply('⚠️ حذف «'+a.title+'» نهائيًا؟',[[{text:'تأكيد الحذف',callback_data:'del:'+a.id},{text:'إلغاء',callback_data:'cancel'}]]);
   }else if(cmd.name==='site'){
    const data=await getEditorial();
    await reply('🏛 '+(data.site?.name||'مدونة معتز العلقمي')+'\n'+(data.site?.tagline||'')+'\n'+SITE);
   }else if(cmd.name==='aboutset'){
    if(!cmd.args||cmd.args.length<10||cmd.args.length>12000)throw Error('اكتب نبذة من 10 أحرف فأكثر.');
    await confirmSetting('about',cmd.args);
   }else if(cmd.name==='quotes'){
    const data=await getEditorial();
    const quotes=data.quotes||[];
    await reply('📚 اقتباسات المدونة: '+quotes.length+'\n'+(quotes.length?quotes.slice(0,20).map((q,i)=>(i+1)+'. '+q.text.slice(0,90)+' — '+q.author).join('\n'):'لا توجد اقتباسات.'));
   }else if(cmd.name==='quoteadd'){
    const items=cmd.args.split('|').map(x=>x.trim());
    if(items.length!==3||!items[0]||items[0].length>1200||items[1].length>160||items[2].length>160)
     throw Error('الصيغة: /quoteadd نص الاقتباس | القائل | المصدر');
    const current=await getEditorial(),quotes=current.quotes||[];
    if(quotes.length>=40)throw Error('لا يمكن إضافة أكثر من 40 اقتباسًا.');
    await setEditorial({...current,quotes:[...quotes,{text:items[0],author:items[1],source:items[2]}]});
    await reply('✅ أضيف الاقتباس إلى المدونة.');
   }else if(cmd.name==='quotedelete'){
    const current=await getEditorial(),quotes=current.quotes||[],index=Number(cmd.args)-1;
    if(!Number.isInteger(index)||index<0||index>=quotes.length)throw Error('اكتب رقم اقتباس موجود من /quotes.');
    const signature=createHash('sha256').update(quotes[index].text).digest('hex').slice(0,12);
    await reply('هل تريد حذف هذا الاقتباس؟\n'+quotes[index].text.slice(0,200),
     [[{text:'تأكيد الحذف',callback_data:'qdel:'+index+':'+signature},{text:'إلغاء',callback_data:'cancel'}]]);
   }else if(cmd.name==='tagline'){
    if(!cmd.args||cmd.args.length>240)throw Error('اكتب عبارة لا تزيد على 240 حرفًا.');
    await confirmSetting('tagline',cmd.args);
   }else await reply('أمر غير معروف. أرسل /help.');
   return res.status(200).json({ok:true});
  }catch(e){
   if(authorized)try{await sqlFactory().query('INSERT INTO blog_public_metadata(id,data) VALUES($1,$2::jsonb)',
    ['telegram_failure:'+randomUUID(),JSON.stringify({at:new Date().toISOString(),command:telegramCommand(msg?.text||msg?.caption)?.name||'callback',code:e?.code==='23514'?'MEDIA_RESERVED':'OPERATION_FAILED'})]);}catch{}
   try{await reply('تعذّر تنفيذ الأمر: '+errorText(e));}catch{}
   return res.status(200).json({ok:true});
  }
 };
}
export default createTelegramHandler();

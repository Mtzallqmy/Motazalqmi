import {useState} from 'react';
import {Megaphone} from 'lucide-react';
import {AnnouncementTicker} from './AnnouncementTicker';
import {normalizeTicker, TICKER_INTERVALS, TICKER_LABELS, TICKER_LIMIT} from '../../shared/announcementTicker';

export function AnnouncementEditor({ticker, save}) {
  const [draft, setDraft] = useState(null);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const updateItem = (index, patch) => setDraft(current => ({...current,
    items:current.items.map((item,key) => key === index ? {...item,...patch} : item)}));
  async function submit(event) {
    event.preventDefault();
    setError('');
    setMessage('');
    setBusy(true);
    try {
      await save(normalizeTicker(draft));
      setDraft(null);
      setMessage('حُفظ شريط الإعلانات.');
    } catch (failure) { setError(failure.message || 'تعذر حفظ شريط الإعلانات.'); }
    finally { setBusy(false); }
  }
  return <section className="announcement-editor">
    <div className="flex flex-wrap items-center justify-between gap-3">
      <h2 className="flex items-center gap-2 font-literature text-2xl font-bold"><Megaphone size={22}/> شريط الإعلانات</h2>
      {!draft && <button type="button" className="announcement-primary" onClick={() => {setDraft(ticker);setMessage('');}}>تحرير شريط الإعلانات</button>}
    </div>
    {message && <p role="status">{message}</p>}
    {!draft && <p>{ticker.enabled ? `ظاهر في الرئيسية · ${ticker.items.length} من العبارات` : 'مخفي من الرئيسية'}</p>}
    {draft && <form onSubmit={submit} className="space-y-4">
      <fieldset disabled={busy} className="space-y-4">
        <div className="flex flex-wrap items-center gap-5">
          <label className="flex items-center gap-2"><input type="checkbox" checked={draft.enabled} onChange={e => setDraft({...draft,enabled:e.target.checked})}/> إظهار الشريط في الرئيسية</label>
          <label className="flex items-center gap-2">وقت التبديل <select value={draft.intervalSeconds} onChange={e => setDraft({...draft,intervalSeconds:Number(e.target.value)})}>
            {TICKER_INTERVALS.map(value => <option key={value} value={value}>{value} ثوانٍ</option>)}
          </select></label>
        </div>
        {draft.items.map((item,index) => <fieldset className="announcement-item" key={index}>
          <legend>العبارة {index + 1}</legend>
          <label>النوع <select value={item.kind} onChange={e => updateItem(index,{kind:e.target.value})}>
            {Object.entries(TICKER_LABELS).map(([id,label]) => <option value={id} key={id}>{label}</option>)}
          </select></label>
          <label>النص<textarea rows={2} value={item.text} maxLength={200} required onChange={e => updateItem(index,{text:e.target.value})}/></label>
          <label>الرابط (اختياري)<input dir="ltr" placeholder="https://… أو /articles/…" value={item.link} maxLength={2048} onChange={e => updateItem(index,{link:e.target.value})}/></label>
          <button type="button" className="text-rust underline" onClick={() => setDraft({...draft,items:draft.items.filter((_,key) => key !== index)})}>إزالة العبارة</button>
        </fieldset>)}
        <button type="button" className="text-teal underline" disabled={draft.items.length >= TICKER_LIMIT} onClick={() => setDraft({...draft,items:[...draft.items,{kind:'announcement',text:'',link:''}]})}>إضافة إعلان أو عبارة</button>
        {draft.enabled && draft.items.some(item => item.text.trim()) && <AnnouncementTicker ticker={{...draft,items:draft.items.filter(item => item.text.trim())}} preview/>}
        {error && <p role="alert" className="text-red-800">{error}</p>}
        <div className="flex gap-3"><button className="announcement-primary" type="submit">حفظ الشريط</button><button type="button" onClick={() => {setDraft(null);setError('');}}>إلغاء</button></div>
      </fieldset>
    </form>}
  </section>;
}

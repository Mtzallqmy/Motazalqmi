import {useEffect, useState} from 'react';
import {ChevronLeft, ChevronRight, Megaphone, Pause, Play} from 'lucide-react';
import {safeTickerLink, TICKER_LABELS} from '../../shared/announcementTicker';

export function AnnouncementTicker({ticker, preview = false}) {
  const [index, setIndex] = useState(0);
  const [paused, setPaused] = useState(false);
  const [focused, setFocused] = useState(false);
  const [hovering, setHovering] = useState(false);
  const items = ticker?.items || [];
  useEffect(() => {
    if (!ticker?.enabled || items.length < 2 || paused || focused || hovering) return;
    const preference = window.matchMedia('(prefers-reduced-motion: reduce)');
    let timer;
    function schedule() {
      clearInterval(timer);
      if (!preference.matches && !document.hidden) {
        timer = setInterval(() => setIndex(current => (current + 1) % items.length), ticker.intervalSeconds * 1000);
      }
    }
    schedule();
    preference.addEventListener('change', schedule);
    document.addEventListener('visibilitychange', schedule);
    return () => {
      clearInterval(timer);
      preference.removeEventListener('change', schedule);
      document.removeEventListener('visibilitychange', schedule);
    };
  }, [ticker?.enabled, ticker?.intervalSeconds, items.length, paused, focused, hovering]);
  if (!ticker?.enabled || !items.length) return null;
  const current = items[index % items.length];
  const link = safeTickerLink(current.link || '');
  const previous = () => setIndex(value => (value + items.length - 1) % items.length);
  const next = () => setIndex(value => (value + 1) % items.length);
  return <section className="announcement-ticker" aria-label="شريط الإعلانات"
    onMouseEnter={() => setHovering(true)} onMouseLeave={() => setHovering(false)}
    onFocus={() => setFocused(true)} onBlur={event => { if (!event.currentTarget.contains(event.relatedTarget)) setFocused(false); }}>
    <div className="announcement-label"><Megaphone size={17} aria-hidden="true"/><span>{TICKER_LABELS[current.kind]}</span></div>
    <div className="announcement-message" key={`${index}-${current.text}`}>
      {link && !preview ? <a href={link}>{current.text}</a> : <span>{current.text}</span>}
    </div>
    {items.length > 1 && <div className="announcement-controls">
      <button type="button" aria-label="الإعلان السابق" onClick={previous}><ChevronRight size={17}/></button>
      <button type="button" aria-label={paused ? 'تشغيل شريط الإعلانات' : 'إيقاف شريط الإعلانات'} onClick={() => setPaused(value => !value)}>
        {paused ? <Play size={15}/> : <Pause size={15}/>}
      </button>
      <button type="button" aria-label="الإعلان التالي" onClick={next}><ChevronLeft size={17}/></button>
    </div>}
  </section>;
}

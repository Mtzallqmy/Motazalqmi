import { Link, useSearchParams } from 'react-router-dom';
import { ArrowLeft, ArrowRight, ClipboardList, HeartPulse, MessageCircleQuestion, Pill, ShieldCheck } from 'lucide-react';
import { HEALTH_NOTICE, HEALTH_SECTION_ID, HEALTH_TOPICS } from '../../shared/healthDesk.js';
import { ArticleCard } from './ArticleCard';
import { EmptyState } from './Ornaments';

const healthPath = `/category/${HEALTH_SECTION_ID}`;
const topicIcons = { safety: ShieldCheck, antibiotics: Pill, pharmacy: MessageCircleQuestion };

export function HealthFeature() {
  return <section className="health-feature" aria-labelledby="health-feature-title">
    <div className="health-feature-emblem" aria-hidden="true"><HeartPulse size={34}/></div>
    <div><h2 id="health-feature-title">الصحة والدواء</h2>
      <p>كيف تقرأ ملصق الدواء؟ وماذا تسأل الصيدلي؟ أدلة عربية قصيرة، مع مصادر للقراءة.</p></div>
    <Link className="health-feature-link" to={healthPath}>تصفّح القسم <ArrowLeft size={19} aria-hidden="true"/></Link>
  </section>;
}

export function HealthNotice() {
  return <aside className="health-notice"><ShieldCheck size={22} aria-hidden="true"/><p>{HEALTH_NOTICE}</p></aside>;
}

export function HealthDesk({ articles, loading, error, interactions }) {
  const [params] = useSearchParams();
  const selected = HEALTH_TOPICS.find(topic => topic.id === params.get('topic'));
  const visible = selected ? articles.filter(article => article.tags?.includes(selected.title)) : articles;
  return <div className="edition health-desk">
    <header className="health-edition-header">
      <div className="health-edition-copy">
        <Link to="/" className="health-home"><ArrowRight size={16} aria-hidden="true"/> الرئيسية</Link>
        <h1>الصحة والدواء</h1>
        <p>مقالات توعوية بالعربية تساعدك على فهم الأدوية، وقراءة ملصقاتها، وطرح الأسئلة المناسبة في الصيدلية.</p>
      </div>
      <div className="health-edition-emblem" aria-hidden="true"><HeartPulse size={96} strokeWidth={1.25}/></div>
    </header>
    <nav className="health-topics" aria-label="مسارات الصحة والدواء">
      {HEALTH_TOPICS.map(topic => {
        const Icon = topicIcons[topic.id];
        return <Link key={topic.id} to={`${healthPath}?topic=${topic.id}`} aria-current={selected?.id === topic.id ? 'page' : undefined}>
          <span className="health-topic-icon"><Icon size={22} aria-hidden="true"/></span>
          <span><strong>{topic.title}</strong><small>{topic.description}</small></span>
          <ArrowLeft size={17} aria-hidden="true"/>
        </Link>;
      })}
    </nav>
    <HealthNotice/>
    <section className="health-articles" aria-labelledby="health-articles-title">
      <div className="health-articles-heading"><h2 id="health-articles-title"><ClipboardList aria-hidden="true"/>{selected?.title ?? 'المقالات التوعوية'}</h2>
        {selected && <Link to={healthPath}>عرض كل المقالات</Link>}</div>
      {error && <p role="alert">{error}</p>}
      {loading ? <p role="status">جار تحميل المقالات…</p> : visible.length ?
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">{visible.map(article => <ArticleCard key={article.id} article={article} {...interactions}/>)}</div> :
        !error && <EmptyState title="لا توجد مقالات في هذا المسار بعد" description="ستظهر المقالات هنا فور نشرها"/>}
    </section>
  </div>;
}

package com.moataz.motazalqmi;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Fully native Android reader. All editorial content comes from Neon via API v1. */
public final class MainActivity extends Activity {
    private BlogStore store;
    private final List<JSONObject> displayed=new ArrayList<>();
    private LinearLayout root,nav,sectionsRow;
    private FrameLayout main;
    private TextView subtitle,network;
    private EditText search;
    private HorizontalScrollView sectionsScroller;
    private ListView list;
    private ArticleAdapter adapter;
    private int tab=0;
    private String category="",query="";
    private String pendingSlug;
    private boolean loading;

    @Override public void onCreate(Bundle b){super.onCreate(b);store=BlogStore.get(this);setup();handleDeepLink(getIntent());render();sync();}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);handleDeepLink(intent);openPending();}
    @Override protected void onResume(){super.onResume();if(tab==2)render();}
    private void setup(){
        root=Ui.vertical(this);root.setBackgroundColor(Ui.PAPER);Ui.window(this,root);
        LinearLayout masthead=Ui.vertical(this);Ui.pad(masthead,this,20,15);masthead.setBackground(Ui.gradient(this));
        TextView ornament=Ui.text(this,"✧  الكلمةُ وطنٌ  ✧",13,Ui.GOLD,true);ornament.setGravity(Gravity.CENTER);masthead.addView(ornament);
        TextView title=Ui.title(this,"مدونة معتز العلقمي",27);title.setTextColor(Ui.WHITE);title.setGravity(Gravity.CENTER);masthead.addView(title,Ui.margins(this,-1,-2,0,8,0,0));
        subtitle=Ui.text(this,"اللغة  •  الأدب  •  الشعر  •  التاريخ  •  الفكر",13,Ui.WHITE,false);subtitle.setGravity(Gravity.CENTER);masthead.addView(subtitle);
        network=Ui.text(this,"جارِ التحقق من أحدث المقالات…",11,Ui.GOLD,false);network.setGravity(Gravity.CENTER);masthead.addView(network,Ui.margins(this,-1,-2,0,8,0,0));
        root.addView(masthead,Ui.margins(this,-1,-2,10,10,10,0));
        search=new EditText(this);search.setSingleLine(true);search.setTextSize(15);search.setHint("ابحث في العناوين والنصوص…");search.setTextColor(Ui.DARK);search.setHintTextColor(Ui.MUTED);
        search.setBackground(Ui.bg(Ui.WHITE,15,this));search.setPadding(Ui.dp(this,14),0,Ui.dp(this,14),0);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int st,int before,int count){query=s.toString();filter();}public void afterTextChanged(Editable e) {}});
        root.addView(search,Ui.margins(this,-1,48,15,10,15,8));
        sectionsScroller=new HorizontalScrollView(this);sectionsScroller.setHorizontalScrollBarEnabled(false);
        sectionsRow=Ui.horizontal(this);Ui.pad(sectionsRow,this,12,3);sectionsScroller.addView(sectionsRow);root.addView(sectionsScroller);
        main=new FrameLayout(this);root.addView(main,new LinearLayout.LayoutParams(-1,0,1));
        nav=Ui.horizontal(this);nav.setBackgroundColor(Ui.WHITE);nav.setElevation(Ui.dp(this,8));root.addView(nav,Ui.lp(this,-1,62));
        setContentView(root);
    }
    private void sync(){loading=true;showSync();store.sync(this,(ok,err)->{loading=false;showSync();render();openPending();});}
    private void showSync(){int count=store.articles().size();
        network.setText(loading?"◌  جاري تحديث المقالات من Neon":(store.syncedAt()>0?"●  متصل بالمكتبة الثقافية  ·  "+count+" مقالة محفوظة":"○  اتصل بالإنترنت لتحميل المكتبة"));
    }
    private void render(){showSync();buildNav();buildChips();search.setVisibility(tab==0||tab==2?View.VISIBLE:View.GONE);sectionsScroller.setVisibility(tab==0?View.VISIBLE:View.GONE);
        main.removeAllViews();
        if(tab==0||tab==2)showArticles();else if(tab==1)showSections();else showAbout();
    }
    private void buildNav(){nav.removeAllViews();String[] labels={"⌂  الرئيسية","▦  الأقسام","☆  المحفوظات","◈  حولنا"};
        for(int i=0;i<labels.length;i++){final int selected=i;TextView v=Ui.text(this,labels[i],12,tab==i?Ui.TEAL:Ui.MUTED,tab==i);v.setGravity(Gravity.CENTER);
            v.setBackground(tab==i?Ui.bg(Ui.PALE,12,this):Ui.bg(Ui.WHITE,12,this));v.setOnClickListener(w->{tab=selected;category="";if(tab!=0){query="";search.setText("");}render();});
            nav.addView(v,new LinearLayout.LayoutParams(0,Ui.dp(this,52),1));}
    }
    private void buildChips(){sectionsRow.removeAllViews();addChip("الكل","");
        for(JSONObject s:store.sections()){if("about".equals(s.optString("id")))continue;addChip(s.optString("title"),s.optString("id"));}
    }
    private void addChip(String text,String id){TextView chip=Ui.pill(this,text,category.equals(id));chip.setOnClickListener(v->{
        if(id.equals("quotes")){tab=1;category="";render();return;}
        category=id;tab=0;render();
    });sectionsRow.addView(chip,Ui.margins(this,-2,42,4,0,4,0));}
    private void showArticles(){
        LinearLayout pane=Ui.vertical(this);
        TextView heading=Ui.dividerTitle(this,tab==2?"المحفوظات على جهازك":category.isEmpty()?"من وحي المقالات":"مقالات القسم");
        pane.addView(heading,Ui.margins(this,-1,48,17,6,17,0));
        TextView caption=Ui.text(this,tab==2?"يمكنك قراءة المقالات المحفوظة دون اتصال بالشبكة.":"نصوص مختارة من بطون الكتب وبلاغة الشعر العربي.",12,Ui.MUTED,false);pane.addView(caption,Ui.margins(this,-1,-2,18,0,18,8));
        list=new ListView(this);list.setDivider(null);list.setDividerHeight(0);list.setCacheColorHint(android.graphics.Color.TRANSPARENT);
        adapter=new ArticleAdapter();list.setAdapter(adapter);list.setOnItemClickListener((p,v,pos,id)->openArticle(displayed.get(pos)));
        pane.addView(list,new LinearLayout.LayoutParams(-1,0,1));main.addView(pane);
        filter();
    }
    private String normalize(String s){return s.toLowerCase(Locale.ROOT).replace('أ','ا').replace('إ','ا').replace('آ','ا').replace('ى','ي').replace('ة','ه');}
    private void filter(){if(adapter==null||list==null||!(tab==0||tab==2))return;displayed.clear();String q=normalize(query.trim());
      for(JSONObject a:store.articles()){
        if(tab==2&&!store.isFavorite(a.optString("id")))continue;
        if(tab==0&&!category.isEmpty()&&!category.equals(a.optString("category")))continue;
        String searchable=normalize(a.optString("title")+" "+a.optString("excerpt")+" "+a.optString("content"));
        if(!q.isEmpty()&&!searchable.contains(q))continue;
        displayed.add(a);
      }
      adapter.notifyDataSetChanged();
      if(displayed.isEmpty()){TextView empty=Ui.text(this,store.articles().isEmpty()?"المكتبة تنتظر أول اتصال بالإنترنت. اسحب لتحديث المحتوى من زر المزامنة في الأقسام.":"لا توجد مقالات مطابقة هنا.",16,Ui.MUTED,false);empty.setGravity(Gravity.CENTER);Ui.pad(empty,this,20,28);list.setEmptyView(null);
        // Empty-state text is drawn above the list to avoid adding/removing children from the list itself.
        if(list.getParent() instanceof LinearLayout){LinearLayout parent=(LinearLayout)list.getParent();
          if(parent.findViewWithTag("empty")!=null)parent.removeView(parent.findViewWithTag("empty"));empty.setTag("empty");parent.addView(empty,2);}
      }else if(list.getParent() instanceof LinearLayout){LinearLayout parent=(LinearLayout)list.getParent();View old=parent.findViewWithTag("empty");if(old!=null)parent.removeView(old);}
    }
    private void showSections(){ScrollView sc=new ScrollView(this);LinearLayout page=Ui.vertical(this);Ui.pad(page,this,17,12);
        page.addView(Ui.dividerTitle(this,"رفوف المكتبة"));
        TextView sync=Ui.pill(this,loading?"تحديث جارٍ…":"⟳  تحديث المقالات من Neon",false);sync.setGravity(Gravity.CENTER);sync.setOnClickListener(v->{if(!loading)sync();});page.addView(sync,Ui.margins(this,-1,48,0,5,0,14));
        for(JSONObject s:store.sections()){
            String id=s.optString("id");if("about".equals(id))continue;
            LinearLayout card=Ui.vertical(this);Ui.card(card,this);Ui.pad(card,this,16,12);
            card.addView(Ui.title(this,"◈  "+s.optString("title"),19));card.addView(Ui.text(this,s.optString("subtitle"),13,Ui.MUTED,false));
            card.setOnClickListener(v->{if("quotes".equals(id)){showQuoteDialog();return;}category=id;tab=0;render();});
            page.addView(card,Ui.margins(this,-1,-2,1,6,1,8));
        }
        page.addView(Ui.dividerTitle(this,"مختارات من الأقوال"));
        for(JSONObject q:store.quotes())page.addView(quoteCard(q),Ui.margins(this,-1,-2,1,6,1,8));
        sc.addView(page);main.addView(sc);
    }
    private LinearLayout quoteCard(JSONObject q){LinearLayout card=Ui.vertical(this);Ui.card(card,this);Ui.pad(card,this,17,16);
        TextView text=Ui.title(this,"❝  "+q.optString("text"),18);card.addView(text);
        card.addView(Ui.text(this,"— "+q.optString("author")+"  ·  "+q.optString("source"),12,Ui.MUTED,false),Ui.margins(this,-1,-2,0,10,0,0));
        return card;
    }
    private void showQuoteDialog(){new android.app.AlertDialog.Builder(this).setTitle("اقتباسات من المدونة")
      .setItems(store.quotes().stream().map(q->q.optString("text")).toArray(String[]::new),(dialog,which)->{
        JSONObject q=store.quotes().get(which);
        new android.app.AlertDialog.Builder(this).setTitle(q.optString("author")).setMessage(q.optString("text")+"\n\n"+q.optString("source")).setPositiveButton("حسنًا",null).show();
      }).setNegativeButton("إغلاق",null).show();
    }
    private void showAbout(){ScrollView sc=new ScrollView(this);LinearLayout content=Ui.vertical(this);Ui.pad(content,this,20,22);
        content.addView(Ui.dividerTitle(this,"رسالة المدونة"));
        JSONObject site=store.site();String about=site!=null?site.optString("about"):"مدونة ثقافية تهتم باللغة والأدب والتاريخ والفكر والشعر العربي.";
        TextView text=Ui.text(this,about,19,Ui.DARK,false);text.setLineSpacing(Ui.dp(this,9),1.24f);content.addView(text);
        content.addView(Ui.gap(this,18));
        content.addView(Ui.title(this,"من أقسامنا",20));
        for(JSONObject cat:store.sections())content.addView(Ui.text(this,"•  "+cat.optString("title"),17,Ui.TEAL,true),Ui.margins(this,-1,-2,0,6,0,6));
        TextView siteLink=Ui.pill(this,"زيارة المدونة الإلكترونية ↗",false);siteLink.setGravity(Gravity.CENTER);
        siteLink.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse("https://motazalqmi.vercel.app"));startActivity(i);});
        content.addView(siteLink,Ui.margins(this,-1,50,0,24,0,16));
        content.addView(Ui.text(this,"برمجة وتطوير معتز العلقمي – تعز – اليمن – 2026م\nالإصدار 1.0.0  |  Android 8.0+  |  "+ NativeIdentity.abi()+"\nالمقالات من Neon، وتحفظ محليًا للقراءة دون إنترنت.",12,Ui.MUTED,false));
        sc.addView(content);main.addView(sc);
    }
    private void openArticle(JSONObject a){Intent i=new Intent(this,ArticleActivity.class);i.putExtra("id",a.optString("id"));startActivity(i);}
    private void handleDeepLink(Intent i){Uri u=i==null?null:i.getData();if(u!=null&&"motazalqmi.vercel.app".equals(u.getHost())){
        List<String> segments=u.getPathSegments();if(segments.size()==2&&"articles".equals(segments.get(0)))pendingSlug=segments.get(1);
    }}
    private void openPending(){if(pendingSlug==null)return;String slug=pendingSlug;JSONObject a=store.articleById(slug);if(a!=null){pendingSlug=null;openArticle(a);}
        else store.retrieveArticle(this,slug,article->{if(article!=null&&slug.equals(pendingSlug)){
          pendingSlug=null;Intent intent=new Intent(this,ArticleActivity.class);intent.putExtra("json",article.toString());startActivity(intent);
        }});
    }
    private final class ArticleAdapter extends BaseAdapter {
        @Override public int getCount(){return displayed.size();}
        @Override public Object getItem(int p){return displayed.get(p);}
        @Override public long getItemId(int p){return p;}
        @Override public View getView(int pos,View convert,ViewGroup parent){JSONObject a=displayed.get(pos);
            LinearLayout card=Ui.horizontal(MainActivity.this);Ui.card(card,MainActivity.this);Ui.pad(card,MainActivity.this,12,13);
            LinearLayout words=Ui.vertical(MainActivity.this);
            TextView badge=Ui.text(MainActivity.this,categoryName(a.optString("category"))+"  •  "+a.optString("publishDate"),11,Ui.GOLD,true);words.addView(badge);
            TextView title=Ui.title(MainActivity.this,a.optString("title"),18);title.setMaxLines(3);title.setEllipsize(android.text.TextUtils.TruncateAt.END);words.addView(title,Ui.margins(MainActivity.this,-1,-2,0,6,0,4));
            TextView detail=Ui.text(MainActivity.this,a.optString("excerpt"),12,Ui.MUTED,false);detail.setMaxLines(2);detail.setEllipsize(android.text.TextUtils.TruncateAt.END);words.addView(detail);
            TextView read=Ui.text(MainActivity.this,"اقرأ المقالة ←  "+a.optString("readTime"),11,Ui.TEAL,true);words.addView(read,Ui.margins(MainActivity.this,-1,-2,0,8,0,0));
            ImageView cover=new ImageView(MainActivity.this);cover.setScaleType(ImageView.ScaleType.CENTER_CROP);cover.setBackground(Ui.bg(Ui.PALE,13,MainActivity.this));
            card.addView(cover,Ui.margins(MainActivity.this,92,112,0,0,12,0));card.addView(words,new LinearLayout.LayoutParams(0,-2,1));
            CoverImages.load(MainActivity.this,cover,a.optString("coverImage"));
            LinearLayout holder=Ui.vertical(MainActivity.this);holder.addView(card,Ui.margins(MainActivity.this,-1,-2,15,7,15,7));
            return holder;
        }
    }
    private String categoryName(String id){for(JSONObject c:store.sections())if(id.equals(c.optString("id")))return c.optString("title");return "مقالة";}
}

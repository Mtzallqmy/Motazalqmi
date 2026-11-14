package com.moataz.motazalqmi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.text.util.Linkify;
import android.text.method.LinkMovementMethod;
import org.json.JSONObject;

public final class ArticleActivity extends Activity {
    private BlogStore store;
    private TextView favorite;
    private JSONObject item;
    private TextView body;
    private float fontSize=18;
    @Override public void onCreate(Bundle state){super.onCreate(state);store=BlogStore.get(this);
        String id=getIntent().getStringExtra("id");
        item=store.articleById(id==null?"":id);
        if(item==null && getIntent().hasExtra("json")){try{item=new JSONObject(getIntent().getStringExtra("json"));}catch(Exception ignored){}}
        if(item==null){finish();return;}
        display();
    }
    private void display(){
        LinearLayout root=Ui.vertical(this);root.setBackgroundColor(Ui.PAPER);Ui.window(this,root);
        LinearLayout top=Ui.horizontal(this);top.setBackgroundColor(Ui.TEAL);Ui.pad(top,this,12,8);
        Button back=Ui.button(this,"رجوع ←",false);back.setOnClickListener(v->finish());top.addView(back,Ui.lp(this,90,45));
        TextView app=Ui.text(this,"مدونة معتز العلقمي",17,Ui.WHITE,true);app.setGravity(Gravity.CENTER);top.addView(app,new LinearLayout.LayoutParams(0,Ui.dp(this,45),1));
        root.addView(top);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        LinearLayout content=Ui.vertical(this);Ui.pad(content,this,18,24);
        String image=item.optString("coverImage");
        if(image.startsWith("https://")){
            ImageView cover=new ImageView(this);cover.setScaleType(ImageView.ScaleType.CENTER_CROP);cover.setBackground(Ui.bg(Ui.PALE,18,this));
            content.addView(cover,Ui.margins(this,-1,185,0,0,0,20));CoverImages.load(this,cover,image);
        }
        TextView category=Ui.text(this,storeCategory(item.optString("category"))+"  •  "+item.optString("publishDate"),13,Ui.GOLD,true);content.addView(category);
        TextView title=Ui.title(this,item.optString("title"),28);title.setLineSpacing(Ui.dp(this,8),1.16f);content.addView(title,Ui.margins(this,-1,-2,0,12,0,14));
        TextView summary=Ui.text(this,item.optString("excerpt"),15,Ui.MUTED,false);content.addView(summary);
        content.addView(Ui.gap(this,18));
        LinearLayout tools=Ui.horizontal(this);
        favorite=Ui.pill(this,"",false);favorite.setOnClickListener(v->{store.toggleFavorite(item.optString("id"));updateFavorite();});updateFavorite();tools.addView(favorite,Ui.margins(this,-2,46,0,0,7,0));
        Button share=Ui.button(this,"مشاركة ↗",false);share.setOnClickListener(v->{
           Intent send=new Intent(Intent.ACTION_SEND);send.setType("text/plain");send.putExtra(Intent.EXTRA_TEXT,item.optString("title")+"\nhttps://motazalqmi.vercel.app/articles/"+item.optString("slug"));startActivity(Intent.createChooser(send,"مشاركة المقال"));
        });tools.addView(share,Ui.margins(this,105,46,0,0,7,0));
        Button smaller=Ui.button(this,"أ−",false);smaller.setOnClickListener(v->resize(-2));tools.addView(smaller,Ui.margins(this,48,46,0,0,7,0));
        Button bigger=Ui.button(this,"أ+",false);bigger.setOnClickListener(v->resize(2));tools.addView(bigger,Ui.lp(this,48,46));
        content.addView(tools);content.addView(Ui.gap(this,20));
        View rule=new View(this);rule.setBackgroundColor(Ui.GOLD);content.addView(rule,Ui.lp(this,-1,1));content.addView(Ui.gap(this,20));
        body=Ui.text(this,"",(int)fontSize,Ui.DARK,false);body.setLineSpacing(Ui.dp(this,9),1.30f);
        body.setText(ArticleFormat.render(item.optString("content")));Linkify.addLinks(body,Linkify.WEB_URLS);body.setMovementMethod(LinkMovementMethod.getInstance());
        content.addView(body);content.addView(Ui.gap(this,32));
        TextView credit=Ui.text(this,"✦  من كلمات معتز العلقمي\nتعز – اليمن  |  جميع الحقوق محفوظة",13,Ui.MUTED,false);credit.setGravity(Gravity.CENTER);content.addView(credit);
        scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }
    private void updateFavorite(){if(favorite!=null)favorite.setText(store.isFavorite(item.optString("id"))?"★ محفوظ":"☆ احفظ");}
    private void resize(int step){fontSize=Math.min(30,Math.max(14,fontSize+step));body.setTextSize(fontSize);}
    private String storeCategory(String id){for(JSONObject o:store.sections())if(id.equals(o.optString("id")))return o.optString("title");return "مقالة";}
}

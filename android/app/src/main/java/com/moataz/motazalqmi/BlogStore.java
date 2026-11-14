package com.moataz.motazalqmi;

import android.content.Context;
import android.app.Activity;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Only the published content from the website's Neon-backed API is cached locally.
 *  Neither DATABASE_URL nor administrator credentials are present in this APK.
 */
public final class BlogStore {
    private static final String BASE="https://motazalqmi.vercel.app";
    private static BlogStore instance;
    private final Context context;
    private final SharedPreferences preferences;
    private JSONObject cache=new JSONObject();
    private boolean syncing;

    private BlogStore(Context c){context=c.getApplicationContext();preferences=context.getSharedPreferences("motazalqmi-reader-v1",Context.MODE_PRIVATE);load();}
    public static synchronized BlogStore get(Context c){if(instance==null)instance=new BlogStore(c);return instance;}
    private File cachedFile(){return new File(context.getFilesDir(),"published-content-v1.json");}
    private void load(){try(FileInputStream in=new FileInputStream(cachedFile())){cache=new JSONObject(read(in,8*1024*1024));}catch(Exception ignored){cache=new JSONObject();}}
    private static String read(InputStream input,int max) throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=input.read(buf))!=-1){if(out.size()+n>max)throw new IllegalArgumentException("Response too large");out.write(buf,0,n);}return out.toString("UTF-8");}
    private static JSONObject fetch(String path) throws Exception {
        URL url=new URL(BASE+path);
        HttpURLConnection c=(HttpURLConnection)url.openConnection();
        c.setConnectTimeout(12000);c.setReadTimeout(20000);c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent","MotazalqmiAndroid/1.0");
        c.setInstanceFollowRedirects(false);
        try{
            if(c.getResponseCode()!=200)throw new IllegalStateException("API HTTP "+c.getResponseCode());
            try(InputStream in=c.getInputStream()){return new JSONObject(read(in,8*1024*1024));}
        }finally{c.disconnect();}
    }
    public synchronized List<JSONObject> articles(){return objects(cache.optJSONArray("articles"));}
    public synchronized List<JSONObject> sections(){return objects(cache.optJSONArray("sections"));}
    public synchronized List<JSONObject> quotes(){return objects(cache.optJSONArray("quotes"));}
    public synchronized JSONObject site(){return cache.optJSONObject("site");}
    private static List<JSONObject> objects(JSONArray arr){List<JSONObject> out=new ArrayList<>();if(arr!=null)for(int i=0;i<arr.length();i++){JSONObject o=arr.optJSONObject(i);if(o!=null)out.add(o);}return out;}
    public synchronized JSONObject articleById(String id){for(JSONObject a:articles())if(id.equals(a.optString("id"))||id.equals(a.optString("slug")))return a;return null;}
    public Set<String> favorites(){return new HashSet<>(preferences.getStringSet("saved",new HashSet<>()));}
    public boolean isFavorite(String id){return favorites().contains(id);}
    public boolean toggleFavorite(String id){Set<String> ids=favorites();boolean added;if(ids.contains(id)){ids.remove(id);added=false;}else{ids.add(id);added=true;}preferences.edit().putStringSet("saved",ids).apply();return added;}
    public long syncedAt(){return preferences.getLong("synced_at",0);}
    public interface OnSync {void completed(boolean success,String error);}
    public void sync(Activity activity,OnSync callback){synchronized(this){if(syncing)return;syncing=true;}
      new Thread(()->{boolean ok=false;String error="";
        try {
            JSONObject result=fetch("/api/v1/bootstrap");
            if(result.optInt("apiVersion")!=1)throw new IllegalStateException("Unsupported API version");
            JSONArray all=result.optJSONArray("articles");if(all==null)throw new IllegalStateException("Missing articles");
            int offset=all.length();int total=result.optInt("total",offset);
            while(offset<total){
                // Full article bodies are requested for genuinely offline reading.
                JSONObject extra=fetch("/api/v1/articles?full=1&limit=100&offset="+offset);
                JSONArray next=extra.optJSONArray("articles");
                if(next==null||next.length()==0)throw new IllegalStateException("Incomplete pagination");
                for(int i=0;i<next.length();i++)all.put(next.getJSONObject(i));
                offset=all.length();
                if(offset>10000)throw new IllegalStateException("Library exceeds safe local cache size");
            }
            result.put("articles",all);
            byte[] bytes=result.toString().getBytes(StandardCharsets.UTF_8);
            if(bytes.length>24*1024*1024)throw new IllegalStateException("Library is too large to cache");
            File tmp=new File(context.getFilesDir(),"published-content-v1.tmp");
            try(FileOutputStream out=new FileOutputStream(tmp)){out.write(bytes);out.getFD().sync();}
            if(!tmp.renameTo(cachedFile()))throw new IllegalStateException("Unable to save offline library");
            synchronized(this){cache=result;}
            preferences.edit().putLong("synced_at",System.currentTimeMillis()).apply();
            ok=true;
        }catch(Exception ex){error="تعذر تحديث المحتوى، ويمكنك قراءة المحفوظ محليًا.";}
        synchronized(this){syncing=false;}
        final boolean success=ok; final String message=error;
        activity.runOnUiThread(()->callback.completed(success,message));
      },"blog-content-sync").start();
    }
    public void retrieveArticle(Activity activity,String slug,java.util.function.Consumer<JSONObject> callback){
      new Thread(()->{JSONObject a=null;try{a=fetch("/api/v1/article?slug="+java.net.URLEncoder.encode(slug,"UTF-8")).optJSONObject("article");}catch(Exception ignored){}
        final JSONObject item=a;activity.runOnUiThread(()->callback.accept(item));
      },"blog-single-article").start();
    }
}

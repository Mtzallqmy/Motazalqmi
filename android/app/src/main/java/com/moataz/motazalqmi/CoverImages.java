package com.moataz.motazalqmi;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Small disk-backed HTTPS image cache; no WebView or browser rendering involved. */
public final class CoverImages {
    private static final ExecutorService WORKERS=Executors.newFixedThreadPool(3);
    public static void load(Context context,ImageView target,String url){
        target.setImageDrawable(null);target.setTag(url);
        if(url==null||!url.startsWith("https://")||url.length()>2048)return;
        WORKERS.execute(()->{
            Bitmap bitmap=null;
            try{
                byte[] hash=MessageDigest.getInstance("SHA-256").digest(url.getBytes("UTF-8"));StringBuilder name=new StringBuilder();for(byte b:hash)name.append(String.format(java.util.Locale.US,"%02x",b));
                File dir=new File(context.getCacheDir(),"covers");if(!dir.exists())dir.mkdirs();File file=new File(dir,name.toString()+".jpg");
                if(!file.isFile()){
                    HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(8000);c.setReadTimeout(10000);c.setInstanceFollowRedirects(false);
                    try{if(c.getResponseCode()!=200)throw new IllegalStateException("Image unavailable");
                      if(c.getContentLengthLong()>4*1024*1024)throw new IllegalStateException("Image too large");
                      try(InputStream input=c.getInputStream();FileOutputStream out=new FileOutputStream(file)){
                        byte[] buf=new byte[8192];int n,total=0;while((n=input.read(buf))!=-1){total+=n;if(total>4*1024*1024)throw new IllegalStateException("Image too large");out.write(buf,0,n);}
                      }
                    }finally{c.disconnect();}
                }
                BitmapFactory.Options opt=new BitmapFactory.Options();opt.inSampleSize=2;bitmap=BitmapFactory.decodeFile(file.getAbsolutePath(),opt);
            }catch(Exception ignored){}
            Bitmap result=bitmap;target.post(()->{if(url.equals(target.getTag())&&result!=null)target.setImageBitmap(result);});
        });
    }
    private CoverImages(){}
}

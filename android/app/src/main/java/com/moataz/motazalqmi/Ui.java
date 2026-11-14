package com.moataz.motazalqmi;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    static final int TEAL=Color.rgb(13,92,99), DARK=Color.rgb(20,49,51), GOLD=Color.rgb(193,147,70);
    static final int PAPER=Color.rgb(250,247,239), WHITE=Color.WHITE, MUTED=Color.rgb(106,115,114), PALE=Color.rgb(227,238,234);
    static int dp(Context c, float v){return (int)(c.getResources().getDisplayMetrics().density*v+0.5f);}
    static GradientDrawable bg(int color,int radius,Context c){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));return d;}
    static GradientDrawable gradient(Context c){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{TEAL,DARK});d.setCornerRadius(dp(c,24));return d;}
    static LinearLayout vertical(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);return l;}
    static LinearLayout horizontal(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.HORIZONTAL);l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    static TextView text(Context c,String s,int size,int color,boolean bold){TextView t=new TextView(c);t.setText(s);t.setTextColor(color);t.setTextSize(size);t.setGravity(Gravity.RIGHT);t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);t.setLineSpacing(dp(c,2),1.18f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    static TextView title(Context c,String s,int size){TextView t=text(c,s,size,DARK,true);t.setTypeface(Typeface.create("serif",Typeface.BOLD));return t;}
    static View gap(Context c,int h){View v=new View(c);v.setLayoutParams(new LinearLayout.LayoutParams(1,dp(c,h)));return v;}
    static LinearLayout.LayoutParams lp(Context c,int width,int height){return new LinearLayout.LayoutParams(width<0?width:dp(c,width),height<0?height:dp(c,height));}
    static LinearLayout.LayoutParams margins(Context c,int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=lp(c,w,h);p.setMargins(dp(c,l),dp(c,t),dp(c,r),dp(c,b));return p;}
    static Button button(Context c,String title,boolean primary){Button b=new Button(c);b.setText(title);b.setTextSize(13);b.setTextColor(primary?WHITE:TEAL);b.setAllCaps(false);b.setPadding(dp(c,8),0,dp(c,8),0);b.setBackground(bg(primary?TEAL:PALE,13,c));return b;}
    static TextView pill(Context c,String title, boolean active){TextView t=text(c,title,13,active?WHITE:TEAL,true);t.setGravity(Gravity.CENTER);t.setPadding(dp(c,15),dp(c,10),dp(c,15),dp(c,10));t.setBackground(bg(active?TEAL:PALE,24,c));return t;}
    static void pad(View v,Context c,int horizontal,int vertical){v.setPadding(dp(c,horizontal),dp(c,vertical),dp(c,horizontal),dp(c,vertical));}
    static void window(Activity a,View root){a.getWindow().setStatusBarColor(TEAL);a.getWindow().setNavigationBarColor(PAPER);
        if(Build.VERSION.SDK_INT>=35){root.setOnApplyWindowInsetsListener((v,in)->{android.graphics.Insets s=in.getInsets(WindowInsets.Type.systemBars());v.setPadding(s.left,s.top,s.right,s.bottom);return in;});}
    }
    static void card(View v,Context c){v.setBackground(bg(WHITE,20,c));v.setElevation(dp(c,3));}
    static TextView dividerTitle(Context c,String title){TextView t=title(c,title,24);t.setPadding(dp(c,3),dp(c,8),dp(c,3),dp(c,8));return t;}
    private Ui(){}
}

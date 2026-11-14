package com.moataz.motazalqmi;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.QuoteSpan;

/** Native TextView renderer for the blog's basic Markdown content. */
final class ArticleFormat {
    static SpannableStringBuilder render(String markdown){
        SpannableStringBuilder b=new SpannableStringBuilder();
        if(markdown==null)return b;
        for(String source:markdown.replace("\r\n","\n").split("\n",-1)){
            String s=source.trim();int head=0;while(head<s.length()&&s.charAt(head)=='#')head++;
            boolean title=head>0&&head<=5&&s.length()>head&&s.charAt(head)==' ';
            boolean quote=s.startsWith(">");boolean bullet=s.startsWith("- ")||s.startsWith("* ");
            if(title)s=s.substring(head).trim();if(quote)s=s.substring(1).trim();if(bullet)s="•  "+s.substring(2);
            // Inline bold markers are rendered as spans without invoking HTML or WebView.
            int start=b.length();
            java.util.regex.Matcher m=java.util.regex.Pattern.compile("\\*\\*(.+?)\\*\\*").matcher(s);
            int last=0;
            while(m.find()){
                b.append(s.substring(last,m.start()));int x=b.length();b.append(m.group(1));b.setSpan(new StyleSpan(Typeface.BOLD),x,b.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);last=m.end();
            }
            b.append(s.substring(last));int end=b.length();
            if(title&&end>start){b.setSpan(new StyleSpan(Typeface.BOLD),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);b.setSpan(new RelativeSizeSpan(head==1?1.5f:1.25f),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);b.setSpan(new ForegroundColorSpan(Ui.TEAL),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}
            if(quote&&end>start)b.setSpan(new ForegroundColorSpan(Ui.TEAL),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            b.append("\n");
        }
        return b;
    }
    private ArticleFormat(){}
}

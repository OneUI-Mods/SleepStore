package dev.ryuya.sleepstore;

import android.app.LocaleManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.LocaleList;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.appbar.MaterialToolbar;

public final class MainActivity extends AppCompatActivity {
    static final String SOURCE="https://github.com/ryuya0124/SleepStore";
    private LinearLayout content;
    public static boolean frameworkLoaded() { return false; }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        int surface=com.google.android.material.color.MaterialColors.getColor(this,com.google.android.material.R.attr.colorSurface,getClass().getName());
        root.setBackgroundColor(surface);
        MaterialToolbar toolbar=new MaterialToolbar(this); toolbar.setTitle(R.string.app_name);
        root.addView(toolbar,new LinearLayout.LayoutParams(-1,dp(64)));
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(20),dp(4),dp(20),dp(24));
        scroll.addView(content); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        ViewCompat.setOnApplyWindowInsetsListener(root,(view,insets)->{
            androidx.core.graphics.Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;
        });
        LinearLayout protection=card();text(protection,getString(R.string.protection_title),20,true,8);text(protection,getString(R.string.protection_body),16,false,0);
        LinearLayout integration=card();text(integration,getString(R.string.setup_title),20,true,8);
        text(integration,getString(frameworkLoaded()?R.string.framework_active:R.string.framework_inactive),16,true,6);
        text(integration,getString(R.string.framework_hint),14,false,12);
        text(integration,getString(R.string.setup_body),16,false,12);
        store(integration,"com.android.vending",R.string.play_store);store(integration,"com.sec.android.app.samsungapps",R.string.galaxy_store);
        text(integration,getString(R.string.scope_notice),14,false,0);
        LinearLayout about=card();text(about,getString(R.string.about_title),20,true,8);text(about,getString(R.string.version,BuildConfig.VERSION_NAME),14,false,8);
        button(about,getString(R.string.language),this::language);
        button(about,getString(R.string.source_code),()->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(SOURCE))));
        button(about,getString(R.string.licenses),this::licenses);
    }
    private LinearLayout card(){
        MaterialCardView card=new MaterialCardView(this); card.setRadius(dp(24));card.setStrokeWidth(0);card.setCardElevation(0);
        card.setCardBackgroundColor(com.google.android.material.color.MaterialColors.getColor(this,com.google.android.material.R.attr.colorSurfaceContainer,getClass().getName()));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(16);content.addView(card,p);
        LinearLayout inner=new LinearLayout(this);inner.setOrientation(LinearLayout.VERTICAL);inner.setPadding(dp(20),dp(20),dp(20),dp(20));card.addView(inner);return inner;
    }
    private void text(LinearLayout parent,String value,int size,boolean title,int bottom){
        TextView text=new TextView(this);text.setText(value);text.setTextSize(size);text.setLineSpacing(dp(3),1);
        text.setTextColor(com.google.android.material.color.MaterialColors.getColor(this,com.google.android.material.R.attr.colorOnSurface,getClass().getName()));
        if(title)text.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(bottom);parent.addView(text,p);
    }
    private void button(LinearLayout parent,String label,Runnable action){
        MaterialButton b=new MaterialButton(this,null,com.google.android.material.R.attr.materialButtonOutlinedStyle);
        b.setText(label);b.setMinHeight(dp(48));b.setOnClickListener(v->action.run());
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(8);parent.addView(b,p);
    }
    private void store(LinearLayout parent,String pkg,int name){
        String version;try{version=getPackageManager().getPackageInfo(pkg,0).versionName;}catch(Exception e){version=getString(R.string.not_installed);}
        text(parent,getString(name)+" · "+version,14,false,4);
        button(parent,getString(R.string.open_store,getString(name)),()->{
            Intent launch=getPackageManager().getLaunchIntentForPackage(pkg);
            if(launch==null){Toast.makeText(this,R.string.store_unavailable,Toast.LENGTH_SHORT).show();return;}
            try{startActivity(launch);}catch(android.content.ActivityNotFoundException e){Toast.makeText(this,R.string.store_unavailable,Toast.LENGTH_SHORT).show();}
        });
    }
    private void language(){
        String[] tags={"","ja","en"};String current=getSystemService(LocaleManager.class).getApplicationLocales().toLanguageTags();int selected=0;
        for(int i=0;i<tags.length;i++)if(tags[i].equals(current))selected=i;
        new MaterialAlertDialogBuilder(this).setTitle(R.string.language)
            .setSingleChoiceItems(new String[]{getString(R.string.system_language),"日本語","English"},selected,(dialog,index)->{
                getSystemService(LocaleManager.class).setApplicationLocales(LocaleList.forLanguageTags(tags[index]));dialog.dismiss();
            }).setNegativeButton(R.string.close,null).show();
    }
    private void licenses(){
        String full=getString(R.string.license_text);
        try(java.io.InputStream in=getAssets().open("LICENSES.txt")){full+="\n\n"+new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}catch(java.io.IOException ignored){}
        TextView text=new TextView(this);text.setText(full);text.setTextSize(14);text.setTextIsSelectable(true);text.setPadding(dp(24),dp(16),dp(24),dp(16));
        ScrollView scroll=new ScrollView(this);scroll.addView(text);
        new MaterialAlertDialogBuilder(this).setTitle(R.string.licenses).setView(scroll).setPositiveButton(R.string.close,null).show();
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}

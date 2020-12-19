package net.justdave.mcstatus.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import net.justdave.mcstatus.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class HelpDialog extends Dialog {
    private final Context mContext;
    private static final String TAG = HelpDialog.class.getSimpleName();

    public HelpDialog(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.generic_dialog);

        TextView tv = findViewById(R.id.info_text);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            /* Nugat and up can actually process images! */
            tv.setText(Html.fromHtml(readRawTextFile(R.raw.main_help), 0, imgGetter, null));
        } else {
            /* below Nugat requires one hell of a hack of a workaround. */
            String text = readRawTextFile(R.raw.main_help);
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            int index = text.indexOf("<img") -1;
            int index2 = text.indexOf("\">") + 2;
            SpannableString string1 = new SpannableString(Html.fromHtml(text.substring(0,index)));
            ImageSpan is = new ImageSpan(mContext, android.R.drawable.ic_menu_add);
            SpannableString string2 = new SpannableString(Html.fromHtml(text.substring(index2)));
            ssb.append(string1).append(" ");
            ssb.setSpan(is, ssb.length()-1, ssb.length(), 0);
            ssb.append(string2);
            tv.setText(ssb);
        }

        Button button = findViewById(R.id.about_ok_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismiss();
            }
        });
 
    }

    public String readRawTextFile(int id) {

        InputStream inputStream = mContext.getResources().openRawResource(id);

        InputStreamReader in = new InputStreamReader(inputStream);
        BufferedReader buf = new BufferedReader(in);

        String line;

        StringBuilder text = new StringBuilder();
        try {
            while ((line = buf.readLine()) != null)
                text.append(line);
        } catch (IOException e) {
            return null;
        }

        return text.toString();
    }

    private final Html.ImageGetter imgGetter = new Html.ImageGetter() {

        public Drawable getDrawable(String source) {
            Drawable drawable;
            Log.i(TAG, "Drawable source: " + source);
            int rid = mContext.getResources().getIdentifier(source, null, null);
            if (rid > 0) {
                drawable = mContext.getResources().getDrawable(rid);
            } else {
                drawable = mContext.getResources().getDrawable(android.R.drawable.stat_notify_error);
            }
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            return drawable;
        }
    };
}

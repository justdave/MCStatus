package net.justdave.mcstatus.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.widget.Button;
import android.widget.TextView;

import net.justdave.mcstatus.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class PrivacyDialog extends Dialog {
    private final Context mContext;

    public PrivacyDialog(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.generic_dialog);

        TextView tv = findViewById(R.id.info_text);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tv.setText(Html.fromHtml(readRawTextFile(R.raw.privacy), Html.FROM_HTML_MODE_LEGACY));
        } else {
            tv.setText(Html.fromHtml(readRawTextFile(R.raw.privacy)));
        }

        Button button = findViewById(R.id.about_ok_button);
        button.setOnClickListener(v -> dismiss());
 
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
}

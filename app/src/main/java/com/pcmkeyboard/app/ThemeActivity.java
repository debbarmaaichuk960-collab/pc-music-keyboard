package com.pcmkeyboard.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/** Keyboard settings: colour theme, gallery photo background, haptics, mic permission. */
public class ThemeActivity extends Activity {

    private static final String PREFS = "pc_music_keyboard";
    private static final int REQ_PICK = 101;
    private static final int REQ_MIC = 102;

    private SharedPreferences prefs;

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void bump() {
        prefs.edit().putLong("theme_stamp", System.currentTimeMillis()).apply();
    }

    private TextView label(String text, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(sp);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setPadding(0, dp(12), 0, dp(4));
        return t;
    }

    private Button button(String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        return b;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        ScrollView sv = new ScrollView(this);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(20), dp(24), dp(20), dp(24));
        sv.addView(col);
        setContentView(sv);

        col.addView(label("Keyboard Theme", 22, true));

        col.addView(label("Colour theme", 15, true));
        String[] names = {"Midnight", "Light", "AMOLED Black", "Purple"};
        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            col.addView(button(names[i], v -> {
                prefs.edit().putInt("theme", idx).apply();
                bump();
                Toast.makeText(this, names[idx] + " set", Toast.LENGTH_SHORT).show();
            }));
        }

        col.addView(label("Photo background (gallery / wallpaper)", 15, true));
        col.addView(button("Gallery se photo chuno", v -> pickImage()));
        col.addView(button("Photo hatao", v -> {
            prefs.edit().putBoolean("bg_image", false).apply();
            new File(getFilesDir(), "kb_bg.jpg").delete();
            bump();
            Toast.makeText(this, "Photo hata di", Toast.LENGTH_SHORT).show();
        }));

        col.addView(label("Photo dimming (keys saaf dikhne ke liye)", 13, false));
        SeekBar dim = new SeekBar(this);
        dim.setMax(230);
        dim.setProgress(prefs.getInt("bg_dim", 110));
        dim.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                prefs.edit().putInt("bg_dim", p).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { bump(); }
        });
        col.addView(dim);

        col.addView(label("Typing feel", 15, true));
        Switch haptic = new Switch(this);
        haptic.setText("Key press vibration (halka sa)");
        haptic.setChecked(prefs.getBoolean("haptic", true));
        haptic.setOnCheckedChangeListener((b, on) -> {
            prefs.edit().putBoolean("haptic", on).apply();
            bump();
        });
        col.addView(haptic);

        col.addView(label("Voice typing", 15, true));
        col.addView(button("Mic permission do", v -> requestMic()));
        col.addView(label("Tip: keyboard me 🎤 button ko long-press karo to English / Hindi badal jayega.",
                12, false));

        col.addView(label("Theme change ke baad keyboard dobara kholo.", 12, false));

        if (getIntent().getBooleanExtra("ask_mic", false)) requestMic();
    }

    private void requestMic() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Mic permission already di hui hai", Toast.LENGTH_SHORT).show();
            return;
        }
        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == REQ_MIC) {
            boolean ok = results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED;
            Toast.makeText(this, ok ? "Mic ready — keyboard me 🎤 dabao" : "Mic permission nahi mili",
                    Toast.LENGTH_LONG).show();
            if (ok && getIntent().getBooleanExtra("ask_mic", false)) finish();
        }
    }

    private void pickImage() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        startActivityForResult(i, REQ_PICK);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_PICK && res == RESULT_OK && data != null && data.getData() != null) {
            try {
                saveBackground(data.getData());
                Toast.makeText(this, "Photo keyboard pe laga di ✔", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Photo load nahi hui", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void saveBackground(Uri uri) throws Exception {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, o);
        }
        int sample = 1;
        while (o.outWidth / sample > 1600 || o.outHeight / sample > 1600) sample *= 2;
        o.inJustDecodeBounds = false;
        o.inSampleSize = sample;

        Bitmap bmp;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            bmp = BitmapFactory.decodeStream(in, null, o);
        }
        if (bmp == null) throw new Exception("decode failed");

        int rot = 0;
        if (Build.VERSION.SDK_INT >= 24) {
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                ExifInterface ex = new ExifInterface(in);
                int ori = ex.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
                if (ori == 6) rot = 90;
                else if (ori == 3) rot = 180;
                else if (ori == 8) rot = 270;
            } catch (Exception ignored) {
            }
        }
        if (rot != 0) {
            Matrix m = new Matrix();
            m.postRotate(rot);
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
        }

        File f = new File(getFilesDir(), "kb_bg.jpg");
        try (FileOutputStream out = new FileOutputStream(f)) {
            bmp.compress(Bitmap.CompressFormat.JPEG, 88, out);
        }
        prefs.edit().putBoolean("bg_image", true).apply();
        bump();
    }
}

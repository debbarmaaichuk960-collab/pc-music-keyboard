package com.pcmkeyboard.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Settings -> Theme / Keyboard style pages (Gboard-like). One activity, three pages. */
public class ThemeActivity extends Activity {

    private static final String PREFS = "pc_music_keyboard";
    private static final int REQ_PICK = 101;
    private static final int REQ_MIC = 102;

    private static final int C_BG = 0xFF231F26;
    private static final int C_TEAL = 0xFF6FD6CC;
    private static final int C_TEXT = 0xFFE6E1E9;
    private static final int C_SUB = 0xFFB5AFBA;
    private static final int C_BORDER = 0xFF3A353F;

    private static final String[] STYLE_NAMES = {"Classic 3D", "Flat rounded", "Outline", "Minimal"};

    private SharedPreferences prefs;
    private ScrollView sv;
    private LinearLayout col;
    private int page = 0;   // 0 settings, 1 theme, 2 keyboard style
    private int tileW;

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void bump() {
        prefs.edit().putLong("theme_stamp", System.currentTimeMillis()).apply();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        getWindow().setStatusBarColor(C_BG);
        getWindow().setNavigationBarColor(C_BG);

        int screenW = getResources().getDisplayMetrics().widthPixels;
        tileW = (screenW - dp(40) - dp(24)) / 3;

        sv = new ScrollView(this);
        sv.setBackgroundColor(C_BG);
        sv.setFillViewport(true);
        col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(20), dp(40), dp(20), dp(40));
        sv.addView(col);
        setContentView(sv);
        sv.setOnApplyWindowInsetsListener((v, ins) -> {
            col.setPadding(dp(20), dp(28) + ins.getSystemWindowInsetTop(), dp(20),
                    dp(40) + ins.getSystemWindowInsetBottom());
            return ins;
        });
        if (savedInstanceState != null) page = savedInstanceState.getInt("page", 0);
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, () -> {
                        if (page != 0) go(0);
                        else finish();
                    });
        }
        render();

        if (getIntent().getBooleanExtra("ask_mic", false)) requestMic();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("page", page);
    }

    @Override
    public void onBackPressed() {
        if (page != 0) go(0);
        else super.onBackPressed();
    }

    private void go(int p) {
        page = p;
        render();
        sv.post(() -> sv.scrollTo(0, 0));
    }

    private void render() {
        col.removeAllViews();
        if (page == 0) buildSettings();
        else if (page == 1) buildTheme();
        else buildStyle();
    }

    // ---------------- shared UI helpers ----------------

    private TextView text(String t, float sp, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(t);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        if (bold) tv.setTypeface(Typeface.DEFAULT_BOLD);
        return tv;
    }

    private void header(String title) {
        TextView back = text("←", 28, 0xFFD0BCFF, false);
        back.setPadding(0, 0, dp(16), dp(8));
        back.setOnClickListener(v -> onBackPressed());
        col.addView(back);
        TextView t = text(title, 36, C_TEXT, false);
        t.setPadding(0, dp(40), 0, dp(24));
        col.addView(t);
    }

    private void section(String title) {
        TextView t = text(title, 18, C_TEAL, false);
        t.setPadding(0, dp(24), 0, dp(12));
        col.addView(t);
    }

    private View row(String title, String sub, View.OnClickListener l) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(dp(4), dp(14), dp(4), dp(14));
        r.addView(text(title, 19, C_TEXT, false));
        if (sub != null) {
            TextView s = text(sub, 14, C_SUB, false);
            s.setPadding(0, dp(3), 0, 0);
            r.addView(s);
        }
        if (l != null) r.setOnClickListener(l);
        return r;
    }

    private GradientDrawable rect(int fill, float radiusDp, int stroke, int strokeDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) d.setStroke(dp(strokeDp), stroke);
        return d;
    }

    /** 3-column grid. */
    private void grid(List<View> tiles) {
        for (int i = 0; i < tiles.size(); i += 3) {
            LinearLayout r = new LinearLayout(this);
            r.setOrientation(LinearLayout.HORIZONTAL);
            for (int j = 0; j < 3; j++) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(tileW, -2);
                lp.setMargins(0, 0, j < 2 ? dp(12) : 0, dp(12));
                if (i + j < tiles.size()) r.addView(tiles.get(i + j), lp);
                else r.addView(new View(this), lp);
            }
            col.addView(r);
        }
    }

    private void roundClip(View v, float radiusDp) {
        final int r = dp(radiusDp);
        v.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), r);
            }
        });
        v.setClipToOutline(true);
    }

    /**
     * Theme tile. mode: 0 solid(c1), 1 gradient(c1->c2), 2 split(c1|c2), 3 photo, 4 "+" tile.
     */
    private View tile(int mode, int c1, int c2, int bar, int dot, Bitmap photo,
                      boolean selected, String caption, View.OnClickListener click,
                      View.OnLongClickListener longClick) {
        LinearLayout holder = new LinearLayout(this);
        holder.setOrientation(LinearLayout.VERTICAL);
        holder.setGravity(Gravity.CENTER_HORIZONTAL);

        int h = (int) (tileW * 0.72f);
        FrameLayout box = new FrameLayout(this);
        box.setBackground(rect(Color.TRANSPARENT, 20, selected || mode == 4 ? C_TEAL : C_BORDER,
                selected || mode == 4 ? 3 : 2));
        holder.addView(box, new LinearLayout.LayoutParams(tileW, h));

        if (mode == 4) {
            TextView plus = text("+", 44, C_TEAL, false);
            plus.setGravity(Gravity.CENTER);
            box.addView(plus, new FrameLayout.LayoutParams(-1, -1));
        } else {
            LinearLayout inner = new LinearLayout(this);
            inner.setOrientation(LinearLayout.HORIZONTAL);
            FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(-1, -1);
            ip.setMargins(dp(5), dp(5), dp(5), dp(5));
            box.addView(inner, ip);
            roundClip(inner, 15);

            if (mode == 3 && photo != null) {
                ImageView iv = new ImageView(this);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setImageBitmap(photo);
                inner.addView(iv, new LinearLayout.LayoutParams(-1, -1));
            } else if (mode == 2) {
                View a = new View(this);
                a.setBackgroundColor(c1);
                View b = new View(this);
                b.setBackgroundColor(c2);
                inner.addView(a, new LinearLayout.LayoutParams(0, -1, 1f));
                inner.addView(b, new LinearLayout.LayoutParams(0, -1, 1f));
            } else {
                View v = new View(this);
                if (mode == 1) {
                    v.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                            new int[]{c1, c2}));
                } else {
                    v.setBackgroundColor(c1);
                }
                inner.addView(v, new LinearLayout.LayoutParams(-1, -1));
            }

            // little "keyboard" bar + dot like Gboard's previews
            View b = new View(this);
            b.setBackground(rect(bar, 4, 0, 0));
            FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams((int) (tileW * 0.42f), dp(9),
                    Gravity.BOTTOM | Gravity.START);
            bp.setMargins(dp(22), 0, 0, dp(14));
            box.addView(b, bp);
            View d = new View(this);
            d.setBackground(rect(dot, 8, 0, 0));
            FrameLayout.LayoutParams dpm = new FrameLayout.LayoutParams(dp(15), dp(11),
                    Gravity.BOTTOM | Gravity.END);
            dpm.setMargins(0, 0, dp(14), dp(14));
            box.addView(d, dpm);

            if (selected) {
                TextView chk = text("✓", 24, 0xFF0B2B28, true);
                chk.setGravity(Gravity.CENTER);
                chk.setBackground(rect(C_TEAL, 24, 0, 0));
                FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(dp(48), dp(48),
                        Gravity.CENTER);
                box.addView(chk, cp);
            }
        }
        if (caption != null && !caption.isEmpty()) {
            TextView c = text(caption, 15, C_TEXT, false);
            c.setGravity(Gravity.CENTER);
            c.setPadding(0, dp(6), 0, 0);
            c.setSingleLine(true);
            holder.addView(c);
        }
        if (click != null) holder.setOnClickListener(click);
        if (longClick != null) holder.setOnLongClickListener(longClick);
        return holder;
    }

    // ---------------- page 0: settings ----------------

    private View prefSwitch(String title, String key, boolean def) {
        Switch sw = new Switch(this);
        sw.setText(title);
        sw.setTextSize(19);
        sw.setTextColor(C_TEXT);
        sw.setPadding(dp(4), dp(14), dp(4), dp(14));
        sw.setChecked(prefs.getBoolean(key, def));
        sw.setOnCheckedChangeListener((b, on) -> {
            prefs.edit().putBoolean(key, on).apply();
            bump();
        });
        return sw;
    }

    @SuppressWarnings("deprecation")
    private void testVibration(View v) {
        boolean systemOn;
        try {
            systemOn = android.provider.Settings.System.getInt(getContentResolver(),
                    android.provider.Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0;
        } catch (RuntimeException e) {
            systemOn = true;
        }
        int flags = 0;
        if (Build.VERSION.SDK_INT < 33 && prefs.getBoolean("haptic_force", true)) {
            flags = android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING;
        }
        v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP, flags);
        if (!systemOn && Build.VERSION.SDK_INT >= 33) {
            Toast.makeText(this, "Phone ki Touch feedback setting band hai, isliye vibration nahi aayegi",
                    Toast.LENGTH_LONG).show();
        }
    }

    private String themeName() {
        if (prefs.getBoolean("bg_image", false)) return "Photo theme";
        int t = prefs.getInt("theme", 0);
        if (t == 100) return "System auto";
        if (t == 101) return "Dynamic colour";
        if (t >= 0 && t < PCKeyboardService.THEME_NAMES.length) return PCKeyboardService.THEME_NAMES[t];
        return "Midnight";
    }

    private void buildSettings() {
        header("Settings");

        col.addView(row("Theme", themeName(), v -> go(1)));
        col.addView(row("Keyboard style",
                "PC: " + STYLE_NAMES[prefs.getInt("pc_style", 0)] + "   •   Mobile: "
                        + STYLE_NAMES[prefs.getInt("mobile_style", 3)], v -> go(2)));

        boolean mobile = "mobile".equals(prefs.getString("keyboard_layout", "pc"));
        col.addView(row("Default keyboard", mobile ? "Mobile QWERTY" : "PC full layout (tap to switch)",
                v -> {
                    prefs.edit().putString("keyboard_layout", mobile ? "pc" : "mobile").apply();
                    bump();
                    render();
                }));

        Switch haptic = new Switch(this);
        haptic.setText("Key press vibration");
        haptic.setTextSize(19);
        haptic.setTextColor(C_TEXT);
        haptic.setPadding(dp(4), dp(14), dp(4), dp(14));
        haptic.setChecked(prefs.getBoolean("haptic", true));
        haptic.setOnCheckedChangeListener((b, on) -> {
            prefs.edit().putBoolean("haptic", on).apply();
            bump();
        });
        col.addView(haptic);

        if (Build.VERSION.SDK_INT >= 33) {
            // Android 13+: the phone's own touch-feedback setting always decides (no bypass possible)
            col.addView(row("Vibration strength",
                    "Android 13+ follows your phone: Settings > Sound & vibration > Vibration & haptics > Touch feedback",
                    null));
        } else {
            col.addView(prefSwitch("Stronger feedback even if touch feedback is off (Android 12 and below)",
                    "haptic_force", true));
        }
        col.addView(row("Test vibration", "Tap to feel one key vibration", v -> testVibration(v)));
        col.addView(prefSwitch("Key click sound", "sound", false));

        boolean hi = "hi-IN".equals(prefs.getString("voice_lang", "en-IN"));
        col.addView(row("Voice typing language", hi ? "Hindi" : "English (India)", v -> {
            prefs.edit().putString("voice_lang", hi ? "en-IN" : "hi-IN").apply();
            bump();
            render();
        }));

        boolean micOk = checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
        col.addView(row("Microphone permission", micOk ? "Allowed" : "Tap to allow", v -> requestMic()));

        col.addView(row("Clear learned words", "Keyboard ne jo words seekhe wo delete karo", v -> {
            prefs.edit().remove("user_words").putLong("words_reset", System.currentTimeMillis()).apply();
            Toast.makeText(this, "Learned words clear ho gaye", Toast.LENGTH_SHORT).show();
        }));

        TextView tip = text("Theme badalne ke baad keyboard dobara kholo.", 13, C_SUB, false);
        tip.setPadding(dp(4), dp(24), 0, 0);
        col.addView(tip);
    }

    // ---------------- page 1: theme ----------------

    private File[] photos() {
        File[] fs = getFilesDir().listFiles((dir, name) ->
                name.startsWith("kb_bg") && name.endsWith(".jpg"));
        if (fs == null) return new File[0];
        Arrays.sort(fs, new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                return Long.compare(b.lastModified(), a.lastModified());
            }
        });
        return fs;
    }

    private final java.util.HashMap<String, Bitmap> thumbCache = new java.util.HashMap<>();
    private final java.util.HashSet<String> thumbLoading = new java.util.HashSet<>();

    private Bitmap decodeThumb(File f) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getAbsolutePath(), o);
        int sample = 1;
        while (o.outWidth / sample > 500) sample *= 2;
        o.inJustDecodeBounds = false;
        o.inSampleSize = sample;
        return BitmapFactory.decodeFile(f.getAbsolutePath(), o);
    }

    /** Returns the cached thumbnail, or null while it loads in the background. */
    private Bitmap thumb(File f) {
        final String key = f.getName() + f.lastModified();
        Bitmap cached = thumbCache.get(key);
        if (cached != null) return cached;
        if (thumbLoading.add(key)) {
            new Thread(() -> {
                final Bitmap t = decodeThumb(f);
                runOnUiThread(() -> {
                    thumbLoading.remove(key);
                    if (t != null) {
                        thumbCache.put(key, t);
                        if (page == 1 && !isFinishing()) render();
                    }
                });
            }).start();
        }
        return null;
    }

    private void selectTheme(int id) {
        prefs.edit().putInt("theme", id).putBoolean("bg_image", false).apply();
        bump();
        render();
    }

    private View themeTile(int id, String caption) {
        boolean usePhoto = prefs.getBoolean("bg_image", false);
        boolean sel = !usePhoto && prefs.getInt("theme", 0) == id;
        int[][] T = PCKeyboardService.THEMES;
        if (id == 100) {
            return tile(2, T[1][0], T[2][0], T[1][2], T[1][5], null, sel, caption,
                    v -> selectTheme(id), null);
        }
        if (id == 101) {
            return tile(0, Color.rgb(247, 232, 243), 0, Color.WHITE, Color.rgb(255, 205, 215), null,
                    sel, caption, v -> selectTheme(id), null);
        }
        int[] c = T[id];
        int mode = c[8] != 0 ? 1 : 0;
        return tile(mode, c[0], c[8], c[2], c[5], null, sel, caption, v -> selectTheme(id), null);
    }

    private void buildTheme() {
        header("Theme");

        section("My themes");
        List<View> mine = new ArrayList<>();
        mine.add(tile(4, 0, 0, 0, 0, null, false, null, v -> pickImage(), null));
        boolean usePhoto = prefs.getBoolean("bg_image", false);
        String cur = prefs.getString("bg_file", "kb_bg.jpg");
        for (File f : photos()) {
            Bitmap th = thumb(f);
            boolean sel = usePhoto && f.getName().equals(cur);
            mine.add(tile(3, 0, 0, Color.argb(190, 255, 255, 255), Color.rgb(100, 150, 255), th, sel, null,
                    v -> {
                        prefs.edit().putString("bg_file", f.getName()).putBoolean("bg_image", true).apply();
                        bump();
                        render();
                    },
                    v -> {
                        new AlertDialog.Builder(this)
                                .setMessage("Ye photo theme delete karein?")
                                .setPositiveButton("Delete", (d, w) -> {
                                    f.delete();
                                    if (f.getName().equals(prefs.getString("bg_file", ""))) {
                                        prefs.edit().putBoolean("bg_image", false).apply();
                                    }
                                    bump();
                                    render();
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                        return true;
                    }));
        }
        grid(mine);

        if (usePhoto) {
            TextView dimLabel = text("Photo dimming (keys saaf dikhne ke liye)", 14, C_SUB, false);
            dimLabel.setPadding(0, dp(4), 0, dp(4));
            col.addView(dimLabel);
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
            TextView hint = text("Photo pe long-press karo = delete", 12, C_SUB, false);
            hint.setPadding(0, dp(6), 0, 0);
            col.addView(hint);
        }

        section("Default");
        List<View> def = new ArrayList<>();
        def.add(themeTile(1, "Default"));
        def.add(themeTile(2, "Default dark"));
        def.add(themeTile(0, "Midnight"));
        def.add(themeTile(3, "Purple"));
        def.add(themeTile(100, "System auto"));
        def.add(themeTile(101, "Dynamic colour"));
        grid(def);

        section("Colours");
        List<View> colours = new ArrayList<>();
        for (int i = 4; i <= 9; i++) colours.add(themeTile(i, null));
        grid(colours);

        section("Light gradient");
        List<View> lg = new ArrayList<>();
        for (int i = 10; i <= 12; i++) lg.add(themeTile(i, null));
        grid(lg);

        section("Dark gradient");
        List<View> dg = new ArrayList<>();
        for (int i = 13; i <= 15; i++) dg.add(themeTile(i, null));
        grid(dg);
    }

    // ---------------- page 2: keyboard style ----------------

    private View miniKey(int style, String label, int[] c) {
        TextView k = new TextView(this);
        k.setText(label);
        k.setTextSize(11);
        k.setGravity(Gravity.CENTER);
        k.setTypeface(Typeface.DEFAULT_BOLD);
        int fill = c[2];
        double lum = (0.299 * Color.red(c[0]) + 0.587 * Color.green(c[0]) + 0.114 * Color.blue(c[0])) / 255.0;
        int plain = lum > 0.6 ? Color.rgb(20, 30, 45) : Color.WHITE;
        switch (style) {
            case 0:
                k.setBackground(rect(fill, 4, Color.argb(160, 0, 0, 0), 1));
                k.setTextColor(Color.rgb(35, 36, 41));
                break;
            case 1:
                k.setBackground(rect(fill, 8, fill, 0));
                k.setTextColor(Color.rgb(35, 36, 41));
                break;
            case 2:
                k.setBackground(rect(Color.TRANSPARENT, 8, c[6], 1));
                k.setTextColor(plain);
                break;
            default:
                k.setTextColor(plain);
                k.setTypeface(Typeface.DEFAULT);
                break;
        }
        return k;
    }

    private View styleTile(int style, boolean mobile, boolean selected) {
        int[] c = PCKeyboardService.THEMES[Math.max(0, Math.min(prefs.getInt("theme", 0),
                PCKeyboardService.THEMES.length - 1))];
        LinearLayout holder = new LinearLayout(this);
        holder.setOrientation(LinearLayout.VERTICAL);
        holder.setGravity(Gravity.CENTER_HORIZONTAL);

        FrameLayout box = new FrameLayout(this);
        box.setBackground(rect(Color.TRANSPARENT, 20, selected ? C_TEAL : C_BORDER, selected ? 3 : 2));
        holder.addView(box, new LinearLayout.LayoutParams(tileW, (int) (tileW * 0.8f)));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.CENTER);
        inner.setBackgroundColor(c[0]);
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(-1, -1);
        ip.setMargins(dp(5), dp(5), dp(5), dp(5));
        box.addView(inner, ip);
        roundClip(inner, 15);

        String[][] rows = {{"Q", "W", "E", "R"}, {"A", "S", "D", "F"}};
        int kw = tileW / 6;
        for (String[] r : rows) {
            LinearLayout rl = new LinearLayout(this);
            rl.setOrientation(LinearLayout.HORIZONTAL);
            for (String l : r) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(kw, (int) (kw * 1.05f));
                lp.setMargins(dp(2), dp(2), dp(2), dp(2));
                rl.addView(miniKey(style, l, c), lp);
            }
            inner.addView(rl);
        }

        TextView cap = text(STYLE_NAMES[style], 14, C_TEXT, false);
        cap.setGravity(Gravity.CENTER);
        cap.setSingleLine(true);
        cap.setPadding(0, dp(6), 0, 0);
        holder.addView(cap);

        holder.setOnClickListener(v -> {
            prefs.edit().putInt(mobile ? "mobile_style" : "pc_style", style).apply();
            bump();
            render();
        });
        return holder;
    }

    private void buildStyle() {
        header("Keyboard style");
        section("PC keyboard");
        List<View> pc = new ArrayList<>();
        int ps = prefs.getInt("pc_style", 0);
        for (int i = 0; i < 4; i++) pc.add(styleTile(i, false, i == ps));
        grid(pc);

        section("Mobile keyboard");
        List<View> mb = new ArrayList<>();
        int ms = prefs.getInt("mobile_style", 3);
        for (int i = 0; i < 4; i++) mb.add(styleTile(i, true, i == ms));
        grid(mb);

        TextView tip = text("Tile ka colour tumhari current theme ka hai.", 12, C_SUB, false);
        tip.setPadding(dp(4), dp(8), 0, 0);
        col.addView(tip);
    }

    // ---------------- mic ----------------

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
            else if (page == 0) render();
        }
    }

    // ---------------- gallery photo ----------------

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
                Toast.makeText(this, "Photo theme laga di ✔", Toast.LENGTH_SHORT).show();
                render();
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

        String name = "kb_bg_" + System.currentTimeMillis() + ".jpg";
        File f = new File(getFilesDir(), name);
        try (FileOutputStream out = new FileOutputStream(f)) {
            bmp.compress(Bitmap.CompressFormat.JPEG, 88, out);
        }
        prefs.edit().putString("bg_file", name).putBoolean("bg_image", true).apply();
        bump();
    }
}

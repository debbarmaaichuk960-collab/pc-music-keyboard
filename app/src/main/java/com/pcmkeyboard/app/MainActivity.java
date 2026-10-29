package com.pcmkeyboard.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

    private final int bg = Color.rgb(16, 17, 20);
    private final int panel = Color.rgb(31, 33, 38);
    private final int red = Color.rgb(229, 57, 53);
    private final int white = Color.rgb(245, 245, 245);

    private LinearLayout root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showHome();
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    private Button button(String label, int color) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(white);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setBackground(shape(color, 18));
        return b;
    }

    private void showHome() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(18, 20, 18, 20);
        root.setBackgroundColor(bg);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        TextView brand = text("PC MUSIC KEYBOARD", 23, white);
        brand.setTypeface(Typeface.DEFAULT_BOLD);
        content.addView(brand);

        TextView subtitle = text(
                "MECHANICAL • MUSIC • CUSTOMIZATION", 11,
                Color.LTGRAY);
        subtitle.setPadding(0, 4, 0, 20);
        content.addView(subtitle);

        LinearLayout status = new LinearLayout(this);
        status.setGravity(Gravity.CENTER_VERTICAL);
        status.setPadding(14, 12, 14, 12);
        status.setBackground(shape(panel, 16));

        TextView statusText = text("●  KEYBOARD STATUS", 14, white);
        status.addView(statusText, new LinearLayout.LayoutParams(
                0, -2, 1));

        TextView statusValue = text("CHECK", 12, red);
        status.addView(statusValue);
        content.addView(status);

        addSpace(content, 18);

        TextView heading = text("YOUR KEYBOARD", 17, white);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        content.addView(heading);

        TextView description = text(
                "A PC-style keyboard for typing and music.", 13,
                Color.LTGRAY);
        description.setPadding(0, 6, 0, 14);
        content.addView(description);

        Button enable = button("Enable Keyboard", red);
        enable.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));
        content.addView(enable, fullWidth());

        Button choose = button("Select PC Music Keyboard", panel);
        choose.setOnClickListener(v -> {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager)
                            getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showInputMethodPicker();
            }
        });
        content.addView(choose, fullWidth());

        addSpace(content, 24);

        TextView toolsHeading = text("QUICK SETTINGS", 17, white);
        toolsHeading.setTypeface(Typeface.DEFAULT_BOLD);
        content.addView(toolsHeading);

        addSpace(content, 8);

        Button settings = button("⚙  Keyboard Settings", panel);
        settings.setOnClickListener(v -> showInfo(
                "Keyboard Settings",
                "Theme, key size, layout, vibration and sound controls will be added in the next development steps."));
        content.addView(settings, fullWidth());

        Button music = button("♫  Music Studio", panel);
        music.setOnClickListener(v -> showInfo(
                "Music Studio",
                "Music mode with piano notes, instruments and recording will be developed next."));
        content.addView(music, fullWidth());

        Button about = button("ⓘ  About PC Music Keyboard", panel);
        about.setOnClickListener(v -> showInfo(
                "About",
                "PC Music Keyboard\\nVersion 1.0\\nA customizable PC-style Android keyboard."));
        content.addView(about, fullWidth());

        addSpace(content, 22);

        TextView note = text(
                "Tip: Enable the keyboard in Android Settings, then select it when typing.",
                12, Color.LTGRAY);
        content.addView(note);

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void showInfo(String title, String message) {
        new android.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void addSpace(LinearLayout layout, int height) {
        View space = new View(this);
        layout.addView(space, new LinearLayout.LayoutParams(1, height));
    }

    private LinearLayout.LayoutParams fullWidth() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                -1, -2);
        p.bottomMargin = 10;
        return p;
    }
}

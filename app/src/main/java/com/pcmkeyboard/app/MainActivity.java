
package com.pcmkeyboard.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.view.inputmethod.InputMethodManager;

public class MainActivity extends Activity {

    private final int bg = Color.rgb(16, 17, 20);
    private final int panel = Color.rgb(31, 33, 38);
    private final int red = Color.rgb(229, 57, 53);
    private final int white = Color.rgb(245, 245, 245);

    private static final String PREFS = "pc_music_keyboard";
    private static final String KEY_LAYOUT = "keyboard_layout";

    private String selectedLayout = "pc";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        selectedLayout = prefs.getString(KEY_LAYOUT, "pc");

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

    private void addLayoutOption(
            LinearLayout parent,
            String title,
            String description,
            String layoutId) {

        boolean selected = selectedLayout.equals(layoutId);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(16, 14, 16, 14);

        GradientDrawable cardBackground = shape(
                selected ? Color.rgb(52, 35, 35) : panel, 18);

        cardBackground.setStroke(
                selected ? 3 : 1,
                selected ? red : Color.rgb(65, 67, 73));

        card.setBackground(cardBackground);
        card.setClickable(true);
        card.setFocusable(true);

        TextView name = text(title, 16, white);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(name);

        TextView detail = text(description, 12, Color.LTGRAY);
        detail.setPadding(0, 6, 0, 8);
        card.addView(detail);

        TextView status = text(
                selected ? "✓ SELECTED" : "TAP TO SELECT",
                11,
                selected ? red : Color.LTGRAY);
        status.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(status);

        card.setOnClickListener(v -> {
            selectedLayout = layoutId;

            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit()
                    .putString(KEY_LAYOUT, layoutId)
                    .apply();

            showHome();
        });

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = 12;
        parent.addView(card, params);
    }

    private void showHome() {
        selectedLayout = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString(KEY_LAYOUT, "pc");

        LinearLayout root = new LinearLayout(this);
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
                "MECHANICAL • MUSIC • CUSTOMIZATION",
                11, Color.LTGRAY);
        subtitle.setPadding(0, 4, 0, 20);
        content.addView(subtitle);

        LinearLayout status = new LinearLayout(this);
        status.setGravity(Gravity.CENTER_VERTICAL);
        status.setPadding(14, 12, 14, 12);
        status.setBackground(shape(panel, 16));

        TextView statusText = text("●  KEYBOARD STATUS", 14, white);
        status.addView(statusText,
                new LinearLayout.LayoutParams(0, -2, 1));

        TextView statusValue = text("CHECK", 12, red);
        status.addView(statusValue);
        content.addView(status);

        addSpace(content, 22);

        TextView layoutHeading =
                text("CHOOSE KEYBOARD LAYOUT", 17, white);
        layoutHeading.setTypeface(Typeface.DEFAULT_BOLD);
        content.addView(layoutHeading);

        TextView layoutDescription = text(
                "Choose the keyboard style you want to use.",
                13, Color.LTGRAY);
        layoutDescription.setPadding(0, 6, 0, 14);
        content.addView(layoutDescription);

        addLayoutOption(
                content,
                "⌨  PC COMPUTER KEYBOARD",
                "Mechanical PC-style layout with Ctrl, Alt, Win, Fn and more.",
                "pc");

        addLayoutOption(
                content,
                "▦  NORMAL MOBILE KEYBOARD",
                "Simple mobile QWERTY layout for everyday typing.",
                "mobile");

        addSpace(content, 12);

        TextView heading = text("KEYBOARD SETUP", 17, white);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        content.addView(heading);

        addSpace(content, 8);

        Button enable = button("Enable Keyboard", red);
        enable.setOnClickListener(v ->
                startActivity(new Intent(
                        Settings.ACTION_INPUT_METHOD_SETTINGS)));
        content.addView(enable, fullWidth());

        Button choose = button("Select PC Music Keyboard", panel);
        choose.setOnClickListener(v -> {
            InputMethodManager imm =
                    (InputMethodManager)
                            getSystemService(INPUT_METHOD_SERVICE);

            if (imm != null) {
                imm.showInputMethodPicker();
            }
        });
        content.addView(choose, fullWidth());

        addSpace(content, 22);

        TextView toolsHeading = text("QUICK SETTINGS", 17, white);
        toolsHeading.setTypeface(Typeface.DEFAULT_BOLD);
        content.addView(toolsHeading);

        addSpace(content, 8);

        Button settings = button("⚙  Keyboard Settings", panel);
        settings.setOnClickListener(v -> showInfo(
                "Keyboard Settings",
                "Theme, key size, vibration and sound controls will be added."));
        content.addView(settings, fullWidth());

        Button music = button("♫  Music Studio", panel);
        music.setOnClickListener(v -> showInfo(
                "Music Studio",
                "Guitar, piano, different drum sets, synthesizer and recording features are planned."));
        content.addView(music, fullWidth());

        Button about = button("ⓘ  About PC Music Keyboard", panel);
        about.setOnClickListener(v -> showInfo(
                "About",
                "PC Music Keyboard\nVersion 1.0\nA customizable keyboard for typing and music."));
        content.addView(about, fullWidth());

        addSpace(content, 22);

        TextView note = text(
                "Your keyboard layout choice is saved in the app.",
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
        layout.addView(space,
                new LinearLayout.LayoutParams(1, height));
    }

    private LinearLayout.LayoutParams fullWidth() {
        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = 10;
        return p;
    }
}

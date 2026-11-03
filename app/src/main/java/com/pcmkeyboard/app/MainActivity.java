
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

    private final int bg = Color.rgb(28, 17, 20);
    private final int panel = Color.rgb(39, 25, 36);
    private final int red = Color.rgb(115, 217, 206);
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

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
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
        b.setTextColor(color == red ? Color.rgb(12, 35, 35) : white);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT_BOLD);

        GradientDrawable background = shape(color, dp(17));
        background.setStroke(dp(1), color == panel
                ? Color.rgb(91, 66, 85) : color);
        b.setBackground(background);
        b.setPadding(dp(12), dp(8), dp(12), dp(8));
        b.setMinHeight(dp(52));
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
        card.setPadding(dp(15), dp(13), dp(15), dp(13));

        GradientDrawable cardBackground = shape(
                selected ? Color.rgb(19, 57, 54) : panel, dp(17));
        cardBackground.setStroke(dp(selected ? 2 : 1),
                selected ? red : Color.rgb(83, 62, 78));

        card.setBackground(cardBackground);
        card.setClickable(true);
        card.setFocusable(true);

        TextView name = text(title, 16, white);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(name);

        TextView detail = text(description, 12, Color.rgb(210, 202, 211));
        detail.setPadding(0, dp(5), 0, dp(9));
        card.addView(detail);

        TextView status = text(
                selected ? "✓  SELECTED" : "TAP TO SELECT",
                12, selected ? red : Color.LTGRAY);
        status.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(status);

        card.setOnClickListener(v -> {
            selectedLayout = layoutId;
            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit().putString(KEY_LAYOUT, layoutId).apply();
            showHome();
        });

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(10);
        parent.addView(card, params);
    }

    private void showHome() {
        selectedLayout = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString(KEY_LAYOUT, "pc");

        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(18), dp(16), dp(20));
        root.setBackgroundColor(bg);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        // Brand header
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView logo = text("K", 34, red);
        logo.setGravity(Gravity.CENTER);
        logo.setTypeface(Typeface.DEFAULT_BOLD);
        logo.setBackground(shape(Color.rgb(7, 39, 39), dp(17)));
        LinearLayout.LayoutParams logoParams =
                new LinearLayout.LayoutParams(dp(58), dp(58));
        logoParams.rightMargin = dp(13);
        header.addView(logo, logoParams);

        LinearLayout brandGroup = new LinearLayout(this);
        brandGroup.setOrientation(LinearLayout.VERTICAL);

        TextView brand = text("KeyPilot", 27, white);
        brand.setTypeface(Typeface.DEFAULT_BOLD);
        brandGroup.addView(brand);

        TextView subtitle = text("SMART  •  EASY  •  POWERFUL",
                10, red);
        subtitle.setPadding(0, dp(3), 0, 0);
        brandGroup.addView(subtitle);

        header.addView(brandGroup,
                new LinearLayout.LayoutParams(0, -2, 1f));

        Button gear = button("⚙", panel);
        gear.setOnClickListener(v ->
                startActivity(new Intent(this, ThemeActivity.class)));
        header.addView(gear,
                new LinearLayout.LayoutParams(dp(48), dp(48)));

        content.addView(header);

        addSpace(content, dp(17));

        // Keyboard status
        boolean enabled = isKeyPilotEnabled();

        LinearLayout status = new LinearLayout(this);
        status.setGravity(Gravity.CENTER_VERTICAL);
        status.setPadding(dp(14), dp(14), dp(14), dp(14));

        GradientDrawable statusBg = shape(
                Color.rgb(39, 25, 36), dp(16));
        statusBg.setStroke(dp(1), Color.rgb(86, 61, 80));
        status.setBackground(statusBg);

        TextView statusText = text("●  KEYBOARD STATUS", 13, white);
        status.addView(statusText,
                new LinearLayout.LayoutParams(0, -2, 1f));

        TextView statusValue = text(
                enabled ? "ENABLED" : "SETUP",
                12, enabled ? red : Color.rgb(255, 190, 95));
        statusValue.setTypeface(Typeface.DEFAULT_BOLD);
        status.addView(statusValue);

        status.setOnClickListener(v -> startActivity(
                new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));
        content.addView(status, fullWidth());

        addSpace(content, dp(17));

        TextView layoutHeading =
                text("CHOOSE KEYBOARD LAYOUT", 17, white);
        layoutHeading.setTypeface(Typeface.DEFAULT_BOLD);
        content.addView(layoutHeading);

        TextView layoutDescription = text(
                "Choose the typing experience you prefer.",
                13, Color.LTGRAY);
        layoutDescription.setPadding(0, dp(5), 0, dp(12));
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

        addSpace(content, dp(7));

        TextView setupHeading = text("KEYBOARD SETUP", 17, white);
        setupHeading.setTypeface(Typeface.DEFAULT_BOLD);
        content.addView(setupHeading);

        addSpace(content, dp(7));

        Button enable = button("⚡  Enable Keyboard", red);
        enable.setOnClickListener(v -> startActivity(
                new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));
        content.addView(enable, fullWidth());

        Button choose = button("✓  Select KeyPilot", panel);
        choose.setOnClickListener(v -> {
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showInputMethodPicker();
        });
        content.addView(choose, fullWidth());

        addSpace(content, dp(13));

        TextView toolsHeading = text("QUICK SETTINGS", 17, white);
        toolsHeading.setTypeface(Typeface.DEFAULT_BOLD);
        content.addView(toolsHeading);

        addSpace(content, dp(7));

        LinearLayout quickRow = new LinearLayout(this);
        quickRow.setOrientation(LinearLayout.HORIZONTAL);

        Button settings = button("⚙  Keyboard Settings", panel);
        settings.setTextSize(12);
        settings.setOnClickListener(v ->
                startActivity(new Intent(this, ThemeActivity.class)));

        LinearLayout.LayoutParams quickParams =
                new LinearLayout.LayoutParams(0, dp(57), 1f);
        quickParams.rightMargin = dp(6);
        quickRow.addView(settings, quickParams);

        Button about = button("ⓘ  About KeyPilot", panel);
        about.setTextSize(12);
        about.setOnClickListener(v -> showInfo(
                "About KeyPilot",
                "KeyPilot\nVersion 1.0\nYour Keyboard, Your Control.\n\n"
                + "A customizable Android keyboard for everyday typing. "
                + "Choose a full PC-style layout or a familiar mobile QWERTY layout, "
                + "personalize themes and key styles, and adjust typing feedback. "
                + "Optional voice typing is available through your device's speech "
                + "recognition service.\n\n"
                + "Support: support.keypilot@gmail.com"));

        quickRow.addView(about,
                new LinearLayout.LayoutParams(0, dp(57), 1f));
        content.addView(quickRow);

        addSpace(content, dp(13));

        // Premium theme entry - store screen will be implemented separately
        LinearLayout premium = new LinearLayout(this);
        premium.setGravity(Gravity.CENTER_VERTICAL);
        premium.setPadding(dp(14), dp(13), dp(14), dp(13));

        GradientDrawable premiumBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.rgb(12, 49, 49), panel});
        premiumBg.setCornerRadius(dp(17));
        premiumBg.setStroke(dp(1), red);
        premium.setBackground(premiumBg);

        LinearLayout premiumText = new LinearLayout(this);
        premiumText.setOrientation(LinearLayout.VERTICAL);

        TextView premiumTitle = text("♛  PREMIUM THEMES", 15, red);
        premiumTitle.setTypeface(Typeface.DEFAULT_BOLD);
        premiumText.addView(premiumTitle);

        TextView premiumDetail = text(
                "Wallpapers • Motion effects • Special keys",
                11, Color.LTGRAY);
        premiumDetail.setPadding(0, dp(4), 0, 0);
        premiumText.addView(premiumDetail);

        premium.addView(premiumText,
                new LinearLayout.LayoutParams(0, -2, 1f));

        TextView explore = text("Explore  ›", 13, red);
        explore.setTypeface(Typeface.DEFAULT_BOLD);
        premium.addView(explore);

        premium.setOnClickListener(v -> {
            Intent storeIntent = new Intent(this, ThemeActivity.class);
            storeIntent.putExtra("open_premium", true);
            startActivity(storeIntent);
        });
        content.addView(premium, fullWidth());

        TextView note = text(
                "ⓘ  Your keyboard layout choice is saved in the app.",
                11, Color.LTGRAY);
        note.setPadding(dp(3), dp(3), 0, dp(8));
        content.addView(note);

        // Reserved banner location. This is not a live advertisement yet.
        LinearLayout adCard = new LinearLayout(this);
        adCard.setOrientation(LinearLayout.VERTICAL);
        adCard.setPadding(dp(12), dp(9), dp(12), dp(10));

        GradientDrawable adBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.rgb(15, 28, 55), Color.rgb(42, 22, 58)});
        adBg.setCornerRadius(dp(15));
        adBg.setStroke(dp(1), red);
        adCard.setBackground(adBg);

        TextView adLabel = text("AD  •  RESERVED SPACE", 9, red);
        adLabel.setTypeface(Typeface.DEFAULT_BOLD);
        adCard.addView(adLabel);

        TextView adTitle = text("Discover Amazing Apps", 15, white);
        adTitle.setTypeface(Typeface.DEFAULT_BOLD);
        adTitle.setPadding(0, dp(4), 0, 0);
        adCard.addView(adTitle);

        TextView adDetail = text(
                "Home-page advertisement will appear here.",
                11, Color.LTGRAY);
        adDetail.setPadding(0, dp(2), 0, 0);
        adCard.addView(adDetail);

        content.addView(adCard, fullWidth());

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
    }

    private boolean isKeyPilotEnabled() {
        InputMethodManager imm =
                (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm == null) return false;

        for (android.view.inputmethod.InputMethodInfo info
                : imm.getEnabledInputMethodList()) {
            if (getPackageName().equals(info.getPackageName())) return true;
        }
        return false;
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

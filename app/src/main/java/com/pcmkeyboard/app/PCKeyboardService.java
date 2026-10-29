package com.pcmkeyboard.app;

import android.inputmethodservice.InputMethodService;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PCKeyboardService extends InputMethodService {

    private final int background = Color.rgb(25, 26, 30);
    private final int keyColor = Color.rgb(224, 226, 230);
    private final int specialColor = Color.rgb(65, 67, 73);
    private final int redColor = Color.rgb(220, 45, 45);
    private final int keyText = Color.rgb(25, 26, 30);

    private boolean shift = false;
    private boolean symbols = false;

    @Override
    public View onCreateInputView() {
        LinearLayout keyboard = new LinearLayout(this);
        keyboard.setOrientation(LinearLayout.VERTICAL);
        keyboard.setPadding(3, 7, 3, 7);
        keyboard.setBackgroundColor(background);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(5, 0, 5, 7);

        TextView title = new TextView(this);
        title.setText("PC MUSIC KEYBOARD");
        title.setTextColor(Color.WHITE);
        title.setTextSize(12);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        toolbar.addView(title, new LinearLayout.LayoutParams(0, 38, 1));

        Button close = makeKey("⌄", specialColor, Color.WHITE);
        close.setOnClickListener(v -> requestHideSelf(0));
        toolbar.addView(close, new LinearLayout.LayoutParams(42, 38));

        keyboard.addView(toolbar);

        String[] numberRows = {
            "1234567890",
            "qwertyuiop",
            "asdfghjkl",
            "zxcvbnm"
        };

        String[] symbolRows = {
            "1234567890",
            "@#$%&*-+()",
            "!\"'/:;?= ",
            "zxcvbnm"
        };

        String[] rows = symbols ? symbolRows : numberRows;

        addRow(keyboard, new String[]{"Esc", "1","2","3","4","5","6","7","8","9","0","⌫"}, 1);
        addRow(keyboard, new String[]{"Tab","q","w","e","r","t","y","u","i","o","p"}, 1);
        addRow(keyboard, new String[]{"Caps","a","s","d","f","g","h","j","k","l","Enter"}, 1);
        addRow(keyboard, new String[]{"Shift","z","x","c","v","b","n","m","⇧"}, 1);
        addRow(keyboard, new String[]{"?123",",","🌐","Space",".","↵"}, 1);

        return keyboard;
    }

    private void addRow(LinearLayout parent, String[] labels, int row) {
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(-1, 49);
        rowParams.bottomMargin = 4;
        parent.addView(line, rowParams);

        for (String label : labels) {
            boolean special = label.length() > 1;
            int color = label.equals("Enter") || label.equals("↵")
                    ? redColor : special ? specialColor : keyColor;
            int foreground = special ? Color.WHITE : keyText;

            Button key = makeKey(label, color, foreground);
            float weight = label.equals("Space") ? 5f :
                    (label.equals("Enter") || label.equals("Shift")
                    || label.equals("⇧") || label.equals("Tab")
                    || label.equals("Caps") || label.equals("⌫")
                    || label.equals("Esc") || label.equals("?123")
                    || label.equals("🌐") ? 1.5f : 1f);

            LinearLayout.LayoutParams p =
                    new LinearLayout.LayoutParams(0, -1, weight);
            p.setMargins(2, 1, 2, 1);
            line.addView(key, p);

            key.setOnClickListener(v -> handleKey(label));
        }
    }

    private Button makeKey(String label, int color, int foreground) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(foreground);
        b.setTextSize(label.length() > 1 ? 10 : 17);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(0, 0, 0, 0);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT_BOLD);

        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(7);
        shape.setStroke(1, Color.rgb(45, 46, 50));
        b.setBackground(shape);
        return b;
    }

    private void handleKey(String label) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        switch (label) {
            case "⌄":
                requestHideSelf(0);
                break;
            case "Esc":
                break;
            case "⌫":
                ic.deleteSurroundingText(1, 0);
                break;
            case "Enter":
            case "↵":
                ic.commitText("\\n", 1);
                break;
            case "Space":
                ic.commitText(" ", 1);
                break;
            case "Tab":
                ic.commitText("\\t", 1);
                break;
            case "Shift":
            case "⇧":
            case "Caps":
                shift = !shift;
                break;
            case "?123":
                symbols = !symbols;
                requestKeyboardRefresh();
                break;
            case "🌐":
                requestShowSelf(0);
                break;
            default:
                String value = label;
                if (shift) {
                    value = label.toUpperCase();
                    shift = false;
                }
                ic.commitText(value, 1);
                break;
        }
    }

    private void requestKeyboardRefresh() {
        setInputView(onCreateInputView());
    }
}

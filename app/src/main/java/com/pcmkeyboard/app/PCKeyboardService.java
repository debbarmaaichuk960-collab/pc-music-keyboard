package com.pcmkeyboard.app;

import android.inputmethodservice.InputMethodService;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PCKeyboardService extends InputMethodService {

    private final int bg = Color.rgb(32, 33, 37);
    private final int frame = Color.rgb(43, 45, 50);
    private final int whiteKey = Color.rgb(239, 240, 244);
    private final int darkKey = Color.rgb(70, 72, 78);
    private final int redKey = Color.rgb(196, 25, 42);

    private boolean shift = false;
    private boolean caps = false;
    private boolean symbols = false;
    private boolean num = false;
    private boolean scr = false;

    private static final int LIGHT = 0;
    private static final int DARK = 1;
    private static final int RED = 2;

    private static class KeySpec {
        String main, top, shifted, symbol, action;
        int type;
        float weight;

        KeySpec(String main, String top, String shifted,
                String symbol, int type, float weight, String action) {
            this.main = main;
            this.top = top;
            this.shifted = shifted;
            this.symbol = symbol;
            this.type = type;
            this.weight = weight;
            this.action = action;
        }
    }

    private KeySpec k(String main, String top, String shifted,
                      String symbol, int type, float weight, String action) {
        return new KeySpec(main, top, shifted, symbol,
                type, weight, action);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), stroke);
        return d;
    }

    @Override
    public View onCreateInputView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(5), dp(5), dp(5), dp(7));
        root.setBackgroundColor(bg);

        // TOP TOOLBAR
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setFillViewport(false);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);

        addChip(toolbar, "× Close", DARK, () -> requestHideSelf(0));
        addChip(toolbar, "▣ Keyboard Mode", DARK, null);
        addChip(toolbar, "● MOBILE [active]", DARK, null);
        addChip(toolbar, caps ? "● CAPS" : "CAPS", caps ? LIGHT : DARK,
                () -> { caps = !caps; refresh(); });
        addChip(toolbar, num ? "● NUM" : "NUM", num ? LIGHT : DARK,
                () -> { num = !num; refresh(); });
        addChip(toolbar, scr ? "● SCR" : "SCR", scr ? LIGHT : DARK,
                () -> { scr = !scr; refresh(); });
        addChip(toolbar, "HHKB 60%", DARK, null);
        addChip(toolbar, "Holy Pandas", DARK, null);
        addChip(toolbar, "Black", DARK, null);
        addChip(toolbar, "◖))", DARK, null);

        scroll.addView(toolbar);
        root.addView(scroll, new LinearLayout.LayoutParams(
                -1, dp(40)));

        // KEYBOARD FRAME
        LinearLayout board = new LinearLayout(this);
        board.setOrientation(LinearLayout.VERTICAL);
        board.setPadding(dp(3), dp(4), dp(3), dp(3));
        board.setBackground(shape(frame, 10, Color.rgb(78, 80, 86)));

        // ROW 1: ESC + NUMBER ROW
        addRow(board, new KeySpec[] {
            k("Esc","","","",RED,1.25f,"ESC"),
            k("1","!","!","!",LIGHT,1,null),
            k("2","@","@","@",LIGHT,1,null),
            k("3","#","#","#",LIGHT,1,null),
            k("4","$","$","$",LIGHT,1,null),
            k("5","%","%","%",LIGHT,1,null),
            k("6","^","^","^",LIGHT,1,null),
            k("7","&","&","&",LIGHT,1,null),
            k("8","*","*","*",LIGHT,1,null),
            k("9","(","(","(",LIGHT,1,null),
            k("0",")",")",")",LIGHT,1,null),
            k("-","_","_","_",LIGHT,1,null),
            k("=","+","+","+ ",LIGHT,1,null),
            k("\\","|","|","|",LIGHT,1,null)
        }, 47);

        // ROW 2: TAB + QWERTY
        addRow(board, new KeySpec[] {
            k("Tab","","","",DARK,1.35f,"TAB"),
            k("q","","Q","@",LIGHT,1,null),
            k("w","","W","#",LIGHT,1,null),
            k("e","","E","$",LIGHT,1,null),
            k("r","","R","%",LIGHT,1,null),
            k("t","","T","&",LIGHT,1,null),
            k("y","","Y","-",LIGHT,1,null),
            k("u","","U","+",LIGHT,1,null),
            k("i","","I","(",LIGHT,1,null),
            k("o","","O",")",LIGHT,1,null),
            k("p","","P","*",LIGHT,1,null),
            k("[","{","{","{",LIGHT,1,null),
            k("]", "}", "}", "}", LIGHT,1,null),
            k("Backspace","","","",DARK,1.55f,"BACKSPACE")
        }, 48);

        // ROW 3: CTRL + HOME ROW + ENTER
        addRow(board, new KeySpec[] {
            k("Ctrl","","","",DARK,1.7f,"CTRL"),
            k("a","","A","!",LIGHT,1,null),
            k("s","","S","\"",LIGHT,1,null),
            k("d","","D","'",LIGHT,1,null),
            k("f","","F",":",LIGHT,1,null),
            k("g","","G",";",LIGHT,1,null),
            k("h","","H","/",LIGHT,1,null),
            k("j","","J","?",LIGHT,1,null),
            k("k","","K","=",LIGHT,1,null),
            k("l","","L","_",LIGHT,1,null),
            k("; ",":",":",":",LIGHT,1,null),
            k("'","\"","\"","\"",LIGHT,1,null),
            k("Enter","","","",RED,1.8f,"ENTER")
        }, 48);

        // ROW 4: SHIFT + BOTTOM LETTERS + FN
        addRow(board, new KeySpec[] {
            k("Shift","","","",DARK,1.9f,"SHIFT"),
            k("z","","Z","<",LIGHT,1,null),
            k("x","","X",">",LIGHT,1,null),
            k("c","","C","~",LIGHT,1,null),
            k("v","","V","^",LIGHT,1,null),
            k("b","","B","`",LIGHT,1,null),
            k("n","","N","[",LIGHT,1,null),
            k("m","","M","]",LIGHT,1,null),
            k(",", "<", "<", "<", LIGHT,1,null),
            k(".",">",">",">",LIGHT,1,null),
            k("/","?","?","?",LIGHT,1,null),
            k("Shift","","","",DARK,1.9f,"SHIFT"),
            k("Fn","","","",DARK,0.85f,"FN")
        }, 48);

        // ROW 5: ALT + WIN + SPACE
        addRow(board, new KeySpec[] {
            k("Alt","","","",DARK,1.15f,"ALT"),
            k("Win","","","",DARK,1.15f,"WIN"),
            k("","", "", "",LIGHT,5.2f,"SPACE"),
            k("Win","","","",DARK,1.15f,"WIN"),
            k("Alt","","","",DARK,1.15f,"ALT")
        }, 49);

        root.addView(board, new LinearLayout.LayoutParams(-1, -2));

        return root;
    }

    private void addChip(LinearLayout parent, String label,
                         int type, Runnable click) {
        TextView chip = new TextView(this);
        chip.setText(label);
        chip.setTextSize(10);
        chip.setTypeface(Typeface.DEFAULT_BOLD);
        chip.setTextColor(type == LIGHT ? Color.DKGRAY : Color.WHITE);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(9), dp(7), dp(9), dp(7));

        int color = type == LIGHT ? whiteKey : darkKey;
        chip.setBackground(shape(color, 7, Color.rgb(24,25,29)));

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(-2, dp(34));
        p.setMargins(dp(2), 0, dp(2), 0);
        parent.addView(chip, p);

        if (click != null) {
            chip.setOnClickListener(v -> click.run());
        }
    }

    private void addRow(LinearLayout parent, KeySpec[] keys, int height) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(-1, dp(height));
        rowParams.bottomMargin = dp(3);
        parent.addView(row, rowParams);

        for (KeySpec spec : keys) {
            FrameLayout shell = new FrameLayout(this);

            int base = spec.type == LIGHT
                    ? Color.rgb(112,114,120)
                    : spec.type == RED
                    ? Color.rgb(105,12,22)
                    : Color.rgb(27,28,32);

            int face = spec.type == LIGHT ? whiteKey
                    : spec.type == RED ? redKey : darkKey;

            int foreground = spec.type == LIGHT
                    ? Color.rgb(35,36,41) : Color.WHITE;

            shell.setBackground(shape(base, 6, Color.rgb(19,20,23)));

            LinearLayout content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setGravity(Gravity.CENTER);
            content.setPadding(dp(2), dp(1), dp(2), dp(1));
            content.setBackground(shape(face, 4,
                    spec.type == LIGHT
                    ? Color.rgb(190,192,198)
                    : Color.rgb(35,36,40)));

            FrameLayout.LayoutParams faceParams =
                    new FrameLayout.LayoutParams(-1, -1);
            faceParams.setMargins(dp(2), dp(1), dp(2), dp(5));
            shell.addView(content, faceParams);

            String label = currentLabel(spec);
            boolean hasTop = !symbols && !spec.top.isEmpty();

            if (hasTop) {
                TextView top = new TextView(this);
                top.setText(spec.top);
                top.setTextSize(8);
                top.setTextColor(foreground);
                top.setGravity(Gravity.TOP | Gravity.START);
                top.setPadding(dp(3), 0, 0, 0);
                content.addView(top, new LinearLayout.LayoutParams(
                        -1, 0, 0.55f));
            }

            TextView main = new TextView(this);
            main.setText(label.isEmpty() ? " " : label);
            main.setTextColor(foreground);
            main.setGravity(Gravity.CENTER);
            main.setTypeface(Typeface.DEFAULT_BOLD);
            main.setIncludeFontPadding(false);

            boolean longLabel = label.length() > 3;
            main.setTextSize(longLabel ? 8 : 15);

            content.addView(main, new LinearLayout.LayoutParams(
                    -1, 0, hasTop ? 1.3f : 1f));

            LinearLayout.LayoutParams keyParams =
                    new LinearLayout.LayoutParams(0, -1, spec.weight);
            keyParams.setMargins(dp(1), 0, dp(1), 0);
            row.addView(shell, keyParams);

            shell.setElevation(dp(2));
            shell.setOnClickListener(v -> handleKey(spec));
        }
    }

    private String currentLabel(KeySpec spec) {
        if (spec.action != null) return spec.main;

        if (symbols && !spec.symbol.isEmpty()) {
            return spec.symbol;
        }

        if ((shift || caps) && !spec.shifted.isEmpty()) {
            return spec.shifted;
        }

        return spec.main;
    }

    private void handleKey(KeySpec spec) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        String action = spec.action;

        if (action == null) {
            String value = currentLabel(spec);
            if (!value.isEmpty()) {
                ic.commitText(value, 1);
            }
            if (shift) {
                shift = false;
                refresh();
            }
            return;
        }

        switch (action) {
            case "BACKSPACE":
                ic.deleteSurroundingText(1, 0);
                break;

            case "SPACE":
                ic.commitText(" ", 1);
                break;

            case "TAB":
                ic.commitText("\t", 1);
                break;

            case "ENTER":
                EditorInfo info = getCurrentInputEditorInfo();
                int actionId = info == null
                        ? EditorInfo.IME_ACTION_NONE
                        : info.imeOptions & EditorInfo.IME_MASK_ACTION;

                if (actionId != EditorInfo.IME_ACTION_NONE
                        && actionId != EditorInfo.IME_ACTION_UNSPECIFIED) {
                    ic.performEditorAction(actionId);
                } else {
                    ic.commitText("\n", 1);
                }
                break;

            case "SHIFT":
                shift = !shift;
                refresh();
                break;

            case "FN":
                symbols = !symbols;
                refresh();
                break;

            case "CAPS":
                caps = !caps;
                refresh();
                break;

            case "ESC":
                ic.sendKeyEvent(new KeyEvent(
                        KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ESCAPE));
                ic.sendKeyEvent(new KeyEvent(
                        KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ESCAPE));
                break;

            default:
                break;
        }
    }

    private void refresh() {
        setInputView(onCreateInputView());
    }
}
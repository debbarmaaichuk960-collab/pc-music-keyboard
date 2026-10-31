package com.pcmkeyboard.app;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.inputmethodservice.InputMethodService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PCKeyboardService extends InputMethodService {

    // ---------- theme (loaded from prefs, changed via ThemeActivity) ----------
    private int bg, frame, whiteKey, darkKey, redKey, accent, mutedText, darkText;
    private Bitmap bgBitmap;
    private long bgStamp = -1;
    private int dimAlpha = 110;
    private boolean hapticOn = true;
    private long lastStamp = 0;
    private String voiceLang = "en-IN";

    // bg, frame, lightKey, darkKey, redKey, accent, muted, darkKeyText
    private static final int[][] THEMES = {
            {Color.rgb(8, 15, 28), Color.rgb(18, 30, 48), Color.rgb(225, 235, 246),
                    Color.rgb(39, 57, 80), Color.rgb(255, 103, 82), Color.rgb(48, 215, 194),
                    Color.rgb(154, 174, 197), Color.WHITE},
            {Color.rgb(232, 237, 244), Color.rgb(205, 214, 226), Color.WHITE,
                    Color.rgb(170, 184, 203), Color.rgb(255, 103, 82), Color.rgb(0, 150, 136),
                    Color.rgb(90, 105, 125), Color.rgb(20, 30, 45)},
            {Color.BLACK, Color.rgb(12, 12, 14), Color.rgb(215, 215, 220),
                    Color.rgb(40, 40, 46), Color.rgb(230, 70, 70), Color.rgb(0, 200, 120),
                    Color.rgb(130, 130, 140), Color.WHITE},
            {Color.rgb(20, 10, 36), Color.rgb(38, 22, 66), Color.rgb(235, 226, 250),
                    Color.rgb(70, 48, 112), Color.rgb(255, 95, 140), Color.rgb(180, 130, 255),
                    Color.rgb(176, 160, 208), Color.WHITE}
    };

    // ---------- state ----------
    private boolean shift, caps, symbols, num, scr, ctrl, alt, altGr, win;
    private boolean mobileLayout = false;
    private boolean emojiOpen = false;
    private boolean needRefresh = false;
    private int symPage = 0;
    private int emojiCat = 0;
    private long lastShiftTap = 0;

    private String pcPanel = "main";
    private String composingWord = "";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<String> userWords = new ArrayList<>();

    // voice
    private SpeechRecognizer recognizer;
    private boolean listening = false;
    private TextView micView;
    private LinearLayout sugMiddle;

    private static final String[] WORDS = {
            "about", "after", "again", "also", "and", "always", "any", "are", "ask",
            "because", "before", "best", "better", "big", "but", "call", "can", "come",
            "could", "day", "do", "does", "done", "down", "for", "from", "get", "give",
            "go", "good", "great", "have", "hello", "help", "here", "home", "how", "just",
            "know", "like", "little", "look", "love", "make", "many", "meet", "more",
            "need", "new", "night", "now", "okay", "only", "please", "right", "see",
            "send", "should", "sorry", "tell", "thanks", "thank", "that", "the", "their",
            "there", "think", "this", "time", "today", "tomorrow", "want", "what", "when",
            "where", "which", "with", "work", "would", "yes", "you", "your",
            // Hinglish
            "aap", "accha", "achha", "agar", "ab", "aur", "bahut", "bhai", "bhi", "bilkul",
            "chalo", "dekho", "haan", "hai", "hain", "hum", "kaha", "kahan", "kaise",
            "kab", "karo", "karna", "kaun", "kya", "kyu", "kyun", "kuch", "mera", "meri",
            "main", "mujhe", "nahi", "nahin", "padhai", "phir", "raha", "rahi", "sab",
            "tum", "tumhara", "theek", "thik", "toh", "wala", "yaar", "zaroor"
    };

    private static final String[] EMOJI_TABS = {"😀", "❤️", "🐱", "🍔", "⚽", "🎭"};
    private static final String[] EMOJI_DATA = {
            "😀 😃 😄 😁 😆 😅 😂 🤣 😊 😇 🙂 🙃 😉 😍 🥰 😘 😗 😋 😛 😜 🤪 😎 🤩 🥳 😏 😒 😞 😔 😟 😕 🙁 😣 😖 😫 😩 🥺 😢 😭 😤 😠 😡 🤬 🤯 😳 🥵 🥶 😱 😨 😰 😥 😓 🤗 🤔 🤭 🤫 😶 😐 😑 😬 🙄 😯 😴 🤤 😪 😵 🤐 🥴 🤢 🤮 🤧 😷 🤒 🤕",
            "❤️ 🧡 💛 💚 💙 💜 🖤 🤍 💔 ❣️ 💕 💞 💓 💗 💖 💘 💝 👍 👎 👌 ✌️ 🤞 🤟 🤘 👋 🤚 ✋ 🙏 👏 🙌 🤝 💪 👀 🔥 ✨ ⭐ 🌟 💯 ✅ ❌ 🎉 🎊",
            "🐶 🐱 🐭 🐹 🐰 🦊 🐻 🐼 🐨 🐯 🦁 🐮 🐷 🐸 🐵 🐔 🐧 🐦 🦆 🦅 🦉 🐺 🐴 🦄 🐝 🦋 🐢 🐍 🐙 🐬 🐳 🐟",
            "🍎 🍌 🍇 🍓 🍉 🍑 🍍 🥭 🍅 🥕 🌽 🍕 🍔 🍟 🌭 🌮 🍗 🍜 🍝 🍣 🍩 🍪 🎂 🍫 🍿 ☕ 🍵 🥤 🍺",
            "⚽ 🏀 🏏 🏈 🎾 🏐 🎱 🏓 🎮 🎯 🎲 🎧 🎤 🎸 🎹 🥁 🎬 📷 🚗 ✈️ 🚀 🏠 📱 💻 ⌚ 💡 🔔 🎁 💰",
            "(╯°□°)╯︵ ┻━┻|¯\\_(ツ)_/¯|( ͡° ͜ʖ ͡°)|ಠ_ಠ|(づ｡◕‿‿◕｡)づ|ʕ•ᴥ•ʔ|(≧◡≦)|(｡♥‿♥｡)|♪┏(・o･)┛♪|(¬_¬)|"
                    + "Good Morning ☀️|Good Night 🌙|Thank you 🙏|Happy Birthday 🎂🎉|I love you ❤️|Miss you 💔|"
                    + "Congratulations 🎊|Bhai 🔥🔥🔥|Mast hai 😎👍|Kya baat hai! 👏|Sorry 🥺🙏|OK Done ✅"
    };

    private static final int LIGHT = 0;
    private static final int DARK = 1;
    private static final int RED = 2;
    private static final int SPACER = 3;

    private static final String PREFS = "pc_music_keyboard";
    private static final String KEY_LAYOUT = "keyboard_layout";

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
        return new KeySpec(main, top, shifted, symbol, type, weight, action);
    }

    private KeySpec gap(float w) {
        return k("", "", "", "", SPACER, w, null);
    }

    // PC helpers
    private KeySpec pcKey(String main, String top) {
        return k(main, top, top, "", LIGHT, 1f, null);
    }

    private KeySpec pcLetter(String l) {
        return k(l, "", l.toUpperCase(Locale.ROOT), "", LIGHT, 1f, null);
    }

    private KeySpec send(String label, String code, int type, float w) {
        return k(label, "", "", code, type, w, "PC_SEND");
    }

    private KeySpec combo(String label, String caption, String combo) {
        return k(label, caption, "", combo, LIGHT, 1f, "COMBO");
    }

    private KeySpec mobileLetter(String letter, String alternate) {
        return k(letter, "", letter.toUpperCase(Locale.ROOT), alternate, LIGHT, 1, null);
    }

    private KeySpec simpleKey(String value) {
        return k(value, "", value, value, LIGHT, 1, null);
    }

    // ---------- util ----------
    private SharedPreferences prefs() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    /** Make colours slightly see-through when a photo background is used. */
    private int a(int c) {
        return bgBitmap != null ? ((c & 0x00FFFFFF) | 0xCC000000) : c;
    }

    private int darken(int c) {
        return Color.rgb((int) (Color.red(c) * 0.6f), (int) (Color.green(c) * 0.6f),
                (int) (Color.blue(c) * 0.6f));
    }

    private int fgFor(KeySpec s, boolean active) {
        if (active) return Color.rgb(7, 28, 39);
        if (s.type == LIGHT) return Color.rgb(35, 36, 41);
        if (s.type == RED) return Color.WHITE;
        return darkText;
    }

    private int labelSp(String s) {
        int n = s.length();
        return n <= 1 ? 15 : n == 2 ? 13 : n == 3 ? 10 : n <= 5 ? 10 : n <= 8 ? 9 : 8;
    }

    /** Light tap vibration on every key. */
    private void haptic(View v) {
    if (!hapticOn || v == null) return;
    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
}

    private void loadTheme(SharedPreferences p) {
        int t = p.getInt("theme", 0);
        if (t < 0 || t >= THEMES.length) t = 0;
        int[] c = THEMES[t];
        bg = c[0];
        frame = c[1];
        whiteKey = c[2];
        darkKey = c[3];
        redKey = c[4];
        accent = c[5];
        mutedText = c[6];
        darkText = c[7];
        hapticOn = p.getBoolean("haptic", true);
        dimAlpha = p.getInt("bg_dim", 110);
        lastStamp = p.getLong("theme_stamp", 0);
        voiceLang = p.getString("voice_lang", "en-IN");

        File f = new File(getFilesDir(), "kb_bg.jpg");
        if (p.getBoolean("bg_image", false) && f.exists()) {
            long stamp = f.lastModified();
            if (bgBitmap == null || stamp != bgStamp) {
                try {
                    bgBitmap = BitmapFactory.decodeFile(f.getAbsolutePath());
                    bgStamp = stamp;
                } catch (Throwable e) {
                    bgBitmap = null;
                }
            }
        } else {
            bgBitmap = null;
        }
    }

    /** Wraps the keyboard in a fixed-height frame with optional photo background. */
    private View wrap(View content, int heightDp) {
        FrameLayout fl = new FrameLayout(this);
        fl.setBackgroundColor(bg);
        if (bgBitmap != null) {
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageBitmap(bgBitmap);
            fl.addView(iv, new FrameLayout.LayoutParams(-1, -1));
            View dim = new View(this);
            dim.setBackgroundColor(Color.argb(dimAlpha, 0, 0, 0));
            fl.addView(dim, new FrameLayout.LayoutParams(-1, -1));
        }
        fl.addView(content, new FrameLayout.LayoutParams(-1, -1));
        int maxH = (int) (getResources().getDisplayMetrics().heightPixels * 0.62f);
        fl.setLayoutParams(new ViewGroup.LayoutParams(-1, Math.min(dp(heightDp), maxH)));
        return fl;
    }

    // ---------- lifecycle ----------
    @Override
    public void onCreate() {
        super.onCreate();
        String saved = prefs().getString("user_words", "");
        if (!saved.isEmpty()) {
            for (String w : saved.split(",")) if (!w.isEmpty()) userWords.add(w);
        }
    }

    @Override
    public void onDestroy() {
        destroyRecognizer();
        super.onDestroy();
    }

    @Override
    public View onCreateInputView() {
        SharedPreferences p = prefs();
        loadTheme(p);
        mobileLayout = "mobile".equals(p.getString(KEY_LAYOUT, "pc"));
        micView = null;
        sugMiddle = null;
        return mobileLayout ? createMobileKeyboard() : createPcKeyboard();
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        if (!restarting) {
            composingWord = "";
            if (shift || ctrl || alt || altGr || win) needRefresh = true;
            shift = false;
            ctrl = false;
            alt = false;
            altGr = false;
            win = false;
            if (emojiOpen) {
                emojiOpen = false;
                needRefresh = true;
            }
        }
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        long stamp = prefs().getLong("theme_stamp", 0);
        boolean rebuild = needRefresh || stamp != lastStamp;
        needRefresh = false;
        if (mobileLayout && autoCaps()) rebuild = true;
        if (rebuild) refresh();
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        stopVoiceNow();
        super.onFinishInputView(finishingInput);
    }

    private void refresh() {
        handler.removeCallbacksAndMessages(null);
        setInputView(onCreateInputView());
    }

    private void showSystemKeyboardPicker() {
    if (android.os.Build.VERSION.SDK_INT >= 28) {
        try {
            if (shouldOfferSwitchingToNextInputMethod()
                    && switchToNextInputMethod(false)) {
                return;
            }
        } catch (RuntimeException ignored) {
        }
    }

    InputMethodManager manager =
            (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

    if (manager != null) {
        manager.showInputMethodPicker();
    }
}

    private void openThemeScreen() {
        requestHideSelf(0);
        Intent i = new Intent(this, ThemeActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    private void switchLayout(String layout) {
        prefs().edit().putString(KEY_LAYOUT, layout).apply();
        shift = false;
        symbols = false;
        emojiOpen = false;
        ctrl = false;
        alt = false;
        altGr = false;
        win = false;
        composingWord = "";
        refresh();
    }

    // =====================================
    // PC KEYBOARD
    // =====================================

    private View createPcKeyboard() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(6), dp(4), dp(6), dp(4));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView brand = new TextView(this);
        brand.setText("PC MUSIC");
        brand.setTextColor(darkTextOnBg());
        brand.setTextSize(13);
        brand.setTypeface(Typeface.DEFAULT_BOLD);
        brand.setLetterSpacing(0.06f);
        brand.setPadding(dp(3), 0, 0, 0);
        header.addView(brand, new LinearLayout.LayoutParams(0, dp(34), 1f));

        addChip(header, "🌐", LIGHT, this::showSystemKeyboardPicker);
        micView = addChip(header, "🎤", DARK, this::toggleVoice);
        addChip(header, "🎨", DARK, this::openThemeScreen);
        addChip(header, "📱", DARK, () -> switchLayout("mobile"));
        addChip(header, "×", DARK, () -> requestHideSelf(0));
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(36)));

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setGravity(Gravity.CENTER);
        tabs.setPadding(0, dp(2), 0, dp(3));
        addPanelTab(tabs, "MAIN", "main");
        addPanelTab(tabs, "F1-12", "function");
        addPanelTab(tabs, "NAV", "navigation");
        addPanelTab(tabs, "NUM", "numpad");
        addPanelTab(tabs, "MEDIA", "media");
        addPanelTab(tabs, "CTRL+", "shortcuts");
        addPanelTab(tabs, "MORE", "extra");
        root.addView(tabs, new LinearLayout.LayoutParams(-1, dp(34)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout board = new LinearLayout(this);
        board.setOrientation(LinearLayout.VERTICAL);
        board.setPadding(dp(3), dp(4), dp(3), dp(3));
        board.setBackground(shape(a(frame), 12, Color.rgb(42, 67, 91)));
        buildPcPanel(board);

        scroll.addView(board, new FrameLayout.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        refreshMicUi();
        return wrap(root, 330);
    }

    private int darkTextOnBg() {
        return bgBitmap != null ? Color.WHITE : (bg == THEMES[1][0] ? Color.rgb(20, 30, 45) : Color.WHITE);
    }

    private void buildPcPanel(LinearLayout board) {
        if ("main".equals(pcPanel)) {
            // every row sums to 15.5 so all keys are the same size
            addRow(board, new KeySpec[]{
                    k("Esc", "", "", "", RED, 1.25f, "ESC"),
                    pcKey("`", "~"), pcKey("1", "!"), pcKey("2", "@"), pcKey("3", "#"),
                    pcKey("4", "$"), pcKey("5", "%"), pcKey("6", "^"), pcKey("7", "&"),
                    pcKey("8", "*"), pcKey("9", "("), pcKey("0", ")"), pcKey("-", "_"),
                    pcKey("=", "+"),
                    k("⌫", "", "", "", DARK, 1.25f, "BACKSPACE")
            }, 40);
            addRow(board, new KeySpec[]{
                    k("Tab", "", "", "", DARK, 1.75f, "TAB"),
                    pcLetter("q"), pcLetter("w"), pcLetter("e"), pcLetter("r"), pcLetter("t"),
                    pcLetter("y"), pcLetter("u"), pcLetter("i"), pcLetter("o"), pcLetter("p"),
                    pcKey("[", "{"), pcKey("]", "}"),
                    k("\\", "|", "|", "", LIGHT, 1.75f, null)
            }, 40);
            addRow(board, new KeySpec[]{
                    k("Caps", "", "", "", DARK, 1.9f, "CAPS"),
                    pcLetter("a"), pcLetter("s"), pcLetter("d"), pcLetter("f"), pcLetter("g"),
                    pcLetter("h"), pcLetter("j"), pcLetter("k"), pcLetter("l"),
                    pcKey(";", ":"), pcKey("'", "\""),
                    k("Enter", "", "", "", RED, 2.6f, "ENTER")
            }, 40);
            addRow(board, new KeySpec[]{
                    k("Shift", "", "", "", DARK, 2.75f, "SHIFT"),
                    pcLetter("z"), pcLetter("x"), pcLetter("c"), pcLetter("v"), pcLetter("b"),
                    pcLetter("n"), pcLetter("m"),
                    pcKey(",", "<"), pcKey(".", ">"), pcKey("/", "?"),
                    send("↑", "DPAD_UP", DARK, 1f),
                    send("Del", "FORWARD_DEL", DARK, 1.75f)
            }, 40);
            addRow(board, new KeySpec[]{
                    k("Ctrl", "", "", "", DARK, 1.5f, "CTRL"),
                    k("Win", "", "", "", DARK, 1.25f, "WIN"),
                    k("Alt", "", "", "", DARK, 1.25f, "ALT"),
                    k("Space", "", "", "", LIGHT, 8.5f, "SPACE"),
                    send("←", "DPAD_LEFT", DARK, 1f),
                    send("↓", "DPAD_DOWN", DARK, 1f),
                    send("→", "DPAD_RIGHT", DARK, 1f)
            }, 40);

        } else if ("function".equals(pcPanel)) {
            addRow(board, new KeySpec[]{
                    send("F1", "F1", LIGHT, 1), send("F2", "F2", LIGHT, 1),
                    send("F3", "F3", LIGHT, 1), send("F4", "F4", LIGHT, 1),
                    send("F5", "F5", LIGHT, 1), send("F6", "F6", LIGHT, 1)
            }, 42);
            addRow(board, new KeySpec[]{
                    send("F7", "F7", LIGHT, 1), send("F8", "F8", LIGHT, 1),
                    send("F9", "F9", LIGHT, 1), send("F10", "F10", LIGHT, 1),
                    send("F11", "F11", LIGHT, 1), send("F12", "F12", LIGHT, 1)
            }, 42);
            addRow(board, new KeySpec[]{
                    k("Esc", "", "", "", RED, 1, "ESC"),
                    send("PrtSc", "SYSRQ", DARK, 1),
                    k("ScrLk", "", "", "SCROLL_LOCK", DARK, 1, "SCROLL_LOCK"),
                    send("Pause", "BREAK", DARK, 1)
            }, 42);

        } else if ("navigation".equals(pcPanel)) {
            addRow(board, new KeySpec[]{
                    send("Insert", "INSERT", DARK, 1), send("Home", "MOVE_HOME", LIGHT, 1),
                    send("PgUp", "PAGE_UP", LIGHT, 1)
            }, 42);
            addRow(board, new KeySpec[]{
                    send("Delete", "FORWARD_DEL", RED, 1), send("End", "MOVE_END", LIGHT, 1),
                    send("PgDn", "PAGE_DOWN", LIGHT, 1)
            }, 42);
            addRow(board, new KeySpec[]{gap(1), send("↑", "DPAD_UP", LIGHT, 1), gap(1)}, 42);
            addRow(board, new KeySpec[]{
                    send("←", "DPAD_LEFT", LIGHT, 1), send("↓", "DPAD_DOWN", LIGHT, 1),
                    send("→", "DPAD_RIGHT", LIGHT, 1)
            }, 42);

        } else if ("numpad".equals(pcPanel)) {
            addRow(board, new KeySpec[]{
                    k("NumLk", "", "", "NUM_LOCK", DARK, 1, "NUM_LOCK"),
                    send("/", "NUMPAD_DIVIDE", LIGHT, 1), send("*", "NUMPAD_MULTIPLY", LIGHT, 1),
                    send("−", "NUMPAD_SUBTRACT", LIGHT, 1)
            }, 40);
            addRow(board, new KeySpec[]{
                    send("7", "NUMPAD_7", LIGHT, 1), send("8", "NUMPAD_8", LIGHT, 1),
                    send("9", "NUMPAD_9", LIGHT, 1), send("+", "NUMPAD_ADD", RED, 1)
            }, 40);
            addRow(board, new KeySpec[]{
                    send("4", "NUMPAD_4", LIGHT, 1), send("5", "NUMPAD_5", LIGHT, 1),
                    send("6", "NUMPAD_6", LIGHT, 1), send("⌫", "DEL", DARK, 1)
            }, 40);
            addRow(board, new KeySpec[]{
                    send("1", "NUMPAD_1", LIGHT, 1), send("2", "NUMPAD_2", LIGHT, 1),
                    send("3", "NUMPAD_3", LIGHT, 1), send("Enter", "NUMPAD_ENTER", RED, 1)
            }, 40);
            addRow(board, new KeySpec[]{
                    send("0", "NUMPAD_0", LIGHT, 2), send(".", "NUMPAD_DOT", LIGHT, 1),
                    send("=", "NUMPAD_EQUALS", LIGHT, 1)
            }, 40);

        } else if ("media".equals(pcPanel)) {
            addRow(board, new KeySpec[]{
                    send("Vol −", "VOLUME_DOWN", DARK, 1), send("Mute", "VOLUME_MUTE", DARK, 1),
                    send("Vol +", "VOLUME_UP", DARK, 1)
            }, 42);
            addRow(board, new KeySpec[]{
                    send("Prev", "MEDIA_PREVIOUS", LIGHT, 1),
                    send("Play/Pause", "MEDIA_PLAY_PAUSE", RED, 1.5f),
                    send("Stop", "MEDIA_STOP", LIGHT, 1), send("Next", "MEDIA_NEXT", LIGHT, 1)
            }, 42);
            addRow(board, new KeySpec[]{
                    send("Calc", "CALCULATOR", LIGHT, 1), send("Search", "SEARCH", LIGHT, 1),
                    send("Mail", "ENVELOPE", LIGHT, 1), send("Browser", "EXPLORER", LIGHT, 1)
            }, 42);

        } else if ("shortcuts".equals(pcPanel)) {
            addRow(board, new KeySpec[]{
                    combo("Copy", "Ctrl+C", "CTRL+C"), combo("Paste", "Ctrl+V", "CTRL+V"),
                    combo("Cut", "Ctrl+X", "CTRL+X"), combo("Sel All", "Ctrl+A", "CTRL+A")
            }, 44);
            addRow(board, new KeySpec[]{
                    combo("Undo", "Ctrl+Z", "CTRL+Z"), combo("Redo", "Ctrl+Y", "CTRL+Y"),
                    combo("Save", "Ctrl+S", "CTRL+S"), combo("Find", "Ctrl+F", "CTRL+F")
            }, 44);
            addRow(board, new KeySpec[]{
                    combo("Print", "Ctrl+P", "CTRL+P"), combo("New", "Ctrl+N", "CTRL+N"),
                    combo("Open", "Ctrl+O", "CTRL+O"), combo("Bold", "Ctrl+B", "CTRL+B")
            }, 44);
            addRow(board, new KeySpec[]{
                    combo("New Tab", "Ctrl+T", "CTRL+T"), combo("Close", "Ctrl+W", "CTRL+W"),
                    combo("Reload", "Ctrl+R", "CTRL+R"), combo("Switch", "Alt+Tab", "ALT+TAB")
            }, 44);
            addRow(board, new KeySpec[]{
                    combo("Desktop", "Win+D", "WIN+D"), combo("Files", "Win+E", "WIN+E"),
                    combo("Quit", "Alt+F4", "ALT+F4"), combo("Reopen", "Ctrl+Sh+T", "CTRL+SHIFT+T")
            }, 44);

        } else if ("extra".equals(pcPanel)) {
            addRow(board, new KeySpec[]{
                    k("AltGr", "", "", "", DARK, 1, "ALTGR"),
                    send("Menu", "MENU", DARK, 1), send("Help", "HELP", LIGHT, 1)
            }, 42);
            addRow(board, new KeySpec[]{
                    send("Sleep", "SLEEP", DARK, 1), send("Wake", "WAKEUP", DARK, 1),
                    k("Esc", "", "", "", RED, 1, "ESC")
            }, 42);
            addRow(board, new KeySpec[]{
                    k("Tab", "", "", "", DARK, 1, "TAB"),
                    k("⌫", "", "", "", DARK, 1, "BACKSPACE"),
                    k("Enter", "", "", "", RED, 1, "ENTER")
            }, 42);
        }
    }

    private void addPanelTab(LinearLayout parent, String label, String panel) {
        TextView tab = new TextView(this);
        boolean selected = panel.equals(pcPanel);
        tab.setText(label);
        tab.setTextSize(8.5f);
        tab.setSingleLine(true);
        tab.setTypeface(Typeface.DEFAULT_BOLD);
        tab.setGravity(Gravity.CENTER);
        tab.setTextColor(selected ? Color.rgb(7, 28, 39) : darkText);
        tab.setBackground(shape(selected ? accent : a(darkKey), 8,
                selected ? accent : Color.rgb(57, 80, 108)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(30), 1f);
        params.setMargins(dp(1.5f), 0, dp(1.5f), 0);
        parent.addView(tab, params);
        tab.setOnClickListener(v -> {
            haptic(v);
            pcPanel = panel;
            refresh();
        });
    }

    private void addRow(LinearLayout parent, KeySpec[] keys, int height) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, dp(height));
        rowParams.bottomMargin = dp(3);
        parent.addView(row, rowParams);

        for (KeySpec spec : keys) {
            if (spec.type == SPACER) {
                row.addView(new View(this), new LinearLayout.LayoutParams(0, -1, spec.weight));
                continue;
            }
            FrameLayout shell = new FrameLayout(this);
            boolean active = isModifierActive(spec.action);
            int face = active ? accent
                    : spec.type == LIGHT ? whiteKey
                    : spec.type == RED ? redKey : darkKey;
            int fg = fgFor(spec, active);

            shell.setBackground(shape(a(darken(face)), 6, Color.rgb(7, 13, 24)));

            LinearLayout content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setGravity(Gravity.CENTER);
            content.setPadding(dp(1), dp(1), dp(1), dp(1));
            content.setBackground(shape(a(face), 4,
                    spec.type == LIGHT ? Color.rgb(160, 184, 211) : Color.rgb(55, 77, 103)));
            FrameLayout.LayoutParams faceParams = new FrameLayout.LayoutParams(-1, -1);
            faceParams.setMargins(dp(1), dp(1), dp(1), dp(3));
            shell.addView(content, faceParams);

            String label = currentLabel(spec);
            boolean hasTop = !spec.top.isEmpty();
            if (hasTop) {
                TextView top = new TextView(this);
                top.setText(spec.top);
                top.setTextSize(7.5f);
                top.setSingleLine(true);
                top.setTextColor(active ? fg : (spec.type == LIGHT ? Color.rgb(90, 100, 118) : fg));
                top.setGravity(Gravity.CENTER);
                content.addView(top, new LinearLayout.LayoutParams(-1, 0, 0.5f));
            }

            TextView main = new TextView(this);
            main.setText(label.isEmpty() ? " " : label);
            main.setTextColor(fg);
            main.setGravity(Gravity.CENTER);
            main.setTypeface(Typeface.DEFAULT_BOLD);
            main.setIncludeFontPadding(false);
            main.setSingleLine(true);
            main.setTextSize(labelSp(label));
            content.addView(main, new LinearLayout.LayoutParams(-1, 0, hasTop ? 1.2f : 1f));

            LinearLayout.LayoutParams keyParams = new LinearLayout.LayoutParams(0, -1, spec.weight);
            keyParams.setMargins(dp(1), 0, dp(1), 0);
            row.addView(shell, keyParams);
            shell.setElevation(dp(2));
            shell.setOnTouchListener(new KeyTouch(spec, false));
        }
    }

    // =====================================
    // MOBILE KEYBOARD
    // =====================================

    private View createMobileKeyboard() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(4), dp(3), dp(4), dp(5));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = new TextView(this);
        brand.setText("PC MUSIC");
        brand.setTextColor(darkTextOnBg());
        brand.setTextSize(12);
        brand.setTypeface(Typeface.DEFAULT_BOLD);
        brand.setPadding(dp(4), 0, 0, 0);
        header.addView(brand, new LinearLayout.LayoutParams(0, dp(32), 1f));
        addChip(header, "PC", DARK, () -> switchLayout("pc"));
        addChip(header, "🎨", DARK, this::openThemeScreen);
        addChip(header, "🌐", LIGHT, this::showSystemKeyboardPicker);
        addChip(header, "×", DARK, () -> requestHideSelf(0));
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(34)));

        if (emojiOpen) {
            buildEmojiPanel(root);
            return wrap(root, 320);
        }

        addMobileSuggestionBar(root);

        LinearLayout board = new LinearLayout(this);
        board.setOrientation(LinearLayout.VERTICAL);
        board.setPadding(dp(3), dp(6), dp(3), dp(1));
        board.setBackground(shape(a(frame), 12, Color.rgb(42, 67, 91)));

        String enter = enterLabel();
        if (!symbols) {
            addMobileRow(board, new KeySpec[]{
                    mobileLetter("q", "1"), mobileLetter("w", "2"), mobileLetter("e", "3"),
                    mobileLetter("r", "4"), mobileLetter("t", "5"), mobileLetter("y", "6"),
                    mobileLetter("u", "7"), mobileLetter("i", "8"), mobileLetter("o", "9"),
                    mobileLetter("p", "0")
            }, 0);
            addMobileRow(board, new KeySpec[]{
                    gap(0.5f),
                    mobileLetter("a", "@"), mobileLetter("s", "#"), mobileLetter("d", "$"),
                    mobileLetter("f", "%"), mobileLetter("g", "&"), mobileLetter("h", "-"),
                    mobileLetter("j", "+"), mobileLetter("k", "("), mobileLetter("l", ")"),
                    gap(0.5f)
            }, 0);
            addMobileRow(board, new KeySpec[]{
                    k(caps ? "⇪" : "⇧", "", "", "", DARK, 1.4f, "SHIFT"),
                    mobileLetter("z", "*"), mobileLetter("x", "\""), mobileLetter("c", "'"),
                    mobileLetter("v", ":"), mobileLetter("b", ";"), mobileLetter("n", "!"),
                    mobileLetter("m", "?"),
                    k("⌫", "", "", "", DARK, 1.4f, "BACKSPACE")
            }, 0);
            addMobileRow(board, new KeySpec[]{
                    k("?123", "", "", "", DARK, 1.4f, "SYMBOLS"),
                    k("😊", "", "", "", DARK, 1f, "EMOJI"),
                    k(",", "", "", "", LIGHT, 0.8f, null),
                    k("English", "", "", "", LIGHT, 4f, "SPACE"),
                    k(".", "", "", "", LIGHT, 0.8f, null),
                    k(enter, "", "", "", RED, 1.4f, "ENTER")
            }, 0);
        } else {
            if (symPage == 0) {
                addMobileRow(board, new KeySpec[]{
                        simpleKey("1"), simpleKey("2"), simpleKey("3"), simpleKey("4"),
                        simpleKey("5"), simpleKey("6"), simpleKey("7"), simpleKey("8"),
                        simpleKey("9"), simpleKey("0")}, 0);
                addMobileRow(board, new KeySpec[]{
                        simpleKey("@"), simpleKey("#"), simpleKey("$"), simpleKey("%"),
                        simpleKey("&"), simpleKey("-"), simpleKey("+"), simpleKey("("),
                        simpleKey(")"), simpleKey("/")}, 0);
                addMobileRow(board, new KeySpec[]{
                        k("=\\<", "", "", "", DARK, 1.4f, "SYMPAGE"),
                        simpleKey("*"), simpleKey("\""), simpleKey("'"), simpleKey(":"),
                        simpleKey(";"), simpleKey("!"), simpleKey("?"),
                        k("⌫", "", "", "", DARK, 1.4f, "BACKSPACE")}, 0);
            } else {
                addMobileRow(board, new KeySpec[]{
                        simpleKey("~"), simpleKey("`"), simpleKey("|"), simpleKey("•"),
                        simpleKey("√"), simpleKey("π"), simpleKey("÷"), simpleKey("×"),
                        simpleKey("¶"), simpleKey("∆")}, 0);
                addMobileRow(board, new KeySpec[]{
                        simpleKey("£"), simpleKey("¢"), simpleKey("€"), simpleKey("¥"),
                        simpleKey("^"), simpleKey("°"), simpleKey("="), simpleKey("{"),
                        simpleKey("}"), simpleKey("\\")}, 0);
                addMobileRow(board, new KeySpec[]{
                        k("?123", "", "", "", DARK, 1.4f, "SYMPAGE"),
                        simpleKey("%"), simpleKey("©"), simpleKey("®"), simpleKey("™"),
                        simpleKey("✓"), simpleKey("["), simpleKey("]"),
                        k("⌫", "", "", "", DARK, 1.4f, "BACKSPACE")}, 0);
            }
            addMobileRow(board, new KeySpec[]{
                    k("ABC", "", "", "", DARK, 1.4f, "SYMBOLS"),
                    k("😊", "", "", "", DARK, 1f, "EMOJI"),
                    simpleKey(","),
                    k("English", "", "", "", LIGHT, 4f, "SPACE"),
                    simpleKey("."),
                    k(enter, "", "", "", RED, 1.4f, "ENTER")
            }, 0);
        }

        root.addView(board, new LinearLayout.LayoutParams(-1, 0, 1f));
        refreshMicUi();
        return wrap(root, 320);
    }

    private String enterLabel() {
        switch (editorAction()) {
            case EditorInfo.IME_ACTION_SEARCH: return "🔍";
            case EditorInfo.IME_ACTION_SEND: return "➤";
            case EditorInfo.IME_ACTION_GO: return "➜";
            case EditorInfo.IME_ACTION_DONE: return "✓";
            case EditorInfo.IME_ACTION_NEXT: return "→";
            default: return "↵";
        }
    }

    private int editorAction() {
        EditorInfo info = getCurrentInputEditorInfo();
        if (info == null) return EditorInfo.IME_ACTION_NONE;
        if ((info.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) {
            return EditorInfo.IME_ACTION_NONE;
        }
        int act = info.imeOptions & EditorInfo.IME_MASK_ACTION;
        return act == EditorInfo.IME_ACTION_UNSPECIFIED ? EditorInfo.IME_ACTION_NONE : act;
    }

    private void addMobileRow(LinearLayout parent, KeySpec[] keys, int heightDp) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rp = heightDp > 0
                ? new LinearLayout.LayoutParams(-1, dp(heightDp))
                : new LinearLayout.LayoutParams(-1, 0, 1f);
        rp.bottomMargin = dp(5);
        parent.addView(row, rp);

        for (KeySpec spec : keys) {
            if (spec.type == SPACER) {
                row.addView(new View(this), new LinearLayout.LayoutParams(0, -1, spec.weight));
                continue;
            }
            boolean active = isModifierActive(spec.action);
            int face = active ? accent
                    : spec.type == LIGHT ? whiteKey
                    : spec.type == RED ? redKey : darkKey;
            int fg = fgFor(spec, active);

            FrameLayout shell = new FrameLayout(this);
            shell.setBackground(shape(a(darken(face)), 8, Color.rgb(7, 13, 24)));

            String label = currentLabel(spec);
            TextView key = new TextView(this);
            key.setText(label);
            key.setTextColor(fg);
            key.setGravity(Gravity.CENTER);
            key.setTypeface(Typeface.DEFAULT_BOLD);
            key.setIncludeFontPadding(false);
            key.setSingleLine(true);
            key.setTextSize(label.length() <= 1 ? 20 : label.length() <= 3 ? 15 : 12);
            key.setBackground(shape(a(face), 7, Color.rgb(75, 99, 128)));
            FrameLayout.LayoutParams faceParams = new FrameLayout.LayoutParams(-1, -1);
            faceParams.setMargins(dp(1), dp(1), dp(1), dp(3));
            shell.addView(key, faceParams);

            boolean letter = spec.action == null && !symbols && spec.type == LIGHT
                    && spec.main.length() == 1 && Character.isLetter(spec.main.charAt(0))
                    && !spec.symbol.isEmpty();
            if (letter) {
                TextView hint = new TextView(this);
                hint.setText(spec.symbol);
                hint.setTextSize(8.5f);
                hint.setTextColor(Color.rgb(90, 100, 118));
                hint.setIncludeFontPadding(false);
                FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(-2, -2,
                        Gravity.TOP | Gravity.END);
                hp.setMargins(0, dp(3), dp(5), 0);
                shell.addView(hint, hp);
            }

            LinearLayout.LayoutParams keyParams = new LinearLayout.LayoutParams(0, -1, spec.weight);
            keyParams.setMargins(dp(2), 0, dp(2), 0);
            row.addView(shell, keyParams);
            shell.setElevation(dp(1));
            shell.setOnTouchListener(new KeyTouch(spec, letter));
        }
    }

    // ---------- suggestion bar + voice button ----------
    private void addMobileSuggestionBar(LinearLayout parent) {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(4), dp(2), dp(4), dp(2));
        bar.setBackground(shape(a(frame), 10, Color.rgb(55, 70, 90)));

        TextView emoji = iconButton("😊");
        emoji.setOnClickListener(v -> {
            haptic(v);
            emojiOpen = true;
            refresh();
        });
        bar.addView(emoji, new LinearLayout.LayoutParams(dp(40), -1));

        sugMiddle = new LinearLayout(this);
        sugMiddle.setOrientation(LinearLayout.HORIZONTAL);
        sugMiddle.setGravity(Gravity.CENTER);
        bar.addView(sugMiddle, new LinearLayout.LayoutParams(0, -1, 1f));

        micView = iconButton("🎤");
        micView.setOnClickListener(v -> {
            haptic(v);
            toggleVoice();
        });
        micView.setOnLongClickListener(v -> {
            voiceLang = "en-IN".equals(voiceLang) ? "hi-IN" : "en-IN";
            prefs().edit().putString("voice_lang", voiceLang).apply();
            Toast.makeText(this, "Voice: " + ("hi-IN".equals(voiceLang) ? "Hindi" : "English (India)"),
                    Toast.LENGTH_SHORT).show();
            return true;
        });
        bar.addView(micView, new LinearLayout.LayoutParams(dp(40), -1));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(38));
        params.bottomMargin = dp(4);
        parent.addView(bar, params);
        updateSuggestions();
    }

    private TextView iconButton(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(19);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private void refreshMicUi() {
        if (micView == null) return;
        micView.setText(listening ? "⏹" : "🎤");
        micView.setTextColor(listening ? Color.WHITE : darkText);
        if (listening) {
            micView.setBackground(shape(redKey, 10, redKey));
        } else if (mobileLayout) {
            micView.setBackground(null);
        } else {
            micView.setBackground(shape(darkKey, 9, Color.rgb(60, 82, 109)));
        }
    }

    private List<String> getSuggestions() {
        List<String> out = new ArrayList<>();
        String prefix = composingWord.toLowerCase(Locale.ROOT);
        if (prefix.isEmpty()) {
            out.add("I");
            out.add("The");
            out.add("Ok");
            return out;
        }
        boolean cap = Character.isUpperCase(composingWord.charAt(0));
        for (String w : userWords) {
            if (out.size() >= 3) break;
            if (w.startsWith(prefix) && !w.equals(prefix) && !out.contains(w)) out.add(w);
        }
        for (String w : WORDS) {
            if (out.size() >= 3) break;
            if (w.startsWith(prefix) && !w.equals(prefix) && !out.contains(w)) out.add(w);
        }
        if (cap) {
            for (int i = 0; i < out.size(); i++) {
                String w = out.get(i);
                out.set(i, Character.toUpperCase(w.charAt(0)) + w.substring(1));
            }
        }
        return out;
    }

    private void updateSuggestions() {
        if (sugMiddle == null) return;
        sugMiddle.removeAllViews();
        if (listening) {
            TextView t = new TextView(this);
            t.setText("🎙 Bolo… (" + ("hi-IN".equals(voiceLang) ? "Hindi" : "English") + ")");
            t.setTextColor(accent);
            t.setTextSize(13);
            t.setTypeface(Typeface.DEFAULT_BOLD);
            t.setGravity(Gravity.CENTER);
            sugMiddle.addView(t, new LinearLayout.LayoutParams(-1, -1));
            return;
        }
        List<String> list = getSuggestions();
        for (int i = 0; i < list.size(); i++) {
            final String word = list.get(i);
            TextView item = new TextView(this);
            item.setText(word);
            item.setTextColor(darkTextOnBg());
            item.setTextSize(14);
            item.setSingleLine(true);
            item.setTypeface(Typeface.DEFAULT_BOLD);
            item.setGravity(Gravity.CENTER);
            sugMiddle.addView(item, new LinearLayout.LayoutParams(0, -1, 1f));
            item.setOnClickListener(v -> {
                haptic(v);
                applyWordSuggestion(word);
            });
            if (i < list.size() - 1) {
                View div = new View(this);
                div.setBackgroundColor(Color.argb(70, 150, 160, 175));
                LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(dp(1), dp(20));
                sugMiddle.addView(div, dp);
            }
        }
    }

    private void applyWordSuggestion(String word) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        if (!composingWord.isEmpty()) ic.deleteSurroundingText(composingWord.length(), 0);
        ic.commitText(word + " ", 1);
        learnWord(word);
        composingWord = "";
        if (autoCaps()) refresh(); else updateSuggestions();
    }

    private void learnWord(String w) {
    if (w == null) return;
    String word = w.toLowerCase(Locale.ROOT);
    if (word.length() < 3 || !word.matches("[a-z']+")) return;
    userWords.remove(word);
    userWords.add(0, word);
    while (userWords.size() > 400) {
        userWords.remove(userWords.size() - 1);
    }
    prefs().edit().putString("user_words",
            android.text.TextUtils.join(",", userWords)).apply();
}

    // ---------- emoji / stickers ----------
    private void buildEmojiPanel(LinearLayout root) {
        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setGravity(Gravity.CENTER);
        for (int i = 0; i < EMOJI_TABS.length; i++) {
            final int idx = i;
            TextView t = iconButton(EMOJI_TABS[i]);
            t.setTextSize(18);
            t.setBackground(shape(idx == emojiCat ? accent : a(darkKey), 9,
                    idx == emojiCat ? accent : Color.rgb(60, 82, 109)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1f);
            lp.setMargins(dp(2), 0, dp(2), 0);
            tabs.addView(t, lp);
            t.setOnClickListener(v -> {
                haptic(v);
                emojiCat = idx;
                refresh();
            });
        }
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, dp(36));
        tp.bottomMargin = dp(4);
        root.addView(tabs, tp);

        boolean stickers = emojiCat == EMOJI_DATA.length - 1;
        String[] items = stickers ? EMOJI_DATA[emojiCat].split("\\|")
                : EMOJI_DATA[emojiCat].split(" ");
        int cols = stickers ? 2 : 8;

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setBackground(shape(a(frame), 12, Color.rgb(42, 67, 91)));
        list.setPadding(dp(3), dp(4), dp(3), dp(4));

        for (int i = 0; i < items.length; i += cols) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int j = 0; j < cols; j++) {
                if (i + j < items.length) {
                    final String item = items[i + j];
                    TextView cell = new TextView(this);
                    cell.setText(item);
                    cell.setGravity(Gravity.CENTER);
                    cell.setTextSize(stickers ? 13 : 24);
                    cell.setTextColor(darkTextOnBg());
                    cell.setSingleLine(stickers);
                    if (stickers) {
                        cell.setBackground(shape(a(darkKey), 8, Color.rgb(60, 82, 109)));
                        cell.setTextColor(darkText);
                    }
                    cell.setOnClickListener(v -> {
                        haptic(v);
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) {
                            ic.commitText(stickers ? item + " " : item, 1);
                            composingWord = "";
                        }
                    });
                    LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp(stickers ? 44 : 42), 1f);
                    if (stickers) cp.setMargins(dp(3), dp(3), dp(3), dp(3));
                    row.addView(cell, cp);
                } else {
                    row.addView(new View(this), new LinearLayout.LayoutParams(0, dp(40), 1f));
                }
            }
            list.addView(row, new LinearLayout.LayoutParams(-1, -2));
        }
        sv.addView(list, new FrameLayout.LayoutParams(-1, -2));
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);
        addMobileRow(bottom, new KeySpec[]{
                k("ABC", "", "", "", DARK, 1.5f, "ABC"),
                k("English", "", "", "", LIGHT, 4f, "SPACE"),
                k("⌫", "", "", "", DARK, 1.5f, "BACKSPACE")
        }, 44);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.topMargin = dp(4);
        root.addView(bottom, bp);
    }

    // =====================================
    // VOICE TYPING
    // =====================================

    private void toggleVoice() {
        if (listening) {
            if (recognizer != null) recognizer.stopListening();
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Pehle mic permission allow karo", Toast.LENGTH_LONG).show();
            requestHideSelf(0);
            Intent i = new Intent(this, ThemeActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra("ask_mic", true);
            startActivity(i);
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Is phone me voice service nahi mili (Google app check karo)",
                    Toast.LENGTH_LONG).show();
            return;
        }
        startVoice();
    }

    private void startVoice() {
        destroyRecognizer();
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { }
            @Override public void onBeginningOfSpeech() { }
            @Override public void onRmsChanged(float rmsdB) { }
            @Override public void onBufferReceived(byte[] buffer) { }
            @Override public void onEndOfSpeech() { }
            @Override public void onEvent(int eventType, Bundle params) { }

            @Override
            public void onPartialResults(Bundle partial) {
                ArrayList<String> p = partial.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                InputConnection ic = getCurrentInputConnection();
                if (ic != null && p != null && !p.isEmpty()) ic.setComposingText(p.get(0), 1);
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> r = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    if (r != null && !r.isEmpty()) ic.commitText(r.get(0) + " ", 1);
                    else ic.finishComposingText();
                }
                composingWord = "";
                endVoiceUi();
            }

            @Override
            public void onError(int error) {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) ic.finishComposingText();
                if (error == SpeechRecognizer.ERROR_NO_MATCH
                        || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    Toast.makeText(PCKeyboardService.this, "Kuch sunai nahi diya, phir try karo",
                            Toast.LENGTH_SHORT).show();
                } else if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    Toast.makeText(PCKeyboardService.this, "Mic permission nahi hai",
                            Toast.LENGTH_SHORT).show();
                }
                endVoiceUi();
            }
        });
        Intent it = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        it.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        it.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        it.putExtra(RecognizerIntent.EXTRA_LANGUAGE, voiceLang);
        listening = true;
        refreshMicUi();
        updateSuggestions();
        recognizer.startListening(it);
    }

    private void endVoiceUi() {
        listening = false;
        destroyRecognizer();
        refreshMicUi();
        updateSuggestions();
    }

    private void stopVoiceNow() {
        if (listening || recognizer != null) {
            listening = false;
            destroyRecognizer();
            refreshMicUi();
        }
    }

    private void destroyRecognizer() {
        if (recognizer != null) {
            try {
                recognizer.cancel();
                recognizer.destroy();
            } catch (RuntimeException ignored) {
            }
            recognizer = null;
        }
    }

    // =====================================
    // SHARED
    // =====================================

    private TextView addChip(LinearLayout parent, String label, int type, Runnable click) {
        TextView chip = new TextView(this);
        chip.setText(label);
        chip.setTextSize(label.length() > 2 ? 10 : 14);
        chip.setTypeface(Typeface.DEFAULT_BOLD);
        chip.setTextColor(type == LIGHT ? Color.rgb(6, 30, 39) : darkText);
        chip.setGravity(Gravity.CENTER);
        chip.setMinWidth(dp(34));
        chip.setPadding(dp(6), dp(4), dp(6), dp(4));
        chip.setBackground(shape(type == LIGHT ? accent : a(darkKey), 9,
                type == LIGHT ? accent : Color.rgb(60, 82, 109)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(30));
        params.setMargins(dp(2), 0, dp(2), 0);
        parent.addView(chip, params);
        if (click != null) {
            chip.setOnClickListener(v -> {
                haptic(v);
                click.run();
            });
        }
        return chip;
    }

    /** Touch handling: haptic on press, key fires on release, hold-to-repeat for backspace,
     *  long-press for the number/symbol hint on mobile letters. */
    private class KeyTouch implements View.OnTouchListener {
        private final KeySpec spec;
        private final boolean allowLongPress;
        private boolean consumed = false;
        private boolean repeating = false;
        private View target;

        private final Runnable repeat = new Runnable() {
            @Override public void run() {
                handleKey(spec);
                handler.postDelayed(this, 50);
            }
        };
        private final Runnable longPress = new Runnable() {
            @Override public void run() {
                consumed = true;
                haptic(target);
                commitAlternate(spec);
            }
        };

        KeyTouch(KeySpec spec, boolean allowLongPress) {
            this.spec = spec;
            this.allowLongPress = allowLongPress;
        }

        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    target = v;
                    consumed = false;
                    repeating = false;
                    v.setAlpha(0.65f);
                    v.setScaleX(0.95f);
                    v.setScaleY(0.95f);
                    haptic(v);
                    if ("BACKSPACE".equals(spec.action)) {
                        repeating = true;
                        handleKey(spec);
                        handler.postDelayed(repeat, 400);
                    } else if (allowLongPress) {
                        handler.postDelayed(longPress, 380);
                    }
                    return true;
                case MotionEvent.ACTION_UP: {
                    boolean wasRepeating = repeating;
                    boolean inside = e.getX() >= 0 && e.getX() <= v.getWidth()
                            && e.getY() >= 0 && e.getY() <= v.getHeight();
                    release(v);
                    if (!wasRepeating && !consumed && inside) handleKey(spec);
                    return true;
                }
                case MotionEvent.ACTION_CANCEL:
                    release(v);
                    return true;
                default:
                    return true;
            }
        }

        private void release(View v) {
            handler.removeCallbacks(repeat);
            handler.removeCallbacks(longPress);
            repeating = false;
            v.setAlpha(1f);
            v.setScaleX(1f);
            v.setScaleY(1f);
        }
    }

    private void commitAlternate(KeySpec spec) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null || spec.symbol.isEmpty()) return;
        ic.commitText(spec.symbol, 1);
        composingWord = "";
        if (shift) {
            shift = false;
            refresh();
        } else {
            updateSuggestions();
        }
    }

    private String currentLabel(KeySpec spec) {
        if (spec.action != null) return spec.main;
        if (symbols && !spec.symbol.isEmpty() && mobileLayout) return spec.symbol;
        boolean letterKey = spec.main.length() == 1 && Character.isLetter(spec.main.charAt(0));
        boolean useShift = letterKey ? (shift ^ caps) : shift;
        if (useShift && !spec.shifted.isEmpty()) return spec.shifted;
        return spec.main;
    }

    private boolean hasOneShotModifier() {
        return shift || ctrl || alt || altGr || win;
    }

    private boolean isModifierActive(String action) {
        if (action == null) return false;
        switch (action) {
            case "SHIFT": return shift || (mobileLayout && caps);
            case "CTRL": return ctrl;
            case "ALT": return alt;
            case "ALTGR": return altGr;
            case "WIN": return win;
            case "CAPS": return caps;
            case "NUM_LOCK": return num;
            case "SCROLL_LOCK": return scr;
            default: return false;
        }
    }

    private void clearOneShotModifiers() {
        shift = false;
        ctrl = false;
        alt = false;
        altGr = false;
        win = false;
    }

    private int lockMeta() {
        int m = 0;
        if (caps) m |= KeyEvent.META_CAPS_LOCK_ON;
        if (num) m |= KeyEvent.META_NUM_LOCK_ON;
        if (scr) m |= KeyEvent.META_SCROLL_LOCK_ON;
        return m;
    }

    /** Sends real modifier key-down events, then the key, then releases the modifiers. */
    private void sendKeyEventsWithMods(InputConnection ic, int keyCode, boolean c, boolean s,
                                       boolean al, boolean ag, boolean w) {
        if (ic == null || keyCode == KeyEvent.KEYCODE_UNKNOWN) return;
        int[] codes = new int[5];
        int[] metas = new int[5];
        int n = 0;
        int meta = lockMeta();
        if (c) {
            meta |= KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON;
            codes[n] = KeyEvent.KEYCODE_CTRL_LEFT; metas[n++] = meta;
        }
        if (s) {
            meta |= KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON;
            codes[n] = KeyEvent.KEYCODE_SHIFT_LEFT; metas[n++] = meta;
        }
        if (al) {
            meta |= KeyEvent.META_ALT_ON | KeyEvent.META_ALT_LEFT_ON;
            codes[n] = KeyEvent.KEYCODE_ALT_LEFT; metas[n++] = meta;
        }
        if (ag) {
            meta |= KeyEvent.META_ALT_ON | KeyEvent.META_ALT_RIGHT_ON;
            codes[n] = KeyEvent.KEYCODE_ALT_RIGHT; metas[n++] = meta;
        }
        if (w) {
            meta |= KeyEvent.META_META_ON | KeyEvent.META_META_LEFT_ON;
            codes[n] = KeyEvent.KEYCODE_META_LEFT; metas[n++] = meta;
        }
        long t = SystemClock.uptimeMillis();
        for (int i = 0; i < n; i++) {
            ic.sendKeyEvent(new KeyEvent(t, t, KeyEvent.ACTION_DOWN, codes[i], 0, metas[i]));
        }
        ic.sendKeyEvent(new KeyEvent(t, t, KeyEvent.ACTION_DOWN, keyCode, 0, meta));
        ic.sendKeyEvent(new KeyEvent(t, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP,
                keyCode, 0, meta));
        for (int i = n - 1; i >= 0; i--) {
            int m = i > 0 ? metas[i - 1] : lockMeta();
            ic.sendKeyEvent(new KeyEvent(t, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP,
                    codes[i], 0, m));
        }
    }

    private void sendKeyCode(InputConnection ic, int keyCode) {
        sendKeyEventsWithMods(ic, keyCode, ctrl, shift, alt, altGr, win);
    }

    private int keyCodeForText(String value) {
        if (value == null || value.length() != 1) return KeyEvent.KEYCODE_UNKNOWN;
        char ch = Character.toUpperCase(value.charAt(0));
        if ((ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9')) {
            return KeyEvent.keyCodeFromString("KEYCODE_" + ch);
        }
        switch (ch) {
            case '`': case '~': return KeyEvent.KEYCODE_GRAVE;
            case '-': case '_': return KeyEvent.KEYCODE_MINUS;
            case '=': case '+': return KeyEvent.KEYCODE_EQUALS;
            case '[': case '{': return KeyEvent.KEYCODE_LEFT_BRACKET;
            case ']': case '}': return KeyEvent.KEYCODE_RIGHT_BRACKET;
            case '\\': case '|': return KeyEvent.KEYCODE_BACKSLASH;
            case ';': case ':': return KeyEvent.KEYCODE_SEMICOLON;
            case '\'': case '"': return KeyEvent.KEYCODE_APOSTROPHE;
            case ',': case '<': return KeyEvent.KEYCODE_COMMA;
            case '.': case '>': return KeyEvent.KEYCODE_PERIOD;
            case '/': case '?': return KeyEvent.KEYCODE_SLASH;
            default: return KeyEvent.KEYCODE_UNKNOWN;
        }
    }

    private void sendPcKey(InputConnection ic, String keyName) {
        if (ic == null || keyName == null || keyName.isEmpty()) return;
        sendKeyCode(ic, KeyEvent.keyCodeFromString("KEYCODE_" + keyName));
    }

    /** Ctrl+C / Ctrl+V / Ctrl+X / Ctrl+A use the editor's own actions (works in every
     *  text field); everything else is sent as real key events with real modifiers. */
    private boolean clipboardAction(InputConnection ic, boolean c, boolean s, boolean al,
                                    boolean w, String key) {
        if (!c || s || al || w) return false;
        switch (key.toUpperCase(Locale.ROOT)) {
            case "C": return ic.performContextMenuAction(android.R.id.copy);
            case "V": return ic.performContextMenuAction(android.R.id.paste);
            case "X": return ic.performContextMenuAction(android.R.id.cut);
            case "A": return ic.performContextMenuAction(android.R.id.selectAll);
            default: return false;
        }
    }

    private void sendCombo(InputConnection ic, String combo) {
        String[] parts = combo.split("\\+");
        boolean c = false, s = false, al = false, w = false;
        for (int i = 0; i < parts.length - 1; i++) {
            switch (parts[i]) {
                case "CTRL": c = true; break;
                case "SHIFT": s = true; break;
                case "ALT": al = true; break;
                case "WIN": w = true; break;
                default: break;
            }
        }
        String key = parts[parts.length - 1];
        if (clipboardAction(ic, c, s, al, w, key)) return;
        int code = key.length() == 1 ? keyCodeForText(key)
                : KeyEvent.keyCodeFromString("KEYCODE_" + key);
        sendKeyEventsWithMods(ic, code, c, s, al, false, w);
    }

    private boolean autoCaps() {
        if (!mobileLayout || caps || shift) return false;
        EditorInfo ei = getCurrentInputEditorInfo();
        InputConnection ic = getCurrentInputConnection();
        if (ei == null || ic == null || ei.inputType == 0) return false;
        if (ic.getCursorCapsMode(ei.inputType) != 0) {
            shift = true;
            return true;
        }
        return false;
    }

    private void endWord() {
        learnWord(composingWord);
        composingWord = "";
    }

    private void finishKey(boolean rebuild) {
        if (mobileLayout && !rebuild && autoCaps()) rebuild = true;
        if (rebuild) refresh();
        else if (mobileLayout) updateSuggestions();
    }

    private void handleKey(KeySpec spec) {
        String action = spec.action;

        if ("SYSTEM_SWITCH".equals(action)) {
            showSystemKeyboardPicker();
            return;
        }
        if ("EMOJI".equals(action)) {
            emojiOpen = true;
            refresh();
            return;
        }
        if ("ABC".equals(action)) {
            emojiOpen = false;
            symbols = false;
            refresh();
            return;
        }

        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        boolean hadMods = hasOneShotModifier();

        if (action == null) {
            String value = currentLabel(spec);
            if (!value.isEmpty()) {
                if (ctrl || alt || altGr || win) {
                    int keyCode = keyCodeForText(spec.main);
                    if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                        if (!clipboardAction(ic, ctrl, shift, alt, win, spec.main)) {
                            sendKeyCode(ic, keyCode);
                        }
                    } else {
                        ic.commitText(value, 1);
                    }
                    composingWord = "";
                } else {
                    ic.commitText(value, 1);
                    if (value.matches("[a-zA-Z']+")) composingWord += value;
                    else endWord();
                }
            }
            clearOneShotModifiers();
            finishKey(hadMods);
            return;
        }

        switch (action) {
            case "BACKSPACE":
                sendKeyCode(ic, KeyEvent.KEYCODE_DEL);
                if (!composingWord.isEmpty()) {
                    composingWord = composingWord.substring(0, composingWord.length() - 1);
                }
                clearOneShotModifiers();
                finishKey(hadMods);
                break;

            case "SPACE":
                if (hasOneShotModifier()) sendKeyCode(ic, KeyEvent.KEYCODE_SPACE);
                else ic.commitText(" ", 1);
                endWord();
                clearOneShotModifiers();
                finishKey(hadMods);
                break;

            case "TAB":
                sendKeyCode(ic, KeyEvent.KEYCODE_TAB);
                clearOneShotModifiers();
                finishKey(hadMods);
                break;

            case "ENTER": {
                if (hasOneShotModifier()) {
                    sendKeyCode(ic, KeyEvent.KEYCODE_ENTER);
                } else {
                    int act = editorAction();
                    if (act != EditorInfo.IME_ACTION_NONE) ic.performEditorAction(act);
                    else sendKeyCode(ic, KeyEvent.KEYCODE_ENTER);
                }
                endWord();
                clearOneShotModifiers();
                finishKey(hadMods);
                break;
            }

            case "SHIFT": {
                long now = SystemClock.uptimeMillis();
                if (mobileLayout) {
                    if (caps) {
                        caps = false;
                        shift = false;
                    } else if (now - lastShiftTap < 400) {
                        caps = true;
                        shift = false;
                    } else {
                        shift = !shift;
                    }
                    lastShiftTap = now;
                } else {
                    shift = !shift;
                }
                refresh();
                break;
            }

            case "SYMBOLS":
                symbols = !symbols;
                symPage = 0;
                refresh();
                break;

            case "SYMPAGE":
                symPage = 1 - symPage;
                refresh();
                break;

            case "CAPS":
                caps = !caps;
                refresh();
                break;

            case "CTRL": ctrl = !ctrl; refresh(); break;
            case "ALT": alt = !alt; refresh(); break;
            case "ALTGR": altGr = !altGr; refresh(); break;
            case "WIN": win = !win; refresh(); break;

            case "PC_SEND":
                sendPcKey(ic, spec.symbol);
                clearOneShotModifiers();
                if (hadMods) refresh();
                break;

            case "COMBO":
                sendCombo(ic, spec.symbol);
                clearOneShotModifiers();
                if (hadMods) refresh();
                break;

            case "NUM_LOCK":
                num = !num;
                sendKeyEventsWithMods(ic, KeyEvent.KEYCODE_NUM_LOCK, false, false, false, false, false);
                refresh();
                break;

            case "SCROLL_LOCK":
                scr = !scr;
                sendKeyEventsWithMods(ic, KeyEvent.KEYCODE_SCROLL_LOCK, false, false, false, false, false);
                refresh();
                break;

            case "ESC":
                sendPcKey(ic, "ESCAPE");
                clearOneShotModifiers();
                if (hadMods) refresh();
                break;

            default:
                break;
        }
    }
}

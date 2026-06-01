package org.zero.qes;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class QesUiShell {
    private static final String SHELL = "QES_UI_SHELL_V4";
    private static final String OLD_RAIL = "QES_FORCE_RAIL_V2";
    private static final String PREF = "qes_settings";

    private QesUiShell() {}

    public static void install(final Activity a) {
        if (a == null) return;

        final FrameLayout content = a.findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0) return;

        Palette p = palette(a);

        for (int i = 0; i < content.getChildCount(); i++) {
            View c = content.getChildAt(i);
            Object tag = c.getTag();
            if (tag != null && SHELL.equals(String.valueOf(tag))) {
                applyTheme(c, p);
                return;
            }
        }

        for (int i = content.getChildCount() - 1; i >= 0; i--) {
            View c = content.getChildAt(i);
            Object tag = c.getTag();
            if (tag != null && OLD_RAIL.equals(String.valueOf(tag))) content.removeViewAt(i);
        }

        if (content.getChildCount() == 0) return;

        final View original = content.getChildAt(0);
        content.removeView(original);

        LinearLayout root = new LinearLayout(a);
        root.setTag(SHELL);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackgroundColor(p.black);
        root.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));

        final ScrollView railScroll = new ScrollView(a);
        railScroll.setFillViewport(false);
        railScroll.setBackground(panel(p.black, p.sea));

        final LinearLayout rail = new LinearLayout(a);
        rail.setOrientation(LinearLayout.VERTICAL);
        rail.setPadding(dp(a,4), dp(a,6), dp(a,4), dp(a,6));
        rail.setBackground(panel(p.black, p.sea));

        TextView logo = railButton(a, "QES", null, null, null, null, p);
        logo.setTextSize(15);
        rail.addView(logo);

        LinearLayout right = new LinearLayout(a);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setBackgroundColor(p.black);

        final TextView header = new TextView(a);
        header.setText("☰ LIŠTA / PŘEHLED");
        header.setTextColor(p.lime);
        header.setTextSize(15);
        header.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(a,8), dp(a,7), dp(a,8), dp(a,7));
        header.setBackground(panel(p.black, p.sea));
        right.addView(header, new LinearLayout.LayoutParams(-1, dp(a,42)));

        FrameLayout body = new FrameLayout(a);
        body.setBackgroundColor(p.black);
        body.setPadding(dp(a,6), dp(a,6), dp(a,6), dp(a,6));
        body.addView(original, new FrameLayout.LayoutParams(-1, -1));
        right.addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));

        final String[] sections = new String[] {
            "PŘEHLED","KLÍČ","ART","TEXT","SOUBOR","COVER","OVĚŘENÍ",
            "TESTY","LOG","MAC / ZERO LOCK","ARCH","ZERO","NASTAVENÍ"
        };

        for (int i = 0; i < sections.length; i++) {
            final String sec = sections[i];
            rail.addView(railButton(a, shortName(sec), sec, original, header, rail, p));
        }

        railScroll.addView(rail, new ScrollView.LayoutParams(-1, -2));

        int w = railWidth(a);
        root.addView(railScroll, new LinearLayout.LayoutParams(w, -1));
        root.addView(right, new LinearLayout.LayoutParams(0, -1, 1f));

        content.addView(root);

        hideOldMenu(original);
        applyTheme(root, p);
    }

    public static void showSettings(final Activity a) {
        final String[] names = new String[] {
            "LIME / BLACK", "CYAN / BLACK", "SEA GREEN TECH", "RED ALERT", "LIGHT GRID"
        };
        new AlertDialog.Builder(a)
            .setTitle("QES PALETA UI")
            .setItems(names, new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int which) {
                    a.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                        .edit().putInt("qes_palette_v4", which).apply();
                    Toast.makeText(a, "Paleta: " + names[which], Toast.LENGTH_SHORT).show();
                    install(a);
                }
            })
            .setPositiveButton("ZAVŘÍT", null)
            .show();
    }

    private static TextView railButton(final Activity a, String text, final String sec, final View original,
                                       final TextView header, final LinearLayout rail, final Palette p) {
        TextView v = new TextView(a);
        v.setText(text);
        v.setTag(sec == null ? "QES_RAIL_LOGO" : "QES_RAIL:" + sec);
        v.setTextColor(sec == null ? p.cyan : p.lime);
        v.setTextSize(sec == null ? 13 : 9);
        v.setGravity(Gravity.CENTER);
        v.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        v.setIncludeFontPadding(false);
        v.setPadding(dp(a,2), dp(a,7), dp(a,2), dp(a,7));
        v.setBackground(panel(p.black, p.sea));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(a,5));
        v.setLayoutParams(lp);

        if (sec != null) {
            v.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View x) {
                    activeRail(rail, sec, p);
                    header.setText("☰ LIŠTA / " + sec);
                    if ("NASTAVENÍ".equals(sec)) {
                        showSettings(a);
                        return;
                    }
                    boolean ok = findAndClick(original, sec);
                    hideOldMenu(original);
                    applyTheme(original, palette(a));
                    if (!ok) Toast.makeText(a, "Sekce: " + sec, Toast.LENGTH_SHORT).show();
                }
            });
        }
        return v;
    }

    private static void activeRail(View v, String sec, Palette p) {
        if (v == null) return;
        Object tag = v.getTag();
        if (tag != null && String.valueOf(tag).startsWith("QES_RAIL:")) {
            boolean on = String.valueOf(tag).equals("QES_RAIL:" + sec);
            v.setBackground(panel(on ? p.seaDark : p.black, on ? p.lime : p.sea));
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup)v;
            for (int i = 0; i < g.getChildCount(); i++) activeRail(g.getChildAt(i), sec, p);
        }
    }

    private static boolean findAndClick(View v, String sec) {
        if (v == null) return false;
        if (v instanceof TextView) {
            String t = norm(String.valueOf(((TextView)v).getText()));
            String n = norm(sec);
            if (v.isEnabled() && v.isClickable() && (t.equals(n) || t.contains(n) || n.contains(t))) {
                v.performClick();
                return true;
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup)v;
            for (int i = 0; i < g.getChildCount(); i++) if (findAndClick(g.getChildAt(i), sec)) return true;
        }
        return false;
    }

    private static void hideOldMenu(View v) {
        if (v == null) return;
        if (v instanceof TextView && v.isClickable()) {
            String n = norm(String.valueOf(((TextView)v).getText()));
            if (isSection(n)) v.setVisibility(View.GONE);
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup)v;
            for (int i = 0; i < g.getChildCount(); i++) hideOldMenu(g.getChildAt(i));
        }
    }

    private static void applyTheme(View v, Palette p) {
        if (v == null) return;

        if (v instanceof Button) {
            Button b = (Button)v;
            b.setAllCaps(false);
            b.setTextColor(p.lime);
            b.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            b.setBackground(panel(p.black, p.sea));
        } else if (v instanceof EditText) {
            EditText e = (EditText)v;
            e.setTextColor(p.lime);
            e.setHintTextColor(p.cyan);
            e.setBackground(panel(p.black, p.sea));
        } else if (v instanceof TextView) {
            TextView t = (TextView)v;
            String n = norm(String.valueOf(t.getText()));
            if (isState(n)) {
                t.setTextColor(p.red);
                t.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
                t.setBackground(panel(p.black, p.red));
                t.setPadding(dp(t.getContext(),5), dp(t.getContext(),3), dp(t.getContext(),5), dp(t.getContext(),3));
            } else if (isSection(n)) {
                t.setTextColor(p.lime);
                t.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            }
        }

        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup)v;
            for (int i = 0; i < g.getChildCount(); i++) applyTheme(g.getChildAt(i), p);
        }
    }

    private static GradientDrawable panel(int fill, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setStroke(1, stroke);
        d.setCornerRadius(0);
        return d;
    }

    private static int dp(Context c, int v) {
        return Math.max(1, (int)(v * c.getResources().getDisplayMetrics().density + 0.5f));
    }

    private static int railWidth(Activity a) {
        float d = a.getResources().getDisplayMetrics().density;
        int wdp = (int)(a.getResources().getDisplayMetrics().widthPixels / d);
        return dp(a, wdp < 380 ? 86 : 108);
    }

    private static String shortName(String s) {
        if ("MAC / ZERO LOCK".equals(s)) return "MAC/ZERO";
        if ("NASTAVENÍ".equals(s)) return "NAST.";
        return s;
    }

    private static boolean isSection(String n) {
        return n.equals("PREHLED") || n.equals("KLIC") || n.equals("ART") || n.equals("TEXT")
            || n.equals("SOUBOR") || n.equals("COVER") || n.equals("OVERENI") || n.equals("TESTY")
            || n.equals("LOG") || n.equals("ARCH") || n.equals("ZERO") || n.equals("NASTAVENI")
            || n.contains("MAC ZERO");
    }

    private static boolean isState(String n) {
        return n.equals("ZAPNUTO") || n.equals("VYPNUTO") || n.equals("ON") || n.equals("OFF")
            || n.equals("OK") || n.equals("FAIL") || n.equals("FAILED") || n.equals("AKTIVNI")
            || n.equals("NEAKTIVNI") || n.equals("ALPHA") || n.equals("BETA") || n.equals("LOCKED")
            || n.equals("UNLOCKED") || n.equals("HARDENED") || n.equals("EXPERIMENT")
            || n.equals("ANO") || n.equals("NE");
    }

    private static String norm(String x) {
        if (x == null) return "";
        return x.toUpperCase(java.util.Locale.ROOT)
            .replace("Ř","R").replace("Ě","E").replace("Š","S").replace("Č","C")
            .replace("Ž","Z").replace("Ý","Y").replace("Á","A").replace("Í","I")
            .replace("É","E").replace("Ů","U").replace("Ú","U")
            .replace("/", " ").replace("-", " ").replace("_", " ")
            .replaceAll("\\s+", " ").trim();
    }

    private static Palette palette(Activity a) {
        int id = a.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt("qes_palette_v4", 0);
        if (id == 1) return new Palette(Color.rgb(0,0,0), Color.rgb(0,255,255), Color.rgb(46,139,118), Color.rgb(0,255,255), Color.rgb(255,40,40), Color.rgb(0,48,48));
        if (id == 2) return new Palette(Color.rgb(0,10,8), Color.rgb(120,255,210), Color.rgb(46,139,118), Color.rgb(0,255,255), Color.rgb(255,45,45), Color.rgb(0,42,34));
        if (id == 3) return new Palette(Color.rgb(10,0,0), Color.rgb(210,255,80), Color.rgb(46,139,118), Color.rgb(0,255,255), Color.rgb(255,0,0), Color.rgb(55,0,0));
        if (id == 4) return new Palette(Color.rgb(230,255,245), Color.rgb(0,90,25), Color.rgb(46,139,118), Color.rgb(0,120,140), Color.rgb(210,0,0), Color.rgb(210,245,235));
        return new Palette(Color.rgb(0,0,0), Color.rgb(150,255,0), Color.rgb(46,139,118), Color.rgb(0,255,255), Color.rgb(255,35,35), Color.rgb(10,35,20));
    }

    private static final class Palette {
        final int black, lime, sea, cyan, red, seaDark;
        Palette(int black, int lime, int sea, int cyan, int red, int seaDark) {
            this.black = black; this.lime = lime; this.sea = sea; this.cyan = cyan; this.red = red; this.seaDark = seaDark;
        }
    }
}

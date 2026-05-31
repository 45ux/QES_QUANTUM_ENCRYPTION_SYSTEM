package org.zero.qes;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

public class MainActivity extends Activity {
    private LinearLayout content;
    private EditText password, seed1, seed2, seed3, seed4, input, output;
    private TextView status;

    private final int BG = Color.rgb(5, 6, 10);
    private final int CARD = Color.rgb(18, 21, 30);
    private final int TEXT = Color.rgb(235, 240, 248);
    private final int MUTED = Color.rgb(145, 155, 170);
    private final int ACCENT = Color.rgb(130, 235, 255);

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setTitle("QES");
        setContentView(app());
        showOverview();
    }

    private ScrollView app() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(22, 22, 22, 22);
        root.setBackgroundColor(BG);

        TextView title = text("QES", 34, TEXT, true);
        title.setLetterSpacing(0.12f);
        root.addView(title);

        root.addView(text("Quantum Encryption System · APK prototype", 14, MUTED, false));
        root.addView(space(14));

        HorizontalScrollView navScroll = new HorizontalScrollView(this);
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);

        nav.addView(navButton("Přehled", v -> showOverview()));
        nav.addView(navButton("Klíč", v -> showKey()));
        nav.addView(navButton("Text", v -> showText()));
        nav.addView(navButton("Soubor", v -> showFiles()));
        nav.addView(navButton("Cover", v -> showCover()));
        nav.addView(navButton("Ověření", v -> showVerify()));
        nav.addView(navButton("Diagnostika", v -> showDiagnostics()));
        nav.addView(navButton("Architektura", v -> showArchitecture()));

        navScroll.addView(nav);
        root.addView(navScroll);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content);

        status = text("", 13, ACCENT, false);
        status.setPadding(0, 18, 0, 0);
        root.addView(status);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        return scroll;
    }

    private void clear() {
        content.removeAllViews();
        status.setText("");
    }

    private void showOverview() {
        clear();
        card("Stav aplikace", 
            "Toto je nativní APK verze QES. Aplikace neběží přes Termux server a nepotřebuje ruční odkaz. UI je rozdělené na samostatné strany: klíč, text, soubor, cover, ověření, diagnostika a architektura.");
        card("Důležité upozornění",
            "QES je vývojový prototyp. Testy ověřují roundtrip, vrstvy, MAC/TAG, citlivost na klíč a detekci změn. Nejde o formální kryptografický audit.");
        card("Další cíl",
            "Do dalších APK patchů se doplní plný Android výběr souborů přes systémový správce souborů, ukládání .qes, .qes128 kapsle a finální Adaptive Labyrinth Cover.");
    }

    private void showKey() {
        clear();
        content.addView(section("Navigace a klíč"));
        password = field("Password", true, "");
        seed1 = field("Seed 1", false, "seed-111");
        seed2 = field("Seed 2", false, "seed-222");
        seed3 = field("Seed 3", false, "seed-333");
        seed4 = field("Seed 4", false, "seed-444");
        content.addView(password);
        content.addView(seed1);
        content.addView(seed2);
        content.addView(seed3);
        content.addView(seed4);
        card("Jak to chápat",
            "Password + seedy + particle tvoří navigaci QES. Z nich vzniká master seed, hidden IV, route seed, masky, tagové klíče a MAC klíč.");
    }

    private void showText() {
        clear();
        ensureKeyFields();
        content.addView(section("Textový režim"));
        input = area("Vstupní text nebo QES ASCII balík");
        output = area("Výstup");
        content.addView(input);

        LinearLayout row = row();
        row.addView(action("Šifrovat", v -> encrypt()));
        row.addView(action("Dešifrovat", v -> decrypt()));
        content.addView(row);
        content.addView(output);
    }

    private void showFiles() {
        clear();
        card("Souborový režim",
            "Tato strana je připravená pro plné šifrování souborů v APK. Cíl: vybrat soubor, zašifrovat do .qes, uložit .qes128 kapsli a kontrolní .txt.");
        card("Výstupy souborového režimu",
            "1) soubor.qes\n2) soubor.qes128\n3) soubor_qes_check.txt\n\nV další verzi se přidá systémový výběr a ukládání souborů přes Android Storage Access Framework.");
    }

    private void showCover() {
        clear();
        card("Cover režim",
            "Cover režim má vzít tajný soubor a cover soubor. Výstupem bude finální cover soubor + QES kapsle. Aktuální bezpečný carrier režim je mezikrok před plným Adaptive Labyrinth Cover.");
        card("Adaptive Labyrinth Cover",
            "Finální cíl: cover soubor se analyzuje jako prostor bodů. Klíč najde startovní bod, seed vytvoří labyrint a šifrovaný payload se rozloží do bodů na trase.");
    }

    private void showVerify() {
        clear();
        card("Ověření",
            "Po šifrování má QES vytvářet public hash a keyed MAC. Public hash ověřuje změnu souboru. Keyed MAC ověřuje, že soubor, kapsle, režim a klíč patří k sobě.");
        card("QES-128 kapsle",
            "Kapsle je samostatný 128B artefakt pro uživatele. Neobsahuje heslo ani plaintext. Obsahuje kotvy, režim, maskovanou délku, final phase, file commitment a MAC.");
    }

    private void showDiagnostics() {
        clear();
        try {
            card("Rust bridge", QesNative.selfTest());
        } catch (Throwable t) {
            card("Rust bridge", "Rust knihovna není načtená: " + t.getMessage());
        }
        card("Diagnostika",
            "Diagnostika má ukazovat rychlost, velikost vstupu, velikost výstupu, hash, MAC, počet bodů, kapacitu cover režimu a testy vrstev.");
    }

    private void showArchitecture() {
        clear();
        content.addView(section("Architektura QES"));
        card("Základní tok",
            "tajná data\n↓\npassword + seedy + particle\n↓\nArgon2id / KDF\n↓\nmaster seed\n↓\nskrytý stav QES\n↓\nQES Core\n↓\nencrypted core\n↓\nNormal Mode nebo Cover Mode\n↓\nfinální soubor + kapsle + hash + MAC");
        card("QES Core není jen XOR ani ARX",
            "QES Core je sada vratných vrstev: permutace, difuze, superpozice, rotace, XOR masky, ARX prvky a tagové vrstvy. XOR a ARX jsou pouze části celého systému.");
        card("Pojmy",
            "Password: hlavní tajná fráze.\nSeed: doplňkový tajný vstup.\nParticle: navigační prvek.\nHidden IV: skrytý startovní stav.\nRoute seed: navigace pro skoky a permutace.\nDifuze: rozlití změny přes data.\nMAC: kontrola integrity pomocí klíče.");
    }

    private void encrypt() {
        try {
            long t = System.currentTimeMillis();
            String r = QesNative.encryptText(
                input.getText().toString(),
                password.getText().toString(),
                seed1.getText().toString(),
                seed2.getText().toString(),
                seed3.getText().toString(),
                seed4.getText().toString(),
                "X", 77, "vektor", 13L, 9
            );
            output.setText(r);
            status.setText("Hotovo. Šifrování " + (System.currentTimeMillis() - t) + " ms.");
        } catch (Throwable e) {
            status.setText("Chyba: " + e.getMessage());
        }
    }

    private void decrypt() {
        try {
            long t = System.currentTimeMillis();
            String r = QesNative.decryptText(
                input.getText().toString(),
                password.getText().toString(),
                seed1.getText().toString(),
                seed2.getText().toString(),
                seed3.getText().toString(),
                seed4.getText().toString(),
                "X", 77, "vektor", 13L, 9
            );
            output.setText(r);
            status.setText("Hotovo. Dešifrování " + (System.currentTimeMillis() - t) + " ms.");
        } catch (Throwable e) {
            status.setText("Chyba: " + e.getMessage());
        }
    }

    private void ensureKeyFields() {
        if (password == null) {
            password = field("Password", true, "");
            seed1 = field("Seed 1", false, "seed-111");
            seed2 = field("Seed 2", false, "seed-222");
            seed3 = field("Seed 3", false, "seed-333");
            seed4 = field("Seed 4", false, "seed-444");
            content.addView(password);
            content.addView(seed1);
            content.addView(seed2);
            content.addView(seed3);
            content.addView(seed4);
        }
    }

    private Button navButton(String s, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextColor(TEXT);
        b.setBackgroundColor(CARD);
        b.setOnClickListener(l);
        return b;
    }

    private Button action(String s, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextColor(Color.rgb(5, 6, 10));
        b.setBackgroundColor(ACCENT);
        b.setOnClickListener(l);
        b.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        return b;
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        return r;
    }

    private EditText field(String hint, boolean secret, String value) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value);
        e.setSingleLine(true);
        e.setTextColor(TEXT);
        e.setHintTextColor(MUTED);
        e.setBackgroundColor(CARD);
        e.setPadding(16, 10, 16, 10);
        if (secret) e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        return e;
    }

    private EditText area(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setMinLines(6);
        e.setGravity(Gravity.TOP);
        e.setTextColor(TEXT);
        e.setHintTextColor(MUTED);
        e.setBackgroundColor(CARD);
        e.setTypeface(Typeface.MONOSPACE);
        e.setPadding(16, 16, 16, 16);
        return e;
    }

    private TextView section(String s) {
        TextView v = text(s.toUpperCase(), 13, ACCENT, true);
        v.setPadding(0, 24, 0, 8);
        return v;
    }

    private void card(String h, String b) {
        TextView v = text(h + "\n\n" + b, 15, TEXT, false);
        v.setBackgroundColor(CARD);
        v.setPadding(18, 18, 18, 18);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 10, 0, 10);
        v.setLayoutParams(p);
        content.addView(v);
    }

    private TextView text(String s, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setLineSpacing(4, 1);
        if (bold) v.setTypeface(Typeface.DEFAULT_BOLD);
        return v;
    }

    private Space space(int h) {
        Space s = new Space(this);
        s.setLayoutParams(new LinearLayout.LayoutParams(1, h));
        return s;
    }
}

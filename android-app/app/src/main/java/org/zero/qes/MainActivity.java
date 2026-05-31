package org.zero.qes;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQ_SECRET_FILE = 100;
    private static final int REQ_QES_FILE = 101;
    private static final int REQ_COVER_FILE = 102;
    private static final int REQ_COVER_FINAL = 103;
    private static final int REQ_SAVE_FILE = 200;

    private static final byte[] COVER_BEGIN = "\n-----BEGIN QES COVER PAYLOAD V1-----\n".getBytes();
    private static final byte[] COVER_END = "\n-----END QES COVER PAYLOAD V1-----\n".getBytes();

    private LinearLayout content;
    private TextView status;
    private EditText passwordField, seed1Field, seed2Field, seed3Field, seed4Field;
    private EditText glyphField, valueField, vectorField, phaseField, amplitudeField;
    private EditText textInput, textOutput, logBox;

    private Uri secretUri;
    private Uri qesUri;
    private Uri coverUri;
    private Uri coverFinalUri;

    private byte[] pendingSaveBytes;
    private String pendingSaveName = "qes_output.bin";
    private String pendingSaveMime = "application/octet-stream";

    private boolean dark = true;
    private String currentPage = "overview";

    private String pass = "";
    private String s1 = "seed-111";
    private String s2 = "seed-222";
    private String s3 = "seed-333";
    private String s4 = "seed-444";
    private String glyph = "X";
    private int particleValue = 77;
    private String vector = "vektor";
    private long phase = 13L;
    private int amplitude = 9;

    private StringBuilder log = new StringBuilder();

    private int BG;
    private int PANEL;
    private int CARD;
    private int TEXT;
    private int MUTED;
    private int ACCENT;
    private int GOOD;
    private int BAD;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setTitle("QES");
        applyColors();
        addLog("QES APK spuštěna.");
        setContentView(app());
        showOverview();
    }

    private void applyColors() {
        if (dark) {
            BG = Color.rgb(4, 5, 10);
            PANEL = Color.rgb(10, 12, 20);
            CARD = Color.rgb(18, 21, 32);
            TEXT = Color.rgb(238, 242, 250);
            MUTED = Color.rgb(150, 158, 174);
            ACCENT = Color.rgb(120, 235, 255);
            GOOD = Color.rgb(120, 255, 185);
            BAD = Color.rgb(255, 110, 110);
        } else {
            BG = Color.rgb(246, 248, 252);
            PANEL = Color.rgb(232, 236, 244);
            CARD = Color.WHITE;
            TEXT = Color.rgb(20, 24, 34);
            MUTED = Color.rgb(88, 96, 112);
            ACCENT = Color.rgb(0, 88, 140);
            GOOD = Color.rgb(0, 125, 75);
            BAD = Color.rgb(190, 40, 40);
        }
    }

    private ScrollView app() {
        applyColors();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(18, 18, 18, 28);
        root.setBackgroundColor(BG);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = text("QES", 34, TEXT, true);
        title.setLetterSpacing(0.14f);
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        Button theme = smallButton(dark ? "SVĚTLÝ" : "TMAVÝ");
        theme.setOnClickListener(v -> {
            saveKeyState();
            dark = !dark;
            addLog("Přepnut motiv: " + (dark ? "tmavý" : "světlý"));
            setContentView(app());
            rebuildCurrentPage();
        });
        head.addView(theme);
        root.addView(head);

        root.addView(text("Quantum Encryption System · native APK · ZERO", 13, MUTED, false));
        root.addView(space(12));

        root.addView(nav());

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content);

        status = text("", 13, ACCENT, false);
        status.setPadding(0, 14, 0, 0);
        root.addView(status);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        return scroll;
    }

    private LinearLayout nav() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(PANEL);
        box.setPadding(6, 6, 6, 6);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.addView(navButton("PŘEHLED", "overview"));
        row1.addView(navButton("KLÍČ", "key"));
        row1.addView(navButton("TEXT", "text"));
        row1.addView(navButton("SOUBOR", "file"));

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.addView(navButton("COVER", "cover"));
        row2.addView(navButton("OVĚŘENÍ", "verify"));
        row2.addView(navButton("DIAGNOSTIKA", "diag"));
        row2.addView(navButton("ARCHITEKTURA", "arch"));

        box.addView(row1);
        box.addView(row2);
        return box;
    }

    private Button navButton(String label, String page) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(label);
        b.setTextSize(11);
        b.setTextColor(page.equals(currentPage) ? ACCENT : TEXT);
        b.setBackgroundColor(page.equals(currentPage) ? CARD : PANEL);
        b.setOnClickListener(v -> {
            saveKeyState();
            currentPage = page;
            setContentView(app());
            rebuildCurrentPage();
        });
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        p.setMargins(3, 3, 3, 3);
        b.setLayoutParams(p);
        return b;
    }

    private void rebuildCurrentPage() {
        if ("key".equals(currentPage)) showKey();
        else if ("text".equals(currentPage)) showText();
        else if ("file".equals(currentPage)) showFile();
        else if ("cover".equals(currentPage)) showCover();
        else if ("verify".equals(currentPage)) showVerify();
        else if ("diag".equals(currentPage)) showDiagnostics();
        else if ("arch".equals(currentPage)) showArchitecture();
        else showOverview();
    }

    private void clear() {
        content.removeAllViews();
        status.setText("");
        textInput = null;
        textOutput = null;
        logBox = null;
    }

    private void showOverview() {
        clear();
        currentPage = "overview";
        hero("FINÁLNÍ APK PROTOTYP",
                "Nativní Android aplikace QES. Bez Termux serveru, bez ručního odkazu. Rozhraní je rozdělené na samostatné stránky pro klíč, text, soubory, cover, ověření, diagnostiku a architekturu.");
        card("Co už funguje",
                "• Rust core je načtený přes JNI\n• textové šifrování a dešifrování\n• souborové šifrování přes Android výběr souboru\n• ukládání výstupů přes systémové ukládání\n• cover carrier režim\n• logy a diagnostika\n• tmavý a světlý motiv");
        card("Bezpečnostní poznámka",
                "QES je vývojový prototyp. Testy ověřují chování implementace, roundtrip, citlivost na klíč a detekci změn. Nejde o formální kryptografický audit.");
        card("Doporučený postup",
                "1) Nastav klíč a seedy.\n2) Otestuj text.\n3) Otestuj soubor.\n4) Otestuj cover.\n5) V diagnostice spusť testy a ulož log.");
    }

    private void showKey() {
        clear();
        currentPage = "key";
        section("NAVIGACE A KLÍČ");
        addKeyPanel();
        card("Jak QES používá klíč",
                "Password + Seed 1–4 + Particle tvoří navigaci. Z ní vzniká master seed, hidden IV, route seed, masky, tagové klíče a MAC klíč. Bez stejné navigace se data nesloží zpět.");
        card("Particle",
                "Particle je doplňkový navigační prvek: glyph, value, vector, phase a amplitude. Nejde o dekoraci, ale o další vstup do trasy a kontrolních vrstev.");
    }

    private void showText() {
        clear();
        currentPage = "text";
        section("TEXTOVÝ REŽIM");
        addCompactKeyPanel();

        textInput = area("Vstupní text nebo QES ASCII balík");
        textOutput = area("Výstup");
        content.addView(textInput);

        LinearLayout row = row();
        row.addView(action("ŠIFROVAT TEXT", v -> encryptTextAction()));
        row.addView(action("DEŠIFROVAT TEXT", v -> decryptTextAction()));
        content.addView(row);

        LinearLayout row2 = row();
        row2.addView(action("KOPÍROVAT VÝSTUP", v -> copyOutput()));
        row2.addView(action("ULOŽIT VÝSTUP", v -> saveTextOutput()));
        content.addView(row2);

        content.addView(textOutput);
    }

    private void showFile() {
        clear();
        currentPage = "file";
        section("SOUBOROVÝ REŽIM");
        addCompactKeyPanel();

        card("Vstupy a výstupy",
                "Vybereš libovolný soubor, QES ho převede do šifrovaného .qes balíku. Dešifrování vezme .qes balík a uloží původní data zpět.");

        LinearLayout r1 = row();
        r1.addView(action("VYBRAT SOUBOR", v -> openFile(REQ_SECRET_FILE)));
        r1.addView(action("VYBRAT .QES", v -> openFile(REQ_QES_FILE)));
        content.addView(r1);

        LinearLayout r2 = row();
        r2.addView(action("ŠIFROVAT SOUBOR", v -> encryptSelectedFile()));
        r2.addView(action("DEŠIFROVAT .QES", v -> decryptSelectedFile()));
        content.addView(r2);

        card("Vybraný soubor", secretUri == null ? "Zatím nevybrán." : secretUri.toString());
        card("Vybraný .qes", qesUri == null ? "Zatím nevybrán." : qesUri.toString());
        card("Kontrola",
                "Po uložení výstupu se do logu zapíše velikost a SHA-256. Interní TAG/MAC kontrola je součástí QES balíku a ověřuje se při dešifrování.");
    }

    private void showCover() {
        clear();
        currentPage = "cover";
        section("COVER REŽIM");
        addCompactKeyPanel();

        card("Cover Carrier V1",
                "Tato APK verze používá funkční cover carrier: vezme cover soubor, připojí k němu QES payload a výsledek uloží jako finální cover soubor. Dešifrování najde QES payload a obnoví tajná data.");

        LinearLayout r1 = row();
        r1.addView(action("TAJNÝ SOUBOR", v -> openFile(REQ_SECRET_FILE)));
        r1.addView(action("COVER SOUBOR", v -> openFile(REQ_COVER_FILE)));
        content.addView(r1);

        LinearLayout r2 = row();
        r2.addView(action("VYTVOŘIT COVER", v -> createCoverCarrier()));
        r2.addView(action("VYBRAT FINÁLNÍ COVER", v -> openFile(REQ_COVER_FINAL)));
        content.addView(r2);

        LinearLayout r3 = row();
        r3.addView(action("DEŠIFROVAT COVER", v -> decryptCoverCarrier()));
        r3.addView(action("ULOŽIT LOG", v -> saveLog()));
        content.addView(r3);

        card("Stav",
                "Tajný soubor: " + (secretUri == null ? "nevybrán" : "vybrán") +
                "\nCover soubor: " + (coverUri == null ? "nevybrán" : "vybrán") +
                "\nFinální cover: " + (coverFinalUri == null ? "nevybrán" : "vybrán"));
        card("Další vývoj",
                "Finální Adaptive Labyrinth Cover bude místo připojení payloadu počítat body, trajektorii a kapacitu cover souboru. Tato APK už má připravený samostatný režim a ovládání.");
    }

    private void showVerify() {
        clear();
        currentPage = "verify";
        section("OVĚŘENÍ");
        card("Public hash",
                "Public hash je veřejný otisk výstupního souboru. Když se soubor změní, hash se změní také.");
        card("Keyed MAC / TAG",
                "Keyed MAC a TAG kontrola ověřují, že soubor, režim a klíčová navigace patří k sobě. Při špatném hesle, seedu nebo particle se dešifrování zastaví.");
        card("QES-128 kapsle",
                "Kapsle je samostatný 128B artefakt. Neobsahuje heslo ani plaintext. Obsahuje kotvy, režim, maskovanou délku, final phase, file commitment a MAC. V této APK je stránka připravená; samostatné ukládání kapsle se doplní jako další nativní výstup.");
    }

    private void showDiagnostics() {
        clear();
        currentPage = "diag";
        section("DIAGNOSTIKA");
        LinearLayout r = row();
        r.addView(action("SPUSTIT TESTY", v -> runDiagnostics()));
        r.addView(action("ULOŽIT LOG", v -> saveLog()));
        content.addView(r);

        LinearLayout r2 = row();
        r2.addView(action("VYČISTIT LOG", v -> clearLog()));
        r2.addView(action("RUST SELF TEST", v -> rustSelfTest()));
        content.addView(r2);

        logBox = area("Diagnostický log");
        logBox.setText(log.toString());
        content.addView(logBox);

        card("Co diagnostika kontroluje",
                "• načtení Rust knihovny\n• textový roundtrip\n• binární roundtrip\n• špatný klíč\n• cover carrier roundtrip\n• rychlost v ms\n• SHA-256 výstupů\n• velikost vstupů a výstupů");
    }

    private void showArchitecture() {
        clear();
        currentPage = "arch";
        section("ARCHITEKTURA ŠIFRY");
        card("Celkový tok",
                "tajná data\n↓\npassword + seedy + particle\n↓\nArgon2id / KDF\n↓\nmaster seed\n↓\nskrytý stav QES\n↓\nQES Core\n↓\nencrypted core\n↓\nnormal / file / cover režim\n↓\nfinální výstup + ověření");
        card("QES Core",
                "QES Core není jen XOR ani jen ARX. Je to sada vratných vrstev: permutace, difuze, superpozice, rotace, XOR masky, ARX prvky a tagové vrstvy.");
        card("Pojmy",
                "Password: hlavní tajná fráze.\nSeed: doplňkový tajný vstup.\nParticle: navigační prvek.\nHidden IV: skrytý startovní stav.\nRoute seed: navigace pro skoky a permutace.\nDifuze: rozlití změny přes data.\nMAC/TAG: kontrola integrity a správného klíče.");
        card("Symetrie",
                "Stejná navigace, která data zašifruje, je potřebná i k jejich obnově. QES je symetrický systém: kdo nemá stejné heslo, seedy a particle, nedostane původní data.");
    }

    private void addKeyPanel() {
        passwordField = field("Password", true, pass);
        seed1Field = field("Seed 1", false, s1);
        seed2Field = field("Seed 2", false, s2);
        seed3Field = field("Seed 3", false, s3);
        seed4Field = field("Seed 4", false, s4);
        glyphField = field("Particle glyph", false, glyph);
        valueField = field("Particle value 0–255", false, String.valueOf(particleValue));
        vectorField = field("Particle vector", false, vector);
        phaseField = field("Phase", false, String.valueOf(phase));
        amplitudeField = field("Amplitude 0–255", false, String.valueOf(amplitude));

        content.addView(passwordField);
        content.addView(seed1Field);
        content.addView(seed2Field);
        content.addView(seed3Field);
        content.addView(seed4Field);
        content.addView(glyphField);
        content.addView(valueField);
        content.addView(vectorField);
        content.addView(phaseField);
        content.addView(amplitudeField);

        content.addView(action("ULOŽIT NAVIGACI", v -> {
            saveKeyState();
            status.setText("Navigace uložena.");
        }));
    }

    private void addCompactKeyPanel() {
        addKeyPanel();
    }

    private void saveKeyState() {
        if (passwordField == null) return;
        pass = passwordField.getText().toString();
        s1 = seed1Field.getText().toString();
        s2 = seed2Field.getText().toString();
        s3 = seed3Field.getText().toString();
        s4 = seed4Field.getText().toString();
        glyph = glyphField.getText().toString().isEmpty() ? "X" : glyphField.getText().toString();
        vector = vectorField.getText().toString();
        particleValue = parseInt(valueField.getText().toString(), 77, 0, 255);
        phase = parseLong(phaseField.getText().toString(), 13L);
        amplitude = parseInt(amplitudeField.getText().toString(), 9, 0, 255);
    }

    private void encryptTextAction() {
        saveKeyState();
        try {
            long started = System.currentTimeMillis();
            String result = QesNative.encryptText(
                    textInput.getText().toString(), pass, s1, s2, s3, s4,
                    glyph, particleValue, vector, phase, amplitude
            );
            textOutput.setText(result);
            long ms = System.currentTimeMillis() - started;
            addLog("Text encrypted: " + ms + " ms, output chars=" + result.length());
            status.setText("Text zašifrován za " + ms + " ms.");
        } catch (Throwable e) {
            addLog("Text encrypt error: " + e.getMessage());
            status.setText("Chyba šifrování: " + e.getMessage());
        }
    }

    private void decryptTextAction() {
        saveKeyState();
        try {
            long started = System.currentTimeMillis();
            String result = QesNative.decryptText(
                    textInput.getText().toString(), pass, s1, s2, s3, s4,
                    glyph, particleValue, vector, phase, amplitude
            );
            textOutput.setText(result);
            long ms = System.currentTimeMillis() - started;
            addLog("Text decrypted: " + ms + " ms");
            status.setText("Text dešifrován za " + ms + " ms.");
        } catch (Throwable e) {
            addLog("Text decrypt error: " + e.getMessage());
            status.setText("Chyba dešifrování: " + e.getMessage());
        }
    }

    private void encryptSelectedFile() {
        saveKeyState();
        if (secretUri == null) {
            status.setText("Nejdřív vyber soubor.");
            return;
        }
        try {
            long started = System.currentTimeMillis();
            byte[] data = readAll(secretUri);
            byte[] out = QesNative.encryptBytes(data, pass, s1, s2, s3, s4, glyph, particleValue, vector, phase, amplitude);
            long ms = System.currentTimeMillis() - started;
            addLog("File encrypted: input=" + data.length + " B, output=" + out.length + " B, ms=" + ms + ", sha256=" + sha256(out));
            saveBytes(out, "encrypted.qes", "application/octet-stream");
        } catch (Throwable e) {
            addLog("File encrypt error: " + e.getMessage());
            status.setText("Chyba souborového šifrování: " + e.getMessage());
        }
    }

    private void decryptSelectedFile() {
        saveKeyState();
        if (qesUri == null) {
            status.setText("Nejdřív vyber .qes soubor.");
            return;
        }
        try {
            long started = System.currentTimeMillis();
            byte[] data = readAll(qesUri);
            byte[] out = QesNative.decryptBytes(data, pass, s1, s2, s3, s4, glyph, particleValue, vector, phase, amplitude);
            long ms = System.currentTimeMillis() - started;
            addLog("File decrypted: input=" + data.length + " B, output=" + out.length + " B, ms=" + ms + ", sha256=" + sha256(out));
            saveBytes(out, "decrypted_output.bin", "application/octet-stream");
        } catch (Throwable e) {
            addLog("File decrypt error: " + e.getMessage());
            status.setText("Chyba souborového dešifrování: " + e.getMessage());
        }
    }

    private void createCoverCarrier() {
        saveKeyState();
        if (secretUri == null || coverUri == null) {
            status.setText("Vyber tajný soubor i cover soubor.");
            return;
        }
        try {
            long started = System.currentTimeMillis();
            byte[] secret = readAll(secretUri);
            byte[] cover = readAll(coverUri);
            byte[] qes = QesNative.encryptBytes(secret, pass, s1, s2, s3, s4, glyph, particleValue, vector, phase, amplitude);
            byte[] finalCover = concat(cover, COVER_BEGIN, qes, COVER_END);
            long ms = System.currentTimeMillis() - started;
            addLog("Cover created: cover=" + cover.length + " B, secret=" + secret.length + " B, final=" + finalCover.length + " B, ms=" + ms + ", sha256=" + sha256(finalCover));
            saveBytes(finalCover, "qes_cover_output.bin", "application/octet-stream");
        } catch (Throwable e) {
            addLog("Cover create error: " + e.getMessage());
            status.setText("Chyba cover režimu: " + e.getMessage());
        }
    }

    private void decryptCoverCarrier() {
        saveKeyState();
        if (coverFinalUri == null) {
            status.setText("Nejdřív vyber finální cover soubor.");
            return;
        }
        try {
            long started = System.currentTimeMillis();
            byte[] finalCover = readAll(coverFinalUri);
            int a = indexOf(finalCover, COVER_BEGIN, 0);
            int b = indexOf(finalCover, COVER_END, a < 0 ? 0 : a + COVER_BEGIN.length);
            if (a < 0 || b < 0 || b <= a) throw new IllegalStateException("QES payload v cover souboru nebyl nalezen.");
            byte[] qes = Arrays.copyOfRange(finalCover, a + COVER_BEGIN.length, b);
            byte[] out = QesNative.decryptBytes(qes, pass, s1, s2, s3, s4, glyph, particleValue, vector, phase, amplitude);
            long ms = System.currentTimeMillis() - started;
            addLog("Cover decrypted: final=" + finalCover.length + " B, payload=" + qes.length + " B, output=" + out.length + " B, ms=" + ms + ", sha256=" + sha256(out));
            saveBytes(out, "cover_decrypted_output.bin", "application/octet-stream");
        } catch (Throwable e) {
            addLog("Cover decrypt error: " + e.getMessage());
            status.setText("Chyba dešifrování coveru: " + e.getMessage());
        }
    }

    private void runDiagnostics() {
        saveKeyState();
        long start = System.currentTimeMillis();
        addLog("=== DIAGNOSTIKA START ===");
        rustSelfTest();

        try {
            String plain = "QES diagnostic test 123.";
            String enc = QesNative.encryptText(plain, pass, s1, s2, s3, s4, glyph, particleValue, vector, phase, amplitude);
            String dec = QesNative.decryptText(enc, pass, s1, s2, s3, s4, glyph, particleValue, vector, phase, amplitude);
            addLog("Text roundtrip: " + (plain.equals(dec) ? "OK" : "FAIL"));

            String bad = QesNative.decryptText(enc, pass + "_bad", s1, s2, s3, s4, glyph, particleValue, vector, phase, amplitude);
            addLog("Wrong key rejection: " + (!plain.equals(bad) ? "OK" : "FAIL"));

            byte[] bin = new byte[]{0,1,2,3,4,5,6,7,8,9,10,11};
            byte[] qes = QesNative.encryptBytes(bin, pass, s1, s2, s3, s4, glyph, particleValue, vector, phase, amplitude);
            byte[] out = QesNative.decryptBytes(qes, pass, s1, s2, s3, s4, glyph, particleValue, vector, phase, amplitude);
            addLog("Binary roundtrip: " + (Arrays.equals(bin, out) ? "OK" : "FAIL") + ", qes=" + qes.length + " B");

            byte[] cover = "cover-data".getBytes();
            byte[] finalCover = concat(cover, COVER_BEGIN, qes, COVER_END);
            int a = indexOf(finalCover, COVER_BEGIN, 0);
            int b = indexOf(finalCover, COVER_END, a + COVER_BEGIN.length);
            byte[] extracted = Arrays.copyOfRange(finalCover, a + COVER_BEGIN.length, b);
            byte[] restored = QesNative.decryptBytes(extracted, pass, s1, s2, s3, s4, glyph, particleValue, vector, phase, amplitude);
            addLog("Cover carrier roundtrip: " + (Arrays.equals(bin, restored) ? "OK" : "FAIL"));

            addLog("Diagnostic total: " + (System.currentTimeMillis() - start) + " ms");
            addLog("=== DIAGNOSTIKA END ===");
            status.setText("Diagnostika dokončena.");
        } catch (Throwable e) {
            addLog("Diagnostic error: " + e.getMessage());
            status.setText("Diagnostika skončila chybou.");
        }

        if (logBox != null) logBox.setText(log.toString());
    }

    private void rustSelfTest() {
        try {
            addLog("Rust selfTest: " + QesNative.selfTest());
        } catch (Throwable e) {
            addLog("Rust selfTest error: " + e.getMessage());
        }
        if (logBox != null) logBox.setText(log.toString());
    }

    private void clearLog() {
        log.setLength(0);
        addLog("Log vyčištěn.");
        if (logBox != null) logBox.setText(log.toString());
    }

    private void saveLog() {
        saveBytes(log.toString().getBytes(), "qes_diagnostic_log.txt", "text/plain");
    }

    private void saveTextOutput() {
        if (textOutput == null) return;
        saveBytes(textOutput.getText().toString().getBytes(), "qes_text_output.txt", "text/plain");
    }

    private void copyOutput() {
        status.setText("Kopírování do schránky doplníme v dalším patchi. Teď použij ULOŽIT VÝSTUP.");
    }

    private void openFile(int request) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        startActivityForResult(i, request);
    }

    private void saveBytes(byte[] bytes, String name, String mime) {
        pendingSaveBytes = bytes;
        pendingSaveName = name;
        pendingSaveMime = mime;
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(mime);
        i.putExtra(Intent.EXTRA_TITLE, name);
        startActivityForResult(i, REQ_SAVE_FILE);
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null) return;

        Uri uri = data.getData();

        try {
            if (request == REQ_SECRET_FILE) {
                secretUri = uri;
                addLog("Vybrán tajný/vstupní soubor: " + uri);
                status.setText("Soubor vybrán.");
                rebuildVisible();
            } else if (request == REQ_QES_FILE) {
                qesUri = uri;
                addLog("Vybrán QES soubor: " + uri);
                status.setText(".qes vybrán.");
                rebuildVisible();
            } else if (request == REQ_COVER_FILE) {
                coverUri = uri;
                addLog("Vybrán cover soubor: " + uri);
                status.setText("Cover soubor vybrán.");
                rebuildVisible();
            } else if (request == REQ_COVER_FINAL) {
                coverFinalUri = uri;
                addLog("Vybrán finální cover soubor: " + uri);
                status.setText("Finální cover vybrán.");
                rebuildVisible();
            } else if (request == REQ_SAVE_FILE) {
                try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                    if (os == null) throw new IllegalStateException("Nelze otevřít výstupní stream.");
                    os.write(pendingSaveBytes);
                }
                addLog("Uloženo: " + pendingSaveName + ", bytes=" + pendingSaveBytes.length + ", sha256=" + sha256(pendingSaveBytes));
                status.setText("Uloženo: " + pendingSaveName);
            }
        } catch (Throwable e) {
            addLog("onActivityResult error: " + e.getMessage());
            status.setText("Chyba práce se souborem: " + e.getMessage());
        }
    }

    private void rebuildVisible() {
        setContentView(app());
        rebuildCurrentPage();
    }

    private byte[] readAll(Uri uri) throws Exception {
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) throw new IllegalStateException("Nelze otevřít soubor.");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) >= 0) {
                bos.write(buf, 0, n);
            }
            return bos.toByteArray();
        }
    }

    private byte[] concat(byte[]... arrays) {
        int len = 0;
        for (byte[] a : arrays) len += a.length;
        byte[] out = new byte[len];
        int p = 0;
        for (byte[] a : arrays) {
            System.arraycopy(a, 0, out, p, a.length);
            p += a.length;
        }
        return out;
    }

    private int indexOf(byte[] data, byte[] needle, int start) {
        if (needle.length == 0) return -1;
        for (int i = Math.max(0, start); i <= data.length - needle.length; i++) {
            boolean ok = true;
            for (int j = 0; j < needle.length; j++) {
                if (data[i + j] != needle[j]) {
                    ok = false;
                    break;
                }
            }
            if (ok) return i;
        }
        return -1;
    }

    private String sha256(byte[] data) throws Exception {
        MessageDigest d = MessageDigest.getInstance("SHA-256");
        byte[] h = d.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : h) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private void addLog(String s) {
        String ts = new SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(new Date());
        log.append("[").append(ts).append("] ").append(s).append("\n");
        if (logBox != null) logBox.setText(log.toString());
    }

    private int parseInt(String s, int def, int min, int max) {
        try {
            int v = Integer.parseInt(s.trim());
            return Math.max(min, Math.min(max, v));
        } catch (Throwable e) {
            return def;
        }
    }

    private long parseLong(String s, long def) {
        try {
            return Long.parseLong(s.trim());
        } catch (Throwable e) {
            return def;
        }
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER);
        return r;
    }

    private Button action(String label, View.OnClickListener l) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(label);
        b.setTextSize(12);
        b.setTextColor(dark ? Color.rgb(5, 6, 10) : Color.WHITE);
        b.setBackgroundColor(ACCENT);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        p.setMargins(4, 6, 4, 6);
        b.setLayoutParams(p);
        return b;
    }

    private Button smallButton(String label) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(label);
        b.setTextSize(11);
        b.setTextColor(TEXT);
        b.setBackgroundColor(CARD);
        return b;
    }

    private EditText field(String hint, boolean secret, String value) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value);
        e.setSingleLine(true);
        e.setTextColor(TEXT);
        e.setHintTextColor(MUTED);
        e.setBackgroundColor(CARD);
        e.setPadding(16, 12, 16, 12);
        if (secret) e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 5, 0, 5);
        e.setLayoutParams(p);
        return e;
    }

    private EditText area(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setMinLines(7);
        e.setGravity(Gravity.TOP | Gravity.START);
        e.setTextColor(TEXT);
        e.setHintTextColor(MUTED);
        e.setBackgroundColor(CARD);
        e.setTypeface(Typeface.MONOSPACE);
        e.setPadding(16, 16, 16, 16);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 8, 0, 10);
        e.setLayoutParams(p);
        return e;
    }

    private TextView text(String s, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setLineSpacing(5, 1);
        if (bold) v.setTypeface(Typeface.DEFAULT_BOLD);
        return v;
    }

    private void hero(String h, String b) {
        TextView v = text(h + "\n\n" + b, 15, TEXT, false);
        v.setBackgroundColor(CARD);
        v.setPadding(18, 18, 18, 18);
        content.addView(v);
    }

    private void section(String s) {
        TextView v = text(s, 13, ACCENT, true);
        v.setLetterSpacing(0.10f);
        v.setPadding(0, 18, 0, 8);
        content.addView(v);
    }

    private void card(String h, String b) {
        TextView v = text(h + "\n\n" + b, 15, TEXT, false);
        v.setBackgroundColor(CARD);
        v.setPadding(18, 18, 18, 18);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 8, 0, 8);
        v.setLayoutParams(p);
        content.addView(v);
    }

    private Space space(int h) {
        Space s = new Space(this);
        s.setLayoutParams(new LinearLayout.LayoutParams(1, h));
        return s;
    }
}

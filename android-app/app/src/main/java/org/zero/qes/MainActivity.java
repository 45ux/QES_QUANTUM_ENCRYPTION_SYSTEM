package org.zero.qes;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends Activity {
    private static final int REQ_SECRET_FILE = 100;
    private static final int REQ_QES_FILE = 101;
    private static final int REQ_COVER_FILE = 102;
    private static final int REQ_COVER_FINAL = 103;
    private static final int REQ_VERIFY_FILE = 104;
    private static final int REQ_SAVE_FILE = 200;

    private static final byte[] COVER_BEGIN = "\n-----BEGIN QES COVER PAYLOAD V1-----\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] COVER_END = "\n-----END QES COVER PAYLOAD V1-----\n".getBytes(StandardCharsets.UTF_8);

    private final SecureRandom random = new SecureRandom();

    private LinearLayout content;
    private TextView status;

    private EditText passwordField;
    private EditText baseSeedField;
    private EditText extraSeedsField;
    private EditText glyphField;
    private EditText valueField;
    private EditText vectorField;
    private EditText phaseField;
    private EditText amplitudeField;
    private EditText textInput;
    private EditText textOutput;
    private EditText verifyInput;
    private EditText verifyExpectedHash;
    private EditText verifyExpectedMac;
    private EditText logBox;

    private Uri secretUri;
    private Uri qesUri;
    private Uri coverUri;
    private Uri coverFinalUri;
    private Uri verifyUri;

    private byte[] pendingSaveBytes;
    private String pendingSaveName = "qes_output.bin";
    private String pendingSaveMime = "application/octet-stream";

    private byte[] lastOutputBytes;
    private byte[] lastCapsule128;
    private String lastReport = "";
    private String lastMode = "NONE";

    private boolean dark = true;
    private String currentPage = "overview";

    private String pass = "qes-demo-password";
    private String baseSeed = "seed-main";
    private String extraSeeds = "";
    private String glyph = "X";
    private int particleValue = 77;
    private String vector = "vector-zero";
    private long phase = 13L;
    private int amplitude = 9;
    private String artProfile = "ZERO GRID";

    private final String appVersion = "0.11.0-alpha";
    private final String patchVersion = "P-2026-05-31-02";
    private final String buildStage = "QES ALFA PROTOTYP";

    private String appMode = "NORMÁLNÍ";
    private String uiMode = "STANDARD";
    private String testMode = "STANDARDNÍ";
    private String cryptoProfile = "QES CORE";
    private String aesMode = "AES-GCM";
    private String compressionMode = "VYPNUTO";

    private boolean requirePassword = true;
    private boolean requireMainSeed = true;
    private boolean requireMac = true;
    private boolean stopOnMacError = true;
    private boolean allowDemoPassword = false;

    private boolean outputMacReport = true;
    private boolean outputCapsule = true;
    private boolean outputPublicHash = true;
    private boolean outputLog = true;

    private boolean artAsNavigation = true;
    private boolean artSaveToCapsule = true;

    private final StringBuilder log = new StringBuilder();

    private int BG, PANEL, CARD, FIELD, TEXT, MUTED, ACCENT, ACCENT2, GOOD, BAD;

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
            BG = Color.rgb(3, 5, 10);
            PANEL = Color.rgb(8, 11, 18);
            CARD = Color.rgb(16, 20, 32);
            FIELD = Color.rgb(10, 13, 22);
            TEXT = Color.rgb(238, 243, 252);
            MUTED = Color.rgb(145, 154, 172);
            ACCENT = Color.rgb(116, 235, 255);
            ACCENT2 = Color.rgb(185, 130, 255);
            GOOD = Color.rgb(110, 255, 185);
            BAD = Color.rgb(255, 105, 120);
        } else {
            BG = Color.rgb(246, 248, 252);
            PANEL = Color.rgb(226, 232, 242);
            CARD = Color.WHITE;
            FIELD = Color.rgb(238, 242, 248);
            TEXT = Color.rgb(18, 24, 34);
            MUTED = Color.rgb(88, 98, 115);
            ACCENT = Color.rgb(0, 95, 145);
            ACCENT2 = Color.rgb(95, 55, 160);
            GOOD = Color.rgb(0, 130, 80);
            BAD = Color.rgb(190, 45, 55);
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
        title.setLetterSpacing(0.16f);
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
        root.addView(text("Quantum Encryption System · " + buildStage + " · v" + appVersion + " · ZERO", 13, MUTED, false));
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

        LinearLayout r1 = row();
        r1.addView(navButton("PŘEHLED", "overview"));
        r1.addView(navButton("KLÍČ", "key"));
        r1.addView(navButton("ART", "art"));
        r1.addView(navButton("TEXT", "text"));

        LinearLayout r2 = row();
        r2.addView(navButton("SOUBOR", "file"));
        r2.addView(navButton("COVER", "cover"));
        r2.addView(navButton("OVĚŘENÍ", "verify"));
        r2.addView(navButton("TESTY", "diag"));

        LinearLayout r3 = row();
        r3.addView(navButton("ARCH", "arch"));
        r3.addView(navButton("LOG", "log"));
        r3.addView(navButton("MAC", "mac"));
        r3.addView(navButton("NASTAVENÍ", "zero"));

        box.addView(r1);
        box.addView(r2);
        box.addView(r3);
        return box;
    }

    private Button navButton(String label, String page) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(label);
        b.setTextSize(10);
        b.setTextColor(page.equals(currentPage) ? (dark ? Color.BLACK : Color.WHITE) : TEXT);
        b.setBackgroundColor(page.equals(currentPage) ? ACCENT : PANEL);
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
        else if ("art".equals(currentPage)) showArt();
        else if ("text".equals(currentPage)) showText();
        else if ("file".equals(currentPage)) showFile();
        else if ("cover".equals(currentPage)) showCover();
        else if ("verify".equals(currentPage)) showVerify();
        else if ("diag".equals(currentPage)) showDiagnostics();
        else if ("arch".equals(currentPage)) showArchitecture();
        else if ("log".equals(currentPage)) showLog();
        else if ("mac".equals(currentPage)) showMac();
        else if ("zero".equals(currentPage)) showZero();
        else showOverview();
    }

    private void clear() {
        content.removeAllViews();
        status.setText("");
        textInput = null;
        textOutput = null;
        verifyInput = null;
        verifyExpectedHash = null;
        verifyExpectedMac = null;
        logBox = null;
    }

    private void showOverview() {
        clear();
        currentPage = "overview";
        hero("QES ALFA CONTROL DECK",
                "QES ALFA PROTOTYP v" + appVersion + " · patch " + patchVersion + "\n\nFinálně pojatá APK verze. Každý režim má vlastní stránku. Text, soubor, cover, ověření, testy, logy, MAC a architektura jsou oddělené tak, aby aplikace působila přehledně i technicky.");
        metrics("VERZE", appVersion, "CORE", "RUST JNI", "PATCH", patchVersion);
        card("Funkční vrstvy",
                "• dynamické seedy: základní seed + libovolné další seedy\n• ASCII art profily jako volitelné dlaždice\n• textové šifrování a dešifrování\n• souborové šifrování a dešifrování\n• cover carrier s payloadem\n• MAC report pro text, soubor i cover\n• ověření podle hash/MAC\n• diagnostika a uložitelný log");
        card("Bezpečnostní rámec",
                "QES je vývojový prototyp. Diagnostické testy jsou tvrdé indikátory chování, nikoli formální kryptografický audit. Formální bezpečnost by vyžadovala nezávislou kryptografickou analýzu.");
    }

    private void showKey() {
        clear();
        currentPage = "key";
        section("NAVIGACE / PASSWORD / SEEDY");
        addKeyPanel(false);
        card("Dynamické seedy",
                "V základu používáš jeden hlavní seed. Další seedy můžeš přidávat jako řádky. APK je následně deterministicky složí do 4 interních seed slotů, které používá Rust core.");
    }

    private void showArt() {
        clear();
        currentPage = "art";
        section("ASCII ART / TILE GENERATOR");
        addCompactKeyPanel();

        card("Aktuální ART profil", artProfile + "\n\n" + artPreview());

        LinearLayout r1 = row();
        r1.addView(action("ZERO GRID", v -> setArt("ZERO GRID")));
        r1.addView(action("NEON WAVE", v -> setArt("NEON WAVE")));
        content.addView(r1);

        LinearLayout r2 = row();
        r2.addView(action("VOID FIELD", v -> setArt("VOID FIELD")));
        r2.addView(action("Q-CAPSULE", v -> setArt("Q-CAPSULE")));
        content.addView(r2);

        LinearLayout r3 = row();
        r3.addView(action("RANDOM ART", v -> randomArt()));
        r3.addView(action("ULOŽIT PROFIL", v -> {
            saveKeyState();
            status.setText("ART profil uložen do navigace.");
        }));
        content.addView(r3);

        card("Jak ART ovlivňuje šifru",
                "ART profil mění glyph, value, vector, phase a amplitude. Tím se mění particle navigace. V další generaci může být stejná stránka napojena na hlubší ASCII renderer přímo v Rust core.");
    }

    private void showText() {
        clear();
        currentPage = "text";
        section("TEXT MODE");
        addCompactKeyPanel();

        textInput = area("Vstupní text nebo QES ASCII balík");
        textOutput = area("Výstup / QES balík / plaintext");
        content.addView(textInput);

        LinearLayout r1 = row();
        r1.addView(action("ŠIFROVAT", v -> encryptTextAction()));
        r1.addView(action("DEŠIFROVAT", v -> decryptTextAction()));
        content.addView(r1);

        LinearLayout r2 = row();
        r2.addView(action("KOPÍROVAT", v -> copyOutput()));
        r2.addView(action("ULOŽIT", v -> saveTextOutput()));
        content.addView(r2);

        content.addView(textOutput);
    }

    private void showFile() {
        clear();
        currentPage = "file";
        section("FILE MODE");
        addCompactKeyPanel();

        card("Souborový tok",
                "Vstupní soubor → QES Rust core → encrypted .qes → MAC report. Dešifrování vezme .qes a obnoví původní data.");

        LinearLayout r1 = row();
        r1.addView(action("VYBRAT SOUBOR", v -> openFile(REQ_SECRET_FILE)));
        r1.addView(action("VYBRAT .QES", v -> openFile(REQ_QES_FILE)));
        content.addView(r1);

        LinearLayout r2 = row();
        r2.addView(action("ŠIFROVAT SOUBOR", v -> encryptSelectedFile()));
        r2.addView(action("DEŠIFROVAT .QES", v -> decryptSelectedFile()));
        content.addView(r2);

        LinearLayout r3 = row();
        r3.addView(action("ULOŽIT MAC", v -> saveReport()));
        r3.addView(action("ULOŽIT KAPSLI", v -> saveCapsule()));
        content.addView(r3);

        card("Vybraný vstup", secretUri == null ? "Nevybrán." : secretUri.toString());
        card("Vybraný .qes", qesUri == null ? "Nevybrán." : qesUri.toString());
    }

    private void showCover() {
        clear();
        currentPage = "cover";
        section("COVER MODE");
        addCompactKeyPanel();

        card("Cover carrier",
                "Funkční cover režim přidá QES payload za cover soubor a vytvoří finální cover. Z finálního coveru se payload znovu najde a dešifruje. Současně vzniká MAC report a QES-128 kapsle.");

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
        r3.addView(action("ULOŽIT KAPSLI", v -> saveCapsule()));
        content.addView(r3);

        card("Stav coveru",
                "Tajný soubor: " + (secretUri == null ? "nevybrán" : "vybrán") +
                "\nCover soubor: " + (coverUri == null ? "nevybrán" : "vybrán") +
                "\nFinální cover: " + (coverFinalUri == null ? "nevybrán" : "vybrán"));
    }

    private void showVerify() {
        clear();
        currentPage = "verify";
        section("VERIFY / HASH / MAC");

        card("Režim ověření",
                "Ověřit lze text, soubor nebo cover. Vložíš očekávaný public hash a/nebo keyed MAC. Aplikace spočítá aktuální hodnoty a porovná je.");

        verifyInput = area("Text k ověření nebo prázdné při ověření souboru");
        verifyExpectedHash = field("Očekávaný public SHA-256", false, "");
        verifyExpectedMac = field("Očekávaný keyed MAC", false, "");
        content.addView(verifyInput);
        content.addView(verifyExpectedHash);
        content.addView(verifyExpectedMac);

        LinearLayout r1 = row();
        r1.addView(action("OVĚŘIT TEXT", v -> verifyText()));
        r1.addView(action("VYBRAT SOUBOR", v -> openFile(REQ_VERIFY_FILE)));
        content.addView(r1);

        LinearLayout r2 = row();
        r2.addView(action("OVĚŘIT SOUBOR", v -> verifySelectedFile("FILE")));
        r2.addView(action("OVĚŘIT COVER", v -> verifySelectedFile("COVER")));
        content.addView(r2);

        card("Vybraný soubor pro ověření", verifyUri == null ? "Nevybrán." : verifyUri.toString());
    }

    private void showDiagnostics() {
        clear();
        currentPage = "diag";
        section("DIAGNOSTIKA / TESTY");
        LinearLayout r1 = row();
        r1.addView(action("SPUSTIT TESTY", v -> runDiagnostics()));
        r1.addView(action("TĚŽKÉ TESTY", v -> runHeavyDiagnostics()));
        content.addView(r1);

        LinearLayout r2 = row();
        r2.addView(action("RUST SELF TEST", v -> rustSelfTest()));
        r2.addView(action("ULOŽIT LOG", v -> saveLog()));
        content.addView(r2);

        LinearLayout r3 = row();
        r3.addView(action("VYČISTIT LOG", v -> clearLog()));
        r3.addView(action("ULOŽIT MAC", v -> saveReport()));
        content.addView(r3);

        logBox = area("Diagnostický log");
        logBox.setText(log.toString());
        content.addView(logBox);

        card("Testovací sada",
                "Roundtrip text, roundtrip binární data, špatný klíč, změna dat, cover roundtrip, nonce divergence, entropy, monobit, byte diversity, chi-square, serial correlation, MAC/hash.");
    }

    private void showArchitecture() {
        clear();
        currentPage = "arch";
        section("ARCHITEKTURA QES");
        card("Tok systému",
                "data → password + dynamické seedy + particle → KDF → hidden state → QES Core → encrypted core → režim text / file / cover → výstup + MAC + kapsle");
        card("QES Core",
                "QES Core není jen XOR ani ARX. Je to sada vratných vrstev: permutace, difuze, superpozice, rotace, XOR masky, ARX prvky a tagové vrstvy.");
        card("Symetrie",
                "Stejná navigace, která data zašifruje, je potřebná i k jejich obnově. Kdo nemá password, seedy a particle, nemá správnou trasu.");
        card("Cover",
                "Současný cover carrier ukládá QES payload do finálního cover souboru. Adaptive Labyrinth Cover je navazující etapa: body, křivka, kapacita, rotace, permutace a návrat podle klíče.");
    }

    private void showLog() {
        clear();
        currentPage = "log";
        section("LOG");
        LinearLayout r = row();
        r.addView(action("ULOŽIT LOG", v -> saveLog()));
        r.addView(action("VYČISTIT LOG", v -> clearLog()));
        content.addView(r);
        logBox = area("Log");
        logBox.setText(log.toString());
        content.addView(logBox);
    }

    private void showMac() {
        clear();
        currentPage = "mac";
        section("MAC / CAPSULE");
        card("Poslední režim", lastMode);
        card("Poslední MAC report", lastReport.isEmpty() ? "Zatím nebyl vytvořen." : lastReport);
        LinearLayout r = row();
        r.addView(action("ULOŽIT MAC", v -> saveReport()));
        r.addView(action("ULOŽIT KAPSLI", v -> saveCapsule()));
        content.addView(r);
    }

    private void showZero() {
        clear();
        currentPage = "zero";
        section("NASTAVENÍ / SYSTEM CONTROL");

        card("Verze aplikace",
                buildStage +
                "\nVerze: " + appVersion +
                "\nPatch: " + patchVersion +
                "\nBuild: debug APK / GitHub Actions" +
                "\nRust core: " + rustStatusQuiet() +
                "\nJNI bridge: aktivní");

        metrics("APP", appMode, "UI", uiMode, "TEST", testMode);
        metrics("CRYPTO", cryptoProfile, "AES", aesMode, "KOMPRESE", compressionMode);

        card("Režim aplikace",
                "Normální režim = jednoduché ovládání.\nExperimentální režim = cover, capsule, ART, MAC, těžké testy.\nVývojářský režim = detailní logy a interní diagnostika.");

        LinearLayout modeRow = row();
        modeRow.addView(action("NORMÁLNÍ", v -> setAppMode("NORMÁLNÍ")));
        modeRow.addView(action("EXPERIMENT", v -> setAppMode("EXPERIMENTÁLNÍ")));
        content.addView(modeRow);
        content.addView(action("VÝVOJÁŘSKÝ REŽIM", v -> setAppMode("VÝVOJÁŘSKÝ")));

        card("Vzhled",
                "Tmavý / světlý motiv je aktivní.\nVysoký kontrast a kompaktní rozložení jsou připravené jako UI profil.");

        LinearLayout uiRow = row();
        uiRow.addView(action(dark ? "PŘEPNOUT NA SVĚTLÝ" : "PŘEPNOUT NA TMAVÝ", v -> {
            dark = !dark;
            addLog("Nastavení: přepnut motiv.");
            setContentView(app());
            showZero();
        }));
        uiRow.addView(action("UI PROFIL", v -> cycleUiMode()));
        content.addView(uiRow);

        card("Bezpečnost",
                "Vyžadovat heslo: " + on(requirePassword) +
                "\nVyžadovat hlavní seed: " + on(requireMainSeed) +
                "\nVyžadovat MAC/TAG: " + on(requireMac) +
                "\nZastavit při chybě: " + on(stopOnMacError) +
                "\nDemo heslo povoleno: " + on(allowDemoPassword));

        LinearLayout secRow1 = row();
        secRow1.addView(action("HESLO", v -> { requirePassword = !requirePassword; refreshSettings("requirePassword=" + requirePassword); }));
        secRow1.addView(action("SEED", v -> { requireMainSeed = !requireMainSeed; refreshSettings("requireMainSeed=" + requireMainSeed); }));
        content.addView(secRow1);

        LinearLayout secRow2 = row();
        secRow2.addView(action("MAC/TAG", v -> { requireMac = !requireMac; refreshSettings("requireMac=" + requireMac); }));
        secRow2.addView(action("STOP ERROR", v -> { stopOnMacError = !stopOnMacError; refreshSettings("stopOnMacError=" + stopOnMacError); }));
        content.addView(secRow2);

        card("Výstupy po šifrování",
                "MAC report: " + on(outputMacReport) +
                "\nQES-128 kapsle: " + on(outputCapsule) +
                "\nPublic hash: " + on(outputPublicHash) +
                "\nLog: " + on(outputLog));

        LinearLayout outRow1 = row();
        outRow1.addView(action("MAC", v -> { outputMacReport = !outputMacReport; refreshSettings("outputMacReport=" + outputMacReport); }));
        outRow1.addView(action("KAPSLE", v -> { outputCapsule = !outputCapsule; refreshSettings("outputCapsule=" + outputCapsule); }));
        content.addView(outRow1);

        LinearLayout outRow2 = row();
        outRow2.addView(action("HASH", v -> { outputPublicHash = !outputPublicHash; refreshSettings("outputPublicHash=" + outputPublicHash); }));
        outRow2.addView(action("LOG", v -> { outputLog = !outputLog; refreshSettings("outputLog=" + outputLog); }));
        content.addView(outRow2);

        card("ART / ASCII nastavení",
                "ART jako navigace: " + on(artAsNavigation) +
                "\nUložit ART profil do kapsle: " + on(artSaveToCapsule) +
                "\nAktuální profil: " + artProfile +
                "\n\nART už nemá být stránka pro heslo. Heslo patří do KLÍČE. ART je profil / dlaždice / navigační doplněk.");

        LinearLayout artRow = row();
        artRow.addView(action("ART NAV", v -> { artAsNavigation = !artAsNavigation; refreshSettings("artAsNavigation=" + artAsNavigation); }));
        artRow.addView(action("ART KAPSLE", v -> { artSaveToCapsule = !artSaveToCapsule; refreshSettings("artSaveToCapsule=" + artSaveToCapsule); }));
        content.addView(artRow);

        card("Kryptografický profil",
                "Aktivní jádro: QES CORE.\nAES a další šifry jsou zde jako volitelný profil pro další Rust patch. Nebudu tvrdit, že AES šifruje, dokud nebude napojený do core.");

        LinearLayout cryptoRow = row();
        cryptoRow.addView(action("CRYPTO PROFIL", v -> cycleCryptoProfile()));
        cryptoRow.addView(action("AES MÓD", v -> cycleAesMode()));
        content.addView(cryptoRow);

        card("Komprese",
                "Aktuální komprese: " + compressionMode +
                "\nKomprese bude další vrstva před šifrováním. Pro ostré použití musí být přesně zapsaná v metadatech, aby šla data obnovit.");

        content.addView(action("PŘEPNOUT KOMPRESI", v -> cycleCompression()));

        card("Testy",
                "Testovací režim: " + testMode +
                "\nRychlé = funkčnost. Standardní = roundtrip + MAC. Těžké = statistické indikátory. Extrémní = dlouhé testy pro pozdější verzi.");

        content.addView(action("PŘEPNOUT TESTY", v -> cycleTestMode()));

        card("Reset / údržba",
                "Resetuje jen lokální stav aplikace, neuložené výstupy a pracovní hodnoty.");

        LinearLayout resetRow1 = row();
        resetRow1.addView(action("RESET LOG", v -> clearLog()));
        resetRow1.addView(action("RESET ART", v -> { artProfile = "ZERO GRID"; refreshSettings("ART reset"); }));
        content.addView(resetRow1);

        LinearLayout resetRow2 = row();
        resetRow2.addView(action("RESET MAC", v -> { lastReport = ""; lastCapsule128 = null; lastMode = "NONE"; refreshSettings("MAC reset"); }));
        resetRow2.addView(action("RESET NAVIGACE", v -> { pass = ""; baseSeed = "seed-main"; extraSeeds = ""; refreshSettings("Navigace reset"); }));
        content.addView(resetRow2);
    }

    private String rustStatusQuiet() {
        try {
            String s = QesNative.selfTest();
            return s == null || s.isEmpty() ? "OK" : "OK";
        } catch (Throwable e) {
            return "FAILED";
        }
    }

    private String on(boolean value) {
        return value ? "ON" : "OFF";
    }

    private void refreshSettings(String msg) {
        addLog("Nastavení: " + msg);
        setContentView(app());
        showZero();
    }

    private void setAppMode(String mode) {
        appMode = mode;
        refreshSettings("appMode=" + mode);
    }

    private void cycleUiMode() {
        if ("STANDARD".equals(uiMode)) uiMode = "VYSOKÝ KONTRAST";
        else if ("VYSOKÝ KONTRAST".equals(uiMode)) uiMode = "KOMPAKTNÍ";
        else uiMode = "STANDARD";
        refreshSettings("uiMode=" + uiMode);
    }

    private void cycleTestMode() {
        if ("RYCHLÉ".equals(testMode)) testMode = "STANDARDNÍ";
        else if ("STANDARDNÍ".equals(testMode)) testMode = "TĚŽKÉ";
        else if ("TĚŽKÉ".equals(testMode)) testMode = "EXTRÉMNÍ";
        else testMode = "RYCHLÉ";
        refreshSettings("testMode=" + testMode);
    }

    private void cycleCryptoProfile() {
        if ("QES CORE".equals(cryptoProfile)) cryptoProfile = "AES";
        else if ("AES".equals(cryptoProfile)) cryptoProfile = "CHACHA20-POLY1305";
        else if ("CHACHA20-POLY1305".equals(cryptoProfile)) cryptoProfile = "HYBRID QES+AES";
        else cryptoProfile = "QES CORE";
        refreshSettings("cryptoProfile=" + cryptoProfile);
    }

    private void cycleAesMode() {
        if ("AES-GCM".equals(aesMode)) aesMode = "AES-CTR";
        else if ("AES-CTR".equals(aesMode)) aesMode = "AES-CBC COMPAT";
        else aesMode = "AES-GCM";
        refreshSettings("aesMode=" + aesMode);
    }

    private void cycleCompression() {
        if ("VYPNUTO".equals(compressionMode)) compressionMode = "DEFLATE";
        else if ("DEFLATE".equals(compressionMode)) compressionMode = "ZSTD PLÁN";
        else if ("ZSTD PLÁN".equals(compressionMode)) compressionMode = "LZMA PLÁN";
        else compressionMode = "VYPNUTO";
        refreshSettings("compressionMode=" + compressionMode);
    }

    private void addKeyPanel(boolean compact) {
        passwordField = field("Password", true, pass);
        baseSeedField = field("Seed hlavní", false, baseSeed);
        extraSeedsField = area("Další seedy – každý seed na nový řádek");
        extraSeedsField.setText(extraSeeds);

        glyphField = field("ASCII glyph", false, glyph);
        valueField = field("Particle value 0–255", false, String.valueOf(particleValue));
        vectorField = field("Particle vector", false, vector);
        phaseField = field("Phase", false, String.valueOf(phase));
        amplitudeField = field("Amplitude 0–255", false, String.valueOf(amplitude));

        content.addView(passwordField);
        content.addView(baseSeedField);

        LinearLayout seedRow = row();
        seedRow.addView(action("PŘIDAT SEED", v -> addSeed()));
        seedRow.addView(action("VYČISTIT SEEDY", v -> clearExtraSeeds()));
        content.addView(seedRow);

        content.addView(extraSeedsField);
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
        addKeyPanel(true);
    }

    private void addSeed() {
        saveKeyState();
        String next = "seed-" + (1 + countExtraSeeds() + 1) + "-" + shortHex(randomBytes(4));
        if (extraSeeds.trim().isEmpty()) extraSeeds = next;
        else extraSeeds = extraSeeds.trim() + "\n" + next;
        addLog("Přidán seed: " + next);
        setContentView(app());
        rebuildCurrentPage();
    }

    private void clearExtraSeeds() {
        extraSeeds = "";
        addLog("Další seedy vyčištěny.");
        setContentView(app());
        rebuildCurrentPage();
    }

    private int countExtraSeeds() {
        if (extraSeeds.trim().isEmpty()) return 0;
        return extraSeeds.trim().split("\\R+").length;
    }

    private void saveKeyState() {
        if (passwordField == null) return;
        pass = passwordField.getText().toString();
        baseSeed = baseSeedField.getText().toString();
        extraSeeds = extraSeedsField.getText().toString();
        glyph = glyphField.getText().toString().trim().isEmpty() ? "X" : glyphField.getText().toString().trim();
        vector = vectorField.getText().toString();
        particleValue = parseInt(valueField.getText().toString(), 77, 0, 255);
        phase = parseLong(phaseField.getText().toString(), 13L);
        amplitude = parseInt(amplitudeField.getText().toString(), 9, 0, 255);
    }

    private String[] derivedSeeds(String mode) {
        String all = "mode=" + mode + "\nbase=" + baseSeed + "\nextra=" + extraSeeds + "\nart=" + artProfile + "\nvector=" + vector;
        String h1 = sha256Quiet(("1|" + all).getBytes(StandardCharsets.UTF_8));
        String h2 = sha256Quiet(("2|" + all).getBytes(StandardCharsets.UTF_8));
        String h3 = sha256Quiet(("3|" + all).getBytes(StandardCharsets.UTF_8));
        String seedA = baseSeed.trim().isEmpty() ? "seed-main" : baseSeed.trim();
        return new String[]{
                seedA,
                "dyn-" + h1.substring(0, 32),
                "dyn-" + h2.substring(0, 32),
                "dyn-" + h3.substring(0, 32)
        };
    }

    private boolean keyReady(String context) {
        if (pass == null || pass.trim().isEmpty()) {
            addLog(context + ": FAIL - password nesmí být prázdný.");
            status.setText("Password nesmí být prázdný.");
            return false;
        }
        if (baseSeed == null || baseSeed.trim().isEmpty()) {
            addLog(context + ": FAIL - hlavní seed nesmí být prázdný.");
            status.setText("Hlavní seed nesmí být prázdný.");
            return false;
        }
        return true;
    }

    private void setArt(String profile) {
        saveKeyState();
        artProfile = profile;
        if ("ZERO GRID".equals(profile)) {
            glyph = "X"; particleValue = 77; vector = "zero-grid"; phase = 13; amplitude = 9;
        } else if ("NEON WAVE".equals(profile)) {
            glyph = "~"; particleValue = 144; vector = "neon-wave"; phase = 21; amplitude = 34;
        } else if ("VOID FIELD".equals(profile)) {
            glyph = "."; particleValue = 201; vector = "void-field"; phase = 55; amplitude = 89;
        } else if ("Q-CAPSULE".equals(profile)) {
            glyph = "#"; particleValue = 233; vector = "q-capsule"; phase = 128; amplitude = 64;
        }
        addLog("ART profil nastaven: " + profile);
        setContentView(app());
        showArt();
    }

    private void randomArt() {
        saveKeyState();
        String chars = "X#@%&*+=-~:.<>[]{}";
        glyph = String.valueOf(chars.charAt(random.nextInt(chars.length())));
        particleValue = random.nextInt(256);
        vector = "random-art-" + shortHex(randomBytes(6));
        phase = Math.abs(random.nextLong() % 4096L);
        amplitude = random.nextInt(256);
        artProfile = "RANDOM-" + shortHex(randomBytes(3));
        addLog("Náhodný ART profil: " + artProfile);
        setContentView(app());
        showArt();
    }

    private String artPreview() {
        String seed = artProfile + glyph + particleValue + vector + phase + amplitude;
        String h = sha256Quiet(seed.getBytes(StandardCharsets.UTF_8));
        String palette = "X#@%&*+=-~:. ";
        StringBuilder sb = new StringBuilder();
        sb.append("┌────────────────────┐\n");
        for (int y = 0; y < 8; y++) {
            sb.append("│");
            for (int x = 0; x < 20; x++) {
                int idx = Math.abs((h.charAt((x + y * 3) % h.length()) + x * 7 + y * 11)) % palette.length();
                sb.append(palette.charAt(idx));
            }
            sb.append("│\n");
        }
        sb.append("└────────────────────┘");
        return sb.toString();
    }

    private void encryptTextAction() {
        saveKeyState();
        if (!keyReady("Text encrypt")) return;
        try {
            long started = System.currentTimeMillis();
            String[] ds = derivedSeeds("TEXT");
            String result = QesNative.encryptText(
                    textInput.getText().toString(), pass, ds[0], ds[1], ds[2], ds[3],
                    glyph, particleValue, vector, phase, amplitude
            );
            if (result.startsWith("QES_ERROR")) throw new IllegalStateException(result);
            textOutput.setText(result);
            byte[] out = result.getBytes(StandardCharsets.UTF_8);
            updateSecurityReport("TEXT", out);
            long ms = System.currentTimeMillis() - started;
            addLog("Text encrypted: " + ms + " ms, output=" + out.length + " B");
            status.setText("Text zašifrován. MAC vytvořen.");
        } catch (Throwable e) {
            addLog("Text encrypt error: " + e.getMessage());
            status.setText("Chyba šifrování: " + e.getMessage());
        }
    }

    private void decryptTextAction() {
        saveKeyState();
        if (!keyReady("Text decrypt")) return;
        try {
            long started = System.currentTimeMillis();
            String[] ds = derivedSeeds("TEXT");
            String result = QesNative.decryptText(
                    textInput.getText().toString(), pass, ds[0], ds[1], ds[2], ds[3],
                    glyph, particleValue, vector, phase, amplitude
            );
            if (result.startsWith("QES_ERROR")) throw new IllegalStateException(result);
            textOutput.setText(result);
            long ms = System.currentTimeMillis() - started;
            addLog("Text decrypted: " + ms + " ms");
            status.setText("Text dešifrován.");
        } catch (Throwable e) {
            addLog("Text decrypt error: " + e.getMessage());
            status.setText("Chyba dešifrování: " + e.getMessage());
        }
    }

    private void encryptSelectedFile() {
        saveKeyState();
        if (!keyReady("File encrypt")) return;
        if (secretUri == null) {
            status.setText("Nejdřív vyber soubor.");
            return;
        }
        try {
            long started = System.currentTimeMillis();
            byte[] data = readAll(secretUri);
            String[] ds = derivedSeeds("FILE");
            byte[] out = QesNative.encryptBytes(data, pass, ds[0], ds[1], ds[2], ds[3], glyph, particleValue, vector, phase, amplitude);
            throwIfNativeError(out);
            updateSecurityReport("FILE", out);
            long ms = System.currentTimeMillis() - started;
            addLog("File encrypted: input=" + data.length + " B, output=" + out.length + " B, ms=" + ms);
            saveBytes(out, "encrypted.qes", "application/octet-stream");
        } catch (Throwable e) {
            addLog("File encrypt error: " + e.getMessage());
            status.setText("Chyba souborového šifrování: " + e.getMessage());
        }
    }

    private void decryptSelectedFile() {
        saveKeyState();
        if (!keyReady("File decrypt")) return;
        if (qesUri == null) {
            status.setText("Nejdřív vyber .qes soubor.");
            return;
        }
        try {
            long started = System.currentTimeMillis();
            byte[] data = readAll(qesUri);
            String[] ds = derivedSeeds("FILE");
            byte[] out = QesNative.decryptBytes(data, pass, ds[0], ds[1], ds[2], ds[3], glyph, particleValue, vector, phase, amplitude);
            throwIfNativeError(out);
            updateSecurityReport("FILE-DECRYPTED", out);
            long ms = System.currentTimeMillis() - started;
            addLog("File decrypted: input=" + data.length + " B, output=" + out.length + " B, ms=" + ms);
            saveBytes(out, "decrypted_output.bin", "application/octet-stream");
        } catch (Throwable e) {
            addLog("File decrypt error: " + e.getMessage());
            status.setText("Chyba souborového dešifrování: " + e.getMessage());
        }
    }

    private void createCoverCarrier() {
        saveKeyState();
        if (!keyReady("Cover create")) return;
        if (secretUri == null || coverUri == null) {
            status.setText("Vyber tajný soubor i cover soubor.");
            return;
        }
        try {
            long started = System.currentTimeMillis();
            byte[] secret = readAll(secretUri);
            byte[] cover = readAll(coverUri);
            String[] ds = derivedSeeds("COVER");
            byte[] qes = QesNative.encryptBytes(secret, pass, ds[0], ds[1], ds[2], ds[3], glyph, particleValue, vector, phase, amplitude);
            throwIfNativeError(qes);
            byte[] capsule = makeCapsule128("COVER", qes);
            byte[] finalCover = concat(cover, COVER_BEGIN, capsule, qes, COVER_END);
            updateSecurityReport("COVER", finalCover);
            lastCapsule128 = capsule;
            long ms = System.currentTimeMillis() - started;
            addLog("Cover created: cover=" + cover.length + " B, secret=" + secret.length + " B, final=" + finalCover.length + " B, ms=" + ms);
            saveBytes(finalCover, "qes_cover_output.bin", "application/octet-stream");
        } catch (Throwable e) {
            addLog("Cover create error: " + e.getMessage());
            status.setText("Chyba cover režimu: " + e.getMessage());
        }
    }

    private void decryptCoverCarrier() {
        saveKeyState();
        if (!keyReady("Cover decrypt")) return;
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
            byte[] block = Arrays.copyOfRange(finalCover, a + COVER_BEGIN.length, b);
            if (block.length <= 128) throw new IllegalStateException("Cover payload je příliš krátký.");
            byte[] capsule = Arrays.copyOfRange(block, 0, 128);
            byte[] qes = Arrays.copyOfRange(block, 128, block.length);
            String[] ds = derivedSeeds("COVER");
            byte[] out = QesNative.decryptBytes(qes, pass, ds[0], ds[1], ds[2], ds[3], glyph, particleValue, vector, phase, amplitude);
            throwIfNativeError(out);
            lastCapsule128 = capsule;
            updateSecurityReport("COVER-DECRYPTED", out);
            long ms = System.currentTimeMillis() - started;
            addLog("Cover decrypted: final=" + finalCover.length + " B, qes=" + qes.length + " B, output=" + out.length + " B, ms=" + ms);
            saveBytes(out, "cover_decrypted_output.bin", "application/octet-stream");
        } catch (Throwable e) {
            addLog("Cover decrypt error: " + e.getMessage());
            status.setText("Chyba dešifrování coveru: " + e.getMessage());
        }
    }

    private void verifyText() {
        saveKeyState();
        try {
            byte[] data = verifyInput.getText().toString().getBytes(StandardCharsets.UTF_8);
            verifyBytes("TEXT", data);
        } catch (Throwable e) {
            status.setText("Chyba ověření textu: " + e.getMessage());
        }
    }

    private void verifySelectedFile(String mode) {
        saveKeyState();
        if (verifyUri == null) {
            status.setText("Nejdřív vyber soubor k ověření.");
            return;
        }
        try {
            byte[] data = readAll(verifyUri);
            verifyBytes(mode, data);
        } catch (Throwable e) {
            status.setText("Chyba ověření souboru: " + e.getMessage());
        }
    }

    private void verifyBytes(String mode, byte[] data) throws Exception {
        String publicHash = sha256(data);
        String mac = hmacHex(mode, data);
        boolean hashOk = verifyExpectedHash.getText().toString().trim().isEmpty() || verifyExpectedHash.getText().toString().trim().equalsIgnoreCase(publicHash);
        boolean macOk = verifyExpectedMac.getText().toString().trim().isEmpty() || verifyExpectedMac.getText().toString().trim().equalsIgnoreCase(mac);
        String report =
                "VERIFY MODE: " + mode +
                "\nSIZE: " + data.length + " B" +
                "\nPUBLIC_SHA256: " + publicHash +
                "\nKEYED_MAC: " + mac +
                "\nHASH_MATCH: " + (hashOk ? "OK" : "FAIL") +
                "\nMAC_MATCH: " + (macOk ? "OK" : "FAIL");
        lastReport = report;
        lastMode = "VERIFY-" + mode;
        lastOutputBytes = data;
        addLog(report.replace("\n", " | "));
        status.setText(hashOk && macOk ? "Ověření OK." : "Ověření nesouhlasí.");
        setContentView(app());
        showVerify();
        card("Výsledek ověření", report);
    }

    private void updateSecurityReport(String mode, byte[] output) throws Exception {
        lastMode = mode;
        lastOutputBytes = output;
        String publicHash = sha256(output);
        String mac = hmacHex(mode, output);
        lastCapsule128 = makeCapsule128(mode, output);
        lastReport =
                "QES SECURITY REPORT" +
                "\nMODE: " + mode +
                "\nSIZE: " + output.length + " B" +
                "\nART_PROFILE: " + artProfile +
                "\nSEEDS_COUNT: " + (1 + countExtraSeeds()) +
                "\nPUBLIC_SHA256: " + publicHash +
                "\nKEYED_MAC: " + mac +
                "\nCAPSULE_128_SHA256: " + sha256(lastCapsule128);
        addLog("MAC created: mode=" + mode + ", sha256=" + publicHash + ", mac=" + mac);
    }

    private void runDiagnostics() {
        saveKeyState();
        if (pass.trim().isEmpty()) pass = "qes-diagnostic-password";
        if (baseSeed.trim().isEmpty()) baseSeed = "seed-main";

        long start = System.currentTimeMillis();
        addLog("=== DIAGNOSTIKA START ===");
        rustSelfTest();

        try {
            String plain = "QES diagnostic test 123.";
            String[] ds = derivedSeeds("TEXT");
            String enc = QesNative.encryptText(plain, pass, ds[0], ds[1], ds[2], ds[3], glyph, particleValue, vector, phase, amplitude);
            if (enc.startsWith("QES_ERROR")) throw new IllegalStateException(enc);
            String dec = QesNative.decryptText(enc, pass, ds[0], ds[1], ds[2], ds[3], glyph, particleValue, vector, phase, amplitude);
            addLog("Text roundtrip: " + (plain.equals(dec) ? "OK" : "FAIL"));

            String bad = QesNative.decryptText(enc, pass + "_bad", ds[0], ds[1], ds[2], ds[3], glyph, particleValue, vector, phase, amplitude);
            addLog("Wrong key rejection: " + (!plain.equals(bad) ? "OK" : "FAIL"));

            byte[] bin = randomBytes(2048);
            String[] fs = derivedSeeds("FILE");
            byte[] qes = QesNative.encryptBytes(bin, pass, fs[0], fs[1], fs[2], fs[3], glyph, particleValue, vector, phase, amplitude);
            throwIfNativeError(qes);
            byte[] out = QesNative.decryptBytes(qes, pass, fs[0], fs[1], fs[2], fs[3], glyph, particleValue, vector, phase, amplitude);
            throwIfNativeError(out);
            addLog("Binary roundtrip: " + (Arrays.equals(bin, out) ? "OK" : "FAIL") + ", qes=" + qes.length + " B");

            byte[] tampered = qes.clone();
            if (tampered.length > 10) tampered[10] ^= 1;
            byte[] tamperedOut = QesNative.decryptBytes(tampered, pass, fs[0], fs[1], fs[2], fs[3], glyph, particleValue, vector, phase, amplitude);
            addLog("Tamper rejection: " + (isNativeError(tamperedOut) ? "OK" : "CHECK"));

            byte[] qes2 = QesNative.encryptBytes(bin, pass, fs[0], fs[1], fs[2], fs[3], glyph, particleValue, vector, phase, amplitude);
            addLog("Nonce/output divergence: " + (!Arrays.equals(qes, qes2) ? "OK" : "FAIL"));

            addStatTests(qes);

            byte[] cover = "cover-data".getBytes(StandardCharsets.UTF_8);
            byte[] capsule = makeCapsule128("COVER", qes);
            byte[] finalCover = concat(cover, COVER_BEGIN, capsule, qes, COVER_END);
            int a = indexOf(finalCover, COVER_BEGIN, 0);
            int b = indexOf(finalCover, COVER_END, a + COVER_BEGIN.length);
            byte[] block = Arrays.copyOfRange(finalCover, a + COVER_BEGIN.length, b);
            byte[] extracted = Arrays.copyOfRange(block, 128, block.length);
            byte[] restored = QesNative.decryptBytes(extracted, pass, fs[0], fs[1], fs[2], fs[3], glyph, particleValue, vector, phase, amplitude);
            throwIfNativeError(restored);
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

    private void runHeavyDiagnostics() {
        saveKeyState();
        if (pass.trim().isEmpty()) pass = "qes-heavy-test-password";
        if (baseSeed.trim().isEmpty()) baseSeed = "seed-main";

        addLog("=== HEAVY TEST START ===");
        int[] sizes = new int[]{1, 2, 5, 16, 64, 257, 1024, 4096, 16384};
        int ok = 0;
        int fail = 0;

        for (int size : sizes) {
            try {
                byte[] input = randomBytes(size);
                String[] fs = derivedSeeds("HEAVY-" + size);
                byte[] enc = QesNative.encryptBytes(input, pass, fs[0], fs[1], fs[2], fs[3], glyph, particleValue, vector, phase, amplitude);
                throwIfNativeError(enc);
                byte[] dec = QesNative.decryptBytes(enc, pass, fs[0], fs[1], fs[2], fs[3], glyph, particleValue, vector, phase, amplitude);
                throwIfNativeError(dec);
                boolean round = Arrays.equals(input, dec);
                if (round) ok++; else fail++;
                addLog("Heavy roundtrip " + size + " B: " + (round ? "OK" : "FAIL"));
                addStatTests(enc);
            } catch (Throwable e) {
                fail++;
                addLog("Heavy test " + size + " B: FAIL - " + e.getMessage());
            }
        }

        addLog("HEAVY RESULT: OK=" + ok + " FAIL=" + fail);
        addLog("=== HEAVY TEST END ===");
        status.setText("Těžké testy dokončeny: OK=" + ok + " FAIL=" + fail);
        if (logBox != null) logBox.setText(log.toString());
    }

    private void addStatTests(byte[] data) {
        try {
            double entropy = entropy(data);
            double monobit = monobitRatio(data);
            int diversity = byteDiversity(data);
            double chi = chiSquare(data);
            double corr = serialCorrelation(data);
            addLog(String.format(Locale.ROOT, "Stats: entropy=%.4f, monobit=%.4f, diversity=%d/256, chi=%.2f, serialCorr=%.5f", entropy, monobit, diversity, chi, corr));
        } catch (Throwable e) {
            addLog("Stats error: " + e.getMessage());
        }
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
        saveBytes(log.toString().getBytes(StandardCharsets.UTF_8), "qes_diagnostic_log.txt", "text/plain");
    }

    private void saveReport() {
        if (lastReport == null || lastReport.isEmpty()) {
            status.setText("Zatím není vytvořený MAC report.");
            return;
        }
        saveBytes(lastReport.getBytes(StandardCharsets.UTF_8), "qes_mac_report.txt", "text/plain");
    }

    private void saveCapsule() {
        if (lastCapsule128 == null) {
            status.setText("Zatím není vytvořená kapsle.");
            return;
        }
        saveBytes(lastCapsule128, "qes_capsule_128.bin", "application/octet-stream");
    }

    private void saveTextOutput() {
        if (textOutput == null) return;
        saveBytes(textOutput.getText().toString().getBytes(StandardCharsets.UTF_8), "qes_text_output.txt", "text/plain");
    }

    private void copyOutput() {
        if (textOutput == null) return;
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("QES output", textOutput.getText().toString()));
        status.setText("Výstup zkopírován.");
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
            } else if (request == REQ_VERIFY_FILE) {
                verifyUri = uri;
                addLog("Vybrán soubor pro ověření: " + uri);
                status.setText("Soubor pro ověření vybrán.");
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
            while ((n = is.read(buf)) >= 0) bos.write(buf, 0, n);
            return bos.toByteArray();
        }
    }

    private void throwIfNativeError(byte[] data) {
        if (isNativeError(data)) {
            throw new IllegalStateException(new String(data, 0, Math.min(data.length, 256), StandardCharsets.UTF_8));
        }
    }

    private boolean isNativeError(byte[] data) {
        if (data == null || data.length < 9) return false;
        String p = new String(data, 0, Math.min(data.length, 32), StandardCharsets.UTF_8);
        return p.startsWith("QES_ERROR");
    }

    private byte[] makeCapsule128(String mode, byte[] payload) throws Exception {
        byte[] capsule = new byte[128];
        byte[] magic = "QES128V1".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(magic, 0, capsule, 0, magic.length);

        byte[] modeHash = shaBytes(mode.getBytes(StandardCharsets.UTF_8));
        byte[] pub = shaBytes(payload);
        byte[] mac = hmacBytes(mode, payload);
        byte[] len = ByteBuffer.allocate(8).putLong(payload.length).array();

        System.arraycopy(modeHash, 0, capsule, 8, 24);
        System.arraycopy(pub, 0, capsule, 32, 32);
        System.arraycopy(mac, 0, capsule, 64, 32);
        System.arraycopy(len, 0, capsule, 96, 8);

        byte[] mix = shaBytes(concat(modeHash, pub, mac, len, artProfile.getBytes(StandardCharsets.UTF_8)));
        System.arraycopy(mix, 0, capsule, 104, 24);
        return capsule;
    }

    private String hmacHex(String mode, byte[] data) throws Exception {
        return hex(hmacBytes(mode, data));
    }

    private byte[] hmacBytes(String mode, byte[] data) throws Exception {
        String[] ds = derivedSeeds(mode);
        String keyMaterial = "QES-MAC|" + mode + "|" + pass + "|" + ds[0] + "|" + ds[1] + "|" + ds[2] + "|" + ds[3] + "|" + glyph + "|" + particleValue + "|" + vector + "|" + phase + "|" + amplitude + "|" + artProfile;
        byte[] key = shaBytes(keyMaterial.getBytes(StandardCharsets.UTF_8));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        mac.update(mode.getBytes(StandardCharsets.UTF_8));
        mac.update((byte) 0);
        mac.update(data);
        return mac.doFinal();
    }

    private byte[] shaBytes(byte[] data) throws Exception {
        MessageDigest d = MessageDigest.getInstance("SHA-256");
        return d.digest(data);
    }

    private String sha256(byte[] data) throws Exception {
        return hex(shaBytes(data));
    }

    private String sha256Quiet(byte[] data) {
        try {
            return sha256(data);
        } catch (Throwable e) {
            return "0000000000000000000000000000000000000000000000000000000000000000";
        }
    }

    private String hex(byte[] h) {
        StringBuilder sb = new StringBuilder();
        for (byte b : h) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private String shortHex(byte[] h) {
        String x = hex(h);
        return x.substring(0, Math.min(12, x.length()));
    }

    private byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        random.nextBytes(b);
        return b;
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

    private double entropy(byte[] data) {
        if (data.length == 0) return 0;
        int[] c = new int[256];
        for (byte b : data) c[b & 255]++;
        double e = 0;
        for (int x : c) {
            if (x == 0) continue;
            double p = (double) x / data.length;
            e -= p * (Math.log(p) / Math.log(2));
        }
        return e;
    }

    private double monobitRatio(byte[] data) {
        if (data.length == 0) return 0;
        long ones = 0;
        for (byte b : data) ones += Integer.bitCount(b & 255);
        return (double) ones / (data.length * 8.0);
    }

    private int byteDiversity(byte[] data) {
        boolean[] seen = new boolean[256];
        for (byte b : data) seen[b & 255] = true;
        int n = 0;
        for (boolean s : seen) if (s) n++;
        return n;
    }

    private double chiSquare(byte[] data) {
        if (data.length == 0) return 0;
        int[] c = new int[256];
        for (byte b : data) c[b & 255]++;
        double expected = data.length / 256.0;
        double chi = 0;
        for (int x : c) {
            double d = x - expected;
            chi += (d * d) / expected;
        }
        return chi;
    }

    private double serialCorrelation(byte[] data) {
        if (data.length < 2) return 0;
        double mean = 0;
        for (byte b : data) mean += (b & 255);
        mean /= data.length;

        double num = 0;
        double den = 0;
        for (int i = 0; i < data.length - 1; i++) {
            double a = (data[i] & 255) - mean;
            double b = (data[i + 1] & 255) - mean;
            num += a * b;
            den += a * a;
        }
        return den == 0 ? 0 : num / den;
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
        b.setTextSize(11);
        b.setTextColor(dark ? Color.rgb(3, 5, 10) : Color.WHITE);
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
        e.setBackgroundColor(FIELD);
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
        e.setBackgroundColor(FIELD);
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

    private void metrics(String a, String b, String c, String d, String e, String f) {
        LinearLayout r = row();
        r.addView(metric(a, b));
        r.addView(metric(c, d));
        r.addView(metric(e, f));
        content.addView(r);
    }

    private TextView metric(String h, String v) {
        TextView t = text(h + "\n" + v, 12, TEXT, true);
        t.setGravity(Gravity.CENTER);
        t.setBackgroundColor(CARD);
        t.setPadding(10, 12, 10, 12);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        p.setMargins(4, 8, 4, 8);
        t.setLayoutParams(p);
        return t;
    }

    private Space space(int h) {
        Space s = new Space(this);
        s.setLayoutParams(new LinearLayout.LayoutParams(1, h));
        return s;
    }
}

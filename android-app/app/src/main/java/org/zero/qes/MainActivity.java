package org.zero.qes;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Intent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.ProgressBar;

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
    private static final int REQ_SAVE_STREAM_ENCRYPT = 201;
    private static final int REQ_SAVE_STREAM_DECRYPT = 202;

    private static final byte[] COVER_BEGIN = "\n-----BEGIN QES COVER PAYLOAD V1-----\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] STREAM_MAGIC_V1 = "QES_STREAM_V1".getBytes(StandardCharsets.UTF_8);

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

    private final String appVersion = "0.12.1-alpha";
    private final String patchVersion = "P-2026-06-01-13-STREAM-VERIFY-ONLY";
    private final String buildStage = "QES ALFA PROTOTYP";

    private String appMode = "NORMÁLNÍ";
    private String uiMode = "STANDARD";
    private String testMode = "STANDARDNÍ";
    private String cryptoProfile = "QES CORE";
    private String aesMode = "AES-GCM";
    private String compressionMode = "VYPNUTO";
    private String interfaceLanguage = "Čeština";

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

    private boolean appShieldEnabled = true;
    private boolean secureScreenEnabled = true;
    private boolean noInternetMode = true;
    private boolean noTelemetryMode = true;
    private boolean noSecretLogging = true;
    private boolean clearClipboardPlanned = true;
    private boolean lockOnBackgroundPlanned = true;

    private boolean sideChannelGuardEnabled = true;
    private boolean constantTimeCompareEnabled = true;
    private boolean secretDependentProgressBlocked = true;
    private boolean secretDependentLoggingBlocked = true;
    private boolean earlyExitMacBlocked = true;
    private boolean hardenedExecutionMode = true;
    private boolean zeroLockEnabled = true;
    private boolean finalSealEnabled = true;
    private boolean payloadLockEnabled = true;
    private boolean capsuleBindingEnabled = true;
    private boolean modeBindingEnabled = true;
    private boolean versionBindingEnabled = true;
    private boolean zeroLockTamperDetection = true;
    private String zeroLockProfile = "STRICT";
    private boolean streamGuardEnabled = true;
    private boolean streamFileModePlanned = true;
    private boolean streamProgressByPublicBlocks = true;
    private boolean streamSecretTimingBlocked = true;
    private int streamBlockSizeBytes = 512 * 1024;
    private String streamBlockProfile = "512 KB";
    private String streamEngineStatus = "JAVA STREAM ALFA";



    private boolean deviceGuardEnabled = true;
    private String performanceMode = "BEZPEČNÝ";
    private int maxHeavyTestBytes = 1024 * 1024;
    private int maxHeavyTestSeconds = 30;
    private boolean operationRunning = false;
    private int progressPercent = 0;
    private String progressPhase = "Připraveno";
    private ProgressBar progressBar;
    private TextView progressLabel;

    private final StringBuilder log = new StringBuilder();

    private int BG, PANEL, CARD, FIELD, TEXT, MUTED, ACCENT, ACCENT2, GOOD, BAD;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setTitle("QES");
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
        );
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
        root.addView(progressPanel());

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

    private LinearLayout progressPanel() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(box(cyanBackDeep(), seaGreen()));
        box.setPadding(12, 10, 12, 10);

        progressLabel = text((operationRunning ? "⟳ " : "● ") + progressPhase + " · " + progressPercent + "%", 12, operationRunning ? ACCENT : MUTED, true);
        box.addView(progressLabel);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(progressPercent);
        progressBar.setIndeterminate(operationRunning && progressPercent <= 0);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 6, 0, 6);
        progressBar.setLayoutParams(p);
        box.addView(progressBar);

        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.setMargins(0, 8, 0, 10);
        box.setLayoutParams(bp);
        return box;
    }

    private void setProgressState(String phase, int percent) {
        progressPhase = phase;
        progressPercent = Math.max(0, Math.min(100, percent));
        runOnUiThread(() -> {
            if (progressLabel != null) {
                progressLabel.setText((operationRunning ? "⟳ " : "● ") + progressPhase + " · " + progressPercent + "%");
                progressLabel.setTextColor(operationRunning ? ACCENT : MUTED);
            }
            if (progressBar != null) {
                progressBar.setIndeterminate(operationRunning && progressPercent <= 0);
                progressBar.setProgress(progressPercent);
            }
            if (status != null) {
                status.setText(progressPhase + " · " + progressPercent + "%");
            }
        });
    }

    private void finishProgress(String phase) {
        operationRunning = false;
        setProgressState(phase, 100);
    }

    private void runGuardedOperation(String title, Runnable job) {
        if (operationRunning) {
            showErrorDialog("Operace už běží", "Počkej, až doběhne aktuální operace. Tím se chrání zařízení proti přetížení.");
            return;
        }

        operationRunning = true;
        setProgressState(title, 1);
        addLog("START: " + title);

        new Thread(() -> {
            try {
                job.run();
                addLog("HOTOVO: " + title);
                runOnUiThread(() -> finishProgress(title + " dokončeno"));
            } catch (Throwable e) {
                addLog("CHYBA " + title + ": " + e.getMessage());
                runOnUiThread(() -> {
                    operationRunning = false;
                    setProgressState("Chyba", 0);
                    showErrorDialog("Chyba: " + title, e.getMessage() == null ? "Neznámá chyba." : e.getMessage());
                });
            }
        }).start();
    }

    private boolean deviceGuardAllows(String name, int plannedBytes) {
        if (!deviceGuardEnabled) return true;

        Runtime rt = Runtime.getRuntime();
        long free = rt.freeMemory();
        long max = rt.maxMemory();

        if (plannedBytes > maxHeavyTestBytes) {
            showErrorDialog("Ochrana zařízení", name + " překračuje limit testu.\n\nLimit: " + maxHeavyTestBytes + " B\nPožadavek: " + plannedBytes + " B\n\nZměň režim výkonu v Nastavení.");
            return false;
        }

        if (free < 8L * 1024L * 1024L) {
            showErrorDialog("Nedostatek paměti", "Aplikace má málo volné paměti. Test zastaven, aby nespadl telefon.");
            return false;
        }

        return true;
    }

    private void showErrorDialog(String title, String message) {
        try {
            new AlertDialog.Builder(this)
                    .setTitle(title == null ? "Chyba" : title)
                    .setMessage(message == null ? "Neznámá chyba." : message)
                    .setPositiveButton("OK", null)
                    .show();
        } catch (Throwable ignored) {
            if (status != null) status.setText((title == null ? "Chyba" : title) + ": " + (message == null ? "" : message));
        }
    }

    private void showInfoDialog(String title, String message) {
        try {
            new AlertDialog.Builder(this)
                    .setTitle(title == null ? "Info" : title)
                    .setMessage(message == null ? "" : message)
                    .setPositiveButton("OK", null)
                    .show();
        } catch (Throwable ignored) {
            if (status != null) status.setText((title == null ? "Info" : title) + ": " + (message == null ? "" : message));
        }
    }

    private void controlTable(String title, String[][] rows) {
        TextView h = text(title, 13, ACCENT, true);
        h.setLetterSpacing(0.10f);
        h.setPadding(14, 12, 14, 12);
        h.setBackground(box(cyanBackDeep(), seaGreen()));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hp.setMargins(0, 18, 0, 4);
        h.setLayoutParams(hp);
        content.addView(h);

        LinearLayout table = new LinearLayout(this);
        table.setOrientation(LinearLayout.VERTICAL);
        table.setBackgroundColor(seaGreen());
        table.setPadding(borderPx(), borderPx(), borderPx(), borderPx());

        for (String[] row : rows) {
            LinearLayout r = new LinearLayout(this);
            r.setOrientation(LinearLayout.HORIZONTAL);
            r.setBackgroundColor(seaGreen());
            r.setPadding(0, 0, 0, borderPx());

            TextView left = tableCell(row.length > 0 ? row[0] : "", false);
            TextView right = tableCell(row.length > 1 ? row[1] : "", true);

            r.addView(left);
            r.addView(right);
            table.addView(r);
        }

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 4, 0, 12);
        table.setLayoutParams(p);
        content.addView(table);
    }

    private TextView tableCell(String value, boolean strong) {
        TextView t = text(value, 13, strong ? ACCENT : TEXT, strong);
        t.setBackgroundColor(cyanBack());
        t.setPadding(12, 10, 12, 10);
        t.setGravity(strong ? Gravity.CENTER : Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, strong ? 0.42f : 0.58f);
        p.setMargins(0, 0, borderPx(), 0);
        t.setLayoutParams(p);
        return t;
    }

    private String yesNo(boolean value) {
        return value ? "ZAPNUTO" : "VYPNUTO";
    }

    private void cycleLanguage() {
        if ("Čeština".equals(interfaceLanguage)) interfaceLanguage = "English";
        else if ("English".equals(interfaceLanguage)) interfaceLanguage = "Polski";
        else if ("Polski".equals(interfaceLanguage)) interfaceLanguage = "Русский";
        else if ("Русский".equals(interfaceLanguage)) interfaceLanguage = "日本語";
        else if ("日本語".equals(interfaceLanguage)) interfaceLanguage = "中文";
        else if ("中文".equals(interfaceLanguage)) interfaceLanguage = "Español";
        else if ("Español".equals(interfaceLanguage)) interfaceLanguage = "Deutsch";
        else if ("Deutsch".equals(interfaceLanguage)) interfaceLanguage = "Français";
        else interfaceLanguage = "Čeština";

        refreshSettings("interfaceLanguage=" + interfaceLanguage);
    }

    private String languageCode() {
        if ("Čeština".equals(interfaceLanguage)) return "CS";
        if ("English".equals(interfaceLanguage)) return "EN";
        if ("Polski".equals(interfaceLanguage)) return "PL";
        if ("Русский".equals(interfaceLanguage)) return "RU";
        if ("日本語".equals(interfaceLanguage)) return "JA";
        if ("中文".equals(interfaceLanguage)) return "ZH";
        if ("Español".equals(interfaceLanguage)) return "ES";
        if ("Deutsch".equals(interfaceLanguage)) return "DE";
        if ("Français".equals(interfaceLanguage)) return "FR";
        return "CS";
    }

    private void cyclePerformanceMode() {
        if ("BEZPEČNÝ".equals(performanceMode)) {
            performanceMode = "VYVÁŽENÝ";
            maxHeavyTestBytes = 10 * 1024 * 1024;
            maxHeavyTestSeconds = 60;
        } else if ("VYVÁŽENÝ".equals(performanceMode)) {
            performanceMode = "MAXIMÁLNÍ";
            maxHeavyTestBytes = 50 * 1024 * 1024;
            maxHeavyTestSeconds = 120;
        } else {
            performanceMode = "BEZPEČNÝ";
            maxHeavyTestBytes = 1024 * 1024;
            maxHeavyTestSeconds = 30;
        }
        refreshSettings("performanceMode=" + performanceMode);
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
        r3.addView(navButton("ZERO LOCK", "mac"));
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
        b.setBackground(page.equals(currentPage) ? boxAccent(ACCENT) : box(cyanBackDeep(), seaGreen()));
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

    private void refreshCurrentPageNoJump(String reason) {
        addLog("UI refresh bez skoku: " + reason);
        if (content != null) {
            content.removeAllViews();
            rebuildCurrentPage();
        } else {
            setContentView(app());
            rebuildCurrentPage();
        }
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
        card("ZERO LOCK", "ZERO LOCK je aktivní ochranný obal výstupu. Vytváří Final Seal nad payloadem, kapslí, režimem, verzí a metadaty.");
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

        controlTable("STREAM STATUS", new String[][]{
                {"Stream Guard", yesNo(streamGuardEnabled)},
                {"Velikost bloku", streamBlockProfile},
                {"Engine", streamEngineStatus},
                {"RAM režim", performanceMode},
                {"Progress", streamProgressByPublicBlocks ? "VEŘEJNÉ BLOKY" : "VYPNUTO"}
        });

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

        LinearLayout rStream = row();
        rStream.addView(action("STREAM ŠIFROVAT", v -> requestStreamEncryptSave()));
        rStream.addView(action("STREAM DEŠIFROVAT", v -> requestStreamDecryptSave()));
        content.addView(rStream);

        LinearLayout rStreamVerify = row();
        rStreamVerify.addView(action("STREAM OVĚŘIT .QES", v -> verifySelectedStreamQesOnly()));
        rStreamVerify.addView(action("STREAM POLICY", v -> showInfoDialog("QES Stream Guard", streamPolicyText())));
        content.addView(rStreamVerify);

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

        controlTable("COVER STREAM STATUS", new String[][]{
                {"Stream Guard", yesNo(streamGuardEnabled)},
                {"Velikost bloku", streamBlockProfile},
                {"Cover režim", "PAYLOAD + KAPSLE"},
                {"ZERO LOCK", yesNo(zeroLockEnabled)},
                {"Final Seal", yesNo(finalSealEnabled)}
        });

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

        controlTable("TEST LIMITS", new String[][]{
                {"Device Guard", yesNo(deviceGuardEnabled)},
                {"Performance", performanceMode},
                {"Max test", String.valueOf(maxHeavyTestBytes) + " B"},
                {"Stream block", streamBlockProfile},
                {"Operace běží", yesNo(operationRunning)}
        });
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
            card("App Shield a Side-Channel Guard",
                "App Shield chrání okolí aplikace: žádný internet, žádná telemetrie, secure screen, žádné tajné logy.\n\nSide-Channel Guard chrání provedení: progress jen podle veřejné délky, constant-time porovnání MAC/hash, zákaz early-exit kontroly a zákaz zobrazování tajných tras.");
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
        section("MAC / ZERO LOCK / FINAL SEAL");

        controlTable("ZERO LOCK STATUS", new String[][]{
                {"ZERO LOCK", yesNo(zeroLockEnabled)},
                {"Final Seal", yesNo(finalSealEnabled)},
                {"Stream Guard", yesNo(streamGuardEnabled)},
                {"Blok", streamBlockProfile},
                {"Payload Lock", yesNo(payloadLockEnabled)},
                {"Capsule Binding", yesNo(capsuleBindingEnabled)},
                {"Mode Binding", yesNo(modeBindingEnabled)},
                {"Version Binding", yesNo(versionBindingEnabled)},
                {"Tamper Detection", yesNo(zeroLockTamperDetection)},
                {"Profile", zeroLockProfile}
        });

        controlTable("POSLEDNÍ VÝSTUP", new String[][]{
                {"Režim", lastMode},
                {"Report", lastReport == null || lastReport.isEmpty() ? "NEVYTVOŘEN" : "VYTVOŘEN"},
                {"Kapsle", lastCapsule128 == null ? "NEVYTVOŘENA" : "VYTVOŘENA"},
                {"Payload", lastOutputBytes == null ? "NEULOŽEN" : String.valueOf(lastOutputBytes.length) + " B"}
        });

        card("ZERO LOCK POLICY", zeroLockPolicyText());

        LinearLayout r1 = row();
        r1.addView(action("ULOŽIT MAC REPORT", v -> saveReport()));
        r1.addView(action("ULOŽIT KAPSLI", v -> saveCapsule()));
        content.addView(r1);

        LinearLayout r2 = row();
        r2.addView(action("ZERO LOCK PROFIL", v -> cycleZeroLockProfile()));
        r2.addView(action("POLICY", v -> showInfoDialog("QES ZERO LOCK", zeroLockPolicyText())));
        content.addView(r2);

        if (lastReport != null && !lastReport.isEmpty()) {
            card("POSLEDNÍ SECURITY REPORT", lastReport);
        } else {
            card("POSLEDNÍ SECURITY REPORT", "Zatím nebyl vytvořen. Zašifruj text, soubor nebo cover.");
        }
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
                "\nVyžadovat MAC / TAG: " + on(requireMac) +
                "\nZastavit při chybě: " + on(stopOnMacError) +
                "\nDemo heslo povoleno: " + on(allowDemoPassword));

        LinearLayout secRow1 = row();
        secRow1.addView(action("HESLO", v -> { requirePassword = !requirePassword; refreshSettings("requirePassword=" + requirePassword); }));
        secRow1.addView(action("SEED", v -> { requireMainSeed = !requireMainSeed; refreshSettings("requireMainSeed=" + requireMainSeed); }));
        content.addView(secRow1);

        LinearLayout secRow2 = row();
        secRow2.addView(action("MAC / TAG", v -> { requireMac = !requireMac; refreshSettings("requireMac=" + requireMac); }));
        secRow2.addView(action("STOP PŘI CHYBĚ", v -> { stopOnMacError = !stopOnMacError; refreshSettings("stopOnMacError=" + stopOnMacError); }));
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
                "\n\nHeslo patří do KLÍČE. ART je profil / dlaždice / navigační doplněk.");

        LinearLayout artRow = row();
        artRow.addView(action("ART NAVIGACE", v -> { artAsNavigation = !artAsNavigation; refreshSettings("artAsNavigation=" + artAsNavigation); }));
        artRow.addView(action("ART DO KAPSLE", v -> { artSaveToCapsule = !artSaveToCapsule; refreshSettings("artSaveToCapsule=" + artSaveToCapsule); }));
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

        content.addView(action("KOMPRESNÍ PROFIL", v -> cycleCompression()));

        controlTable("APP SHIELD / SANDBOX", new String[][]{
                {"App Shield", yesNo(appShieldEnabled)},
                {"Internet permission", noInternetMode ? "VYPNUTO" : "POVOLENO"},
                {"Cleartext traffic", "ZAKÁZÁNO"},
                {"Telemetry", noTelemetryMode ? "VYPNUTO" : "POVOLENO"},
                {"Secure screen", yesNo(secureScreenEnabled)},
                {"Secret logging", noSecretLogging ? "ZAKÁZÁNO" : "POVOLENO"},
                {"Clipboard timeout", clearClipboardPlanned ? "PŘIPRAVENO" : "VYPNUTO"},
                {"Lock on background", lockOnBackgroundPlanned ? "PŘIPRAVENO" : "VYPNUTO"}
        });

        LinearLayout shieldRow = row();
        shieldRow.addView(action("APP SHIELD", v -> { appShieldEnabled = !appShieldEnabled; refreshSettings("appShieldEnabled=" + appShieldEnabled); }));
        shieldRow.addView(action("SECURE SCREEN", v -> { secureScreenEnabled = !secureScreenEnabled; refreshSettings("secureScreenEnabled=" + secureScreenEnabled); }));
        content.addView(shieldRow);

        controlTable("SIDE-CHANNEL GUARD", new String[][]{
                {"Side-Channel Guard", yesNo(sideChannelGuardEnabled)},
                {"Constant-time compare", yesNo(constantTimeCompareEnabled)},
                {"Secret progress", secretDependentProgressBlocked ? "ZAKÁZÁNO" : "POVOLENO"},
                {"Secret logging", secretDependentLoggingBlocked ? "ZAKÁZÁNO" : "POVOLENO"},
                {"Early-exit MAC", earlyExitMacBlocked ? "ZAKÁZÁNO" : "POVOLENO"},
                {"Hardened execution", yesNo(hardenedExecutionMode)}
        });

        LinearLayout sideRow1 = row();
        sideRow1.addView(action("SIDE GUARD", v -> { sideChannelGuardEnabled = !sideChannelGuardEnabled; refreshSettings("sideChannelGuardEnabled=" + sideChannelGuardEnabled); }));
        sideRow1.addView(action("CONST TIME", v -> { constantTimeCompareEnabled = !constantTimeCompareEnabled; refreshSettings("constantTimeCompareEnabled=" + constantTimeCompareEnabled); }));
        content.addView(sideRow1);

        LinearLayout sideRow2 = row();
        sideRow2.addView(action("HARDENED", v -> { hardenedExecutionMode = !hardenedExecutionMode; refreshSettings("hardenedExecutionMode=" + hardenedExecutionMode); }));
        sideRow2.addView(action("POLICY", v -> showInfoDialog("QES Side-Channel Guard", sideChannelPolicyText())));
        content.addView(sideRow2);

        content.addView(action("APP SHIELD POLICY", v -> showInfoDialog("QES App Shield", appShieldPolicyText())));

        controlTable("STREAM GUARD / BLOCK ENGINE", new String[][]{
                {"Stream Guard", yesNo(streamGuardEnabled)},
                {"Souborový stream", streamFileModePlanned ? "PŘIPRAVENO" : "VYPNUTO"},
                {"Velikost bloku", streamBlockProfile},
                {"Block bytes", String.valueOf(streamBlockSizeBytes)},
                {"Progress podle bloků", yesNo(streamProgressByPublicBlocks)},
                {"Secret timing", streamSecretTimingBlocked ? "ZAKÁZÁNO" : "POVOLENO"},
                {"Engine", streamEngineStatus}
        });

        LinearLayout streamRow1 = row();
        streamRow1.addView(action("STREAM GUARD", v -> { streamGuardEnabled = !streamGuardEnabled; refreshSettings("streamGuardEnabled=" + streamGuardEnabled); }));
        streamRow1.addView(action("VELIKOST BLOKU", v -> cycleStreamBlockSize()));
        content.addView(streamRow1);

        LinearLayout streamRow2 = row();
        streamRow2.addView(action("BLOCK PROGRESS", v -> { streamProgressByPublicBlocks = !streamProgressByPublicBlocks; refreshSettings("streamProgressByPublicBlocks=" + streamProgressByPublicBlocks); }));
        streamRow2.addView(action("STREAM POLICY", v -> showInfoDialog("QES Stream Guard", streamPolicyText())));
        content.addView(streamRow2);

        controlTable("OCHRANA ZAŘÍZENÍ", new String[][]{
                {"Device Guard", yesNo(deviceGuardEnabled)},
                {"Režim výkonu", performanceMode},
                {"Max test", String.valueOf(maxHeavyTestBytes) + " B"},
                {"Max čas", String.valueOf(maxHeavyTestSeconds) + " s"},
                {"Operace běží", yesNo(operationRunning)}
        });

        LinearLayout guardRow = row();
        guardRow.addView(action("DEVICE GUARD", v -> { deviceGuardEnabled = !deviceGuardEnabled; refreshSettings("deviceGuardEnabled=" + deviceGuardEnabled); }));
        guardRow.addView(action("REŽIM VÝKONU", v -> cyclePerformanceMode()));
        content.addView(guardRow);

        card("Testy",
                "Testovací režim: " + testMode +
                "\nRychlé = funkčnost. Standardní = roundtrip + MAC. Těžké = statistické indikátory. Extrémní = dlouhé testy pro pozdější verzi.");

        content.addView(action("TESTOVACÍ PROFIL", v -> cycleTestMode()));

        controlTable("JAZYK / LOCALIZATION", new String[][]{
                {"Aktuální jazyk", interfaceLanguage},
                {"Kód jazyka", languageCode()},
                {"Dostupné jazyky", "CS · EN · PL · RU · JA · ZH · ES · DE · FR"},
                {"Stav překladu", "Alfa slovník"}
        });

        content.addView(action("PŘEPNOUT JAZYK", v -> cycleLanguage()));

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
            String result = QesNative.selfTest();
            return result == null || result.isEmpty() ? "OK" : "OK";
        } catch (Throwable e) {
            return "FAILED";
        }
    }

    private String on(boolean value) {
        return value ? "ON" : "OFF";
    }

    private void refreshSettings(String msg) {
        addLog("Nastavení: " + msg);
        refreshCurrentPageNoJump(msg);
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
        navigationStatusCard();
    }

    private void addSeed() {
        saveKeyState();
        String next = "seed-" + (1 + countExtraSeeds() + 1) + "-" + shortHex(randomBytes(4));
        if (extraSeeds.trim().isEmpty()) extraSeeds = next;
        else extraSeeds = extraSeeds.trim() + "\n" + next;
        addLog("Přidán seed: " + next);
        refreshCurrentPageNoJump("rebuild");
    }

    private void clearExtraSeeds() {
        extraSeeds = "";
        addLog("Další seedy vyčištěny.");
        refreshCurrentPageNoJump("rebuild");
    }

    private int countExtraSeeds() {
        if (extraSeeds.trim().isEmpty()) return 0;
        return extraSeeds.trim().split("\\R+").length;
    }

    private void navigationStatusCard() {
        String passwordState = (pass == null || pass.trim().isEmpty()) ? "CHYBÍ" : "NASTAVENO";
        String seedState = (baseSeed == null || baseSeed.trim().isEmpty()) ? "CHYBÍ" : String.valueOf(1 + countExtraSeeds());

        controlTable("NAVIGACE AKTIVNÍ", new String[][]{
                {"Password", passwordState},
                {"Seedů", seedState},
                {"ART profil", artProfile},
                {"ART navigace", yesNo(artAsNavigation)},
                {"MAC / TAG", yesNo(requireMac)},
                {"Kapsle", yesNo(outputCapsule)},
                {"ZERO LOCK", yesNo(zeroLockEnabled)},
                {"Final Seal", yesNo(finalSealEnabled)},
                {"Stream Guard", yesNo(streamGuardEnabled)},
                {"Blok", streamBlockProfile},
                {"Kryptografický profil", cryptoProfile}
        });

        content.addView(action("ZMĚNIT V KLÍČI", v -> {
            currentPage = "key";
            setContentView(app());
            showKey();
        }));
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
        refreshCurrentPageNoJump("art");
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
        refreshCurrentPageNoJump("art");
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
            showErrorDialog("Chyba šifrování", e.getMessage());
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
            showErrorDialog("Chyba dešifrování", e.getMessage());
        }
    }

    private void requestStreamEncryptSave() {
        saveKeyState();
        if (!keyReady("Stream file encrypt")) return;
        if (secretUri == null) {
            showErrorDialog("Chybí soubor", "Nejdřív vyber vstupní soubor.");
            return;
        }
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/octet-stream");
        i.putExtra(Intent.EXTRA_TITLE, "encrypted_stream.qes");
        startActivityForResult(i, REQ_SAVE_STREAM_ENCRYPT);
    }

    private void requestStreamDecryptSave() {
        saveKeyState();
        if (!keyReady("Stream file decrypt")) return;
        if (qesUri == null) {
            showErrorDialog("Chybí .qes", "Nejdřív vyber streamový .qes soubor.");
            return;
        }
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/octet-stream");
        i.putExtra(Intent.EXTRA_TITLE, "decrypted_stream_output.bin");
        startActivityForResult(i, REQ_SAVE_STREAM_DECRYPT);
    }

    private void streamEncryptSelectedFile(Uri outputUri) {
        saveKeyState();
        if (!deviceGuardAllows("Stream šifrování", streamBlockSizeBytes)) return;

        runGuardedOperation("Stream šifrování souboru", () -> {
            try (InputStream is = getContentResolver().openInputStream(secretUri);
                 OutputStream os = getContentResolver().openOutputStream(outputUri)) {

                if (is == null) throw new IllegalStateException("Nelze otevřít vstupní soubor.");
                if (os == null) throw new IllegalStateException("Nelze otevřít výstupní soubor.");

                MessageDigest publicDigest = MessageDigest.getInstance("SHA-256");
                Mac streamMac = newStreamingMac("FILE-STREAM");

                os.write(STREAM_MAGIC_V1);
                writeInt(os, streamBlockSizeBytes);

                byte[] buffer = new byte[streamBlockSizeBytes];
                long blockIndex = 0;
                long plainTotal = 0;
                long cipherTotal = 0;

                setProgressState("Stream: čtení bloků", 1);

                int n;
                while ((n = is.read(buffer)) >= 0) {
                    if (n == 0) continue;

                    byte[] plainBlock = Arrays.copyOf(buffer, n);
                    String[] ds = derivedSeeds("FILE-STREAM-" + blockIndex);

                    byte[] encryptedBlock = QesNative.encryptBytes(
                            plainBlock,
                            pass,
                            ds[0],
                            ds[1],
                            ds[2],
                            ds[3],
                            glyph,
                            particleValue,
                            vector,
                            phase,
                            amplitude
                    );
                    throwIfNativeError(encryptedBlock);

                    writeInt(os, n);
                    writeInt(os, encryptedBlock.length);
                    os.write(encryptedBlock);

                    publicDigest.update(encryptedBlock);
                    streamMac.update(longToBytes(blockIndex));
                    streamMac.update(intToBytes(n));
                    streamMac.update(intToBytes(encryptedBlock.length));
                    streamMac.update(encryptedBlock);

                    blockIndex++;
                    plainTotal += n;
                    cipherTotal += encryptedBlock.length;

                    int pct = (int) Math.min(95, 1 + (blockIndex % 95));
                    setProgressState("Stream encrypt blok " + blockIndex + " · " + plainTotal + " B", pct);
                }

                writeInt(os, -1);
                writeLong(os, blockIndex);
                writeLong(os, plainTotal);
                writeLong(os, cipherTotal);

                byte[] publicHash = publicDigest.digest();
                byte[] finalMac = streamMac.doFinal();

                os.write(publicHash);
                os.write(finalMac);
                os.flush();

                lastMode = "FILE-STREAM";
                lastOutputBytes = null;
                lastCapsule128 = makeCapsule128("FILE-STREAM", concat(publicHash, finalMac));
                lastReport =
                        "QES STREAM SECURITY REPORT" +
                        "\nMODE: FILE-STREAM" +
                        "\nAPP_VERSION: " + appVersion +
                        "\nPATCH: " + patchVersion +
                        "\nSTREAM_BLOCK: " + streamBlockProfile +
                        "\nBLOCK_COUNT: " + blockIndex +
                        "\nPLAIN_SIZE: " + plainTotal + " B" +
                        "\nCIPHER_SIZE: " + cipherTotal + " B" +
                        "\nPUBLIC_SHA256: " + hex(publicHash) +
                        "\nSTREAM_MAC: " + hex(finalMac) +
                        "\nZERO_LOCK: " + yesNo(zeroLockEnabled) +
                        "\nFINAL_SEAL: " + zeroLockMacHex("FILE-STREAM", concat(publicHash, finalMac));

                addLog("Stream encrypt done: blocks=" + blockIndex + ", plain=" + plainTotal + " B, cipher=" + cipherTotal + " B");
                final long finalBlockIndex = blockIndex;
                runOnUiThread(() -> status.setText("Stream šifrování dokončeno. Bloků: " + finalBlockIndex));
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
    }


    private void verifySelectedStreamQesOnly() {
        saveKeyState();

        if (!keyReady("Stream verify only")) return;

        if (qesUri == null) {
            showErrorDialog("Chybí .qes", "Nejdřív vyber streamový .qes soubor.");
            return;
        }

        runGuardedOperation("Stream ověření .qes", () -> {
            try (InputStream is = getContentResolver().openInputStream(qesUri)) {
                if (is == null) throw new IllegalStateException("Nelze otevřít streamový .qes soubor.");

                byte[] magic = readFullyExact(is, STREAM_MAGIC_V1.length);
                if (!constantTimeEquals(magic, STREAM_MAGIC_V1)) {
                    throw new IllegalStateException("Neplatný QES stream formát.");
                }

                int storedBlockSize = readInt(is);
                if (storedBlockSize <= 0 || storedBlockSize > 8 * 1024 * 1024) {
                    throw new IllegalStateException("Neplatná velikost stream bloku.");
                }

                MessageDigest publicDigest = MessageDigest.getInstance("SHA-256");
                Mac streamMac = newStreamingMacWithBlockSize("FILE-STREAM", storedBlockSize);

                long blockIndex = 0;
                long plainTotal = 0;
                long cipherTotal = 0;

                setProgressState("Stream verify only: čtení bloků", 1);

                while (true) {
                    int plainLen = readInt(is);
                    if (plainLen == -1) break;

                    int encLen = readInt(is);
                    if (plainLen < 0 || encLen <= 0) {
                        throw new IllegalStateException("Poškozený stream blok.");
                    }

                    byte[] encryptedBlock = readFullyExact(is, encLen);

                    publicDigest.update(encryptedBlock);
                    streamMac.update(longToBytes(blockIndex));
                    streamMac.update(intToBytes(plainLen));
                    streamMac.update(intToBytes(encLen));
                    streamMac.update(encryptedBlock);

                    blockIndex++;
                    plainTotal += plainLen;
                    cipherTotal += encLen;

                    int pct = (int) Math.min(95, 1 + (blockIndex % 95));
                    setProgressState("Stream verify only blok " + blockIndex + " · " + cipherTotal + " B", pct);
                }

                long storedBlocks = readLong(is);
                long storedPlainTotal = readLong(is);
                long storedCipherTotal = readLong(is);

                byte[] storedHash = readFullyExact(is, 32);
                byte[] storedMac = readFullyExact(is, 32);

                byte[] publicHash = publicDigest.digest();
                byte[] finalMac = streamMac.doFinal();

                boolean ok =
                        storedBlocks == blockIndex &&
                        storedPlainTotal == plainTotal &&
                        storedCipherTotal == cipherTotal &&
                        constantTimeEquals(storedHash, publicHash) &&
                        constantTimeEquals(storedMac, finalMac);

                if (!ok) {
                    throw new IllegalStateException("Stream ověření selhalo: hash/MAC nebo metadata nesouhlasí.");
                }

                lastMode = "FILE-STREAM-VERIFY-ONLY";
                lastOutputBytes = null;
                lastCapsule128 = makeCapsule128("FILE-STREAM-VERIFY-ONLY", concat(publicHash, finalMac));
                lastReport =
                        "QES STREAM VERIFY-ONLY REPORT" +
                        "\nMODE: FILE-STREAM-VERIFY-ONLY" +
                        "\nAPP_VERSION: " + appVersion +
                        "\nPATCH: " + patchVersion +
                        "\nSTORED_BLOCK_SIZE: " + storedBlockSize +
                        "\nBLOCK_COUNT: " + blockIndex +
                        "\nPLAIN_SIZE: " + plainTotal + " B" +
                        "\nCIPHER_SIZE: " + cipherTotal + " B" +
                        "\nPUBLIC_SHA256: " + hex(publicHash) +
                        "\nSTREAM_MAC: " + hex(finalMac) +
                        "\nVERIFY_ONLY: OK" +
                        "\nWRITE_POLICY: NO_OUTPUT_WRITTEN";

                addLog("Stream verify-only OK: blocks=" + blockIndex + ", plain=" + plainTotal + " B, cipher=" + cipherTotal + " B");

                final long finalBlocks = blockIndex;
                runOnUiThread(() -> status.setText("Stream .qes ověřen bez zápisu výstupu. Bloků: " + finalBlocks));
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void streamDecryptSelectedFile(Uri outputUri) {
        saveKeyState();
        if (!deviceGuardAllows("Stream dešifrování", streamBlockSizeBytes)) return;

        runGuardedOperation("Stream verify-first dešifrování souboru", () -> {
            try {
                if (qesUri == null) throw new IllegalStateException("Chybí streamový .qes soubor.");
                if (outputUri == null) throw new IllegalStateException("Chybí výstupní soubor.");

                byte[] verifiedHash;
                byte[] verifiedMac;
                int storedBlockSize;
                long verifiedBlocks;
                long verifiedPlainTotal;
                long verifiedCipherTotal;

                // FÁZE 1: pouze ověření. Žádný plaintext se ještě nezapisuje.
                try (InputStream verifyInput = getContentResolver().openInputStream(qesUri)) {
                    if (verifyInput == null) throw new IllegalStateException("Nelze otevřít streamový .qes soubor.");

                    byte[] magic = readFullyExact(verifyInput, STREAM_MAGIC_V1.length);
                    if (!constantTimeEquals(magic, STREAM_MAGIC_V1)) {
                        throw new IllegalStateException("Neplatný QES stream formát.");
                    }

                    storedBlockSize = readInt(verifyInput);
                    if (storedBlockSize <= 0 || storedBlockSize > 8 * 1024 * 1024) {
                        throw new IllegalStateException("Neplatná velikost stream bloku.");
                    }

                    MessageDigest publicDigest = MessageDigest.getInstance("SHA-256");
                    Mac streamMac = newStreamingMacWithBlockSize("FILE-STREAM", storedBlockSize);

                    long blockIndex = 0;
                    long plainTotal = 0;
                    long cipherTotal = 0;

                    setProgressState("Stream verify-first: ověřování", 1);

                    while (true) {
                        int plainLen = readInt(verifyInput);
                        if (plainLen == -1) break;

                        int encLen = readInt(verifyInput);
                        if (plainLen < 0 || encLen <= 0) {
                            throw new IllegalStateException("Poškozený stream blok.");
                        }

                        byte[] encryptedBlock = readFullyExact(verifyInput, encLen);

                        publicDigest.update(encryptedBlock);
                        streamMac.update(longToBytes(blockIndex));
                        streamMac.update(intToBytes(plainLen));
                        streamMac.update(intToBytes(encLen));
                        streamMac.update(encryptedBlock);

                        blockIndex++;
                        plainTotal += plainLen;
                        cipherTotal += encLen;

                        int pct = (int) Math.min(45, 1 + (blockIndex % 45));
                        setProgressState("Stream verify blok " + blockIndex + " · " + cipherTotal + " B", pct);
                    }

                    long storedBlocks = readLong(verifyInput);
                    long storedPlainTotal = readLong(verifyInput);
                    long storedCipherTotal = readLong(verifyInput);

                    byte[] storedHash = readFullyExact(verifyInput, 32);
                    byte[] storedMac = readFullyExact(verifyInput, 32);

                    byte[] publicHash = publicDigest.digest();
                    byte[] finalMac = streamMac.doFinal();

                    boolean ok =
                            storedBlocks == blockIndex &&
                            storedPlainTotal == plainTotal &&
                            storedCipherTotal == cipherTotal &&
                            constantTimeEquals(storedHash, publicHash) &&
                            constantTimeEquals(storedMac, finalMac);

                    if (!ok) {
                        throw new IllegalStateException("Stream MAC / hash nesouhlasí. Dešifrování bylo zastaveno před zápisem výstupu.");
                    }

                    verifiedHash = publicHash;
                    verifiedMac = finalMac;
                    verifiedBlocks = blockIndex;
                    verifiedPlainTotal = plainTotal;
                    verifiedCipherTotal = cipherTotal;
                }

                // FÁZE 2: až po ověření se otevře výstup a zapisuje plaintext.
                try (InputStream decryptInput = getContentResolver().openInputStream(qesUri);
                     OutputStream os = getContentResolver().openOutputStream(outputUri)) {

                    if (decryptInput == null) throw new IllegalStateException("Nelze znovu otevřít streamový .qes soubor.");
                    if (os == null) throw new IllegalStateException("Nelze otevřít výstupní soubor.");

                    byte[] magic = readFullyExact(decryptInput, STREAM_MAGIC_V1.length);
                    if (!constantTimeEquals(magic, STREAM_MAGIC_V1)) {
                        throw new IllegalStateException("Neplatný QES stream formát při dešifrování.");
                    }

                    int blockSizeSecondPass = readInt(decryptInput);
                    if (blockSizeSecondPass != storedBlockSize) {
                        throw new IllegalStateException("Stream hlavička se mezi ověřením a dešifrováním změnila.");
                    }

                    long blockIndex = 0;
                    long plainTotal = 0;
                    long cipherTotal = 0;

                    setProgressState("Stream verify-first: dešifrování", 50);

                    while (true) {
                        int plainLen = readInt(decryptInput);
                        if (plainLen == -1) break;

                        int encLen = readInt(decryptInput);
                        if (plainLen < 0 || encLen <= 0) {
                            throw new IllegalStateException("Poškozený stream blok při dešifrování.");
                        }

                        byte[] encryptedBlock = readFullyExact(decryptInput, encLen);
                        String[] ds = derivedSeeds("FILE-STREAM-" + blockIndex);

                        byte[] plainBlock = QesNative.decryptBytes(
                                encryptedBlock,
                                pass,
                                ds[0],
                                ds[1],
                                ds[2],
                                ds[3],
                                glyph,
                                particleValue,
                                vector,
                                phase,
                                amplitude
                        );
                        throwIfNativeError(plainBlock);

                        if (plainBlock.length != plainLen) {
                            throw new IllegalStateException("Délka dešifrovaného bloku nesouhlasí.");
                        }

                        os.write(plainBlock);

                        blockIndex++;
                        plainTotal += plainBlock.length;
                        cipherTotal += encryptedBlock.length;

                        int pct = (int) Math.min(98, 50 + (blockIndex % 48));
                        setProgressState("Stream decrypt blok " + blockIndex + " · " + plainTotal + " B", pct);
                    }

                    long storedBlocks = readLong(decryptInput);
                    long storedPlainTotal = readLong(decryptInput);
                    long storedCipherTotal = readLong(decryptInput);
                    readFullyExact(decryptInput, 32);
                    readFullyExact(decryptInput, 32);

                    if (storedBlocks != verifiedBlocks ||
                        storedPlainTotal != verifiedPlainTotal ||
                        storedCipherTotal != verifiedCipherTotal ||
                        blockIndex != verifiedBlocks ||
                        plainTotal != verifiedPlainTotal ||
                        cipherTotal != verifiedCipherTotal) {
                        throw new IllegalStateException("Stream metadata nesouhlasí po druhém průchodu.");
                    }

                    os.flush();
                }

                lastMode = "FILE-STREAM-DECRYPTED";
                lastOutputBytes = null;
                lastCapsule128 = makeCapsule128("FILE-STREAM-DECRYPTED", concat(verifiedHash, verifiedMac));
                lastReport =
                        "QES STREAM VERIFY-FIRST REPORT" +
                        "\nMODE: FILE-STREAM-DECRYPTED" +
                        "\nAPP_VERSION: " + appVersion +
                        "\nPATCH: " + patchVersion +
                        "\nSTORED_BLOCK_SIZE: " + storedBlockSize +
                        "\nBLOCK_COUNT: " + verifiedBlocks +
                        "\nPLAIN_SIZE: " + verifiedPlainTotal + " B" +
                        "\nCIPHER_SIZE: " + verifiedCipherTotal + " B" +
                        "\nPUBLIC_SHA256: " + hex(verifiedHash) +
                        "\nSTREAM_MAC: " + hex(verifiedMac) +
                        "\nVERIFY_FIRST: OK" +
                        "\nWRITE_POLICY: WRITE_AFTER_VERIFY";

                addLog("Stream verify-first decrypt OK: blocks=" + verifiedBlocks + ", plain=" + verifiedPlainTotal + " B");
                final long finalBlockIndexDecrypt = verifiedBlocks;
                runOnUiThread(() -> status.setText("Stream verify-first dešifrování OK. Bloků: " + finalBlockIndexDecrypt));
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
    }

    private Mac newStreamingMac(String mode) throws Exception {
        return newStreamingMacWithBlockSize(mode, streamBlockSizeBytes);
    }

    private Mac newStreamingMacWithBlockSize(String mode, int blockSizeForMac) throws Exception {
        String[] ds = derivedSeeds(mode);
        String keyMaterial =
                "QES-STREAM-MAC|" +
                mode +
                "|" + pass +
                "|" + ds[0] +
                "|" + ds[1] +
                "|" + ds[2] +
                "|" + ds[3] +
                "|" + glyph +
                "|" + particleValue +
                "|" + vector +
                "|" + phase +
                "|" + amplitude +
                "|" + artProfile +
                "|" + zeroLockProfile;

        byte[] key = shaBytes(keyMaterial.getBytes(StandardCharsets.UTF_8));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        mac.update(mode.getBytes(StandardCharsets.UTF_8));
        mac.update((byte) 0);
        mac.update(STREAM_MAGIC_V1);
        mac.update(intToBytes(blockSizeForMac));
        return mac;
    }

    private void writeInt(OutputStream os, int value) throws Exception {

        os.write(intToBytes(value));
    }

    private void writeLong(OutputStream os, long value) throws Exception {
        os.write(longToBytes(value));
    }

    private int readInt(InputStream is) throws Exception {
        byte[] b = readFullyExact(is, 4);
        return ((b[0] & 255) << 24) |
               ((b[1] & 255) << 16) |
               ((b[2] & 255) << 8) |
               (b[3] & 255);
    }

    private long readLong(InputStream is) throws Exception {
        byte[] b = readFullyExact(is, 8);
        long v = 0;
        for (int i = 0; i < 8; i++) {
            v = (v << 8) | (b[i] & 255L);
        }
        return v;
    }

    private byte[] readFullyExact(InputStream is, int len) throws Exception {
        byte[] out = new byte[len];
        int p = 0;
        while (p < len) {
            int n = is.read(out, p, len - p);
            if (n < 0) throw new IllegalStateException("Neočekávaný konec streamu.");
            p += n;
        }
        return out;
    }

    private byte[] intToBytes(int value) {
        return new byte[]{
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value
        };
    }

    private byte[] longToBytes(long value) {
        return new byte[]{
                (byte) (value >>> 56),
                (byte) (value >>> 48),
                (byte) (value >>> 40),
                (byte) (value >>> 32),
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value
        };
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
            showErrorDialog("Chyba souborového šifrování", e.getMessage());
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
            showErrorDialog("Chyba souborového dešifrování", e.getMessage());
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
            showErrorDialog("Chyba cover režimu", e.getMessage());
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
            showErrorDialog("Chyba dešifrování coveru", e.getMessage());
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
        boolean hashOk = constantTimeHexEquals(verifyExpectedHash.getText().toString(), publicHash);
        boolean macOk = constantTimeHexEquals(verifyExpectedMac.getText().toString(), mac);
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

        String zeroLockSeal = zeroLockEnabled ? zeroLockMacHex(mode, output) : "ZERO_LOCK_DISABLED";

        lastReport =
                "QES SECURITY REPORT" +
                "\nMODE: " + mode +
                "\nSIZE: " + output.length + " B" +
                "\nAPP_VERSION: " + appVersion +
                "\nPATCH: " + patchVersion +
                "\nART_PROFILE: " + artProfile +
                "\nCRYPTO_PROFILE: " + cryptoProfile +
                "\nSEEDS_COUNT: " + (1 + countExtraSeeds()) +
                "\nPUBLIC_SHA256: " + publicHash +
                "\nKEYED_MAC: " + mac +
                "\nCAPSULE_128_SHA256: " + sha256(lastCapsule128) +
                "\nZERO_LOCK: " + yesNo(zeroLockEnabled) +
                "\nZERO_LOCK_PROFILE: " + zeroLockProfile +
                "\nFINAL_SEAL: " + zeroLockSeal +
                "\nPAYLOAD_LOCK: " + yesNo(payloadLockEnabled) +
                "\nCAPSULE_BINDING: " + yesNo(capsuleBindingEnabled) +
                "\nMODE_BINDING: " + yesNo(modeBindingEnabled) +
                "\nVERSION_BINDING: " + yesNo(versionBindingEnabled) +
                "\nTAMPER_DETECTION: " + yesNo(zeroLockTamperDetection);

        addLog("Security report created: mode=" + mode + ", sha256=" + publicHash + ", zeroLock=" + yesNo(zeroLockEnabled));
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

        int planned = "MAXIMÁLNÍ".equals(performanceMode) ? 50 * 1024 * 1024 : ("VYVÁŽENÝ".equals(performanceMode) ? 10 * 1024 * 1024 : 1024 * 1024);
        if (!deviceGuardAllows("Těžké testy", planned)) return;

        runGuardedOperation("Těžké testy", () -> {
            addLog("=== HEAVY TEST START ===");
            int[] sizes;
            if ("MAXIMÁLNÍ".equals(performanceMode)) {
                sizes = new int[]{1, 2, 5, 16, 64, 257, 1024, 4096, 16384, 65536, 262144};
            } else if ("VYVÁŽENÝ".equals(performanceMode)) {
                sizes = new int[]{1, 2, 5, 16, 64, 257, 1024, 4096, 16384};
            } else {
                sizes = new int[]{1, 2, 5, 16, 64, 257, 1024, 4096};
            }

            int ok = 0;
            int fail = 0;

            for (int i = 0; i < sizes.length; i++) {
                int size = sizes[i];
                setProgressState("Těžké testy: " + size + " B", Math.max(1, (i * 100) / sizes.length));

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
            int finalOk = ok;
            int finalFail = fail;
            runOnUiThread(() -> status.setText("Těžké testy dokončeny: OK=" + finalOk + " FAIL=" + finalFail));
        });
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
            } else if (request == REQ_SAVE_STREAM_ENCRYPT) {
                streamEncryptSelectedFile(uri);
            } else if (request == REQ_SAVE_STREAM_DECRYPT) {
                streamDecryptSelectedFile(uri);
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
        refreshCurrentPageNoJump("rebuild");
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

    private void cycleStreamBlockSize() {
        if ("256 KB".equals(streamBlockProfile)) {
            streamBlockProfile = "512 KB";
            streamBlockSizeBytes = 512 * 1024;
        } else if ("512 KB".equals(streamBlockProfile)) {
            streamBlockProfile = "1 MB";
            streamBlockSizeBytes = 1024 * 1024;
        } else {
            streamBlockProfile = "256 KB";
            streamBlockSizeBytes = 256 * 1024;
        }
        refreshSettings("streamBlockProfile=" + streamBlockProfile);
    }

    private String streamPolicyText() {
        return "QES Stream Guard:" +
                "\n\n1. Velké soubory se mají zpracovávat po blocích." +
                "\n2. Progress smí ukazovat jen veřejný počet bloků a zpracovanou velikost." +
                "\n3. Tajné trasy, seed, hidden IV ani interní permutace se nesmí zobrazovat." +
                "\n4. Cílem je, aby aplikace nedržela celý soubor v RAM." +
                "\n5. Aktuální alfa připravuje UI a limity. Další patch napojí stream engine přímo do Rust core.";
    }

    private String streamStatusText() {
        return "Stream Guard: " + yesNo(streamGuardEnabled) +
                "\nStream file mode: " + (streamFileModePlanned ? "PŘIPRAVENO" : "VYPNUTO") +
                "\nBlok: " + streamBlockProfile +
                "\nBlok bajtů: " + streamBlockSizeBytes +
                "\nProgress podle bloků: " + yesNo(streamProgressByPublicBlocks) +
                "\nSecret timing blocked: " + yesNo(streamSecretTimingBlocked) +
                "\nEngine: " + streamEngineStatus;
    }

    private String zeroLockMacHex(String mode, byte[] payload) throws Exception {
        byte[] capsule = lastCapsule128 == null ? makeCapsule128(mode, payload) : lastCapsule128;

        String policy =
                "ZERO_LOCK|" +
                "profile=" + zeroLockProfile +
                "|mode=" + mode +
                "|app=" + appVersion +
                "|patch=" + patchVersion +
                "|art=" + artProfile +
                "|crypto=" + cryptoProfile +
                "|len=" + payload.length +
                "|finalSeal=" + finalSealEnabled +
                "|payloadLock=" + payloadLockEnabled +
                "|capsuleBinding=" + capsuleBindingEnabled +
                "|modeBinding=" + modeBindingEnabled +
                "|versionBinding=" + versionBindingEnabled;

        byte[] policyBytes = policy.getBytes(StandardCharsets.UTF_8);
        return hex(hmacBytes("ZERO_LOCK_FINAL_SEAL|" + mode, concat(payload, capsule, policyBytes)));
    }

    private String zeroLockStateText() {
        return "ZERO LOCK: " + yesNo(zeroLockEnabled) +
                "\nFinal Seal: " + yesNo(finalSealEnabled) +
                "\nPayload Lock: " + yesNo(payloadLockEnabled) +
                "\nCapsule Binding: " + yesNo(capsuleBindingEnabled) +
                "\nMode Binding: " + yesNo(modeBindingEnabled) +
                "\nVersion Binding: " + yesNo(versionBindingEnabled) +
                "\nTamper Detection: " + yesNo(zeroLockTamperDetection) +
                "\nProfile: " + zeroLockProfile;
    }

    private String zeroLockPolicyText() {
        return "QES ZERO LOCK je ochranný obal výstupu." +
                "\n\nChrání vazbu mezi payloadem, kapslí, režimem, verzí, ART profilem, kryptografickým profilem a finální pečetí." +
                "\n\nZERO LOCK není magická neproniknutelná zeď. Je to kryptografická hranice integrity: pokud se změní payload, kapsle, režim nebo metadata, final seal nesouhlasí.";
    }

    private void cycleZeroLockProfile() {
        if ("STRICT".equals(zeroLockProfile)) zeroLockProfile = "BALANCED";
        else if ("BALANCED".equals(zeroLockProfile)) zeroLockProfile = "EXPERIMENTAL";
        else zeroLockProfile = "STRICT";
        refreshSettings("zeroLockProfile=" + zeroLockProfile);
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

    private String sanitizeLogMessage(String message) {
        if (message == null) return "";
        String m = message;
        if (noSecretLogging) {
            m = m.replaceAll("(?i)password\\s*=\\s*[^,;\\n ]+", "password=<hidden>");
            m = m.replaceAll("(?i)seed\\s*=\\s*[^,;\\n ]+", "seed=<hidden>");
            m = m.replaceAll("(?i)key\\s*=\\s*[^,;\\n ]+", "key=<hidden>");
            m = m.replaceAll("(?i)mac_key\\s*=\\s*[^,;\\n ]+", "mac_key=<hidden>");
            m = m.replaceAll("(?i)route_seed\\s*=\\s*[^,;\\n ]+", "route_seed=<hidden>");
            m = m.replaceAll("(?i)hidden_iv\\s*=\\s*[^,;\\n ]+", "hidden_iv=<hidden>");
        }
        return m;
    }

    private boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null) return false;

        int max = Math.max(a.length, b.length);
        int diff = a.length ^ b.length;

        for (int i = 0; i < max; i++) {
            byte av = i < a.length ? a[i] : 0;
            byte bv = i < b.length ? b[i] : 0;
            diff |= av ^ bv;
        }

        return diff == 0;
    }

    private boolean constantTimeHexEquals(String expected, String actual) {
        if (expected == null || expected.trim().isEmpty()) return true;
        if (actual == null) actual = "";

        byte[] a = expected.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        byte[] b = actual.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);

        if (!constantTimeCompareEnabled) {
            return expected.trim().equalsIgnoreCase(actual.trim());
        }

        return constantTimeEquals(a, b);
    }

    private String sideChannelPolicyText() {
        return "QES Side-Channel Guard pravidla:" +
                "\\n1. Progress ukazuje pouze veřejnou délku a počet bloků." +
                "\\n2. Progress nesmí ukazovat tajnou trasu, route seed ani hidden IV." +
                "\\n3. MAC/hash porovnání v Java vrstvě běží constant-time." +
                "\\n4. Log nesmí obsahovat heslo, seedy, MAC key ani tajné mezikroky." +
                "\\n5. Hardened režim nesmí měnit počet kroků podle tajného obsahu." +
                "\\n6. Experimentální labyrint může mít tajnější trasu, ale není doporučen pro ostré použití.";
    }

    private String appShieldPolicyText() {
        return "QES App Shield:" +
                "\\nInternet permission: VYPNUTO" +
                "\\nCleartext provoz: ZAKÁZÁNO" +
                "\\nTelemetry: VYPNUTO" +
                "\\nSecure screen: ZAPNUTO" +
                "\\nLogování tajných hodnot: ZAKÁZÁNO" +
                "\\nClipboard timeout: PŘIPRAVENO" +
                "\\nZámek při pozadí: PŘIPRAVENO";
    }

    private void addLog(String s) {
        String safe = sanitizeLogMessage(s);
        String ts = new SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(new Date());
        log.append("[").append(ts).append("] ").append(safe).append("\n");

        if (logBox != null) {
            runOnUiThread(() -> logBox.setText(log.toString()));
        }
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

    private int seaGreen() {
        return Color.rgb(46, 139, 87);
    }

    private int cyanBack() {
        return dark ? Color.rgb(4, 30, 34) : Color.rgb(226, 250, 252);
    }

    private int cyanBackDeep() {
        return dark ? Color.rgb(3, 18, 24) : Color.rgb(238, 253, 255);
    }

    private int borderPx() {
        float px = getResources().getDisplayMetrics().xdpi * 0.15f / 25.4f;
        return Math.max(1, Math.round(px));
    }

    private GradientDrawable box(int fill, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.RECTANGLE);
        g.setColor(fill);
        g.setStroke(borderPx(), stroke);
        g.setCornerRadius(0);
        return g;
    }

    private GradientDrawable boxAccent(int fill) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.RECTANGLE);
        g.setColor(fill);
        g.setStroke(Math.max(borderPx(), 1), ACCENT);
        g.setCornerRadius(0);
        return g;
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
        b.setBackground(boxAccent(ACCENT));
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
        b.setBackground(box(cyanBack(), seaGreen()));
        return b;
    }

    private EditText field(String hint, boolean secret, String value) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value);
        e.setSingleLine(true);
        e.setTextColor(TEXT);
        e.setHintTextColor(MUTED);
        e.setBackground(box(cyanBack(), seaGreen()));
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
        e.setBackground(box(cyanBack(), seaGreen()));
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
        v.setBackground(box(cyanBackDeep(), seaGreen()));
        v.setPadding(20, 20, 20, 20);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 8, 0, 12);
        v.setLayoutParams(p);
        content.addView(v);
    }

    private void section(String s) {
        TextView v = text(s, 13, ACCENT, true);
        v.setLetterSpacing(0.12f);
        v.setPadding(14, 12, 14, 12);
        v.setBackground(box(cyanBackDeep(), seaGreen()));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 18, 0, 8);
        v.setLayoutParams(p);
        content.addView(v);
    }

    private void card(String h, String b) {
        TextView v = text(h + "\n\n" + b, 15, TEXT, false);
        v.setBackground(box(cyanBack(), seaGreen()));
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
        t.setBackground(box(cyanBack(), seaGreen()));
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

    // ============================================================
    // QES VERIFY-FIRST STREAM HELPERS
    // P-2026-05-31-11-VERIFY-FIRST-PREP
    //
    // Smysl:
    // - nikdy nepřepisovat cílový soubor dřív, než projde ověření,
    // - dešifrovat nejdřív do dočasného souboru,
    // - po úspěchu atomicky nahradit výstup,
    // - při chybě dočasný soubor smazat,
    // - porovnání hash/MAC dělat constant-time stylem.
    //
    // Tyto helpery nesmí logovat hesla, seedy, route seed, hidden IV,
    // MAC key ani jiné tajné hodnoty.
    // ============================================================

    private boolean qesVerifyFirstStreamHelpersInstalled() {
        return true;
    }

    private java.io.File qesMakeTempOutputFile(java.io.File finalOutputFile) throws java.io.IOException {
        java.io.File parent = finalOutputFile.getParentFile();
        if (parent == null) {
            parent = new java.io.File(".");
        }

        String baseName = finalOutputFile.getName();
        if (baseName == null || baseName.trim().isEmpty()) {
            baseName = "qes-output";
        }

        java.io.File tmp = java.io.File.createTempFile(baseName + ".", ".qes.tmp", parent);
        return tmp;
    }

    private void qesDeleteQuietly(java.io.File file) {
        if (file == null) {
            return;
        }
        try {
            if (file.exists()) {
                boolean ignored = file.delete();
            }
        } catch (Throwable ignored) {
            // Záměrně nelogovat citlivé cesty ani tajné hodnoty.
        }
    }

    private void qesAtomicReplaceFile(java.io.File tempFile, java.io.File finalFile) throws java.io.IOException {
        if (tempFile == null || finalFile == null) {
            throw new java.io.IOException("QES atomic replace: chybí dočasný nebo cílový soubor.");
        }

        if (!tempFile.exists()) {
            throw new java.io.IOException("QES atomic replace: dočasný soubor neexistuje.");
        }

        java.nio.file.Path tempPath = tempFile.toPath();
        java.nio.file.Path finalPath = finalFile.toPath();

        try {
            java.nio.file.Files.move(
                tempPath,
                finalPath,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                java.nio.file.StandardCopyOption.ATOMIC_MOVE
            );
        } catch (java.nio.file.AtomicMoveNotSupportedException atomicMoveNotSupported) {
            java.nio.file.Files.move(
                tempPath,
                finalPath,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    private byte[] qesSha256FileBytes(java.io.File file) throws java.io.IOException {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[1024 * 256];

            try (java.io.InputStream in = new java.io.BufferedInputStream(new java.io.FileInputStream(file))) {
                int read;
                while ((read = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }

            return digest.digest();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new java.io.IOException("SHA-256 není dostupné v zařízení.", e);
        }
    }

    private String qesHex(byte[] data) {
        if (data == null) {
            return "";
        }

        char[] hex = new char[data.length * 2];
        final char[] alphabet = "0123456789abcdef".toCharArray();

        for (int i = 0; i < data.length; i++) {
            int v = data[i] & 0xff;
            hex[i * 2] = alphabet[v >>> 4];
            hex[i * 2 + 1] = alphabet[v & 0x0f];
        }

        return new String(hex);
    }

    private String qesSha256FileHex(java.io.File file) throws java.io.IOException {
        return qesHex(qesSha256FileBytes(file));
    }

    private boolean qesConstantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null) {
            return false;
        }

        int diff = a.length ^ b.length;
        int max = Math.max(a.length, b.length);

        for (int i = 0; i < max; i++) {
            byte av = i < a.length ? a[i] : 0;
            byte bv = i < b.length ? b[i] : 0;
            diff |= av ^ bv;
        }

        return diff == 0;
    }

    private byte[] qesHexToBytes(String hex) throws java.io.IOException {
        if (hex == null) {
            throw new java.io.IOException("QES hex decode: vstup je prázdný.");
        }

        String clean = hex.trim();
        if ((clean.length() & 1) != 0) {
            throw new java.io.IOException("QES hex decode: lichá délka.");
        }

        byte[] out = new byte[clean.length() / 2];

        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(clean.charAt(i * 2), 16);
            int lo = Character.digit(clean.charAt(i * 2 + 1), 16);

            if (hi < 0 || lo < 0) {
                throw new java.io.IOException("QES hex decode: neplatný znak.");
            }

            out[i] = (byte) ((hi << 4) | lo);
        }

        return out;
    }

    private boolean qesConstantTimeHexEquals(String expectedHex, String actualHex) throws java.io.IOException {
        return qesConstantTimeEquals(qesHexToBytes(expectedHex), qesHexToBytes(actualHex));
    }

    private void qesCopyStream(java.io.InputStream in, java.io.OutputStream out) throws java.io.IOException {
        byte[] buffer = new byte[1024 * 256];
        int read;

        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    private void qesEnsureReadableFile(java.io.File file) throws java.io.IOException {
        if (file == null) {
            throw new java.io.IOException("Soubor není vybraný.");
        }
        if (!file.exists()) {
            throw new java.io.IOException("Soubor neexistuje.");
        }
        if (!file.isFile()) {
            throw new java.io.IOException("Vybraná cesta není soubor.");
        }
        if (!file.canRead()) {
            throw new java.io.IOException("Soubor nelze číst.");
        }
    }

    private void qesEnsureWritableParent(java.io.File file) throws java.io.IOException {
        if (file == null) {
            throw new java.io.IOException("Výstupní soubor není určený.");
        }

        java.io.File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            boolean created = parent.mkdirs();
            if (!created && !parent.exists()) {
                throw new java.io.IOException("Nelze vytvořit výstupní složku.");
            }
        }

        if (parent != null && !parent.canWrite()) {
            throw new java.io.IOException("Do výstupní složky nelze zapisovat.");
        }
    }


}

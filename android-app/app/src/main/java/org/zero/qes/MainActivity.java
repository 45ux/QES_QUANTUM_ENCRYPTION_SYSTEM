package org.zero.qes;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private EditText input;
    private EditText output;
    private EditText password;
    private EditText seed1;
    private EditText seed2;
    private EditText seed3;
    private EditText seed4;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("QES");
        setContentView(buildUi());

        try {
            status.setText(QesNative.selfTest());
        } catch (Throwable t) {
            status.setText("Rust knihovna zatím není dostupná: " + t.getMessage());
        }
    }

    private ScrollView buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);
        root.setBackgroundColor(Color.rgb(5, 6, 10));

        root.addView(title("QES"));
        root.addView(subtitle("Quantum Encryption System · Android APK prototype"));
        root.addView(info("Prototyp plnohodnotné Android aplikace. UI běží nativně v Androidu. Šifrovací jádro běží v Rustu přes JNI. Bez serveru, bez WebView, bez ručního odkazu."));

        root.addView(section("Klíč a seedy"));
        password = field("Password", true, "");
        seed1 = field("Seed 1", false, "seed-111");
        seed2 = field("Seed 2", false, "seed-222");
        seed3 = field("Seed 3", false, "seed-333");
        seed4 = field("Seed 4", false, "seed-444");

        root.addView(password);
        root.addView(seed1);
        root.addView(seed2);
        root.addView(seed3);
        root.addView(seed4);

        root.addView(section("Textový režim"));
        input = area("Vstup: text nebo QES ASCII balík");
        output = area("Výstup");

        root.addView(input);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);

        buttons.addView(button("Šifrovat", this::encryptText));
        buttons.addView(button("Dešifrovat", this::decryptText));

        root.addView(buttons);
        root.addView(output);

        status = new TextView(this);
        status.setTextColor(Color.rgb(150, 255, 215));
        status.setTextSize(13);
        status.setPadding(0, 24, 0, 0);
        root.addView(status);

        root.addView(info("Další etapa: výběr souborů, ukládání .qes, QES-128 kapsle, public hash, keyed MAC a Adaptive Labyrinth Cover."));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        return scroll;
    }

    private void encryptText() {
        try {
            long started = System.currentTimeMillis();
            String result = QesNative.encryptText(
                    input.getText().toString(),
                    password.getText().toString(),
                    seed1.getText().toString(),
                    seed2.getText().toString(),
                    seed3.getText().toString(),
                    seed4.getText().toString(),
                    "X",
                    77,
                    "vektor",
                    13L,
                    9
            );
            output.setText(result);
            status.setText("Šifrování dokončeno za " + (System.currentTimeMillis() - started) + " ms · výstup " + result.length() + " znaků");
        } catch (Throwable t) {
            status.setText("Chyba šifrování: " + t.getMessage());
        }
    }

    private void decryptText() {
        try {
            long started = System.currentTimeMillis();
            String result = QesNative.decryptText(
                    input.getText().toString(),
                    password.getText().toString(),
                    seed1.getText().toString(),
                    seed2.getText().toString(),
                    seed3.getText().toString(),
                    seed4.getText().toString(),
                    "X",
                    77,
                    "vektor",
                    13L,
                    9
            );
            output.setText(result);
            status.setText("Dešifrování dokončeno za " + (System.currentTimeMillis() - started) + " ms");
        } catch (Throwable t) {
            status.setText("Chyba dešifrování: " + t.getMessage());
        }
    }

    private TextView title(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(Color.WHITE);
        view.setTextSize(32);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setLetterSpacing(0.08f);
        return view;
    }

    private TextView subtitle(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(Color.rgb(180, 190, 205));
        view.setTextSize(14);
        view.setPadding(0, 0, 0, 24);
        return view;
    }

    private TextView section(String text) {
        TextView view = new TextView(this);
        view.setText(text.toUpperCase());
        view.setTextColor(Color.rgb(150, 255, 215));
        view.setTextSize(13);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setLetterSpacing(0.12f);
        view.setPadding(0, 28, 0, 10);
        return view;
    }

    private TextView info(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(Color.rgb(210, 215, 225));
        view.setTextSize(14);
        view.setLineSpacing(4f, 1f);
        view.setPadding(0, 10, 0, 14);
        return view;
    }

    private EditText field(String hint, boolean passwordMode, String defaultValue) {
        EditText edit = new EditText(this);
        edit.setHint(hint);
        edit.setText(defaultValue);
        edit.setSingleLine(true);
        edit.setTextColor(Color.WHITE);
        edit.setHintTextColor(Color.rgb(120, 130, 145));
        edit.setBackgroundColor(Color.rgb(18, 21, 30));
        edit.setPadding(18, 12, 18, 12);
        if (passwordMode) {
            edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 6, 0, 6);
        edit.setLayoutParams(params);
        return edit;
    }

    private EditText area(String hint) {
        EditText edit = new EditText(this);
        edit.setHint(hint);
        edit.setMinLines(5);
        edit.setGravity(Gravity.TOP | Gravity.START);
        edit.setTextColor(Color.WHITE);
        edit.setHintTextColor(Color.rgb(120, 130, 145));
        edit.setBackgroundColor(Color.rgb(18, 21, 30));
        edit.setPadding(18, 18, 18, 18);
        edit.setTypeface(Typeface.MONOSPACE);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 6, 0, 12);
        edit.setLayoutParams(params);
        return edit;
    }

    private Button button(String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.rgb(5, 6, 10));
        button.setBackgroundColor(Color.rgb(150, 255, 215));
        button.setOnClickListener(v -> action.run());

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        params.setMargins(6, 6, 6, 12);
        button.setLayoutParams(params);
        return button;
    }
}

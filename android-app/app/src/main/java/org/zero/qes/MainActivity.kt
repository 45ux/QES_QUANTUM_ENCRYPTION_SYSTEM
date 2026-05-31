package org.zero.qes

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private lateinit var input: EditText
    private lateinit var output: EditText
    private lateinit var password: EditText
    private lateinit var seed1: EditText
    private lateinit var seed2: EditText
    private lateinit var seed3: EditText
    private lateinit var seed4: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "QES"
        setContentView(buildUi())
        runCatching { status.text = QesNative.selfTest() }
            .onFailure { status.text = "Rust knihovna zatím není vložena: ${it.message}" }
    }

    private fun buildUi(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.rgb(5, 6, 10))
        }

        root.addView(title("QES"))
        root.addView(subtitle("Quantum Encryption System · native Android starter"))
        root.addView(info("Prototyp plnohodnotné aplikace. UI běží nativně v Androidu. Šifrovací jádro je Rust core přes JNI. Bez serveru, bez odkazu, bez WebView."))

        password = field("Password", true, "")
        seed1 = field("Seed 1", false, "seed-111")
        seed2 = field("Seed 2", false, "seed-222")
        seed3 = field("Seed 3", false, "seed-333")
        seed4 = field("Seed 4", false, "seed-444")

        root.addView(section("Klíč a seedy"))
        root.addView(password)
        root.addView(seed1)
        root.addView(seed2)
        root.addView(seed3)
        root.addView(seed4)

        root.addView(section("Textový režim"))
        input = area("Vstup: text nebo QES ASCII balík")
        output = area("Výstup")
        root.addView(input)

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        buttons.addView(button("Šifrovat") { encryptText() })
        buttons.addView(button("Dešifrovat") { decryptText() })
        root.addView(buttons)
        root.addView(output)

        status = TextView(this).apply {
            setTextColor(Color.rgb(150, 255, 215))
            textSize = 13f
            setPadding(0, 24, 0, 0)
        }
        root.addView(status)

        root.addView(info("Další krok: napojení systémového výběru souborů, QES-128 kapsle, hash/MAC karta a Adaptive Labyrinth Cover. Tato verze je základní native skeleton pro APK."))
        return ScrollView(this).apply { addView(root) }
    }

    private fun encryptText() {
        val started = System.currentTimeMillis()
        val result = QesNative.encryptText(
            input.text.toString(), password.text.toString(), seed1.text.toString(), seed2.text.toString(),
            seed3.text.toString(), seed4.text.toString(), "X", 77, "vektor", 13L, 9
        )
        output.setText(result)
        status.text = "Šifrování dokončeno za ${System.currentTimeMillis() - started} ms · výstup ${result.length} znaků"
    }

    private fun decryptText() {
        val started = System.currentTimeMillis()
        val result = QesNative.decryptText(
            input.text.toString(), password.text.toString(), seed1.text.toString(), seed2.text.toString(),
            seed3.text.toString(), seed4.text.toString(), "X", 77, "vektor", 13L, 9
        )
        output.setText(result)
        status.text = "Dešifrování dokončeno za ${System.currentTimeMillis() - started} ms"
    }

    private fun title(text: String) = TextView(this).apply {
        this.text = text
        setTextColor(Color.WHITE)
        textSize = 32f
        typeface = Typeface.DEFAULT_BOLD
        letterSpacing = 0.08f
    }

    private fun subtitle(text: String) = TextView(this).apply {
        this.text = text
        setTextColor(Color.rgb(180, 190, 205))
        textSize = 14f
        setPadding(0, 0, 0, 24)
    }

    private fun section(text: String) = TextView(this).apply {
        this.text = text.uppercase()
        setTextColor(Color.rgb(150, 255, 215))
        textSize = 13f
        typeface = Typeface.DEFAULT_BOLD
        letterSpacing = 0.12f
        setPadding(0, 28, 0, 10)
    }

    private fun info(text: String) = TextView(this).apply {
        this.text = text
        setTextColor(Color.rgb(210, 215, 225))
        textSize = 14f
        setLineSpacing(4f, 1f)
        setPadding(0, 10, 0, 14)
    }

    private fun field(hint: String, passwordMode: Boolean, defaultValue: String) = EditText(this).apply {
        this.hint = hint
        setText(defaultValue)
        setSingleLine(true)
        setTextColor(Color.WHITE)
        setHintTextColor(Color.rgb(120, 130, 145))
        setBackgroundColor(Color.rgb(18, 21, 30))
        setPadding(18, 12, 18, 12)
        if (passwordMode) inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, 6, 0, 6)
        }
    }

    private fun area(hint: String) = EditText(this).apply {
        this.hint = hint
        minLines = 5
        gravity = Gravity.TOP or Gravity.START
        setTextColor(Color.WHITE)
        setHintTextColor(Color.rgb(120, 130, 145))
        setBackgroundColor(Color.rgb(18, 21, 30))
        setPadding(18, 18, 18, 18)
        typeface = Typeface.MONOSPACE
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, 6, 0, 12)
        }
    }

    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        setTextColor(Color.rgb(5, 6, 10))
        setBackgroundColor(Color.rgb(150, 255, 215))
        setOnClickListener { runCatching(action).onFailure { status.text = "Chyba: ${it.message}" } }
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            setMargins(6, 6, 6, 12)
        }
    }
}

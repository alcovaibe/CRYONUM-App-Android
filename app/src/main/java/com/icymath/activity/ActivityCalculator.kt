package com.icymath.activity

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.icymath.BuildConfig
import com.icymath.R
import com.icymath.managers.HistoryManager
import com.icymath.managers.PolicyManager
import com.icymath.managers.SystemUiManager
import com.icymath.managers.ThemeManager
import com.icymath.utils.SecurityUtils
import org.mariuszgromada.math.mxparser.Constant
import org.mariuszgromada.math.mxparser.Expression
import org.mariuszgromada.math.mxparser.License
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import java.util.regex.Pattern

class ActivityCalculator : AppCompatActivity() {

    // UI
    private lateinit var inputDisplay: EditText
    private lateinit var resultDisplay: TextView
    private var btnDegRad: Button? = null

    // State
    private var isInverted = false
    private var isRadians = true
    private var memoryValue = 0.0

    // Pref keys
    private val PREFS_NAME = "calc_prefs_v1"
    private val KEY_INPUT = "key_input"
    private val KEY_RESULT = "key_result"
    private val KEY_MEM = "key_mem"
    private val KEY_INV = "key_inv"
    private val KEY_RAD = "key_rad"

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        
        SecurityUtils.checkLock(this)

        // confirm non-commercial use of mXparser
        License.iConfirmNonCommercialUse("com.example.icymath")

        setContentView(R.layout.activity_calculator)

        SystemUiManager.applyEdgeToEdge(this)

        // find views
        inputDisplay = findViewById(R.id.inputDisplay)
        resultDisplay = findViewById(R.id.resultDisplay)

        val btnInv = findViewById<Button>(R.id.btnInv)
        btnDegRad = findViewById(R.id.btnDegRad)
        val btnMenu = findViewById<Button>(R.id.btnMenu)
        val btnEquals = findViewById<Button>(R.id.btnEquals)
        val btnC = findViewById<Button>(R.id.btnC)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnSwitchMode = findViewById<ImageButton>(R.id.btnSwitchMode)

        // Memory buttons
        val btnMC = findViewById<Button>(R.id.btnMC)
        val btnMPlus = findViewById<Button>(R.id.btnMPlus)
        val btnMMinus = findViewById<Button>(R.id.btnMMinus)
        val btnMR = findViewById<Button>(R.id.btnMR)

        // disable system soft keyboard
        try {
            inputDisplay.showSoftInputOnFocus = false
        } catch (ignored: Throwable) {
        }
        inputDisplay.isCursorVisible = true
        inputDisplay.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

        restoreState()

        btnBack?.setOnClickListener { finish() }

        btnSwitchMode?.setOnClickListener { toggleOrientation() }

        btnMenu?.setOnClickListener { Toast.makeText(this, "Menu pressed", Toast.LENGTH_SHORT).show() }

        btnInv?.setOnClickListener {
            isInverted = !isInverted
            updateInvButtonsText()
            saveState()
        }

        btnDegRad?.setOnClickListener {
            isRadians = !isRadians
            updateDegRadText()
            saveState()
        }

        btnC?.setOnClickListener {
            inputDisplay.setText("")
            resultDisplay.setText("")
            saveState()
        }

        btnEquals?.setOnClickListener {
            val raw = inputDisplay.text.toString()
            if (raw.trim().isEmpty()) {
                resultDisplay.text = ""
                return@setOnClickListener
            }
            try {
                val res = CalculatorEngine.evaluate(raw, isRadians)
                inputDisplay.setText(res)
                inputDisplay.setSelection(inputDisplay.text.length)
                resultDisplay.text = ""
                
                HistoryManager.addHistoryEntry(
                    this@ActivityCalculator,
                    com.icymath.items.HistoryItem(raw, res)
                )

                saveState()
            } catch (e: Exception) {
                resultDisplay.text = "Ошибка"
            }
        }

        btnMC?.setOnClickListener {
            memoryValue = 0.0
            Toast.makeText(this, "Memory cleared", Toast.LENGTH_SHORT).show()
            saveState()
        }
        btnMPlus?.setOnClickListener {
            val cur = tryParseDisplayToDouble(resultDisplay.text.toString(), inputDisplay.text)
            memoryValue += cur
            Toast.makeText(this, "M+ (added)", Toast.LENGTH_SHORT).show()
            saveState()
        }
        btnMMinus?.setOnClickListener {
            val cur = tryParseDisplayToDouble(resultDisplay.text.toString(), inputDisplay.text)
            memoryValue -= cur
            Toast.makeText(this, "M- (subtracted)", Toast.LENGTH_SHORT).show()
            saveState()
        }
        btnMR?.setOnClickListener {
            val memStr = CalculatorEngine.formatDoubleForDisplay(memoryValue)
            insertTextAtCursor(memStr)
        }

        // numeric buttons 0..9
        val numericIds = intArrayOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        )
        for (id in numericIds) {
            findViewById<View>(id)?.let { b ->
                if (b is Button) {
                    b.setOnClickListener { insertTextAtCursor(b.text.toString()) }
                }
            }
        }

        // operator and other direct buttons
        val directIds = intArrayOf(
            R.id.btnPlus, R.id.btnMinus, R.id.btnMultiply, R.id.btnDivide,
            R.id.btnParenLeft, R.id.btnParenRight, R.id.btnComma, R.id.btnPercent,
            R.id.btnPi, R.id.btnE, R.id.btnMod, R.id.btnFactorial
        )
        for (id in directIds) {
            findViewById<View>(id)?.let { v ->
                if (v is Button) {
                    v.setOnClickListener { insertTextAtCursor(v.text.toString()) }
                }
            }
        }

        // function buttons
        val funcIds = intArrayOf(
            R.id.btnSin, R.id.btnCos, R.id.btnTan, R.id.btnCot,
            R.id.btnLn, R.id.btnLog, R.id.btnRootN
        )
        for (id in funcIds) {
            findViewById<View>(id)?.let { v ->
                if (v is Button) {
                    v.setOnClickListener {
                        val mapping = getFunctionInsert(v.text.toString())
                        insertTextAtCursor(mapping)
                    }
                }
            }
        }

        updateInvButtonsText()
        updateDegRadText()

        intent.getStringExtra("expression")?.let { inputDisplay.setText(it) }
        intent.getStringExtra("result")?.let { resultDisplay.text = it }

        inputDisplay.onFocusChangeListener = View.OnFocusChangeListener { _, _ -> saveState() }
    }

    override fun onResume() {
        super.onResume()
        SecurityUtils.checkLock(this)
        checkPolicy()
    }

    private fun checkPolicy() {
        if (!PolicyManager.isPolicyAccepted(this)) {
            val currentVersion = PolicyManager.getAcceptedVersion(this)
            if (currentVersion == 0) {
                PolicyManager.showFirstLaunchDialog(this)
            } else {
                PolicyManager.showAcceptDialog(this)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        saveState()
    }

    private fun toggleOrientation() {
        requestedOrientation = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        
        window.decorView.postDelayed({
            if (!isFinishing) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }, 400)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun updateInvButtonsText() {
        findViewById<Button>(R.id.btnSin)?.text = if (isInverted) "asin" else "sin"
        findViewById<Button>(R.id.btnCos)?.text = if (isInverted) "acos" else "cos"
        findViewById<Button>(R.id.btnTan)?.text = if (isInverted) "atan" else "tan"
        findViewById<Button>(R.id.btnLn)?.text = if (isInverted) "exp" else "ln"
        findViewById<Button>(R.id.btnLog)?.text = if (isInverted) "10^" else "log"
        findViewById<Button>(R.id.btnCot)?.text = if (isInverted) "acot" else "cot"
        saveState()
    }

    private fun updateDegRadText() {
        btnDegRad?.text = if (isRadians) "Rad" else "Deg"
    }

    private fun getFunctionInsert(buttonLabel: String?): String {
        if (buttonLabel == null) return ""
        return when (buttonLabel) {
            "10^" -> "10^("
            "Root" -> "root("
            "√" -> "sqrt("
            else -> "$buttonLabel("
        }
    }

    private fun insertTextAtCursor(text: String) {
        val start = inputDisplay.selectionStart.coerceAtLeast(0)
        val end = inputDisplay.selectionEnd.coerceAtLeast(0)

        val a = start.coerceAtMost(end)
        val b = start.coerceAtLeast(end)

        var editable = inputDisplay.text
        if (editable == null) {
            editable = SpannableStringBuilder()
            inputDisplay.setText(editable)
        }

        editable.replace(a, b, text)

        val pos = a + text.length
        inputDisplay.setSelection(pos)

        saveState()
    }

    private fun tryParseDisplayToDouble(resultText: String?, inputText: CharSequence?): Double {
        var s = if (resultText.isNullOrBlank()) inputText?.toString() ?: "" else resultText
        s = s.replace(',', '.')
        return s.toDoubleOrNull() ?: 0.0
    }

    private fun formatDoubleForDisplay(value: Double): String {
        return CalculatorEngine.formatDoubleForDisplay(value)
    }

    private fun saveState() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_INPUT, inputDisplay.text.toString())
            putString(KEY_RESULT, resultDisplay.text.toString())
            putString(KEY_MEM, memoryValue.toString())
            putBoolean(KEY_INV, isInverted)
            putBoolean(KEY_RAD, isRadians)
            apply()
        }
    }

    private fun restoreState() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        inputDisplay.setText(prefs.getString(KEY_INPUT, ""))
        resultDisplay.text = prefs.getString(KEY_RESULT, "")
        memoryValue = prefs.getString(KEY_MEM, "0.0")?.toDoubleOrNull() ?: 0.0
        isInverted = prefs.getBoolean(KEY_INV, false)
        isRadians = prefs.getBoolean(KEY_RAD, true)
    }

    object CalculatorEngine {
        private var cachedLocale: Locale? = null
        private var _df: DecimalFormat? = null

        private fun getDf(): DecimalFormat {
            val currentLocale = Locale.getDefault()
            if (_df == null || cachedLocale != currentLocale) {
                cachedLocale = currentLocale
                val symbols = DecimalFormatSymbols(currentLocale).apply {
                    decimalSeparator = ','
                }
                _df = DecimalFormat("#.############", symbols).apply {
                    maximumFractionDigits = 12
                    isGroupingUsed = false
                }
            }
            return _df!!
        }

        init {
            try {
                Constant("pi", Math.PI)
                Constant("e", Math.E)
            } catch (ignored: Throwable) {
            }
        }

        @JvmStatic
        fun evaluate(rawExpression: String, radians: Boolean): String {
            if (BuildConfig.DEBUG) Log.d("CalcDebug", "Raw input: '$rawExpression'")
            val normalized = normalizeExpression(rawExpression)
            if (BuildConfig.DEBUG) Log.d("CalcDebug", "Normalized: '$normalized'")
            if (normalized.isBlank()) {
                throw IllegalArgumentException("Пустое выражение")
            }
            val exprForParser = if (radians) normalized else convertTrigToDegrees(normalized)
            if (BuildConfig.DEBUG) Log.d("CalcDebug", "Expr for parser: '$exprForParser'")

            val expr = Expression(exprForParser)
            val result = try {
                expr.calculate()
            } catch (ex: Exception) {
                if (BuildConfig.DEBUG) Log.e("CalcDebug", "Exception during calculate(): ${ex.message}", ex)
                Double.NaN
            }
            if (BuildConfig.DEBUG) Log.d("CalcDebug", "Raw result (double): $result")
            if (result.isNaN() || result.isInfinite()) {
                throw IllegalArgumentException("Не удалось посчитать выражение")
            }
            return formatResult(result)
        }

        private fun normalizeExpression(input: String?): String {
            if (input == null) return ""
            var s = input.trim()
            s = s.replace('\u00A0', ' ')
            s = s.replace("\u200B", "")

            s = s.replace('−', '-')
            s = s.replace("×", "*")
            s = s.replace("÷", "/")
            s = s.replace("·", "*")

            s = s.replace(',', '.')
            s = s.replace("π", "pi")

            val percentPattern = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)%")
            val m = percentPattern.matcher(s)
            val sb = StringBuffer()
            while (m.find()) {
                val num = m.group(1)
                m.appendReplacement(sb, "($num/100)")
            }
            m.appendTail(sb)
            s = sb.toString()

            s = s.replace("√", "sqrt")
            s = s.replace(Regex("[^0-9a-zA-Z.+\\-*/^()%!_,]"), "")

            return s
        }

        private fun convertTrigToDegrees(expr: String): String {
            var s = expr
            s = s.replace(Regex("(?i)sin\\("), "sin_rad(")
            s = s.replace(Regex("(?i)cos\\("), "cos_rad(")
            s = s.replace(Regex("(?i)tan\\("), "tan_rad(")

            s = s.replace(Regex("(?i)asin\\("), "asin_deg(")
            s = s.replace(Regex("(?i)acos\\("), "acos_deg(")
            s = s.replace(Regex("(?i)atan\\("), "atan_deg(")

            val defs = "sin_rad(x)=sin(x*pi/180);" +
                    "cos_rad(x)=cos(x*pi/180);" +
                    "tan_rad(x)=tan(x*pi/180);" +
                    "asin_deg(x)=asin(x)*180/pi;" +
                    "acos_deg(x)=acos(x)*180/pi;" +
                    "atan_deg(x)=atan(x)*180/pi;"

            return defs + s
        }

        private fun formatResult(value: Double): String {
            return getDf().format(value).replace('.', ',')
        }

        fun formatDoubleForDisplay(value: Double): String {
            return getDf().format(value).replace('.', ',')
        }
    }
}

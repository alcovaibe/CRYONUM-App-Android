package com.icymath.activity

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import com.icymath.BuildConfig
import com.icymath.R
import com.icymath.managers.HistoryManager
import com.icymath.managers.PolicyManager
import com.icymath.managers.SystemUiManager
import com.icymath.managers.ThemeManager
import com.icymath.ui.activity.CalculatorScreenBridge
import com.icymath.utils.SecurityUtils
import org.mariuszgromada.math.mxparser.Constant
import org.mariuszgromada.math.mxparser.Expression
import org.mariuszgromada.math.mxparser.License
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import java.util.regex.Pattern

class ActivityCalculator : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "calc_prefs_v1"
        private const val KEY_INPUT = "key_input"
        private const val KEY_RESULT = "key_result"
        private const val KEY_MEM = "key_mem"
        private const val KEY_INV = "key_inv"
        private const val KEY_RAD = "key_rad"
    }

    // State
    private val inputState = mutableStateOf("")
    private val resultState = mutableStateOf("")
    private val isInvertedState = mutableStateOf(false)
    private val isRadiansState = mutableStateOf(true)
    private var memoryValue = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        
        SecurityUtils.checkLock(this)

        // confirm non-commercial use of mXparser
        License.iConfirmNonCommercialUse("com.example.icymath")

        val composeView = ComposeView(this)
        setContentView(composeView)
        SystemUiManager.applyEdgeToEdge(this)

        restoreState()

        updateUi(composeView)

        intent.getStringExtra("expression")?.let { inputState.value = it }
        intent.getStringExtra("result")?.let { resultState.value = it }
    }

    private fun updateUi(composeView: ComposeView) {
        CalculatorScreenBridge.setCalculatorContent(
            composeView = composeView,
            input = inputState.value,
            result = resultState.value,
            isInverted = isInvertedState.value,
            isRadians = isRadiansState.value,
            onBackClick = { finish() },
            onToggleOrientation = { toggleOrientation() },
            onKeyClick = { key -> handleKeyClick(key, composeView) },
            onLaunchPolicyViewer = { isFirst ->
                PolicyManager.launchPolicyViewer(this, isFirstLaunchMode = isFirst)
            },
            onExitApp = { finishAffinity() }
        )
    }

    private fun handleKeyClick(key: String, composeView: ComposeView) {
        when (key) {
            "C", getString(R.string.Clean) -> {
                inputState.value = ""
                resultState.value = ""
            }
            "inv" -> {
                isInvertedState.value = !isInvertedState.value
            }
            "deg_rad" -> {
                isRadiansState.value = !isRadiansState.value
            }
            "=" -> {
                evaluateResult()
            }
            "MC" -> {
                memoryValue = 0.0
                Toast.makeText(this, "Memory cleared", Toast.LENGTH_SHORT).show()
            }
            "M+" -> {
                memoryValue += tryParseDisplayToDouble(resultState.value, inputState.value)
                Toast.makeText(this, "M+ (added)", Toast.LENGTH_SHORT).show()
            }
            "M-" -> {
                memoryValue -= tryParseDisplayToDouble(resultState.value, inputState.value)
                Toast.makeText(this, "M- (subtracted)", Toast.LENGTH_SHORT).show()
            }
            "MR" -> {
                val memStr = CalculatorEngine.formatDoubleForDisplay(memoryValue)
                inputState.value += memStr
            }
            "menu" -> {
                Toast.makeText(this, "Menu pressed", Toast.LENGTH_SHORT).show()
            }
            "ⁿ√" -> inputState.value += "root("
            getString(R.string.symbol_power2) -> inputState.value += "^2"
            getString(R.string.symbol_percent) -> inputState.value += "%"
            getString(R.string.module) -> inputState.value += "|"
            else -> {
                val toInsert = when(key) {
                    "sin", "cos", "tan", "cot", "asin", "acos", "atan", "acot", "ln", "log" -> "$key("
                    else -> key
                }
                inputState.value += toInsert
            }
        }
        saveState()
        updateUi(composeView)
    }

    private fun evaluateResult() {
        val raw = inputState.value
        if (raw.trim().isEmpty()) {
            resultState.value = ""
            return
        }
        try {
            val res = CalculatorEngine.evaluate(raw, isRadiansState.value)
            inputState.value = res
            resultState.value = ""
            
            HistoryManager.addHistoryEntry(
                this@ActivityCalculator,
                com.icymath.items.HistoryItem(raw, res)
            )

            saveState()
        } catch (e: Exception) {
            resultState.value = "Ошибка"
        }
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
                PolicyManager.requestFirstLaunchDialog()
            } else {
                PolicyManager.requestAcceptDialog()
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

    private fun tryParseDisplayToDouble(resultText: String, inputText: String): Double {
        var s = if (resultText.isBlank()) inputText else resultText
        s = s.replace(',', '.')
        return s.toDoubleOrNull() ?: 0.0
    }

    private fun saveState() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_INPUT, inputState.value)
            putString(KEY_RESULT, resultState.value)
            putString(KEY_MEM, memoryValue.toString())
            putBoolean(KEY_INV, isInvertedState.value)
            putBoolean(KEY_RAD, isRadiansState.value)
            apply()
        }
    }

    private fun restoreState() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        inputState.value = prefs.getString(KEY_INPUT, "") ?: ""
        resultState.value = prefs.getString(KEY_RESULT, "") ?: ""
        memoryValue = prefs.getString(KEY_MEM, "0.0")?.toDoubleOrNull() ?: 0.0
        isInvertedState.value = prefs.getBoolean(KEY_INV, false)
        isRadiansState.value = prefs.getBoolean(KEY_RAD, true)
    }

    private fun formatDoubleForDisplay(value: Double): String {
        return CalculatorEngine.formatDoubleForDisplay(value)
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
            s = s.replace(":", "/")
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

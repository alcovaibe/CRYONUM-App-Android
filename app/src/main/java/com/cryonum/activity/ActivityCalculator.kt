package com.cryonum.activity

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import androidx.core.content.edit
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import com.cryonum.R
import com.cryonum.calculator.CalcValue
import com.cryonum.calculator.CalculatorEngine
import com.cryonum.calculator.CalculatorViewModel
import com.cryonum.items.HistoryItem
import com.cryonum.managers.*
import com.cryonum.ui.activity.CalculatorScreen
import com.cryonum.ui.theme.CryonumTheme
import com.cryonum.utils.SecurityUtils

class ActivityCalculator : AppCompatActivity() {
    private val state: CalculatorViewModel by viewModels()
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.applyLocale(newBase, LocaleManager.getSavedLanguage(newBase)))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        SecurityUtils.checkLock(this)
        restoreState()
        val view = ComposeView(this)
        setContentView(view)
        SystemUiManager.applyEdgeToEdge(this)
        view.setContent {
            CryonumTheme {
                CalculatorScreen(
                    input = state.input,
                    result = if (state.busy) getString(R.string.calc_working) else state.result,
                    isInverted = state.inverted,
                    isRadians = state.radians,
                    onBackClick = { finish() },
                    onToggleOrientation = {
                        requestedOrientation = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    },
                    onKeyClick = ::key,
                    onLaunchPolicyViewer = { PolicyManager.launchPolicyViewer(this, isFirstLaunchMode = it) },
                    onExitApp = { finishAffinity() },
                    complexMode = state.complex,
                    fractionMode = state.fractions
                )
            }
        }
    }
    private fun key(key: String) {
        when (key) {
            "C", getString(R.string.Clean) -> { state.cancel(); state.input = ""; state.result = ""; state.completed = false }
            "cancel" -> state.cancel()
            else -> {
                if (state.busy) return
                when (key) {
                    "inv" -> state.inverted = !state.inverted
                    "deg_rad" -> { state.radians = !state.radians; state.completed = false }
                    "complex" -> { state.complex = !state.complex; state.completed = false }
                    "fraction" -> { state.fractions = !state.fractions; if (state.completed) state.result = state.ans.display(state.fractions) }
                    "=" -> if (!state.completed) calculate()
                    "MC" -> state.memory = CalcValue.ZERO
                    "M+", "M-" -> calculate(if (key == "M+") 1 else -1)
                    "MR" -> insert("(${state.memory.canonical()})")
                    "⌫" -> { state.input = state.input.dropLast(1); state.result = ""; state.completed = false }
                    else -> insert(when (key) {
                        "ⁿ√" -> "root("
                        getString(R.string.symbol_power2) -> "^2"
                        getString(R.string.module) -> "abs("
                        "sin", "cos", "tan", "cot", "asin", "acos", "atan", "acot", "ln", "log", "sqrt" -> "$key("
                        else -> key
                    })
                }
            }
        }
    }
    private fun insert(token: String) {
        val prefix = if (state.completed) {
            if (token.firstOrNull() in listOf('+', '-', '*', '/', ':', '×', '^', '%', '!')) "Ans" else ""
        } else state.input
        if (prefix.length + token.length > CalculatorEngine.MAX_INPUT) { state.result = getString(R.string.calc_limit); return }
        state.input = prefix + token; state.result = ""; state.completed = false
    }
    private fun calculate(memorySign: Int = 0) {
        // Application context only: the ViewModel may outlive this Activity on rotation.
        val app = applicationContext
        val radians = state.radians
        val complex = state.complex
        state.calculate(memorySign, onSuccess = { expression, result, previousAns ->
            HistoryManager.addHistoryEntry(app, HistoryItem(expression, result).copy(radians = radians, complex = complex, previousAns = previousAns))
        }, onError = { reason -> app.getString(when (reason) {
            "division_zero" -> R.string.calc_division_zero
            "complex_disabled" -> R.string.calc_complex_disabled
            "input_limit", "depth_limit", "number_limit", "time_limit" -> R.string.calc_limit
            "domain", "domain_or_overflow", "overflow", "underflow", "integer_required" -> R.string.calc_domain
            else -> R.string.calc_syntax
        }) })
    }
    override fun onResume() {
        super.onResume(); SecurityUtils.checkLock(this)
        if (!PolicyManager.isPolicyAccepted(this)) {
            if (PolicyManager.getAcceptedVersion(this) == 0) PolicyManager.requestFirstLaunchDialog() else PolicyManager.requestAcceptDialog()
        }
    }
    override fun onPause() { super.onPause(); saveState() }
    private fun saveState() {
        getSharedPreferences("calc_prefs_v2", MODE_PRIVATE).edit {
            putString("input", state.input).putString("result", state.result)
            putString("ans", state.ans.canonical()).putString("memory", state.memory.canonical())
            putBoolean("radians", state.radians).putBoolean("inverted", state.inverted)
            putBoolean("complex", state.complex).putBoolean("fractions", state.fractions)
            putBoolean("completed", state.completed)
        }
    }
    private fun restoreState() {
        if (state.restored) return
        state.restored = true
        val prefs = getSharedPreferences("calc_prefs_v2", MODE_PRIVATE)
        val legacy = getSharedPreferences("calc_prefs_v1", MODE_PRIVATE)
        state.input = prefs.getString("input", legacy.getString("key_input", "")).orEmpty().take(CalculatorEngine.MAX_INPUT)
        state.result = prefs.getString("result", "").orEmpty()
        state.radians = prefs.getBoolean("radians", true); state.inverted = prefs.getBoolean("inverted", false)
        state.complex = prefs.getBoolean("complex", false); state.fractions = prefs.getBoolean("fractions", true)
        var invalidSavedValue = false
        fun value(key: String) = runCatching { CalculatorEngine.evaluate(prefs.getString(key, "0").orEmpty(), complex = true) }.getOrElse { invalidSavedValue = true; CalcValue.ZERO }
        state.ans = value("ans"); state.memory = if (prefs.contains("memory")) value("memory") else runCatching { CalculatorEngine.evaluate("approx(${legacy.getString("key_mem", "0.0")})") }.getOrDefault(CalcValue.ZERO); state.completed = prefs.getBoolean("completed", false)
        if (invalidSavedValue) { state.input = ""; state.result = getString(R.string.calc_syntax); state.completed = false }
        intent.getStringExtra("expression")?.let { state.input = it.take(CalculatorEngine.MAX_INPUT); state.completed = false; state.result = ""
            state.radians = intent.getBooleanExtra("radians", true)
            state.complex = intent.getBooleanExtra("complex", false)
            state.ans = runCatching { CalculatorEngine.evaluate(intent.getStringExtra("previousAns") ?: "0", complex = true) }.getOrDefault(CalcValue.ZERO) }
    }
}

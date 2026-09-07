package com.cryonum.calculator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class CalculatorViewModel : ViewModel() {
    var input by mutableStateOf("")
    var result by mutableStateOf("")
    var radians by mutableStateOf(true)
    var inverted by mutableStateOf(false)
    var complex by mutableStateOf(false)
    var fractions by mutableStateOf(true)
    var busy by mutableStateOf(false)
        private set
    var ans = CalcValue.ZERO
    var memory = CalcValue.ZERO
    var completed = false
    var restored = false
    private var task: Job? = null
    private var cancelled = AtomicBoolean(false)
    fun cancel() { cancelled.set(true); task?.cancel(); busy = false }
    fun calculate(memorySign: Int = 0, onSuccess: (String, String, String) -> Unit, onError: (String) -> String) {
        if (busy) return
        val expression = if (memorySign != 0 && completed) "Ans" else input
        val useRadians = radians
        val useComplex = complex
        val previous = ans
        cancelled = AtomicBoolean(false)
        val stop = cancelled
        busy = true
        task = viewModelScope.launch {
            try {
                val value = withContext(Dispatchers.Default) { CalculatorEngine.evaluate(expression, useRadians, useComplex, previous) { stop.get() } }
                if (memorySign == 0) {
                    val historyExpression = expression
                    ans = value; completed = true; result = value.display(fractions)
                    onSuccess(historyExpression, result, previous.canonical())
                } else {
                    memory = if (memorySign > 0) memory + value else memory - value
                    result = value.display(fractions)
                }
            } catch (e: CalculationException) {
                completed = false
                result = onError(e.reason)
            } finally { if (cancelled === stop) busy = false }
        }
    }
    override fun onCleared() { cancel() }
}

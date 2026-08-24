package com.cryonum

import com.cryonum.activity.ActivityCalculator
import org.junit.Test
import org.junit.Assert.assertEquals

class CalculatorEngineTests {

    @Test
    fun testBasicArithmetic() {
        val r = ActivityCalculator.CalculatorEngine.evaluate("2+3*4", true)
        assertEquals("14", r.replace(',', '.'))
    }

    @Test
    fun testPowerAndRoots() {
        val r2 = ActivityCalculator.CalculatorEngine.evaluate("2^3", true)
        assertEquals("8", r2.replace(',', '.'))

        val r3 = ActivityCalculator.CalculatorEngine.evaluate("sqrt(9)", true)
        assertEquals("3", r3.replace(',', '.'))
    }

    @Test
    fun testTrigDegrees() {
        val r = ActivityCalculator.CalculatorEngine.evaluate("sin(30)", false)
        assertEquals("0.5", r.replace(',', '.'))
    }

    @Test
    fun testLogAndExp() {
        val r = ActivityCalculator.CalculatorEngine.evaluate("ln(e)", true)
        assertEquals("1", r.replace(',', '.'))

        val r2 = ActivityCalculator.CalculatorEngine.evaluate("log(10)", true)
        assertEquals("1", r2.replace(',', '.'))
    }

    @Test
    fun testPercent() {
        val r = ActivityCalculator.CalculatorEngine.evaluate("50%", true)
        assertEquals("0.5", r.replace(',', '.'))
    }
}

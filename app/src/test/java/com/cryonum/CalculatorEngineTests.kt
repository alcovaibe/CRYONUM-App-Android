package com.cryonum

import com.cryonum.calculator.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CancellationException

class CalculatorEngineTests {
    private fun exact(s: String, expected: String, complex: Boolean = false) {
        val v = CalculatorEngine.evaluate(s, complex = complex)
        assertTrue(s, v.exact); assertEquals(s, expected, v.display())
    }
    private fun near(s: String, expected: Double, radians: Boolean = true) {
        assertEquals(s, expected, CalculatorEngine.evaluate(s, radians).complex().real, maxOf(1e-14, kotlin.math.abs(expected) * 1e-12))
    }
    @Test fun testBasicArithmetic() { exact("2+3*4", "14"); exact("2(3+4)", "14"); exact("3:2", "3/2") }
    @Test fun testPowerAndRoots() { exact("2^3", "8"); exact("sqrt(9)", "3"); exact("-2^2", "-4"); exact("2^3^2", "512"); exact("2^-3", "1/8"); exact("(-2)^2", "4"); near("root(3;-8)", -2.0) }
    @Test fun testTrigDegrees() { near("sin(30)", 0.5, false); near("asin(0.5)",30.0,false); near("acos(0.5)",60.0,false); near("atan(1)",45.0,false); near("acot(-1)",135.0,false); near("cot(45)",1.0,false); near("sin(asin(0.5))",0.5,false) }
    @Test fun testLogAndExp() { near("ln(e)",1.0); near("log(10)",1.0); near("log(2;8)",3.0); near("exp(0)",1.0) }
    @Test fun testPercent() { exact("50%","1/2"); exact("200+10%","220"); exact("200-10%","180"); exact("200+10%+10%","242"); exact("200+10%*2","1001/5"); exact("200*(5+5)%","20"); exact("200+(10%)","220") }
    @Test fun exactFractionsAndTinyValues() { exact("1/3+1/6","1/2"); exact("0.1+0.2","3/10"); exact("1E-20*1E20","1"); exact("1e-20","1/100000000000000000000"); exact("9007199254740993-9007199254740992","1"); exact("-0","0") }
    @Test fun complexArithmetic() { exact("i^2","-1",true); exact("sqrt(-4)","0+2i",true); exact("(1+i)/(1-i)","0+1i",true); val v=CalculatorEngine.evaluate("ln(-1)",complex=true).complex(); assertEquals(0.0,v.real,1e-14);assertEquals(Math.PI,v.imaginary,1e-14) }
    @Test fun modulusFactorialAndNestedFunctions() { exact("|-3|","3"); exact("abs(-abs(-3))","3"); exact("0!","1");exact("5!","120"); exact("sqrt(4/9)","2/3");near("cos(0)",1.0);near("tan(0)",0.0) }
    @Test fun invalidInputIsRejected() { for(s in listOf("2\u00003","1\u0000","1 2","2@3","2..3","1/0","0^0","sqrt(-1)","i","3+","sin(","log(1;2)","(-1)!","3.5!","50%%","|3","2\u200b3","ln(0)","sin(1;2)","root(0;2)","1E999999999")) assertThrows(s,CalculationException::class.java) {CalculatorEngine.evaluate(s)} }
    @Test fun fullPrecisionPersistence() { for(s in listOf("1E-300", "200!", "1/(2^1000)", "sin(1)+cos(2)*i")) { val v=CalculatorEngine.evaluate(s,complex=true);val restored=CalculatorEngine.evaluate(v.canonical(),complex=true);assertEquals(v.display(),restored.display()) } }
    @Test fun noRoundingFeedback() { val a=CalculatorEngine.evaluate("1/3");a.display(false);exact("1/3*3","1");assertEquals("1",CalculatorEngine.evaluate("Ans*3",ans=a).display());val b=CalculatorEngine.evaluate("sin(1)");assertEquals(b.complex().real,CalculatorEngine.evaluate(b.canonical(),complex=true).complex().real,0.0) }
    @Test fun boundsAndCancellation() { for(s in listOf("(".repeat(40)+"1"+")".repeat(40),"2^10000","9".repeat(1025),"201!")) assertThrows(CalculationException::class.java){CalculatorEngine.evaluate(s)};assertThrows(CancellationException::class.java){CalculatorEngine.evaluate("1+1",cancelled={true})} }
    @Test fun polesAndDomains() { assertThrows(CalculationException::class.java){CalculatorEngine.evaluate("tan(90)",false)};assertThrows(CalculationException::class.java){CalculatorEngine.evaluate("cot(0)")};assertThrows(CalculationException::class.java){CalculatorEngine.evaluate("asin(2)")} }
}

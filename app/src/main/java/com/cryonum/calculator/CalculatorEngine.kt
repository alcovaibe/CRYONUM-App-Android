package com.cryonum.calculator

import org.apache.commons.math3.complex.Complex
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import java.util.concurrent.CancellationException
import kotlin.math.*

class CalculationException(val reason: String) : IllegalArgumentException(reason)
private fun valid(ok: Boolean, reason: String) { if (!ok) throw CalculationException(reason) }

/** Reduced exact fraction. Limits apply to intermediates, not only displayed results. */
class Rational private constructor(val n: BigInteger, val d: BigInteger) {
    operator fun plus(b: Rational) = of(n * b.d + b.n * d, d * b.d)
    operator fun minus(b: Rational) = of(n * b.d - b.n * d, d * b.d)
    operator fun times(b: Rational) = of(n * b.n, d * b.d)
    operator fun div(b: Rational) = of(n * b.d, d * b.n)
    operator fun unaryMinus() = of(-n, d)
    fun doubleValue(): Double = BigDecimal(n).divide(BigDecimal(d), MathContext.DECIMAL128).toDouble()
    override fun toString() = if (d == BigInteger.ONE) "$n" else "$n/$d"
    companion object {
        const val MAX_BITS = 2048
        val ZERO = of(BigInteger.ZERO)
        val ONE = of(BigInteger.ONE)
        fun of(n: BigInteger, d: BigInteger = BigInteger.ONE): Rational {
            valid(d.signum() != 0, "division_zero")
            valid(n.bitLength() <= MAX_BITS && d.bitLength() <= MAX_BITS, "number_limit")
            val g = n.gcd(d) * BigInteger.valueOf(d.signum().toLong())
            return Rational(n / g, d / g)
        }
        fun parse(s: String): Rational {
            val decimal = try { BigDecimal(s) } catch (_: NumberFormatException) { throw CalculationException("syntax") }
            valid(abs(decimal.scale().toLong()) <= 1000 && decimal.precision() <= 1000, "number_limit")
            return if (decimal.scale() >= 0) of(decimal.unscaledValue(), BigInteger.TEN.pow(decimal.scale()))
            else of(decimal.unscaledValue() * BigInteger.TEN.pow(-decimal.scale()))
        }
    }
}

/** Exact Gaussian rational, or an explicitly approximate complex value. */
class CalcValue private constructor(val re: Rational?, val im: Rational?, private val approximate: Complex?) {
    val exact get() = re != null
    val realOnly get() = if (exact) im!!.n.signum() == 0 else approximate!!.imaginary == 0.0
    val zero get() = if (exact) re!!.n.signum() == 0 && im!!.n.signum() == 0 else approximate == Complex.ZERO
    fun complex(): Complex {
        val z = approximate ?: Complex(re!!.doubleValue(), im!!.doubleValue())
        valid(!z.isNaN && !z.isInfinite, "overflow")
        if (exact) valid((re!!.n.signum() == 0 || z.real != 0.0) && (im!!.n.signum() == 0 || z.imaginary != 0.0), "underflow")
        return z
    }
    operator fun plus(b: CalcValue): CalcValue = if (exact && b.exact) exact(re!! + b.re!!, im!! + b.im!!) else approx(complex().add(b.complex()))
    operator fun minus(b: CalcValue) = this + (-b)
    operator fun unaryMinus(): CalcValue = if (exact) exact(-re!!, -im!!) else approx(complex().negate())
    operator fun times(b: CalcValue): CalcValue = if (exact && b.exact) exact(re!! * b.re!! - im!! * b.im!!, re * b.im + im * b.re) else { val z = complex().multiply(b.complex()); valid(z != Complex.ZERO || zero || b.zero, "underflow"); approx(z) }
    operator fun div(b: CalcValue): CalcValue {
        valid(!b.zero, "division_zero")
        return if (exact && b.exact) {
            val denominator = b.re!! * b.re + b.im!! * b.im
            exact((re!! * b.re + im!! * b.im) / denominator, (im * b.re - re * b.im) / denominator)
        } else { val z = complex().divide(b.complex()); valid(z != Complex.ZERO || zero, "underflow"); approx(z) }
    }
    fun canonical(): String = if (exact) { if (realOnly) "($re)" else "($re)+($im)*i" } else { if (realOnly) "approx(${approximate!!.real})" else "approx(${approximate!!.real})+approx(${approximate.imaginary})*i" }
    fun display(fractions: Boolean = true): String {
        fun fraction(r: Rational): String = if (fractions) r.toString() else readable(BigDecimal(r.n).divide(BigDecimal(r.d), DISPLAY))
        val a = if (exact) fraction(re!!) else decimal(approximate!!.real)
        val b = if (exact) fraction(im!!) else decimal(approximate!!.imaginary)
        val negative = if (exact) im!!.n.signum() < 0 else approximate!!.imaginary < 0
        val body = if (realOnly) a else "$a${if (negative) "" else "+"}${b}i"
        return if (!exact || !fractions) "≈ $body" else body
    }
    companion object {
        private val DISPLAY = MathContext(12, RoundingMode.HALF_UP)
        private fun readable(v: BigDecimal): String { val n = v.stripTrailingZeros(); return if (n.abs() >= BigDecimal("0.000001") && n.abs() < BigDecimal("1E12")) n.toPlainString() else n.toString() }
        private fun decimal(d: Double) = if (d == 0.0) "0" else readable(BigDecimal.valueOf(d).round(DISPLAY))
        val ZERO = exact(Rational.ZERO)
        val ONE = exact(Rational.ONE)
        val I = exact(Rational.ZERO, Rational.ONE)
        fun exact(re: Rational, im: Rational = Rational.ZERO) = CalcValue(re, im, null)
        fun integer(n: Int) = exact(Rational.of(BigInteger.valueOf(n.toLong())))
        fun approx(z: Complex): CalcValue {
            valid(!z.isNaN && !z.isInfinite, "domain_or_overflow")
            return CalcValue(null, null, Complex(if (z.real == 0.0) 0.0 else z.real, if (z.imaginary == 0.0) 0.0 else z.imaginary))
        }
        fun approx(x: Double) = approx(Complex(x))
    }
}

/** Small calculator grammar. No assignments, scripts, recursion or user-defined functions. */
object CalculatorEngine {
    const val MAX_INPUT = 4096
    const val MAX_DEPTH = 32
    fun evaluate(raw: String, radians: Boolean = true, complex: Boolean = false, ans: CalcValue = CalcValue.ZERO, cancelled: () -> Boolean = { false }): CalcValue =
        Parser(raw, radians, complex, ans, cancelled).parse()

    private data class Node(val value: CalcValue, val percent: Boolean = false)
    private class Parser(raw: String, val radians: Boolean, val allowComplex: Boolean, val ans: CalcValue, val cancelled: () -> Boolean) {
        val text = raw.replace('−', '-').replace('×', '*').replace('·', '*').replace('÷', '/').replace(':', '/').replace(',', '.').replace("π", "pi").replace("√", "sqrt")
        var p = 0
        var depth = 0
        val deadline = System.nanoTime() + 2_000_000_000L
        init { valid(raw.isNotBlank(), "empty"); valid(raw.length <= MAX_INPUT, "input_limit") }
        fun tick() {
            if (cancelled() || Thread.currentThread().isInterrupted) throw CancellationException("cancelled")
            valid(System.nanoTime() < deadline, "time_limit")
        }
        fun skip() { while (p < text.length && text[p].isWhitespace()) p++ }
        fun take(c: Char): Boolean { skip(); return if (p < text.length && text[p] == c) { p++; true } else false }
        fun peek(): Char { skip(); return text.getOrElse(p) { '\u0000' } }
        fun parse(): CalcValue {
            val v = sum().value
            skip()
            valid(p == text.length, "syntax")
            valid(allowComplex || v.realOnly, "complex_disabled")
            return v
        }
        fun sum(): Node {
            tick(); depth++; valid(depth <= MAX_DEPTH, "depth_limit")
            try {
                var a = product()
                while (true) {
                    val op = peek(); if (op != '+' && op != '-') return a
                    p++; val b = product(); val right = if (b.percent) a.value * b.value else b.value
                    a = Node(if (op == '+') a.value + right else a.value - right)
                }
            } finally { depth-- }
        }
        fun product(): Node {
            var a = unary()
            while (true) {
                val op = peek()
                val implicit = op == '(' || op.isLetter() && op != '\u0000'
                if (op != '*' && op != '/' && !implicit) return a
                if (!implicit) p++
                val b = unary()
                a = Node(if (op == '/') a.value / b.value else a.value * b.value)
            }
        }
        fun unary(): Node {
            tick(); depth++; valid(depth <= MAX_DEPTH, "depth_limit")
            try {
                if (take('+')) return unary()
                if (take('-')) { val n = unary(); return Node(-n.value, n.percent) }
                return power()
            } finally { depth-- }
        }
        fun power(): Node {
            val a = postfix()
            return if (take('^')) Node(pow(a.value, unary().value)) else a
        }
        fun postfix(): Node {
            var a = atom()
            while (true) {
                a = when {
                    take('%') -> { valid(!a.percent, "syntax"); Node(a.value / CalcValue.integer(100), true) }
                    take('!') -> {
                        val n = integer(a.value, 0, 200)
                        var v = CalcValue.ONE
                        for (i in 2..n) { tick(); v *= CalcValue.integer(i) }
                        Node(v)
                    }
                    else -> return a
                }
            }
        }
        fun atom(): Node {
            tick()
            if (take('(')) { val a = sum(); valid(take(')'), "parenthesis"); return a }
            if (take('|')) { val a = sum(); valid(take('|'), "parenthesis"); return Node(absolute(a.value)) }
            skip(); val start = p
            if (peek().isDigit() || peek() == '.') {
                while (p < text.length && text[p] in '0'..'9') p++
                if (p < text.length && text[p] == '.') { p++; while (p < text.length && text[p] in '0'..'9') p++ }
                if (p < text.length && text[p] in "Ee" && p + 1 < text.length && (text[p + 1].isDigit() || text[p + 1] in "+-")) {
                    p++; if (p < text.length && text[p] in "+-") p++
                    val exp = p; while (p < text.length && text[p] in '0'..'9') p++
                    valid(p > exp, "syntax")
                }
                valid(p > start, "syntax")
                return Node(CalcValue.exact(Rational.parse(text.substring(start, p))))
            }
            while (p < text.length && text[p] in 'a'..'z' || p < text.length && text[p] in 'A'..'Z') p++
            val name = text.substring(start, p)
            when (name) {
                "pi" -> return Node(CalcValue.approx(Math.PI))
                "e" -> return Node(CalcValue.approx(Math.E))
                "i" -> { valid(allowComplex, "complex_disabled"); return Node(CalcValue.I) }
                "Ans" -> { valid(allowComplex || ans.realOnly, "complex_disabled"); return Node(ans) }
            }
            valid(name in setOf("sin", "cos", "tan", "cot", "asin", "acos", "atan", "acot", "ln", "log", "sqrt", "root", "abs", "exp", "approx"), "syntax")
            valid(take('('), "parenthesis")
            val args = mutableListOf(sum().value)
            while (take(';')) { valid(args.size < 2, "arguments"); args += sum().value }
            valid(take(')'), "parenthesis")
            return Node(function(name, args))
        }
        fun integer(v: CalcValue, min: Int, max: Int): Int {
            valid(v.exact && v.realOnly && v.re!!.d == BigInteger.ONE, "integer_required")
            valid(v.re!!.n >= BigInteger.valueOf(min.toLong()) && v.re.n <= BigInteger.valueOf(max.toLong()), "number_limit")
            return v.re.n.toInt()
        }
        fun pow(a: CalcValue, b: CalcValue): CalcValue {
            if (b.exact && b.realOnly && b.re!!.d == BigInteger.ONE) {
                var n = integer(b, -1000, 1000)
                valid(!(a.zero && n <= 0), "domain")
                var factor = if (n < 0) CalcValue.ONE / a else a
                n = abs(n); var result = CalcValue.ONE
                while (n > 0) { tick(); if (n and 1 == 1) result *= factor; n = n shr 1; if (n > 0) factor *= factor }
                return result
            }
            valid(!a.zero || b.realOnly && b.complex().real > 0, "domain")
            if (a.zero) return CalcValue.ZERO
            if (a.realOnly && b.realOnly) {
                val x = a.complex().real; val y = b.complex().real
                if (x >= 0) { val v = x.pow(y); valid(v != 0.0 || x == 0.0, "underflow"); return CalcValue.approx(v) }
                if (!allowComplex && b.exact && b.re!!.d.testBit(0)) return CalcValue.approx((-x).pow(y) * if (b.re.n.testBit(0)) -1 else 1)
                valid(allowComplex, "domain")
            }
            return CalcValue.approx(a.complex().pow(b.complex()))
        }
        fun absolute(a: CalcValue): CalcValue = if (a.exact && a.realOnly) { if (a.re!!.n.signum() < 0) -a else a } else CalcValue.approx(a.complex().abs())
        fun squareRoot(a: CalcValue): CalcValue {
            if (a.exact && a.realOnly) {
                val n = a.re!!.n.abs(); val d = a.re.d
                fun sqrtInt(x: BigInteger): BigInteger {
                    var low = BigInteger.ZERO; var high = BigInteger.ONE.shiftLeft((x.bitLength() + 1) / 2 + 1)
                    while (high - low > BigInteger.ONE) { tick(); val m = (low + high).shiftRight(1); if (m * m <= x) low = m else high = m }
                    return low
                }
                val nr = sqrtInt(n); val dr = sqrtInt(d)
                if (nr * nr == n && dr * dr == d) {
                    val r = Rational.of(nr, dr)
                    if (a.re.n.signum() >= 0) return CalcValue.exact(r)
                    valid(allowComplex, "domain"); return CalcValue.exact(Rational.ZERO, r)
                }
            }
            valid(allowComplex || a.realOnly && a.complex().real >= 0, "domain")
            return CalcValue.approx(a.complex().sqrt())
        }
        fun function(name: String, args: List<CalcValue>): CalcValue {
            if (name == "root") {
                valid(args.size == 2, "arguments"); val n = integer(args[0], 1, 1000)
                return if (n == 2) squareRoot(args[1]) else pow(args[1], CalcValue.ONE / CalcValue.integer(n))
            }
            if (name == "log" && args.size == 2) {
                valid(!args[0].zero && args[0].complex() != Complex.ONE, "domain")
                return function("ln", listOf(args[1])) / function("ln", listOf(args[0]))
            }
            valid(args.size == 1, "arguments")
            val a = args[0]
            if (name == "abs") return absolute(a)
            if (name == "sqrt") return squareRoot(a)
            if (name == "approx") return CalcValue.approx(a.complex())
            var z = a.complex()
            if (name in setOf("sin", "cos", "tan", "cot") && !radians) z = z.multiply(Math.PI / 180.0)
            if (!allowComplex) {
                valid(a.realOnly, "complex_disabled")
                valid(name !in setOf("ln", "log") || z.real > 0, "domain")
                valid(name !in setOf("asin", "acos") || z.real in -1.0..1.0, "domain")
            }
            valid(name !in setOf("ln", "log") || !a.zero, "domain")
            if (z.imaginary == 0.0 && name in setOf("tan", "cot")) {
                // Exact binary64 pole positions; near a pole is not silently snapped to it.
                valid(if (name == "tan") (z.real - Math.PI / 2) % Math.PI != 0.0 else z.real % Math.PI != 0.0, "domain")
            }
            var result = if (z.imaginary == 0.0 && (name !in setOf("asin", "acos") || z.real in -1.0..1.0) && (name !in setOf("ln", "log") || z.real > 0)) {
                Complex(when (name) {
                    "sin" -> sin(z.real); "cos" -> cos(z.real); "tan" -> tan(z.real); "cot" -> 1 / tan(z.real)
                    "asin" -> asin(z.real); "acos" -> acos(z.real); "atan" -> atan(z.real); "acot" -> atan2(1.0, z.real)
                    "ln" -> ln(z.real); "log" -> log10(z.real); "exp" -> exp(z.real)
                    else -> throw CalculationException("syntax")
                })
            } else when (name) {
                "sin" -> z.sin(); "cos" -> z.cos(); "tan" -> z.tan(); "cot" -> z.tan().reciprocal()
                "asin" -> z.asin(); "acos" -> z.acos(); "atan" -> z.atan(); "acot" -> Complex(Math.PI / 2).subtract(z.atan())
                "ln" -> z.log(); "log" -> z.log().divide(ln(10.0)); "exp" -> z.exp()
                else -> throw CalculationException("syntax")
            }
            if (name in setOf("asin", "acos", "atan", "acot") && !radians) result = result.multiply(180.0 / Math.PI)
            if (name == "exp") valid(result != Complex.ZERO, "underflow")
            return CalcValue.approx(result)
        }
    }
}

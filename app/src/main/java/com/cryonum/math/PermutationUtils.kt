package com.cryonum.math

object PermutationUtils {

    /**
     * Подсчет инверсий в подстановке.
     * Используем простой цикл для малых N, так как в данном приложении
     * подстановки обычно короткие. Для N > 1000 лучше использовать Merge Sort.
     */
    fun countInversions(permutation: List<Int>): Int {
        var inversions = 0
        val size = permutation.size

        for (i in 0 until size) {
            val current = permutation[i]
            for (j in i + 1 until size) {
                if (current > permutation[j]) {
                    inversions++
                }
            }
        }
        return inversions
    }

    fun calculateParity(inversions: Int): Boolean {
        return inversions % 2 == 0
    }
}

/** Two rows describe a bijection of 1..n. Compact notation remains available for n <= 9. */
object PermutationInput {
    fun row(text: String): List<Int> {
        require(text.length <= 4096)
        val s = text.trim()
        require(s.isNotEmpty() && s.all { it in '0'..'9' || it.isWhitespace() || it == ';' })
        val tokens = if (s.any { it.isWhitespace() || it == ';' }) s.split(Regex("[\\s;]+")).filter(String::isNotEmpty) else s.map(Char::toString)
        require(tokens.size in 1..1000)
        val values = tokens.map { it.toIntOrNull() ?: throw IllegalArgumentException("Invalid element") }
        require(values.toSet() == (1..values.size).toSet())
        return values
    }
    fun normalized(top: String, bottom: String): List<Int> {
        val a = row(top); val b = row(bottom)
        require(a.size == b.size)
        val byDomain = IntArray(a.size)
        a.indices.forEach { byDomain[a[it] - 1] = b[it] }
        return byDomain.toList()
    }
}

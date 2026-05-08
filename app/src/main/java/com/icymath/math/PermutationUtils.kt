package com.icymath.math

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

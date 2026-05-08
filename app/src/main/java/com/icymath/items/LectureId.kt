package com.icymath.items

import com.icymath.R

enum class LectureId(val titleResId: Int, val assetFileName: String) {
    ALGEBRAIC_STRUCTURES(R.string.algebraic_structures, "Основные алгебраические структуры.pdf"),
    DIVISIBILITY_IN_INTEGERS(R.string.divisibility_in_integers, "Делимость в кольце целых чисел нацело и с остатком.pdf"),
    GCD_LCM(R.string.gcd_lcm, "НОД и НОК целых чисел. Взаимно простые числа.pdf"),
    PRIME_NUMBERS(R.string.prime_numbers, "Простые числа.pdf"),
    NUMERIC_COMPARISONS(R.string.numeric_comparisons, "Числовые сравнения.pdf"),
    SOLVING_COMPARISONS(R.string.solving_comparisons, "Решение сравнений.pdf"),
    COMPLEX_NUMBERS_1(R.string.complex_numbers_1, "Комплексные числа. Часть 1.pdf"),
    COMPLEX_NUMBERS_2(R.string.complex_numbers_2, "Комплексные числа. Часть 2.pdf"),
    SLU_GAUSS(R.string.slu_gauss, "СЛУ. Метод Гаусса.pdf"),
    MATRICES(R.string.matrices, "Матрицы.pdf"),
    DETERMINANTS(R.string.determinants, "Определители.pdf"),
    PERMUTATIONS(R.string.permutations, "Подстановки.pdf");

    fun titleResId(): Int = titleResId
    fun assetFileName(): String = assetFileName
}

package com.icymath.utils

/**
 * Утилита для фильтрации вводимых данных.
 */
object InputFilter {
    /**
     * Оставляет в строке только цифры (0-9).
     * Все остальные символы будут удалены.
     */
    fun filterOnlyDigits(input: String): String {
        return input.filter { it.isDigit() }
    }
}

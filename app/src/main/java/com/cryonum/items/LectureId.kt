package com.cryonum.items

import com.cryonum.R

enum class LectureId(val titleResId: Int) {
    ALGEBRAIC_STRUCTURES(R.string.algebraic_structures),
    DIVISIBILITY_IN_INTEGERS(R.string.divisibility_in_integers),
    GCD_LCM(R.string.gcd_lcm),
    PRIME_NUMBERS(R.string.prime_numbers),
    NUMERIC_COMPARISONS(R.string.numeric_comparisons),
    SOLVING_COMPARISONS(R.string.solving_comparisons),
    COMPLEX_NUMBERS_1(R.string.complex_numbers_1),
    COMPLEX_NUMBERS_2(R.string.complex_numbers_2),
    SLU_GAUSS(R.string.slu_gauss),
    MATRICES(R.string.matrices),
    DETERMINANTS(R.string.determinants),
    PERMUTATIONS(R.string.permutations);

    fun titleResId(): Int = titleResId
}

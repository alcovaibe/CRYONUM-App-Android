package com.cryonum.pdf

import kotlin.math.sqrt

object PdfRenderBudget {
    const val MAX_PIXELS = 2_000_000
    fun dimensions(width: Int, height: Int): Pair<Int, Int> {
        require(width > 0 && height > 0)
        val scale = minOf(1.5, sqrt(MAX_PIXELS.toDouble() / (width.toDouble() * height)), 4096.0 / maxOf(width, height))
        return maxOf(1, (width * scale).toInt()) to maxOf(1, (height * scale).toInt())
    }
}

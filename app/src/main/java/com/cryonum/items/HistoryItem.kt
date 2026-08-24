package com.cryonum.items

import com.google.gson.annotations.SerializedName

data class HistoryItem(
    @SerializedName("type") val type: HistoryType = HistoryType.SUBSTITUTION,
    @SerializedName("topRow") val topRow: String? = "",
    @SerializedName("bottomRow") val bottomRow: String? = "",
    @SerializedName("inversionCount") val inversionCount: Int = 0,
    @SerializedName("parity") val parity: String? = "",
    @SerializedName("expression") val expression: String? = null,
    @SerializedName("result") val result: String? = null,
    @SerializedName("lastAccessed") val lastAccessed: Long = System.currentTimeMillis()
) {
    enum class HistoryType {
        SUBSTITUTION,
        CALCULATION
    }

    // Constructor for substitutions
    constructor(topRow: String?, bottomRow: String?, inversionCount: Int, parity: String?) : this(
        type = HistoryType.SUBSTITUTION,
        topRow = topRow,
        bottomRow = bottomRow,
        inversionCount = inversionCount,
        parity = parity,
        lastAccessed = System.currentTimeMillis()
    )

    // Constructor for calculations
    constructor(expression: String?, result: String?) : this(
        type = HistoryType.CALCULATION,
        expression = expression,
        result = result,
        lastAccessed = System.currentTimeMillis()
    )

    fun copyWithLastAccessed(lastAccessed: Long): HistoryItem {
        return copy(lastAccessed = lastAccessed)
    }
}

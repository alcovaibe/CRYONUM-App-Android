package com.cryonum.items

data class LanguageItem(
    val code: String,
    val displayName: String,
    val flagEmoji: String,
    val isSelected: Boolean
) {
    fun withSelected(selected: Boolean): LanguageItem {
        if (this.isSelected == selected) return this
        return copy(isSelected = selected)
    }
}

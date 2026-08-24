package com.cryonum.items

data class ReferenceItem(
    val type: ItemType,
    val lectureId: LectureId? = null,
    val titleResId: Int? = null
)

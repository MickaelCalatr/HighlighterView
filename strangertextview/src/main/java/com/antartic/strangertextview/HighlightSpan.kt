package com.antartic.strangertextview

import android.graphics.RectF

class HighlightSpan constructor(
        val text: String,
        var percent: Float,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
) : RectF(left, top, right, bottom) {

    fun resetAnim() {
        this.percent = 0f
    }
}

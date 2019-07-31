package com.antartic.strangertextview

import android.graphics.RectF

/**
 * This class is just an extend of RectF that contain the text and a value to animate the rect.
 */
class HighlightSpan constructor(
        val text: String,
        var percent: Float,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
) : RectF(left, top, right, bottom) {

    /**
     * This function reset the [percent] variable then reset the animation.
     */
    fun resetAnim() {
        this.percent = 0f
    }
}

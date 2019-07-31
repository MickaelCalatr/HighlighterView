package com.antartic.strangertextview.utils

import android.view.View
import android.view.ViewGroup

/**
 * This function is used to allow to draw outside of the View. It allow it by setting up clipToChildren and
 * clipToPadding to false
 */
fun View.setAllParentsClip(enabled: Boolean) {
    var parentView = this.parent

    while (parentView != null && parentView is ViewGroup) {
        val viewGroup = parentView

        viewGroup.clipChildren = enabled
        viewGroup.clipToPadding = enabled
        parentView = viewGroup.parent
    }
}
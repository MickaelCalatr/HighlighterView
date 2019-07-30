package com.antartic.strangertextview.utils

import android.view.View
import android.view.ViewGroup


fun View.setAllParentsClip(enabled: Boolean) {
    var parentView = this.parent

    while (parentView != null && parentView is ViewGroup) {
        val viewGroup = parentView

        viewGroup.clipChildren = enabled
        viewGroup.clipToPadding = enabled
        parentView = viewGroup.parent
    }
}
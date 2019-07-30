package com.idean.highlighterview

import android.content.Context
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val animationList = listOf<HighlighterView>(titleTextView)

        titleTextView.spans = listOf(Pair(0, titleTextView.text.length))
        titleTextView.callBack = { rect, str ->
            showPopupWindow(titleTextView, rect, str)
        }

        animationList.forEach {
            it.startAnimation()
        }

        reset.setOnClickListener {
            animationList.forEach {
                it.resetAnimation()
                it.startAnimation()
            }
        }
    }


    // For the example
    private fun showPopupWindow(v: View, rectF: RectF, value: String) {
        val layoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val layout = layoutInflater.inflate(R.layout.popup_windows, null)

        val textView = layout.findViewById<TextView>(R.id.selectedText)
        textView.text = value

        layout.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))

        // Creating the PopupWindow
        val popupWindow = PopupWindow(this)

        popupWindow.contentView = layout
        popupWindow.width = LinearLayout.LayoutParams.WRAP_CONTENT
        popupWindow.height = LinearLayout.LayoutParams.WRAP_CONTENT
        popupWindow.isFocusable = true

        // Clear the default translucent background
        popupWindow.setBackgroundDrawable(BitmapDrawable())

        val x = v.x + rectF.left.toInt() + 20
        val y = v.y + rectF.bottom.toInt() + layout.measuredHeight + 50
        popupWindow.showAtLocation(layout, Gravity.NO_GRAVITY, x.toInt(), y.toInt())
    }
}

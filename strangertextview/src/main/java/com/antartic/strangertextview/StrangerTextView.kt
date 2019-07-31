package com.antartic.strangertextview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatTextView
import com.antartic.strangertextview.utils.setAllParentsClip

/**
 * Interface used to handle the click callback for the click on rects.
 */
interface StrangerCallBack {
    /**
     * Is called when the rect is clicked, [rect] is the position of the item and [str] is the text inside
     */
    fun onClickOnItem(rect: RectF, str: String)
}

class StrangerTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {
    /**
     * Segment data class used to save and rewrite each line of the textView
     */
    private data class Segment(
        val x: Float,
        val y: Float,
        val str: String
    )

    /**
     * Span data class, used to create the spans with start and end position related to the text
     * length.
     */
    private data class Span(
        var start: Int,
        var end: Int
    )

    // Attributes
    private var isAnimated: Boolean = false
    private var marginStart: Int = 0
    private var marginEnd: Int = 0
    private var marginTop: Int = 0
    private var marginBottom: Int = 0
    private var animationDuration: Long = 500L
    private var color: Int = Color.CYAN

    private var rect = RectF()

    /**
     * This is the list of square, HighlightSpan extend from RectF and contain percent variable used to animate it.
     */
    private val squareList = mutableListOf<HighlightSpan>()

    /**
     * Simple list used to replace the span pair list, this list can ba modified and reset.
     */
    private val spanModel = mutableListOf<Span>()

    /**
     * List of each line of the textView with the start, the end and the text for each line.
     * Used in onDraw to draw the text on the spans.
     */
    private val textInfo = mutableListOf<Segment>()

    /**
     * Boolean used to allow the onDraw() function to draw the spans.
     */
    private var isReadyToDraw = false
    /**
     * Boolean used to know if the view has been already created.
     */
    private var alreadyInstantiate = false

    /**
     * This is the callback executed when a click on the rects are performed.
     * RecF     is the RectF of the square, it contains the position and the dimension.
     * String   this is the text in the rect
     */
    var callBack: StrangerCallBack? = null

    /**
     * This is the list of Pair used to initialise all span for teh text.
     */
    var spans: List<Pair<Int, Int>>? = null
        set(value) {
            field = value
            if (!this.alreadyInstantiate) {
                instantiate()
            } else {
                reInstantiate()
            }
        }


    init {
        context.obtainStyledAttributes(attrs, R.styleable.StrangerTextView)?.apply {
            this@StrangerTextView.isAnimated = getBoolean(R.styleable.StrangerTextView_animated, false)
            this@StrangerTextView.marginStart = getDimension(R.styleable.StrangerTextView_marginStart, 0f).toInt()
            this@StrangerTextView.marginEnd = getDimension(R.styleable.StrangerTextView_marginEnd, 0f).toInt()
            this@StrangerTextView.marginTop = getDimension(R.styleable.StrangerTextView_marginTop, 0f).toInt()
            this@StrangerTextView.marginBottom = getDimension(R.styleable.StrangerTextView_marginBottom, 0f).toInt()
            this@StrangerTextView.color = getColor(R.styleable.StrangerTextView_color, color)
            this@StrangerTextView.animationDuration =
                getInteger(R.styleable.StrangerTextView_animationDuration, 500).toLong()
            recycle()
        }
        post {
            this.setAllParentsClip(false)
        }

    }


    // INITIALISE THE VIEW
    /**
     * Clear all the lists (spans, texts and squares)
     * This function is called when a span is added or the text is changed.
     */
    private fun clearView() {
        this.textInfo.clear()
        this.spanModel.clear()
        this.squareList.clear()
        this.isReadyToDraw = false
    }

    /**
     * Instantiate the all view.
     * If the layout view is null, call [onPreDraw] then :
     * - Measure the textView position
     * - Initialise all the span positions
     * - Create the span rects
     *
     */
    private fun instantiate() {
        if (layout == null) {
            onPreDraw()
        }

        if (text.isNotEmpty() && layout != null) {
            measureTextView()
            initSpans()
            createSpans()
            this.isReadyToDraw = true
            this.alreadyInstantiate = true
         }
    }

    /**
     * If the view is already draw, clean all the view. Then call the instantiate function.
     */
    private fun reInstantiate() {
        if (this.isReadyToDraw) {
            invalidate()
            requestLayout()
            clearView()
        }
        instantiate()
    }



    // CLICK EVENT
    override fun performClick(): Boolean {
        return super.performClick()
    }

    /**
     * This function is called when a click is perfomed on the screen. The function call hasOnClickOnRect to check the
     * click position and the squares position.
     * If hasOnClickOnRect return true, then invoke the callBack function and call the performClick function.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            this.squareList.forEach {
                if (hasOnClickOnRect(it, event)) {
                    this.callBack?.onClickOnItem(it, it.text)
                    performClick()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * The function check the click position and the squares position.
     *
     * @return  true if the click is on a rect, false if it's not.
     */
    private fun hasOnClickOnRect(rectF: RectF, event: MotionEvent): Boolean {
        val touchX = event.x
        val touchY = event.y
        val rectTouched = RectF(
            rectF.left + this.marginStart,
            rectF.top + this.marginTop,
            rectF.right - this.marginEnd,
            rectF.bottom - this.marginBottom
        )
        return rectTouched.contains(touchX, touchY)
    }



    // SPAN HANDLERS
    /**
     * This function create all span models from the list of Pair. Check also the integrity of the
     * spans and add each span positions in the list if the start is different from the end.
     */
    private fun initSpans() {
        this.spans?.forEach {
            val start = checkIntegritySpan(it.first, this.text.length)
            val end = checkIntegritySpan(it.second, this.text.length)

            if (start != end) {
                val span = Span(start, end)
                this.spanModel.add(span)
            }
        }
    }

    /**
     * This function check the maximum and the minimum of a span.
     * If the start position is smaller than 0              -> 0 is returned
     * If the end position is bigger than the text length   -> text length is return
     * Else the position is returned
     *
     * !!!!
     * !!!! Main problem come from that the length start from 1 and the character count start from 0. Then, I had
     * !!!! to add one in some case.
     * !!!!
     */
    private fun checkIntegritySpan(pos: Int, max: Int): Int {
        return when {
            pos >= max -> max
            pos <= 0 -> 0
            else -> pos
        }
    }

    /**
     * Extract the pixel position of [start] and [end] offset and create the rect for this span.
     * @return  the HighlightSpan created with all the positions.
     */
    private fun extractSpan(start: Int, end: Int, text: String): HighlightSpan {
        val line = getLine(start)
        val baseLine = layout.getLineBaseline(line)

        val left = layout.getPrimaryHorizontal(start)
        val right = layout.getPrimaryHorizontal(end)
        val top = baseLine + paint.fontMetrics.ascent
        val bottom = baseLine + paint.fontMetrics.descent
        val percent = if (this.isAnimated) 0f else 1f

        return HighlightSpan(text, percent, left, top, right, bottom)
    }

    /**
     * This function is called when a span is on multiple lines. The function extrat the position of start and end for
     * each line and create a span for each.
     *
     * @return  the span list for the whole text block (from [start] to [end])
     */
    private fun extractMultipleSpans(start: Int, end: Int): List<Rect> {
        val text = this.text.substring(start, end)
        val spansRect = mutableListOf<Rect>()
        val lastLine = getLine(end)
        var startChar = start
        var lastChar: Int

        while (startChar < end) {
            lastChar = findLastCharPosition(startChar, end, lastLine)
            this.squareList.add(extractSpan(startChar, lastChar, text))
            startChar = lastChar + 1
        }
        return spansRect
    }

    /**
     * This function find the last character position on a line.
     * If the [start] position is on the last line AND [end] is equal to the text length then return end
     * If the [start] position is on the last line return end + 1
     * Else return the position of the character in the text.
     */
    private fun findLastCharPosition(start: Int, end: Int, lastLine: Int): Int {
        val line = getLine(start)

        if (line == lastLine) {
            return if (end == this.text.length) end else end + 1
        } else {
            var size = 0
            this.textInfo.take(line + 1).forEach {
                size += it.str.length
            }
            return size - 1
        }
    }

    /**
     * This function add each span from [spans] to the [spanModel] list.
     * If the span is on more than one line the function will call [extractMultipleSpans] to cut it in multiple span.
     */
    private fun createSpans() {
        this.spanModel.forEach {
            if (getLine(it.start) == getLine(it.end)) {
                val spanRect = extractSpan(it.start, it.end, this.text.substring(it.start, it.end))
                this.squareList.add(spanRect)
            } else {
                extractMultipleSpans(it.start, it.end)
            }
        }
    }



    // TEXT HANDLERS
    /**
     * This function measure the text position after the view initialisation.
     * The function save all the positions for each lines in the [textInfo] list.
     * The function is call each time that the text is changed.
     */
    private fun measureTextView() {
        val top = (paint.fontMetrics.ascent - paint.fontMetrics.top).toInt()

        for (i in 0 until lineCount) {
            val start = layout.getLineStart(i)
            val end = layout.getLineEnd(i)

            val line = this.text.substring(start, end)
            val yCoordinate = layout.getLineTop(i) - top
            val xCoordinate = layout.getPrimaryHorizontal(start)

            val segment = Segment(xCoordinate, yCoordinate.toFloat(), line)
            this.textInfo.add(segment)
        }
    }

    /**
     * This function return the line of the offset if the [textInfo] list.
     */
    private fun getLine(offset: Int): Int {
        var chars = 0
        var i = 0

        this.textInfo.forEach {
            chars += it.str.length
            if (offset < chars) {
                return i
            }
            i++
        }
        return i - 1
    }

    override fun onDraw(canvas: Canvas) {
        if (!this.isReadyToDraw) {
            return super.onDraw(canvas)
        }

        paint.color = this.color

        // Draw squares
        canvas.save()
        this.squareList.forEach {
            rect.set(
                it.left + this.marginStart,
                it.top + this.marginTop,
                it.left + (it.width() - this.marginEnd) * it.percent,
                it.bottom - this.marginBottom
            )
            canvas.drawRect(rect, paint)
        }
        canvas.restore()

        // draw text
        paint.color = textColors.defaultColor
        this.layout.draw(canvas)
    }

    // ANIMATION
    /**
     * This function is used to start animation when the view is created
     */
    fun startAnimation() {
        post {
            this.generateEnterAnimation().forEach {
                it.start()
            }
        }
    }

    /**
     * This function generate the animation for each square.
     * Each animation will be set using the duration from attributes.
     */
    private fun generateEnterAnimation(): List<ValueAnimator> {
        return this.squareList.map { span ->
            val animator = ValueAnimator.ofFloat(0f, 1f)

            animator.duration = this.animationDuration
            animator.addUpdateListener {
                val value = it.animatedValue as Float
                span.percent = value
                invalidate()
            }
            animator
        }
    }

    /**
     * This function reset the view animation.
     */
    fun resetAnimation() {
        post {
            resetToInitialState()
        }
    }

    /**
     * Reset animation for each squares
     */
    private fun resetToInitialState() {
        this.squareList.forEach { it.resetAnim() }
        invalidate()
    }



    // UPDATE TextView
    override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)

        if (!this.alreadyInstantiate) {
            return
        }

        // Re-instantiate the whole view
        reInstantiate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (!this.alreadyInstantiate) {
            return
        }

        // Re-instantiate the whole view
        reInstantiate()
    }
}
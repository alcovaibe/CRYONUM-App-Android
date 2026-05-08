package com.icymath.pdf

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ScrollView

class ZoomableTwoDScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val horizontalScrollView: HorizontalScrollView = HorizontalScrollView(context).apply {
        overScrollMode = OVER_SCROLL_NEVER
    }
    private val verticalScrollView: ScrollView = ScrollView(context).apply {
        overScrollMode = OVER_SCROLL_NEVER
    }

    var scaleFactor = 1.0f
        set(value) {
            val clamped = value.coerceIn(minScale, maxScale)
            val inner = verticalScrollView.getChildAt(0)
            if (inner != null) {
                animateScale(inner, field, clamped)
            }
            field = clamped
        }

    private val minScale = 1.0f
    private val maxScale = 3.0f

    private var scaleAnimator: ValueAnimator? = null
    private var content: View? = null
    private var lastX = 0f
    private var lastY = 0f

    private val scaleGestureDetector: ScaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val oldScale = scaleFactor
            scaleFactor = (scaleFactor * detector.scaleFactor).coerceIn(minScale, maxScale)

            val pivotX = detector.focusX + horizontalScrollView.scrollX
            val pivotY = detector.focusY + verticalScrollView.scrollY

            content?.let {
                it.pivotX = pivotX
                it.pivotY = pivotY
                animateScale(it, oldScale, scaleFactor)
            }
            return true
        }
    })

    init {
        // add horizontal container as first child
        val hlp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(horizontalScrollView, hlp)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        // Expect original content to be present as second child (index 1)
        if (childCount > 1) {
            val rootContent = getChildAt(1)
            removeView(rootContent)

            content = rootContent
            val wrapLp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            verticalScrollView.addView(content, wrapLp)
            horizontalScrollView.addView(verticalScrollView, wrapLp)
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(ev)

        if (scaleFactor <= 1.1f) return false

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = ev.x
                lastY = ev.y
            }
            MotionEvent.ACTION_MOVE -> return true
        }

        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(ev)

        if (scaleFactor <= 1.1f) return false

        when (ev.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.x - lastX
                val dy = ev.y - lastY

                horizontalScrollView.scrollBy((-dx).toInt(), 0)
                verticalScrollView.scrollBy(0, (-dy).toInt())

                lastX = ev.x
                lastY = ev.y
            }
            MotionEvent.ACTION_UP -> performClick()
        }

        return true
    }

    private fun animateScale(view: View, from: Float, to: Float) {
        scaleAnimator?.cancel()
        if (from == to) {
            view.scaleX = to
            view.scaleY = to
            return
        }
        scaleAnimator = ValueAnimator.ofFloat(from, to).apply {
            duration = 120
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                view.scaleX = value
                view.scaleY = value
            }
            start()
        }
    }
}

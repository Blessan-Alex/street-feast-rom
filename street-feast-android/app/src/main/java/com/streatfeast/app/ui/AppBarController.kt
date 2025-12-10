package com.streatfeast.app.ui

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.view.isVisible

/**
 * Simple controller to toggle a header/app bar with a tap or swipe gesture.
 * Attach to a view (e.g., a thin handle or the top padding area).
 */
class AppBarController(
    private val appBar: View,
    private val handleView: View? = null,
    private val onVisibilityChanged: ((Boolean) -> Unit)? = null
) {
    private var isVisible: Boolean = appBar.isVisible

    private val gestureDetector = GestureDetector(appBar.context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            toggle()
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            val startY = e1?.y ?: return false
            val dy = e2.y - startY
            when {
                dy > 40f -> show()   // swipe down
                dy < -40f -> hide()  // swipe up
                else -> return false
            }
            return true
        }
    })

    fun attach() {
        // Only attach to handle view to avoid interfering with back button and other controls
        handleView?.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
        // Do NOT attach to appBar itself - it interferes with back button and other interactive elements
    }

    fun toggle() {
        if (isVisible) hide() else show()
    }

    fun show() {
        if (!isVisible) {
            isVisible = true
            appBar.visibility = View.VISIBLE
            onVisibilityChanged?.invoke(true)
        }
    }

    fun hide() {
        if (isVisible) {
            isVisible = false
            appBar.visibility = View.GONE
            onVisibilityChanged?.invoke(false)
        }
    }
}


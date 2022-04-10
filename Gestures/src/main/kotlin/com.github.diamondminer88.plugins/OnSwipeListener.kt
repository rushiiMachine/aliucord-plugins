package com.github.diamondminer88.plugins

import android.annotation.SuppressLint
import android.content.Context
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.View.OnTouchListener
import kotlin.math.absoluteValue

private const val SWIPE_THRESHOLD = 100
private const val SWIPE_VELOCITY_THRESHOLD = 100

@SuppressLint("ClickableViewAccessibility")
open class OnSwipeListener(ctx: Context) : OnTouchListener {
	private val gestureDetector = GestureDetector(ctx, GestureListener())

	override fun onTouch(v: View, event: MotionEvent): Boolean {
		return gestureDetector.onTouchEvent(event)
	}

	private inner class GestureListener : SimpleOnGestureListener() {
		override fun onLongPress(e: MotionEvent) {
			onLongPress()
		}

		override fun onDown(e: MotionEvent): Boolean {
			onClick()
			return true
		}

		override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
			var result = false
			try {
				val diffY = e2.y - e1.y
				val diffX = e2.x - e1.x
				if (diffX.absoluteValue > diffY.absoluteValue) {
					if (diffX.absoluteValue > SWIPE_THRESHOLD && velocityX.absoluteValue > SWIPE_VELOCITY_THRESHOLD) {
						if (diffX > 0) onSwipeRight()
						else onSwipeLeft()
						result = true
					}
				} else if (diffY.absoluteValue > SWIPE_THRESHOLD && velocityY.absoluteValue > SWIPE_VELOCITY_THRESHOLD) {
					if (diffY > 0) onSwipeBottom()
					else onSwipeTop()
					result = true
				}
			} catch (exception: Exception) {
				exception.printStackTrace()
			}
			return result
		}
	}

	open fun onSwipeRight() {}
	open fun onSwipeLeft() {}
	open fun onSwipeTop() {}
	open fun onSwipeBottom() {}
	open fun onClick() {}
	open fun onLongPress() {}
}

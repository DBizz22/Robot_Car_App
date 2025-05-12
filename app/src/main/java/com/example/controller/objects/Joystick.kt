package com.example.controller.objects

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class ThumbstickView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private var outerRadius = 10f
    private var innerRadius = 5f
    private var centerX = 0f
    private var centerY = 0f
    private var innerX = 0f
    private var innerY = 0f

    var joystickListener: ((Float, Float) -> Unit)? = null  // Callback for X-Y movement

    private val outerPaint = Paint().apply {
        color = Color.GRAY
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val innerPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        outerRadius = min(w, h) / 3f
        innerRadius = outerRadius / 2f
        innerX = centerX
        innerY = centerY
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw outer circle (background)
        canvas.drawCircle(centerX, centerY, outerRadius, outerPaint)
        // Draw inner circle (thumbstick)
        canvas.drawCircle(innerX, innerY, innerRadius, innerPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - centerX
                val dy = event.y - centerY
                val distance = sqrt(dx * dx + dy * dy)

                if (distance < outerRadius) {
                    innerX = event.x
                    innerY = event.y
                } else {
                    val angle = atan2(dy, dx)
                    innerX = centerX + cos(angle) * outerRadius
                    innerY = centerY + sin(angle) * outerRadius
                }

                // Normalize movement to -1 to 1 range
                val normalizedX = (innerX - centerX) / outerRadius
                val normalizedY = (innerY - centerY) / outerRadius
                joystickListener?.invoke(normalizedX, normalizedY)
            }

            MotionEvent.ACTION_UP -> {
                // Reset inner circle to center when released
                innerX = centerX
                innerY = centerY
                joystickListener?.invoke(0f, 0f)  // Stop movement
            }
        }
        invalidate()
        return true
    }
}

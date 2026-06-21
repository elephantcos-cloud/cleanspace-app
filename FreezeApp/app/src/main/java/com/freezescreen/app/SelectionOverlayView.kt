package com.freezescreen.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View

class SelectionOverlayView(context: Context) : View(context) {

    var onSelectionCompleted: ((Rect) -> Unit)? = null
    var onCancel: (() -> Unit)? = null

    private val paint = Paint().apply {
        color = Color.parseColor("#6600CCFF")
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private var startX = 0f
    private var startY = 0f
    private var endX = 0f
    private var endY = 0f
    private var drawing = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw dim background
        canvas.drawColor(Color.parseColor("#44000000"))
        if (drawing) {
            val rect = getSelectionRect()
            canvas.drawRect(rect, paint)
            canvas.drawRect(rect, borderPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                drawing = true
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                endX = event.x
                endY = event.y
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                drawing = false
                val rect = getSelectionRect()
                if (rect.width() > 20 && rect.height() > 20) {
                    onSelectionCompleted?.invoke(rect)
                } else {
                    onCancel?.invoke()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun getSelectionRect(): Rect {
        val left = Math.min(startX, endX).toInt()
        val top = Math.min(startY, endY).toInt()
        val right = Math.max(startX, endX).toInt()
        val bottom = Math.max(startY, endY).toInt()
        return Rect(left, top, right, bottom)
    }
}

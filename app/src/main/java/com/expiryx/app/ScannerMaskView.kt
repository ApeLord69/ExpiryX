package com.expiryx.app

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class ScannerMaskView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val maskPaint = Paint().apply {
        color = Color.parseColor("#99000000") // Semi-transparent dark
        style = Paint.Style.FILL
    }

    private val transparentPaint = Paint().apply {
        color = Color.TRANSPARENT
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val frameRect = RectF()
    private val cornerRadius = 10f * resources.displayMetrics.density // match scan_frame_border corners

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Draw full screen dark mask
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), maskPaint)

        // 2. Draw clear cutout in center
        // Rect dimensions should match scan_frame_border in XML
        val frameWidth = 300f * resources.displayMetrics.density
        val frameHeight = 300f * resources.displayMetrics.density
        
        val left = (width - frameWidth) / 2f
        val top = (height - frameHeight) / 2f
        frameRect.set(left, top, left + frameWidth, top + frameHeight)

        canvas.drawRoundRect(frameRect, cornerRadius, cornerRadius, transparentPaint)
    }
}

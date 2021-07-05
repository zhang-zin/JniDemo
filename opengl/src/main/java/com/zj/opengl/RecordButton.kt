package com.zj.opengl

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatTextView

class RecordButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    var startRecording: (() -> Unit?)? = null
    var stopRecording: (() -> Unit?)? = null

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (startRecording == null || stopRecording == null) {
            return false
        }
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                startRecording?.invoke()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                stopRecording?.invoke()
            }

        }
        return true
    }
}


package com.anymore.where

import android.content.Context
import android.graphics.Rect
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

/**
 * Created by lym on 2021/6/8.
 */
object PageNavigator {
    private const val TAG = "PageNavigator"

    @JvmStatic
    private var lastTriggerTime = 0L
    private const val MIN_TRIGGER_TIME = 3 * 1000L

    @JvmStatic
    fun onActivityTouchEvent(activity: AppCompatActivity, event: MotionEvent?) {
        if (event == null) return
        if (isTouchValid(event)) {
            val builder = StringBuilder()
            builder.append(activity.javaClass.name)
            val touchX = event.x
                .toInt()
            val touchY = event.y
                .toInt()
            activity.supportFragmentManager.fragments.forEach { fragment ->
                onFragmentTouched(fragment, touchX, touchY, 0, builder)
            }
            val message = builder.toString()
            toast(activity, message)
            log("Current Page:\n$message")
        }
    }

    @JvmStatic
    fun onTouch(context: Context, event: MotionEvent?, touchTarget: Any?) {
        if (event == null || touchTarget == null) return
        if (isTouchValid(event)) {
            toast(context, touchTarget::class.qualifiedName)
        }
    }

    @JvmStatic
    private fun isTouchValid(event: MotionEvent): Boolean {
        if (SystemClock.elapsedRealtime() - lastTriggerTime < MIN_TRIGGER_TIME) return false
        val action = event.action
        val res = action in listOf(MotionEvent.ACTION_POINTER_UP) && event.pointerCount == 3
        return res.apply {
            if (this) {
                lastTriggerTime = SystemClock.elapsedRealtime()
            }
        }
    }

    @JvmStatic
    @Suppress("DEPRECATION")
    private fun onFragmentTouched(
        fragment: Fragment?,
        touchX: Int,
        touchY: Int,
        level: Int,
        builder: StringBuilder
    ) {
        if (fragment == null || fragment.isDetached || !fragment.userVisibleHint || fragment.isHidden) {
            return
        }
        fragment.view?.let {
            val rect = Rect()
            it.getGlobalVisibleRect(rect)
            if (rect.contains(touchX, touchY)) {
                builder.append("\n")
                for (i in 0..level) {
                    builder.append("-")
                }
                builder.append(fragment.javaClass.name)
                fragment.childFragmentManager.fragments.forEach { childFragment ->
                    onFragmentTouched(childFragment, touchX, touchY, level + 1, builder)
                }
            }
        }
    }

    @JvmStatic
    private fun log(message: String?) {
        if (message.isNullOrBlank()) return
        Log.d(TAG, message)
    }

    @JvmStatic
    private fun toast(context: Context, message: String?) {
        if (message.isNullOrBlank()) return
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
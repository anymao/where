package com.anymore.where.demo

import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity

/**
 * Created by lym on 2021/6/8.
 */
open class BaseActivity : AppCompatActivity() {

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        return super.dispatchTouchEvent(ev)
    }
}
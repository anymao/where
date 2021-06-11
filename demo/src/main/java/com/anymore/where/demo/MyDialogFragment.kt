package com.anymore.where.demo

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialog
import androidx.appcompat.app.AppCompatDialogFragment

/**
 * Created by anymore on 2021/6/11.
 */
class MyDialogFragment constructor(): AppCompatDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AppCompatDialog(requireContext()).apply {
            setContentView(R.layout.fragment_my_dialog)
            setCancelable(false)
        }
    }
}
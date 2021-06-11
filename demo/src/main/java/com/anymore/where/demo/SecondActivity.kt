package com.anymore.where.demo

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDialog

/**
 * Created by anymore on 2021/6/11.
 */
class SecondActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)
        findViewById<Button>(R.id.btn_show_dialog).setOnClickListener {
            showDialog()
        }

        findViewById<Button>(R.id.btn_show_dialog_fragment).setOnClickListener {
            MyDialogFragment().show(supportFragmentManager,"dialogFragment")
        }
    }


    private fun showDialog(){
//        val dialog = AlertDialog.Builder(this).setTitle("dialog").setMessage("this is a alertDialog")
//            .setPositiveButton("知道了"
//            ) { dialog, which -> dialog?.dismiss() }.setCancelable(false)
//            .create()
//        dialog.show()
        val dialog = AppCompatDialog(this)
        dialog.setTitle("dialog")
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setContentView(R.layout.fragment_my_dialog)
        dialog.show()
    }
}
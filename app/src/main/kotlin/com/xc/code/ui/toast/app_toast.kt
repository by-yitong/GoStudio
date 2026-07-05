package com.xc.code.ui.toast

import android.content.Context
import android.widget.Toast

object app_toast {
    const val LENGTH_SHORT = Toast.LENGTH_SHORT
    const val LENGTH_LONG = Toast.LENGTH_LONG

    fun show(context: Context, message: String, duration: Int = LENGTH_SHORT) {
        Toast.makeText(context, message, duration).show()
    }
}

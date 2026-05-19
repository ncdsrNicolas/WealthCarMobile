package br.edu.fatecpg.wealthcar_teste

import android.app.Activity
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast

fun Activity.showCustomToast(message: String) {
    val layout = layoutInflater.inflate(
        R.layout.custom_toast,
        findViewById<ViewGroup>(R.id.custom_toast_container)
    )

    val text: TextView = layout.findViewById(R.id.custom_toast_text)
    text.text = message

    val toast = Toast(applicationContext)
    toast.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 100)
    toast.duration = Toast.LENGTH_SHORT
    toast.view = layout
    toast.show()
}
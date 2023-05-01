package com.bilabila.mizuki

import androidx.appcompat.app.AppCompatActivity
import com.bilabila.mizuki_ui.UI

class MainActivity : AppCompatActivity() {

    override fun onStart() {
        super.onStart()
        UI().setContent(this) {
            ""
        }
    }
}

package com.bilabila.mizuki_ui

import androidx.activity.compose.setContent
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity

@Keep
class UI {

    @Keep
    fun setContent(context: AppCompatActivity, api: (String) -> String) {
        val repo = Repo(context)
        context.setContent {
            App(repo)
        }
    }
}
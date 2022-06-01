package com.example.rscan

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun ObjectDetection_click(view: View) {
        val intent = Intent(this, ObjectDetection::class.java)
        startActivity(intent)
    }


}
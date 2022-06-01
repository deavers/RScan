package com.example.rscan

// Стандартные библиотеки
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity

// Класс Splash скрина
class SpalshScreen : AppCompatActivity() {

    // При действии открытия
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Открытие Главного экрана
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)

        // Задержка 3 секунды
        val handler = Handler()
        handler.postDelayed({
        }, 3000)

        // Завершение SplashScreen экрана
        finish()
    }
}
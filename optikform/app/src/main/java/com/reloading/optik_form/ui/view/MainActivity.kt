package com.reloading.optik_form.ui.view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.reloading.optik_form.R

/**
 * Uygulamanın ana aktivitesi.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // activity_main.xml layout'unu ayarlar
        setContentView(R.layout.activity_main)

        // DashboardFragment'i başlangıç fragmenti olarak yerleştirir
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView, DashboardFragment())
            .commit()
    }
}

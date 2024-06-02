package com.example.organizemedicine.view

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.organizemedicine.R

class SplashActivity : AppCompatActivity() {

    private val splashTimeout: Long = 3000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Find the ImageView
        val logoImageView = findViewById<ImageView>(R.id.roundedImageView)

        // Load the animation
        val rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate)

        // Start the animation
        logoImageView.startAnimation(rotateAnimation)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MedicineFeedActivity::class.java))
            finish()
        }, splashTimeout)
    }
}
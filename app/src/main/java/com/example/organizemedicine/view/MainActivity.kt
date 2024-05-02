package com.example.organizemedicine.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.organizemedicine.R
import com.example.organizemedicine.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth



    override fun onCreate(savedInstanceState: Bundle?) {
        FirebaseApp.initializeApp(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        auth = Firebase.auth







        //güncel kullanıcı girişi
        val currentUser = auth.currentUser
        if (currentUser != null){
            val intent = Intent(this@MainActivity, MedicineFeedActivity::class.java)
            startActivity(intent)
            finish()
        }


        binding.createAccount.setOnClickListener {
            val intent = Intent(this@MainActivity, AccountCreateActivity::class.java)
            startActivity(intent)
        }

    }

    fun signInClicked(view : View){
        val email = binding.emailText.text.toString()
        val password = binding.passwordText.text.toString()

        if(email == "" || password == ""){
            Toast.makeText(this,"Enter email and password!", Toast.LENGTH_LONG).show()
        }
        else{
            //success
            auth.signInWithEmailAndPassword(email,password).addOnSuccessListener {
                val intent = Intent(this@MainActivity, MedicineFeedActivity::class.java) // bunu değiş kullanıcı ekranına atsın
                startActivity(intent)
                finish()
            }.addOnFailureListener{
                //failure
                Toast.makeText(this@MainActivity,it.localizedMessage,Toast.LENGTH_LONG).show()
            }

        }

    }
}
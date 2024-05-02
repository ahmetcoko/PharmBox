package com.example.organizemedicine.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.organizemedicine.R
import com.example.organizemedicine.databinding.ActivityHomeBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.auth.User
import com.google.firebase.firestore.firestore

class HomeActivity : AppCompatActivity() {
    private lateinit var binding : ActivityHomeBinding
    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        firestoreDb = Firebase.firestore
        auth = FirebaseAuth.getInstance()



        fun fetchCurrentUserInformation() {
            val currentUser = auth.currentUser
            currentUser?.let { user ->
                firestoreDb.collection("Users").document(user.uid)
                    .get()
                    .addOnSuccessListener { documentSnapshot ->
                        if (documentSnapshot.exists()) {
                            // Manually extract each field
                            val username = documentSnapshot.getString("username") ?: "N/A"
                            val age = documentSnapshot.getLong("age")?.toInt() ?: "N/A" // Firestore stores numbers as Long
                            val height = documentSnapshot.getDouble("height") ?: "N/A"
                            val weight = documentSnapshot.getDouble("weight") ?: "N/A"

                            // Update the UI with the retrieved data
                            binding.usernameTextView.text = username
                            binding.userAgeTextView.text = age.toString()
                            binding.userHeightTextView.text = String.format("%.2f", height)
                            binding.userWeightTextView.text = String.format("%.2f", weight)

                            // Additional handling for other user attributes if necessary
                        } else {
                            Toast.makeText(this, "User does not exist", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this, exception.localizedMessage, Toast.LENGTH_SHORT).show()
                    }
            } ?: run {
                Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show()
            }
        }

        fetchCurrentUserInformation()


        binding.apply {
            bottomMenu.setItemSelected(R.id.bottom_profile)
            bottomMenu.setOnItemSelectedListener {
                if (it == R.id.bottom_upload){
                    startActivity(Intent(this@HomeActivity,PostUploadActivity::class.java))
                }
                else if (it == R.id.bottom_search){
                    startActivity(Intent(this@HomeActivity,SearchActivity::class.java))
                }
                else if (it == R.id.bottom_home){
                    startActivity(Intent(this@HomeActivity,MedicineFeedActivity::class.java))
                }
            }
        }
    }
}
package com.example.organizemedicine.view

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.organizemedicine.databinding.ActivityAccountCreateBinding
import com.example.organizemedicine.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AccountCreateActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAccountCreateBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance() // Initialize Firebase Auth
        binding = ActivityAccountCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.createAccountButton.setOnClickListener {
            signUpClicked()
        }

    }

    private fun signUpClicked() {
        val firstName = binding.firstName.text.toString().trim()
        val lastName = binding.lastName.text.toString().trim()
        val username = binding.username.text.toString().trim()
        val phone = binding.phone.text.toString().trim()
        val email = binding.email.text.toString().trim()
        val password = binding.password.text.toString().trim()
        val confirmPassword = binding.confirmPassword.text.toString().trim()
        val age = binding.ageEditText.text.toString().toIntOrNull()
        val height = binding.heightEditText.text.toString().toDoubleOrNull()
        val weight = binding.weightEditText.text.toString().toDoubleOrNull()

        // Validation
        when {
            firstName.isEmpty() -> Toast.makeText(this, "Please enter your first name.", Toast.LENGTH_SHORT).show()
            lastName.isEmpty() -> Toast.makeText(this, "Please enter your last name.", Toast.LENGTH_SHORT).show()
            username.isEmpty() -> Toast.makeText(this, "Please enter a username.", Toast.LENGTH_SHORT).show()
            phone.isEmpty() -> Toast.makeText(this, "Please enter your phone number.", Toast.LENGTH_SHORT).show()
            email.isEmpty() -> Toast.makeText(this, "Please enter an email address.", Toast.LENGTH_SHORT).show()
            password.isEmpty() -> Toast.makeText(this, "Please enter a password.", Toast.LENGTH_SHORT).show()
            confirmPassword.isEmpty() -> Toast.makeText(this, "Please confirm your password.", Toast.LENGTH_SHORT).show()
            password != confirmPassword -> Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
            else -> {
                // Create the account
                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        // Authentication succeeded
                        val firebaseUser = auth.currentUser
                        val userId = firebaseUser?.uid

                        // Create a User object with details from input fields
                        val user = User(
                            firstName = firstName,
                            lastName = lastName,
                            username = username,
                            phoneNumber = phone,
                            email = email,
                            age = age,             // New property
                            height = height,       // New property
                            weight = weight,       // New property

                        )

                        // Store in Firestore
                        userId?.let {
                            FirebaseFirestore.getInstance().collection("Users")
                                .document(it)
                                .set(user)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this,
                                        "User details saved successfully",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    // Navigate to another activity or perform other actions
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        this,
                                        "Failed to save user details: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }

                        Toast.makeText(this, "Account created successfully for $username", Toast.LENGTH_LONG).show()
                        // You may want to add more user information in Firebase Database
                        // Redirect to sign-in or other activity
                    } else {
                            Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}

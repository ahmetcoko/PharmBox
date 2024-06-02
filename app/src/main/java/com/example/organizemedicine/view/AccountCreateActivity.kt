package com.example.organizemedicine.view

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
        auth = FirebaseAuth.getInstance()
        binding = ActivityAccountCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.createAccountButton.setOnClickListener {
            signUpClicked()
        }

        binding.privacyPolicyText.setOnClickListener{
            showPrivacyPolicyDialog()
        }

    }

    private fun showPrivacyPolicyDialog() {
        AlertDialog.Builder(this)
            .setMessage("By registering for our application, you acknowledge that not all reviews of medications shared on this platform are accurate or reliable. Users of this application should exercise discretion and consult healthcare professionals for accurate medical advice and information. The opinions and experiences shared by users are subjective and may not reflect the efficacy or safety of medications for all individuals. The platform does not endorse or verify the accuracy of user-generated content. By proceeding, you agree to use this application at your own risk and accept responsibility for your interactions and decisions based on the information provided herein.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    private fun signUpClicked() {

        if (!binding.privacySwitch.isChecked) {
            Toast.makeText(this, "You should give permission that you accept privacy policy", Toast.LENGTH_SHORT).show()
            return
        }


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
            firstName.isEmpty() || firstName.length > 20 -> Toast.makeText(this, "Please enter your first name (max 20 characters).", Toast.LENGTH_SHORT).show()
            lastName.isEmpty() || lastName.length > 20 -> Toast.makeText(this, "Please enter your last name (max 20 characters).", Toast.LENGTH_SHORT).show()
            username.isEmpty() || username.length > 20 -> Toast.makeText(this, "Please enter a username (max 20 characters).", Toast.LENGTH_SHORT).show()
            phone.isEmpty() || phone.length !in 11..13 || !phone.all { it.isDigit() } -> Toast.makeText(this, "Please enter your phone number (11-13 digits).", Toast.LENGTH_SHORT).show()
            email.isEmpty() || !email.contains("@") -> Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
            password.isEmpty() || password.length < 6 -> Toast.makeText(this, "Please enter a password (at least 6 characters).", Toast.LENGTH_SHORT).show()
            confirmPassword.isEmpty() -> Toast.makeText(this, "Please confirm your password.", Toast.LENGTH_SHORT).show()
            password != confirmPassword -> Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
            age == null || age !in 0..120 -> Toast.makeText(this, "Please enter a valid age (0-120).", Toast.LENGTH_SHORT).show()
            height == null || height !in 0.0..250.0 -> Toast.makeText(this, "Please enter a valid height (0-250).", Toast.LENGTH_SHORT).show()
            weight == null || weight !in 0.0..350.0 -> Toast.makeText(this, "Please enter a valid weight (0-350).", Toast.LENGTH_SHORT).show()

            else -> {
                // Create the account
                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val firebaseUser = auth.currentUser
                        val userId = firebaseUser?.uid


                        val user = User(
                            firstName = firstName,
                            lastName = lastName,
                            username = username,
                            phoneNumber = phone,
                            email = email,
                            age = age,
                            height = height,
                            weight = weight,

                        )


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
                    } else {
                            Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}

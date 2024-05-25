package com.example.organizemedicine.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.organizemedicine.R
import com.example.organizemedicine.databinding.ActivityHomeBinding
import com.example.organizemedicine.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestoreDb = FirebaseFirestore.getInstance()

        setupBottomNavigation()
        fetchCurrentUserInformation()
        //fetchLikedPosts()

        binding.signOutBtn.setOnClickListener() {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.bmiImageView.setOnClickListener {
            bmiClick(it)
        }
    }

    override fun onResume() {
        super.onResume()
       // fetchLikedPosts()  // Refresh liked posts when returning to the activity
    }

    private fun setupBottomNavigation() {
        binding.bottomMenu.setItemSelected(R.id.bottom_profile)
        binding.bottomMenu.setOnItemSelectedListener {
            when (it) {
                R.id.bottom_upload -> startActivity(Intent(this, PostUploadActivity::class.java))
                R.id.bottom_search -> startActivity(Intent(this, SearchActivity::class.java))
                R.id.bottom_home -> startActivity(Intent(this, MedicineFeedActivity::class.java))
                R.id.bottom_Pharmacies -> startActivity(Intent(this, PharmaciesActivity::class.java))
            }
        }
    }

    private fun fetchCurrentUserInformation() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            firestoreDb.collection("Users").document(user.uid)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val username = documentSnapshot.getString("username") ?: "N/A"
                        val age = documentSnapshot.getLong("age")?.toInt() ?: "N/A"
                        val heightCm = documentSnapshot.getDouble("height") ?: 0.0
                        val weight = documentSnapshot.getDouble("weight") ?: 0.0

                        // Convert height to meters
                        val height = heightCm / 100

                        // Calculate BMI
                        val bmi = if (height != 0.0) {
                            weight / (height * height)
                        } else {
                            0.0
                        }

                        binding.usernameTextView.text = username
                        binding.userAgeTextView.text = age.toString()
                        binding.userHeightTextView.text = String.format("%.2f", heightCm)
                        binding.userWeightTextView.text = String.format("%.2f", weight)
                        binding.bmiTextView.text = String.format("BMI : %.2f", bmi) // Set the calculated BMI
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

    /*private fun fetchLikedPosts() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            firestoreDb.collection("Posts").whereArrayContains("likedBy", user.uid)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    binding.favoritesContainer.removeAllViews() // Clear previous views
                    for (document in querySnapshot.documents) {
                        val post = document.toObject(Post::class.java)
                        post?.let {
                            addPostToFavoriteSection(it)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Error fetching favorites: ${exception.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        }
    }*/

    private fun addPostToFavoriteSection(post: Post) {
        val imageView = ImageView(this)
        imageView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = 8.dpToPx()
            marginEnd = 8.dpToPx()
        }
        Picasso.get().load(post.downloadUrl).into(imageView)
        binding.favoritesContainer.addView(imageView)
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    fun bmiClick(view: View) {
        val bmiValueString = binding.bmiTextView.text.toString().split(":")[1].trim()
        val bmiValue = bmiValueString.replace(",", ".").toDouble()

        val bmiStatus = when {
            bmiValue < 18.5 -> "Underweight"
            bmiValue < 24.9 -> "Normal weight"
            bmiValue < 29.9 -> "Overweight"
            else -> "Obesity"
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("BMI Information")
            .setMessage("Your BMI is $bmiValue, which is considered $bmiStatus.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }
}

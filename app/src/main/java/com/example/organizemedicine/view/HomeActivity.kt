package com.example.organizemedicine.view

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.organizemedicine.R
import com.example.organizemedicine.adapter.CommentAdapter
import com.example.organizemedicine.adapter.HomeRecyclerAdapter
import com.example.organizemedicine.adapter.OnShareButtonClickListener
import com.example.organizemedicine.databinding.ActivityHomeBinding
import com.example.organizemedicine.model.Comment
import com.example.organizemedicine.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class HomeActivity : AppCompatActivity(), OnShareButtonClickListener, OnCommentButtonClickListener {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var postArrayList: ArrayList<Post>
    private lateinit var homeAdapter: HomeRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestoreDb = FirebaseFirestore.getInstance()

        postArrayList = ArrayList()
        binding.homeRecyclerView.layoutManager = LinearLayoutManager(this)
        homeAdapter = HomeRecyclerAdapter(postArrayList, this, this)
        binding.homeRecyclerView.adapter = homeAdapter

        // Method to fetch posts

        getData()

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

    override fun onCommentButtonClick(view: View) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.comment_dailog_layout)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCancelable(true)

        val recyclerViewComments = dialog.findViewById<RecyclerView>(R.id.recyclerViewComments)
        recyclerViewComments.layoutManager = LinearLayoutManager(this)

        // Retrieve comments associated with the post
        val postId = view.tag.toString()  // Make sure to set the post ID as the tag of the button/view
        firestoreDb.collection("Posts").document(postId)
            .get()
            .addOnSuccessListener { document ->
                val commentsList = ArrayList<Comment>()
                val comments = document.get("comments")
                if (comments != null) {
                    val commentsData = comments as List<Map<String, Any>>
                    for (comment in commentsData) {
                        val content = comment["content"] as String
                        val username = comment["username"] as String
                        commentsList.add(Comment(username, content))
                    }
                }
                recyclerViewComments.adapter = CommentAdapter(commentsList)
            }

        val btnAddComment = dialog.findViewById<Button>(R.id.btnAddComment)
        val editTextComment = dialog.findViewById<EditText>(R.id.editTextComment)

        btnAddComment.setOnClickListener {
            val comment = editTextComment.text.toString()
            if (comment.isNotBlank()) {
                // Retrieve the username from Firestore
                firestoreDb.collection("Users").document(auth.currentUser?.uid!!)
                    .get()
                    .addOnSuccessListener { document ->
                        val username = document.getString("username")
                        if (username != null) {
                            // Add the comment to Firestore
                            val newComment = hashMapOf(
                                "username" to username,
                                "content" to comment
                            )
                            firestoreDb.collection("Posts").document(postId)
                                .update("comments", FieldValue.arrayUnion(newComment))
                            dialog.dismiss()
                        } else {
                            // Handle the case where the username is null
                            Toast.makeText(this, "Failed to retrieve username", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        // Handle the error
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
        dialog.show()
    }


    override fun onShareButtonClick(view: View) {
        sharePost(view)
    }

    private fun getData(){
        val currentUserId = auth.currentUser?.uid
        firestoreDb.collection("Posts").orderBy("date", Query.Direction.DESCENDING).addSnapshotListener { value, error ->
            if (error != null) {
                Toast.makeText(this, error.localizedMessage, Toast.LENGTH_LONG).show()
            } else {
                if (value != null) {
                    if (!value.isEmpty) {
                        val documents = value.documents
                        postArrayList.clear()
                        for (document in documents) {
                            val likedBy = document.get("likedBy") as? List<String> ?: listOf()
                            if (likedBy.contains(currentUserId)) {
                                val comment = document.getString("comment") ?: ""
                                val userEmail = document.getString("userEmail") ?: ""
                                val downloadUrl = document.getString("downloadUrl") ?: ""
                                val score = document.getDouble("score")?.toFloat() ?: 0.0f
                                val isLiked = likedBy.contains(currentUserId)
                                val likeCount = likedBy.size

                                val postId = document.id
                                val post = Post(postId, userEmail, comment, downloadUrl, score, isLiked, likeCount, likedBy)
                                postArrayList.add(post)
                            }
                        }
                        homeAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    fun sharePost(view: View) {
        // Ensure the view has been laid out
        view.post {
            // Create a bitmap from the view
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)

            // Save the bitmap to a file
            try {
                val file = File(externalCacheDir, "shared_image.png")
                val fileOutputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                fileOutputStream.flush()
                fileOutputStream.close()

                val fileUri = FileProvider.getUriForFile(this@HomeActivity, "$packageName.provider", file)

                // Create the share intent
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    type = "image/*"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Share image via"))
            } catch (e: IOException) {
                Toast.makeText(this, "Failed to share the image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

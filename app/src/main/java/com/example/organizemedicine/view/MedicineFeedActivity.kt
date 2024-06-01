package com.example.organizemedicine.view

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.organizemedicine.R
import com.example.organizemedicine.adapter.CommentAdapter
import com.example.organizemedicine.adapter.FeedRecyclerAdapter
import com.example.organizemedicine.adapter.OnShareButtonClickListener
import com.example.organizemedicine.databinding.ActivityMedicineFeedBinding
import com.example.organizemedicine.model.Comment
import com.example.organizemedicine.model.Post
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

interface OnCommentButtonClickListener {
    fun onCommentButtonClick(view: View)
}


class MedicineFeedActivity : AppCompatActivity(), OnShareButtonClickListener ,OnCommentButtonClickListener {

    private lateinit var binding: ActivityMedicineFeedBinding
    private lateinit var auth : FirebaseAuth
    private lateinit var db : FirebaseFirestore
    private lateinit var postArrayList : ArrayList<Post>
    private lateinit var feedAdapter: FeedRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedicineFeedBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        auth = Firebase.auth
        db = Firebase.firestore
        postArrayList = ArrayList<Post>()
        getData()
        binding.feedRecyclerView.layoutManager = LinearLayoutManager(this)
        feedAdapter = FeedRecyclerAdapter(postArrayList, this ,this)
        binding.feedRecyclerView.adapter = feedAdapter

        binding.apply {
            bottomMenu.setItemSelected(R.id.bottom_home)
            bottomMenu.setOnItemSelectedListener {
                if (it == R.id.bottom_upload){
                    startActivity(Intent(this@MedicineFeedActivity,PostUploadActivity::class.java))
                }
                else if (it == R.id.bottom_search){
                    startActivity(Intent(this@MedicineFeedActivity,SearchActivity::class.java))
                }
                else if (it == R.id.bottom_profile){
                    startActivity(Intent(this@MedicineFeedActivity,HomeActivity::class.java))
                }
                else if (it == R.id.bottom_Pharmacies){
                    startActivity(Intent(this@MedicineFeedActivity,PharmaciesActivity::class.java))
                }
            }
        }

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
        db.collection("Posts").document(postId)
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
                db.collection("Users").document(auth.currentUser?.uid!!)
                    .get()
                    .addOnSuccessListener { document ->
                        val username = document.getString("username")
                        if (username != null) {
                            // Add the comment to Firestore
                            val newComment = hashMapOf(
                                "username" to username,
                                "content" to comment
                            )
                            db.collection("Posts").document(postId)
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
        db.collection("Posts").orderBy("date", Query.Direction.DESCENDING).addSnapshotListener { value, error ->
            if (error != null) {
                Toast.makeText(this, error.localizedMessage, Toast.LENGTH_LONG).show()
            } else {
                if (value != null) {
                    if (!value.isEmpty) {
                        val documents = value.documents
                        postArrayList.clear()
                        for (document in documents) {
                            val comment = document.getString("comment") ?: ""
                            val userEmail = document.getString("userEmail") ?: ""
                            val downloadUrl = document.getString("downloadUrl") ?: ""
                            val score = document.getDouble("score")?.toFloat() ?: 0.0f  // Assuming score is stored as a Double in Firestore
                            val likedBy = document.get("likedBy") as? List<String> ?: listOf()
                            val isLiked = likedBy.contains(auth.currentUser?.uid)
                            val likeCount = likedBy.size
                            val medicineName = document.getString("medicineName") ?: "" // Retrieve the medicineName from Firestore

                            val postId = document.id // Get the document ID and use it as the postId
                            val post = Post(postId, userEmail, comment, downloadUrl, score, isLiked, likeCount, likedBy, medicineName) // Add medicineName here
                            postArrayList.add(post)
                        }
                        feedAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }



    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.medicine_menu,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_post -> {
                val intent = Intent(this, PostUploadActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.signout -> {
                auth.signOut()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
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

                val fileUri = FileProvider.getUriForFile(this@MedicineFeedActivity, "$packageName.provider", file)

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


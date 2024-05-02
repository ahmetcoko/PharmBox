package com.example.organizemedicine.view

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.organizemedicine.R
import com.example.organizemedicine.adapter.FeedRecyclerAdapter
import com.example.organizemedicine.databinding.ActivityMedicineFeedBinding
import com.example.organizemedicine.model.Post
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore

class MedicineFeedActivity : AppCompatActivity() {

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
        feedAdapter = FeedRecyclerAdapter(postArrayList)
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
            }
        }

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

                            val post = Post(userEmail, comment, downloadUrl, score)
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
                startActivity(intent)
                finish()
                return true
            }
            R.id.findPharmacies -> {  // New case for Find Pharmacies
                val intent = Intent(this, PharmaciesActivity::class.java)
                startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }



}
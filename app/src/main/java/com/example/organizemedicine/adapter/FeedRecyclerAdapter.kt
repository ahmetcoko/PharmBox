package com.example.organizemedicine.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.organizemedicine.R
import com.example.organizemedicine.databinding.RecyclerRowBinding
import com.example.organizemedicine.model.Post
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.squareup.picasso.Picasso


interface OnShareButtonClickListener {
    fun onShareButtonClick(view: View)
}

class FeedRecyclerAdapter(private val postList: ArrayList<Post>, private val listener: OnShareButtonClickListener) : RecyclerView.Adapter<FeedRecyclerAdapter.PostHolder>() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostHolder {
        val binding = RecyclerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostHolder(binding, this)
    }

    override fun onBindViewHolder(holder: PostHolder, position: Int) {
        val post = postList[position]
        holder.bind(post, listener)
    }

    override fun getItemCount(): Int {
        return postList.size
    }

    class PostHolder(private val binding: RecyclerRowBinding, private val adapter: FeedRecyclerAdapter) : RecyclerView.ViewHolder(binding.root) {
        fun bind(post: Post, listener: OnShareButtonClickListener) {
            binding.recyclerEmailText.text = post.userEmail
            binding.recyclerCommentText.text = post.comment
            binding.likeImageView.setImageResource(if (post.isLiked) R.drawable.liked else R.drawable.unliked)
            binding.likeNum.text = post.likeCount.toString()

            setupShareButton(post, listener)
        }

        private fun setupShareButton(post: Post, listener: OnShareButtonClickListener) {
            Picasso.get().load(post.downloadUrl).into(binding.recyclerImageView, object : com.squareup.picasso.Callback {
                override fun onSuccess() {
                    binding.shareImageView.setOnClickListener {
                        listener.onShareButtonClick(binding.root) // Pass the root view of the item
                    }
                    binding.likeImageView.setOnClickListener {
                        likePost(post)
                    }
                }

                override fun onError(e: Exception?) {
                    // Optionally handle errors, e.g., show a placeholder image or a message
                }
            })
        }

        private fun likePost(post: Post) {
            val postRef = adapter.db.collection("Posts").document(post.postId)
            adapter.db.runTransaction { transaction ->
                val snapshot = transaction.get(postRef)
                var likedBy = snapshot.get("likedBy") as List<String>? ?: listOf()

                if (likedBy.contains(adapter.auth.currentUser?.uid)) {
                    likedBy = likedBy - adapter.auth.currentUser!!.uid!!

                } else {
                    likedBy = likedBy + adapter.auth.currentUser!!.uid!!
                }

                transaction.update(postRef, "likedBy", likedBy)
                // Update the post object
                post.isLiked = likedBy.contains(adapter.auth.currentUser?.uid)
                post.likeCount = likedBy.size

                // No need to return anything
                null
            }.addOnSuccessListener {
                // Update the UI
                binding.likeImageView.setImageResource(if (post.isLiked) R.drawable.liked else R.drawable.unliked)
                binding.likeNum.text = post.likeCount.toString()
            }.addOnFailureListener { e ->
                Log.e("FeedRecyclerAdapter", "Error updating likes", e)
            }
        }
    }
}

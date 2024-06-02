package com.example.organizemedicine.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.organizemedicine.R
import com.example.organizemedicine.databinding.MedicineRowBinding
import com.example.organizemedicine.model.Post
import com.example.organizemedicine.view.MedicineInfoActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.squareup.picasso.Picasso


interface OnCommentButtonClickListener {
    fun onCommentButtonClick(view: View)
}


class MedicineAdapter(private val postList: ArrayList<Post>, private val listener: OnShareButtonClickListener, private val commentListener: MedicineInfoActivity) : RecyclerView.Adapter<MedicineAdapter.PostHolder>() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostHolder {
        val binding = MedicineRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostHolder(binding, this)
    }

    override fun onBindViewHolder(holder: PostHolder, position: Int) {
        val post = postList[position]
        holder.bind(post, listener,commentListener)
    }

    override fun getItemCount(): Int {
        return postList.size
    }

    class PostHolder(private val binding: MedicineRowBinding, private val adapter: MedicineAdapter) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            post: Post, listener: OnShareButtonClickListener,
            commentListener: MedicineInfoActivity
        ) {
            binding.recyclerEmailText.text ="@" + post.username
            binding.recyclerCommentText.text = post.comment
            binding.likeImageView.setImageResource(if (post.isLiked) R.drawable.liked else R.drawable.unliked)
            binding.likeNum.text = post.likeCount.toString()
            binding.commentNum.text = post.commentsCount.toString()
            binding.fullnameTextView.text = post.fullname

            binding.likeImageView.setOnClickListener {
                likePost(post)
            }

            binding.commentImageView.setOnClickListener{
                it.tag = post.postId
                commentListener.onCommentButtonClick(it)
            }
            setupShareButton(post, listener)
        }
        init {
            binding.dots.visibility = View.GONE
        }

        private fun setupShareButton(post: Post, listener: OnShareButtonClickListener) {
            if (post.downloadUrl.isNullOrEmpty()) {
                binding.recyclerImageView.visibility = View.GONE
            } else {
                Picasso.get().load(post.downloadUrl).into(binding.recyclerImageView, object : com.squareup.picasso.Callback {
                    override fun onSuccess() {
                        binding.shareImageView.setOnClickListener {
                            listener.onShareButtonClick(binding.root)
                        }
                        binding.likeImageView.setOnClickListener {
                            likePost(post)
                        }
                    }
                    override fun onError(e: Exception?) {
                        binding.recyclerImageView.setImageResource(R.drawable.no_image)
                        Toast.makeText(binding.root.context, "Failed to load image", Toast.LENGTH_SHORT).show()
                    }
                })
            }
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
                post.isLiked = likedBy.contains(adapter.auth.currentUser?.uid)
                post.likeCount = likedBy.size

                null
            }.addOnSuccessListener {
                binding.likeImageView.setImageResource(if (post.isLiked) R.drawable.liked else R.drawable.unliked)
                binding.likeNum.text = post.likeCount.toString()
            }.addOnFailureListener { e ->
                Log.e("MedicineAdapter", "Error updating likes", e)
            }
        }
    }
}
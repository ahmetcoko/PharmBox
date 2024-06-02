package com.example.organizemedicine.adapter

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.organizemedicine.R
import com.example.organizemedicine.databinding.RecyclerRowBinding
import com.example.organizemedicine.model.Post
import com.example.organizemedicine.view.HomeActivity
import com.example.organizemedicine.view.MedicineInfoActivity
import com.example.organizemedicine.view.OnCommentButtonClickListener
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.squareup.picasso.Picasso

interface OnHomeShareButtonClickListener {
    fun onShareButtonClick(view: View)
}

interface OnHomeCommentButtonClickListener {
    fun onCommentButtonClick(view: View)
}

class HomeRecyclerAdapter(
    private val postList: ArrayList<Post>,
    private val shareListener: HomeActivity,
    private val commentListener: HomeActivity
) : RecyclerView.Adapter<HomeRecyclerAdapter.PostHolder>() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostHolder {
        val binding = RecyclerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostHolder(binding, this ,postList, db)
    }

    override fun onBindViewHolder(holder: PostHolder, position: Int) {
        val post = postList[position]
        holder.bind(post, shareListener, commentListener)
    }

    override fun getItemCount(): Int {
        return postList.size
    }

    class PostHolder(private val binding: RecyclerRowBinding, private val adapter: HomeRecyclerAdapter,private val postArrayList: ArrayList<Post>,private val firestoreDb: FirebaseFirestore = Firebase.firestore) : RecyclerView.ViewHolder(binding.root) {
        private val auth = Firebase.auth
        fun bind(post: Post, listener: OnShareButtonClickListener, commentListener: OnCommentButtonClickListener) {
            binding.recyclerEmailText.text = "@" + post.username
            binding.recyclerCommentText.text = post.comment
            // Check if the current user has liked the post
            post.isLiked = post.likedBy.contains(auth.currentUser?.uid)
            binding.likeImageView.setImageResource(if (post.isLiked) R.drawable.liked else R.drawable.unliked)
            binding.likeNum.text = post.likeCount.toString()
            binding.commentNum.text = post.commentsCount.toString()
            binding.fullnameTextView.text = post.fullname

            binding.commentImageView.tag = post.postId

            // Set up image every time bind is called
            if (post.downloadUrl.isNullOrEmpty()) {
                binding.recyclerImageView.visibility = View.GONE
            } else {
                binding.recyclerImageView.visibility = View.VISIBLE
                Picasso.get().load(post.downloadUrl).into(binding.recyclerImageView, object : com.squareup.picasso.Callback {
                    override fun onSuccess() {
                        // Image loaded successfully
                    }

                    override fun onError(e: Exception?) {
                        binding.recyclerImageView.setImageResource(R.drawable.no_image)
                        Toast.makeText(binding.root.context, "Failed to load image", Toast.LENGTH_SHORT).show()
                    }
                })
            }

            binding.likeImageView.setOnClickListener {
                likePost(post)
            }

            binding.commentImageView.setOnClickListener {
                commentListener.onCommentButtonClick(it)
            }

            binding.shareImageView.setOnClickListener {
                listener.onShareButtonClick(binding.root)
            }
        }

        init {
            binding.dots.setOnClickListener { view ->
                showPopupMenu(view)
            }
        }


        private fun showPopupMenu(view: View) {
            val popupMenu = PopupMenu(view.context, view)
            popupMenu.menuInflater.inflate(R.menu.popup_menu, popupMenu.menu)

            popupMenu.setOnMenuItemClickListener { menuItem ->
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) {
                    return@setOnMenuItemClickListener false
                }

                when (menuItem.itemId) {
                    R.id.go_to_medicine_details -> {
                        val post = postArrayList[position]
                        val medicineName = post.medicineName
                        if (medicineName != null) {
                            val intent = Intent(view.context, MedicineInfoActivity::class.java)
                            intent.putExtra("medicine_name", medicineName)
                            view.context.startActivity(intent)
                        }
                        true
                    }
                    R.id.delete_post -> {
                        val post = postArrayList[position]
                        deletePost(post.postId, post.userEmail)
                        true
                    }
                    else -> false
                }
            }

            popupMenu.show()
        }
        private fun deletePost(postId: String, userEmail: String) {
            if (userEmail == auth.currentUser?.email) {
                firestoreDb.collection("Posts").document(postId)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(itemView.context, "Post deleted successfully", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(itemView.context, "You can't delete this post", Toast.LENGTH_SHORT).show()
            }
        }


        private fun setupShareButton(post: Post, shareListener: HomeActivity) {
            if (post.downloadUrl.isNullOrEmpty()) {
                binding.recyclerImageView.visibility = View.GONE
            } else {
                Picasso.get().load(post.downloadUrl).into(binding.recyclerImageView, object : com.squareup.picasso.Callback {
                    override fun onSuccess() {
                        binding.shareImageView.setOnClickListener {
                            shareListener.onShareButtonClick(binding.root)
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

                // Find the post in the postList and update it
                val index = postArrayList.indexOfFirst { it.postId == post.postId }
                if (index != -1) {
                    postArrayList[index].isLiked = likedBy.contains(adapter.auth.currentUser?.uid)
                    postArrayList[index].likeCount = likedBy.size
                }

                null
            }.addOnSuccessListener {
                // Notify the adapter that the item has changed
                val index = postArrayList.indexOfFirst { it.postId == post.postId }
                if (index != -1) {
                    adapter.notifyItemChanged(index)
                }
            }.addOnFailureListener { e ->
                Log.e("FeedRecyclerAdapter", "Error updating likes", e)
            }
        }

    }
}

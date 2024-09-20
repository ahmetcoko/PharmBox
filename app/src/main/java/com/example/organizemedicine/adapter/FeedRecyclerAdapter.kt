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
import com.example.organizemedicine.view.MedicineInfoActivity
import com.example.organizemedicine.view.OnCommentButtonClickListener
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.squareup.picasso.Picasso


interface OnShareButtonClickListener {
    fun onShareButtonClick(view: View)
}


class FeedRecyclerAdapter(private val postList: ArrayList<Post>, private val listener: OnShareButtonClickListener,private val commentListener: OnCommentButtonClickListener) : RecyclerView.Adapter<FeedRecyclerAdapter.PostHolder>() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostHolder {
        val binding = RecyclerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostHolder(binding, this ,db, postList)
    }

    override fun onBindViewHolder(holder: PostHolder, position: Int) {
        val post = postList[position]
        holder.bind(post, listener, commentListener)



    }

    override fun getItemCount(): Int {
        return postList.size
    }

    class PostHolder(private val binding: RecyclerRowBinding, private val adapter: FeedRecyclerAdapter,private val firestoreDb: FirebaseFirestore = Firebase.firestore, private val postArrayList: ArrayList<Post>) : RecyclerView.ViewHolder(binding.root) {
        private val auth = Firebase.auth
        fun bind(post: Post, listener: OnShareButtonClickListener, commentListener: OnCommentButtonClickListener) {
            binding.recyclerEmailText.text = "@" + post.username
            binding.recyclerCommentText.text = post.comment
            binding.likeImageView.setImageResource(if (post.isLiked) R.drawable.liked else R.drawable.unliked)
            binding.likeNum.text = post.likeCount.toString()
            binding.commentNum.text = post.commentsCount.toString()
            binding.fullnameTextView.text = post.fullname

            binding.commentImageView.tag = post.postId

            
            if (post.downloadUrl.isNullOrEmpty()) {
                binding.recyclerImageView.visibility = View.GONE
            } else {
                binding.recyclerImageView.visibility = View.VISIBLE
                Picasso.get().load(post.downloadUrl).into(binding.recyclerImageView, object : com.squareup.picasso.Callback {
                    override fun onSuccess() {
                       
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

                
                val index = postArrayList.indexOfFirst { it.postId == post.postId }
                if (index != -1) {
                    postArrayList[index].isLiked = likedBy.contains(adapter.auth.currentUser?.uid)
                    postArrayList[index].likeCount = likedBy.size
                }

                null
            }.addOnSuccessListener {
                
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

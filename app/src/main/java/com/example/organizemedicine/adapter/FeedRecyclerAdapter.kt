package com.example.organizemedicine.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.organizemedicine.databinding.RecyclerRowBinding
import com.example.organizemedicine.model.Post
import com.squareup.picasso.Picasso

class FeedRecyclerAdapter(private val postList: ArrayList<Post>) : RecyclerView.Adapter<FeedRecyclerAdapter.PostHolder>() {

    class PostHolder(val binding: RecyclerRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostHolder {
        val binding = RecyclerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostHolder(binding)
    }

    override fun getItemCount(): Int {
        return postList.size
    }

    override fun onBindViewHolder(holder: PostHolder, position: Int) {
        val post = postList[position]
        holder.binding.recyclerEmailText.text = post.email
        holder.binding.recyclerCommentText.text = post.comment
        /*holder.binding.recyclerScoreText.text = "Score: ${post.score}" */ // Display the score here
        Picasso.get().load(post.downloadUrl).into(holder.binding.recyclerImageView)
    }
}

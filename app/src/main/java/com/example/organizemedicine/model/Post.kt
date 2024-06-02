package com.example.organizemedicine.model

data class Post(
    var postId: String,
    var userEmail: String,
    var comment: String,
    var downloadUrl: String,
    var score: Float,
    var isLiked: Boolean,
    var likeCount: Int,
    var likedBy: List<String>,
    val medicineName: String = "",
    var commentsCount: Int = 0, // Add commentsCount property
    var username: String = "", // Add username property
    var fullname: String = "" // Add fullname property
) {
    constructor() : this("", "", "", "", 0.0f, false, 0, listOf(), "", 0, "", "")
}

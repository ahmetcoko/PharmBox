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
    val medicineName: String = ""
) {

    constructor() : this("", "", "", "", 0.0f, false, 0, listOf())
}
